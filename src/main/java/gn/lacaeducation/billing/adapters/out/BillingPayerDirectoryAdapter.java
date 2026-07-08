package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.payments.PayerDirectory;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fournit le nom de l'élève payeur au module {@code payments} (pour l'impression du reçu). Vit dans
 * {@code billing}, qui connaît déjà élèves et paiements : implémente le port {@link PayerDirectory}
 * possédé par payments, évitant une dépendance directe payments → students (cycle).
 */
@Component
@RequiredArgsConstructor
public class BillingPayerDirectoryAdapter implements PayerDirectory {

    private final StudentDataProvider studentDataProvider;

    @Override
    public Optional<String> studentName(Long studentId) {
        if (studentId == null) return Optional.empty();
        return studentDataProvider.findStudent(studentId)
                .map(s -> ((s.firstName() == null ? "" : s.firstName()) + " "
                        + (s.lastName() == null ? "" : s.lastName())).trim())
                .filter(n -> !n.isBlank());
    }
}
