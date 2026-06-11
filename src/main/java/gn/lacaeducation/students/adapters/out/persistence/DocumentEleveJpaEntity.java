package gn.lacaeducation.students.adapters.out.persistence;

import gn.lacaeducation.students.domain.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "student_documents")
@Getter @Setter @NoArgsConstructor
class DocumentEleveJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Enumerated(EnumType.STRING) @Column(name = "document_type") private DocumentType documentType;
    @Column(name = "file_url") private String fileUrl;
    @Column(name = "added_at") private LocalDateTime addedAt;
}
