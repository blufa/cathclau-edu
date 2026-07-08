package gn.lacaeducation.biometric.adapters.out.persistence.mapper;

import gn.lacaeducation.biometric.adapters.out.persistence.BiometricDeviceJpaEntity;
import gn.lacaeducation.biometric.adapters.out.persistence.BiometricEnrollmentJpaEntity;
import gn.lacaeducation.biometric.adapters.out.persistence.BiometricPunchJpaEntity;
import gn.lacaeducation.biometric.domain.BiometricDevice;
import gn.lacaeducation.biometric.domain.BiometricEnrollment;
import gn.lacaeducation.biometric.domain.BiometricPunch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BiometricMapper {
    BiometricDevice toDomain(BiometricDeviceJpaEntity e);
    BiometricDeviceJpaEntity toEntity(BiometricDevice d);

    BiometricEnrollment toDomain(BiometricEnrollmentJpaEntity e);
    BiometricEnrollmentJpaEntity toEntity(BiometricEnrollment d);

    BiometricPunch toDomain(BiometricPunchJpaEntity e);
    BiometricPunchJpaEntity toEntity(BiometricPunch d);
}
