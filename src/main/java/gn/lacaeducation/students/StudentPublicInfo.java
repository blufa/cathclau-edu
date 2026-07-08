package gn.lacaeducation.students;

import java.time.LocalDate;

public record StudentPublicInfo(Long id, Long tenantId, String firstName, String lastName, String email, String phone,
                                String studentNumber, LocalDate birthDate, String birthPlace, String gender) {}
