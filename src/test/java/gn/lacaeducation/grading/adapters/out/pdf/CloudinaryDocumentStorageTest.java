package gn.lacaeducation.grading.adapters.out.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudinaryDocumentStorageTest {

    @Test
    void sha1Hex_isDeterministicAndCorrect() {
        // Known SHA-1 of "abc"
        assertThat(CloudinaryDocumentStorage.sha1Hex("abc"))
                .isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
    }

    @Test
    void store_withoutCredentials_failsFast() {
        CloudinaryDocumentStorage storage = new CloudinaryDocumentStorage("", "", "", null);

        assertThatThrownBy(() -> storage.store("report-card-1.pdf", new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cloudinary credentials");
    }
}
