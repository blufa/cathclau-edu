package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface StudentCreditJpaRepository extends JpaRepository<StudentCreditJpaEntity, Long> {
    List<StudentCreditJpaEntity> findByStudentIdOrderByCreatedAtAsc(Long studentId);

    /** Verrou pessimiste (§4 : "deux encaissements simultanés ne consomment pas deux fois le même
     * crédit") — pris pour la durée de la transaction d'application, le temps de lire remainingAmount
     * et de le décrémenter sans qu'une autre transaction concurrente ne lise la même valeur avant le
     * commit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM StudentCreditJpaEntity c WHERE c.id = :id")
    Optional<StudentCreditJpaEntity> lockById(Long id);
}
