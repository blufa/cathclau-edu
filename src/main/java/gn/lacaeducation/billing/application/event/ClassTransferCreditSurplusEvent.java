package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/** Publié quand un transfert de classe laisse un reliquat de crédit non affectable (le parent avait
 * déjà réglé plus que ce que coûtent les mensualités de la nouvelle classe) — ce surplus n'est reporté
 * sur aucune facture, il doit être remboursé au parent. Consommé par {@code communication} pour
 * alerter l'école (§retour comptable : rien ne signalait ce cas avant, le reliquat n'était que logué). */
public record ClassTransferCreditSurplusEvent(Long tenantId, Long studentId, Long oldClassId, Long newClassId,
                                               BigDecimal surplusAmount) {}
