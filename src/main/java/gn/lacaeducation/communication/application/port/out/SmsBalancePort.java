package gn.lacaeducation.communication.application.port.out;

import java.util.List;

/**
 * Consultation du solde SMS du compte fournisseur (Orange : {@code GET /sms/admin/v1/contracts}).
 * Le solde est global au compte plateforme (un élément par pays souscrit).
 */
public interface SmsBalancePort {

    /** Un contrat/solde SMS par pays. Liste vide si le fournisseur n'est pas configuré. */
    List<SmsBalance> fetchBalances();

    record SmsBalance(
            String country,
            String offerName,
            Integer availableUnits,
            String status,
            String expirationDate) {}
}
