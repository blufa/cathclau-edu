package gn.lacaeducation.students.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentDocument {
    private Long id;
    private Long studentId;
    private DocumentType documentType;
    private String fileUrl;
    private LocalDateTime addedAt;
}
