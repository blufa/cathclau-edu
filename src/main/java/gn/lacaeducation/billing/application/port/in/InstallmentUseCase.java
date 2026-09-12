package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface InstallmentUseCase {
    record CreateInstallmentCommand(Long feeId, String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    record UpdateInstallmentCommand(String label, BigDecimal amount, LocalDate dueDate, List<String> months) {}
    /** Une période de la répartition trimestrielle pondérée (ex. « Trimestre 1 », 40 %, échéance). */
    record TermSpec(String label, BigDecimal percentage, LocalDate dueDate) {}
    /** Génère N échéances depuis un total annuel et une répartition en % (ex. 40/30/30). */
    record GenerateTermsCommand(Long feeId, BigDecimal annualTotal, List<TermSpec> terms) {}
    Installment create(CreateInstallmentCommand command);
    List<Installment> generateTerms(GenerateTermsCommand command);
    Installment update(Long id, UpdateInstallmentCommand command);
    Optional<Installment> findById(Long id);
    List<Installment> findByFeeId(Long feeId);
    void delete(Long id);
    /**
     * Déclenche immédiatement, pour le tenant courant, la vérification des échéances en retard (même
     * traitement que le job planifié quotidien de 6h — utile pour tester une relance sans attendre le
     * lendemain matin, ou si le serveur n'a pas tourné en continu depuis le dernier passage du job).
     */
    void checkOverdueNow();
}
