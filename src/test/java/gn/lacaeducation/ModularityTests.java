package gn.lacaeducation;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
// import org.springframework.modulith.docs.Documenter; // décommenter après ajout de spring-modulith-starter-test

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(LacaeducationApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    // @Test
    // void documentsModularStructure() {
    //     new Documenter(modules).writeDocumentation();
    // }
}
