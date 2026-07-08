package gn.lacaeducation.payments;

import java.util.Optional;

/**
 * SPI publique du module {@code payments} : résout le nom de l'élève payeur pour l'imprimer sur le
 * reçu (snapshot au moment du règlement). Implémentée par un module qui connaît les élèves
 * ({@code billing}), ce qui évite une dépendance directe payments → students (cycle).
 */
public interface PayerDirectory {
    Optional<String> studentName(Long studentId);
}
