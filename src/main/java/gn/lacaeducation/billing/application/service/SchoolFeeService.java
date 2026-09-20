package gn.lacaeducation.billing.application.service;
import gn.lacaeducation.billing.application.port.in.SchoolFeeUseCase;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class SchoolFeeService implements SchoolFeeUseCase {
    private final SchoolFeeRepository repo;
    private final InstallmentRepository installmentRepo;
    private final InvoiceRepository invoiceRepo;
    @Override public SchoolFee create(CreateFeeCommand c) {
        return repo.save(SchoolFee.builder().tenantId(c.tenantId()).classId(c.classId()).routeId(c.routeId())
                .type(c.type()).amount(c.amount()).academicYear(c.academicYear()).label(c.label()).build());
    }
    @Override public SchoolFee update(Long id, UpdateFeeCommand c) {
        SchoolFee f = load(id); f.setClassId(c.classId()); f.setRouteId(c.routeId()); f.setType(c.type()); f.setAmount(c.amount());
        f.setAcademicYear(c.academicYear()); f.setLabel(c.label());
        return repo.save(f);
    }
    @Override
    @Transactional(readOnly=true)
    public Optional<SchoolFee> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly=true)
    public List<SchoolFee> findByTenantId(Long t) {
        return repo.findByTenantId(t);
    }

    @Override
    @Transactional(readOnly=true)
    public List<SchoolFee> findByClassId(Long c) {
        return repo.findByClassId(c);
    }

    @Override
    @Transactional(readOnly=true)
    public List<SchoolFee> findByRouteId(Long r) {
        return repo.findByRouteId(r);
    }

    @Override
    @Transactional(readOnly=true)
    public List<SchoolFee> findByTenantIdAndAcademicYear(Long t, String y) {
        return repo.findByTenantIdAndAcademicYear(t, y);
    }

    @Override
    public void delete(Long id) {
        load(id); // 404 si le frais n'existe pas
        // Un frais déjà facturé à des élèves ne doit jamais être supprimé (données financières réelles) —
        // message métier clair plutôt que de laisser remonter la violation de contrainte SQL brute.
        if (!invoiceRepo.findByFeeId(id).isEmpty()) {
            throw new BusinessRuleException(
                    "Impossible de supprimer ce frais : des factures ont déjà été émises pour ce frais. "
                            + "Modifiez son montant/libellé si besoin, ou contactez le support pour un cas particulier.");
        }
        // Aucune facture : l'échéancier (installments) n'est qu'une définition, pas des données
        // financières réelles — supprimable sans risque avec le frais.
        for (Installment i : installmentRepo.findByFeeId(id)) {
            installmentRepo.deleteById(i.getId());
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly=true)
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }
    private SchoolFee load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SchoolFee", id));
    }
}
