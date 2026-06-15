package gn.lacaeducation.grading.adapters.out.pdf;

import gn.lacaeducation.grading.application.port.out.DocumentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores generated PDFs on the local filesystem (placeholder for an object-storage adapter).
 * Configurable via {@code app.pdf.dir}; defaults to a temp directory.
 */
@Component
class FileSystemDocumentStorage implements DocumentStoragePort {

    private final Path baseDir;

    FileSystemDocumentStorage(@Value("${app.pdf.dir:}") String dir) {
        this.baseDir = (dir == null || dir.isBlank())
                ? Path.of(System.getProperty("java.io.tmpdir"), "lacaeducation-pdfs")
                : Path.of(dir);
    }

    @Override
    public String store(String filename, byte[] content) {
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(filename);
            Files.write(target, content);
            return target.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store PDF " + filename, e);
        }
    }

    @Override
    public byte[] read(String location) {
        try {
            return Files.readAllBytes(Path.of(location));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF " + location, e);
        }
    }
}
