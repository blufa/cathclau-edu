package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.Prefecture;
import gn.lacaeducation.platform.domain.PrefectureType;
import gn.lacaeducation.platform.domain.Tenant;

import java.math.BigDecimal;
import java.util.List;

/** SUPER_ADMIN : gestion des préfectures/communes (périmètre DPE/DCE), rattachement des écoles et
 * affectation des comptes DPE_ADMIN. Voir DPE_ARCHITECTURE_ANALYSIS.md §5-8. */
public interface PrefectureUseCase {

    Prefecture create(PrefectureCommand command);
    Prefecture update(Long id, PrefectureCommand command);
    Prefecture archive(Long id);
    Prefecture reactivate(Long id);
    List<Prefecture> findAll();
    Prefecture findById(Long id);

    /** Écoles actuellement rattachées à cette préfecture/commune. */
    List<Tenant> schoolsOf(Long prefectureId);
    /** Rattache une école à une préfecture/commune (remplace le rattachement précédent le cas échéant). */
    Tenant assignSchool(Long prefectureId, Long tenantId);
    /** Détache une école de toute préfecture/commune. */
    Tenant unassignSchool(Long tenantId);

    void assignUser(Long prefectureId, Long userId);
    void unassignUser(Long prefectureId, Long userId);
    List<Long> prefectureIdsOfUser(Long userId);

    record PrefectureCommand(String nom, PrefectureType type, String region, String pays,
                              BigDecimal latitude, BigDecimal longitude) {}
}
