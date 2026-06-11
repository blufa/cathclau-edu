package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.StudentDocument;
import java.util.List;

public interface StudentDocumentRepository {
    StudentDocument save(StudentDocument document);
    List<StudentDocument> findByStudentId(Long studentId);
    void deleteDocumentById(Long id);
}
