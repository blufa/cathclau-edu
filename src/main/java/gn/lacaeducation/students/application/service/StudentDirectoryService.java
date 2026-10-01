package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.out.ParentRepository;
import gn.lacaeducation.students.application.port.out.StudentDirectoryPort;
import gn.lacaeducation.students.application.port.out.StudentRepository;
import gn.lacaeducation.students.domain.StudentDirectoryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Cross-school student directory (features §E): lets a school verify a transferred student's previous
 * school and last class/promotion decision. Requires an exact matricule or a name of ≥ 3 chars to
 * prevent broad scraping, returns only minimal fields, and audits every lookup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentDirectoryService {

    private static final int SUGGESTION_LIMIT = 25;

    private final StudentDirectoryPort directory;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public List<StudentDirectoryEntry> search(String matricule, String name) {
        String m = matricule == null ? "" : matricule.trim();
        String q = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (m.isEmpty() && q.length() < 3) {
            throw new BusinessRuleException("Fournissez un matricule exact ou un nom d'au moins 3 caractères");
        }
        // Audit: cross-tenant access is sensitive — record who searched what.
        log.info("Cross-school directory lookup by tenant={} matricule='{}' name='{}'", TenantContext.get(), m, q);
        return directory.search(m, q);
    }

    /** Suggestions de noms d'écoles de la plateforme (école d'origine d'un élève transféré). */
    public List<String> searchSchools(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        return directory.searchSchools(q);
    }

    /** Suggestions de "dernière classe" déjà saisies sur la plateforme (§retour comptable). */
    public List<String> searchPreviousClasses(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 1) return List.of();
        return directory.searchPreviousClasses(q);
    }

    /**
     * Suggestions de prénom/nom/adresse déjà saisis pour d'autres élèves OU parents du MÊME tenant
     * (§retour comptable : fratries et quartiers fréquents) — contrairement à {@link #searchSchools} /
     * {@link #searchPreviousClasses}, SCOPÉ au tenant (pas de fuite entre écoles), combine les deux
     * tables (étudiants et parents) pour que le nom d'un parent déjà client suggère aussi le nom d'un
     * frère/sœur, et inversement.
     */
    public List<String> suggestFirstNames(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        Long tenantId = TenantContext.get();
        return merge(studentRepository.suggestFirstNames(tenantId, q, SUGGESTION_LIMIT),
                parentRepository.suggestFirstNames(tenantId, q, SUGGESTION_LIMIT));
    }

    public List<String> suggestLastNames(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        Long tenantId = TenantContext.get();
        return merge(studentRepository.suggestLastNames(tenantId, q, SUGGESTION_LIMIT),
                parentRepository.suggestLastNames(tenantId, q, SUGGESTION_LIMIT));
    }

    public List<String> suggestAddresses(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) return List.of();
        Long tenantId = TenantContext.get();
        return merge(studentRepository.suggestAddresses(tenantId, q, SUGGESTION_LIMIT),
                parentRepository.suggestAddresses(tenantId, q, SUGGESTION_LIMIT));
    }

    private List<String> merge(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(SUGGESTION_LIMIT)
                .toList();
    }
}
