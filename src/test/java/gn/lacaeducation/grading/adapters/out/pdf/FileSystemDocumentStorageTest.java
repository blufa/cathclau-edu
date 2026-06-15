package gn.lacaeducation.grading.adapters.out.pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemDocumentStorageTest {

    @Test
    void store_thenRead_roundTripsContent(@TempDir Path dir) {
        FileSystemDocumentStorage storage = new FileSystemDocumentStorage(dir.toString());
        byte[] content = "hello-pdf".getBytes(StandardCharsets.UTF_8);

        String location = storage.store("doc-1.pdf", content);

        assertThat(location).isNotBlank();
        assertThat(storage.read(location)).isEqualTo(content);
    }

    @Test
    void store_isIdempotentForSameFilename(@TempDir Path dir) {
        FileSystemDocumentStorage storage = new FileSystemDocumentStorage(dir.toString());

        String first = storage.store("doc.pdf", "v1".getBytes(StandardCharsets.UTF_8));
        String second = storage.store("doc.pdf", "v2".getBytes(StandardCharsets.UTF_8));

        assertThat(first).isEqualTo(second);
        assertThat(storage.read(second)).isEqualTo("v2".getBytes(StandardCharsets.UTF_8));
    }
}
