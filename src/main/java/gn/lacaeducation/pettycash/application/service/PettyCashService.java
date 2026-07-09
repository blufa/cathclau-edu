package gn.lacaeducation.pettycash.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.pettycash.application.port.out.PettyCashFundRepository;
import gn.lacaeducation.pettycash.application.port.out.PettyCashTransactionRepository;
import gn.lacaeducation.pettycash.domain.PettyCashFund;
import gn.lacaeducation.pettycash.domain.PettyCashTransaction;
import gn.lacaeducation.pettycash.domain.PettyCashTxType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Petite caisse (Manuel §7.1) : caisse à fonds fixe, bons de sortie et réapprovisionnements. Le solde
 * courant est recalculé à chaque opération ; le réapprovisionnement ramène vers le fonds fixe.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PettyCashService {

    private final PettyCashFundRepository fundRepo;
    private final PettyCashTransactionRepository txRepo;

    public record FundCommand(Long tenantId, String label, BigDecimal floatAmount, String custodian, Boolean active) {}
    public record TxCommand(LocalDate txDate, PettyCashTxType txType, BigDecimal amount, String voucherNumber,
                            String description, String category, String recipient, String justificationUrl, Long recordedBy) {}

    public PettyCashFund createFund(FundCommand c) {
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le libellé de la caisse est requis");
        BigDecimal floatAmt = nz(c.floatAmount());
        return fundRepo.save(PettyCashFund.builder()
                .tenantId(c.tenantId()).label(c.label()).floatAmount(floatAmt).currentBalance(floatAmt)
                .custodian(c.custodian()).active(c.active() == null || c.active()).createdAt(LocalDateTime.now()).build());
    }

    public PettyCashFund updateFund(Long id, FundCommand c) {
        PettyCashFund f = loadFund(id);
        f.setLabel(c.label()); f.setFloatAmount(nz(c.floatAmount())); f.setCustodian(c.custodian());
        f.setActive(c.active() == null || c.active());
        return fundRepo.save(f);
    }

    public void deleteFund(Long id) {
        txRepo.deleteByFundId(id);
        fundRepo.deleteById(id);
    }

    public PettyCashTransaction record(Long fundId, TxCommand c) {
        PettyCashFund f = loadFund(fundId);
        if (c.amount() == null || c.amount().signum() <= 0) throw new BusinessRuleException("Le montant doit être positif");
        PettyCashTxType type = c.txType() != null ? c.txType() : PettyCashTxType.DISBURSEMENT;
        if (type == PettyCashTxType.DISBURSEMENT && c.amount().compareTo(nz(f.getCurrentBalance())) > 0) {
            throw new BusinessRuleException("Solde de petite caisse insuffisant (" + nz(f.getCurrentBalance()) + ")");
        }
        PettyCashTransaction tx = txRepo.save(PettyCashTransaction.builder()
                .tenantId(f.getTenantId()).fundId(fundId)
                .txDate(c.txDate() != null ? c.txDate() : LocalDate.now())
                .txType(type).amount(c.amount()).voucherNumber(c.voucherNumber()).description(c.description())
                .category(c.category()).recipient(c.recipient()).justificationUrl(c.justificationUrl())
                .recordedBy(c.recordedBy()).createdAt(LocalDateTime.now()).build());
        BigDecimal balance = nz(f.getCurrentBalance());
        f.setCurrentBalance(type == PettyCashTxType.DISBURSEMENT ? balance.subtract(c.amount()) : balance.add(c.amount()));
        fundRepo.save(f);
        return tx;
    }

    public void deleteTransaction(Long txId) {
        PettyCashTransaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("PettyCashTransaction", txId));
        txRepo.deleteById(txId);
        PettyCashFund f = loadFund(tx.getFundId());
        BigDecimal balance = nz(f.getCurrentBalance());
        f.setCurrentBalance(tx.getTxType() == PettyCashTxType.DISBURSEMENT
                ? balance.add(tx.getAmount()) : balance.subtract(tx.getAmount()));
        fundRepo.save(f);
    }

    /** Montant suggéré pour ramener le solde au fonds fixe. */
    @Transactional(readOnly = true)
    public BigDecimal replenishmentSuggestion(Long fundId) {
        PettyCashFund f = loadFund(fundId);
        return nz(f.getFloatAmount()).subtract(nz(f.getCurrentBalance())).max(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<PettyCashFund> funds(Long tenantId) { return fundRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public PettyCashFund fund(Long id) { return loadFund(id); }

    @Transactional(readOnly = true)
    public List<PettyCashTransaction> transactions(Long fundId) { return txRepo.findByFundId(fundId); }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private PettyCashFund loadFund(Long id) { return fundRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("PettyCashFund", id)); }
}
