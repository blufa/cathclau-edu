package gn.lacaeducation.grading.application.port.out;

/** Stores and retrieves generated document bytes (PDFs). */
public interface DocumentStoragePort {
    /** Persists the content and returns an opaque location (path/URL). */
    String store(String filename, byte[] content);
    /** Reads back previously stored content by its location. */
    byte[] read(String location);
}
