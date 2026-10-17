package gn.lacaeducation.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatriculeAllocatorTest {

    private TenantInfoProvider providerWithFormat(String format) {
        TenantPublicInfo info = new TenantPublicInfo(1L, "École Test", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, format, 4, null, false);
        return new TenantInfoProvider() {
            @Override public Optional<TenantPublicInfo> findTenantInfo(Long tenantId) { return Optional.of(info); }
            @Override public boolean isTenantActive(Long tenantId) { return true; }
        };
    }

    /** Incident de production (Groupe Scolaire Barry Dubaï) : un format sans {NUM} (ex. "GSBD24 0000")
     * génère le même candidat à chaque itération — si ce candidat est déjà pris, la recherche doit
     * s'arrêter avec une erreur claire plutôt que de tourner indéfiniment (saturation CPU de la base). */
    @Test
    void resolveUnique_formatWithoutNumToken_candidateAlwaysTaken_throwsInsteadOfLoopingForever() {
        TenantInfoProvider provider = providerWithFormat("GSBD24 0000");

        assertThatThrownBy(() -> MatriculeAllocator.resolveUnique(provider, 1L, 429L, c -> true))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolveUnique_findsFirstFreeCandidate() {
        TenantInfoProvider provider = providerWithFormat("{PREFIXE}-{NUM}");

        // "École Test" -> sigle "TES" (voir SchoolCode : "École" est un mot vide générique ignoré,
        // ne reste que "Test", un seul mot significatif -> ses 3 premières lettres).
        String result = MatriculeAllocator.resolveUnique(provider, 1L, 1L,
                c -> c.equals("TES-0001") || c.equals("TES-0002"));

        assertThat(result).isEqualTo("TES-0003");
    }

    @Test
    void resolveUnique_sharedCounter_advancesPastEachResolvedCandidate() {
        // §StaffMatriculeBackfill : plusieurs personnes résolues de suite avec le même compteur ne
        // doivent jamais se voir attribuer le même matricule.
        TenantInfoProvider provider = providerWithFormat("{PREFIXE}-{NUM}");
        AtomicLong seq = new AtomicLong(1L);
        java.util.Set<String> taken = new java.util.HashSet<>();

        String first = MatriculeAllocator.resolveUnique(provider, 1L, seq, taken::contains);
        taken.add(first);
        String second = MatriculeAllocator.resolveUnique(provider, 1L, seq, taken::contains);

        assertThat(first).isNotEqualTo(second);
    }
}
