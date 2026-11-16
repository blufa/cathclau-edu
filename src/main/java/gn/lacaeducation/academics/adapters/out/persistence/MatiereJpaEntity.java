package gn.lacaeducation.academics.adapters.out.persistence;

import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subjects")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class MatiereJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    private BigDecimal coefficient;
    @Column(columnDefinition = "TEXT") private String syllabus;
    /** Cycle auquel appartient cette matière (nullable — matières créées avant cette fonctionnalité,
     * ou toujours pertinentes quel que soit le cycle) — §retour utilisateur : "les mêmes matières
     * s'affichent à tous les niveaux", sert à filtrer l'affichage et les affectations par niveau. */
    @Enumerated(EnumType.STRING) @Column(name = "cycle") private SchoolCycle cycle;
}
