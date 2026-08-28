package gn.lacaeducation.attendance.adapters.out.persistence;

import gn.lacaeducation.attendance.application.port.out.ChildPickupRepository;
import gn.lacaeducation.attendance.domain.ChildPickup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class ChildPickupPersistenceAdapter implements ChildPickupRepository {
    private final ChildPickupJpaRepository jpa;

    @Override public ChildPickup save(ChildPickup p) { return toDomain(jpa.save(toEntity(p))); }

    @Override public List<ChildPickup> findByStudentIdAndDate(Long studentId, LocalDate date) {
        return jpa.findByStudentIdAndPickupDate(studentId, date).stream().map(this::toDomain).toList();
    }

    private ChildPickup toDomain(ChildPickupJpaEntity e) {
        return ChildPickup.builder().id(e.getId()).tenantId(e.getTenantId()).studentId(e.getStudentId())
                .classId(e.getClassId()).pickupDate(e.getPickupDate()).pickupTime(e.getPickupTime())
                .pickerParentId(e.getPickerParentId()).pickerName(e.getPickerName())
                .pickerRelation(e.getPickerRelation()).createdAt(e.getCreatedAt()).build();
    }

    private ChildPickupJpaEntity toEntity(ChildPickup p) {
        ChildPickupJpaEntity e = new ChildPickupJpaEntity();
        e.setId(p.getId()); e.setTenantId(p.getTenantId()); e.setStudentId(p.getStudentId());
        e.setClassId(p.getClassId()); e.setPickupDate(p.getPickupDate()); e.setPickupTime(p.getPickupTime());
        e.setPickerParentId(p.getPickerParentId()); e.setPickerName(p.getPickerName());
        e.setPickerRelation(p.getPickerRelation()); e.setCreatedAt(p.getCreatedAt());
        return e;
    }
}
