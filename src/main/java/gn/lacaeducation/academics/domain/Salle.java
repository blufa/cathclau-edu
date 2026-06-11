package gn.lacaeducation.academics.domain;

import gn.lacaeducation.academics.domain.valueobject.TypeSalle;
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
public class Salle {
    private Long id;
    private Long tenantId;
    private String label;
    private String building;
    private Integer capacity;
    private TypeSalle type;
    private Boolean available;
}
