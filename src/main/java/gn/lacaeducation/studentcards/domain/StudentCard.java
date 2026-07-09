package gn.lacaeducation.studentcards.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentCard {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long cardTemplateId;
    private Long academicYearId;
    private Long classId;
    private String studentNumber;
    private String cardNumber;
    private String qrPayload;
    private String qrSignature;
    private String photoUrl;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private CardStatus status;
    private String pdfUrl;
    private Long replacesCardId;
    private LocalDateTime createdAt;
}
