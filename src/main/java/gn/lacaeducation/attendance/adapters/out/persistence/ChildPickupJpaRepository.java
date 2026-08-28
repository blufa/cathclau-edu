package gn.lacaeducation.attendance.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface ChildPickupJpaRepository extends JpaRepository<ChildPickupJpaEntity, Long> {
    List<ChildPickupJpaEntity> findByStudentIdAndPickupDate(Long studentId, LocalDate date);
}
