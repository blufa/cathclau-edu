package gn.lacaeducation.academics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseMatiere {
    private Long id;
    private Long schoolClassId;
    private Long subjectId;
    private Long teacherId;
    /** Coefficient de la matière pour cette classe (null = coefficient par défaut de la matière). */
    private java.math.BigDecimal coefficient;
}
