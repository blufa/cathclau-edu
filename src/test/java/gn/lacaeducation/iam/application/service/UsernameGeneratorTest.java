package gn.lacaeducation.iam.application.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameGeneratorTest {

    private static final Predicate<String> NONE_TAKEN = u -> false;

    @Test
    void generate_normalizesAccentsSpacesAndCase() {
        assertThat(UsernameGenerator.generate("Jéan Paul", "CAMARÁ", NONE_TAKEN))
                .isEqualTo("jeanpaul.camara");
    }

    @Test
    void generate_appendsSuffixOnCollision() {
        Set<String> taken = Set.of("jean.camara", "jean.camara2");
        assertThat(UsernameGenerator.generate("Jean", "Camara", taken::contains))
                .isEqualTo("jean.camara3");
    }

    @Test
    void generate_blankNames_fallsBackToUser() {
        assertThat(UsernameGenerator.generate("  ", null, NONE_TAKEN)).isEqualTo("user");
    }
}
