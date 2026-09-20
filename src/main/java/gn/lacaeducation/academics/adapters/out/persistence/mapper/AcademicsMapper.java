package gn.lacaeducation.academics.adapters.out.persistence.mapper;

import gn.lacaeducation.academics.adapters.out.persistence.AnneeScolaireJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.ClasseJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.ClasseMatiereJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.EmploiDuTempsJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.MatiereJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.NiveauJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.PeriodeJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.SalleJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.ScheduleExceptionJpaEntity;
import gn.lacaeducation.academics.adapters.out.persistence.SchoolEventJpaEntity;
import gn.lacaeducation.academics.domain.AnneeScolaire;
import gn.lacaeducation.academics.domain.Classe;
import gn.lacaeducation.academics.domain.ClasseMatiere;
import gn.lacaeducation.academics.domain.EmploiDuTemps;
import gn.lacaeducation.academics.domain.Matiere;
import gn.lacaeducation.academics.domain.Niveau;
import gn.lacaeducation.academics.domain.Periode;
import gn.lacaeducation.academics.domain.Salle;
import gn.lacaeducation.academics.domain.ScheduleException;
import gn.lacaeducation.academics.domain.SchoolEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcademicsMapper {

    AnneeScolaire toDomain(AnneeScolaireJpaEntity entity);

    AnneeScolaireJpaEntity toEntity(AnneeScolaire domain);

    Periode toDomain(PeriodeJpaEntity entity);

    PeriodeJpaEntity toEntity(Periode domain);

    Salle toDomain(SalleJpaEntity entity);

    SalleJpaEntity toEntity(Salle domain);

    Niveau toDomain(NiveauJpaEntity entity);

    NiveauJpaEntity toEntity(Niveau domain);

    Classe toDomain(ClasseJpaEntity entity);

    ClasseJpaEntity toEntity(Classe domain);

    Matiere toDomain(MatiereJpaEntity entity);

    MatiereJpaEntity toEntity(Matiere domain);

    ClasseMatiere toDomain(ClasseMatiereJpaEntity entity);

    ClasseMatiereJpaEntity toEntity(ClasseMatiere domain);

    EmploiDuTemps toDomain(EmploiDuTempsJpaEntity entity);

    EmploiDuTempsJpaEntity toEntity(EmploiDuTemps domain);

    ScheduleException toDomain(ScheduleExceptionJpaEntity entity);

    ScheduleExceptionJpaEntity toEntity(ScheduleException domain);

    SchoolEvent toDomain(SchoolEventJpaEntity entity);

    SchoolEventJpaEntity toEntity(SchoolEvent domain);
}
