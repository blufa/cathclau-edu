package gn.lacaeducation.iam.application.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameGeneratorTest {

    private static final Predicate<String> NONE_TAKEN = u -> false;

    @Test
    void generate_lastNameAtRandomNumber_normalizesAccentsSpacesAndCase() {
        assertThat(UsernameGenerator.generate("Jéan Paul", "CAMARÁ", NONE_TAKEN))
                .matches("camara@\\d{3,}");
    }

    @Test
    void generate_incrementsNumberOnCollision() {
        // Bloque toute la plage de départ possible (100-999) — le générateur doit incrémenter au-delà
        // plutôt que de retirer au hasard indéfiniment, quel que soit le numéro de départ tiré.
        Set<String> taken = new java.util.HashSet<>();
        for (int n = 100; n <= 999; n++) taken.add("camara@" + n);

        String result = UsernameGenerator.generate("Jean", "Camara", taken::contains);

        assertThat(result).matches("camara@\\d+");
        assertThat(taken).doesNotContain(result);
        assertThat(Integer.parseInt(result.split("@")[1])).isGreaterThan(999);
    }

    @Test
    void generate_blankLastName_fallsBackToFirstName() {
        assertThat(UsernameGenerator.generate("Jean", "  ", NONE_TAKEN)).matches("jean@\\d{3,}");
    }

    @Test
    void generate_blankNames_fallsBackToUser() {
        assertThat(UsernameGenerator.generate("  ", null, NONE_TAKEN)).matches("user@\\d{3,}");
    }
}
