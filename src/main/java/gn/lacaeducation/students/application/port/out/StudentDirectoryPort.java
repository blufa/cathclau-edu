package gn.lacaeducation.students.application.port.out;

import gn.lacaeducation.students.domain.StudentDirectoryEntry;
import java.util.List;

/** Cross-tenant student directory lookup (bypasses tenant scoping by design — see adapter). */
public interface StudentDirectoryPort {
    /** Search every school by exact matricule and/or name fragment (already lower-cased). Capped. */
    List<StudentDirectoryEntry> search(String matricule, String nameQuery);

    /** Noms d'écoles de la plateforme correspondant au fragment (déjà en minuscules). Capé. */
    List<String> searchSchools(String query);
}
