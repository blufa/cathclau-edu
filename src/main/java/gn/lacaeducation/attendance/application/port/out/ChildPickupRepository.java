package gn.lacaeducation.attendance.application.port.out;

import gn.lacaeducation.attendance.domain.ChildPickup;

import java.time.LocalDate;
import java.util.List;

public interface ChildPickupRepository {
    ChildPickup save(ChildPickup pickup);
    List<ChildPickup> findByStudentIdAndDate(Long studentId, LocalDate date);
}
