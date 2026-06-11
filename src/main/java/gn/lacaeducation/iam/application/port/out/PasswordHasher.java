package gn.lacaeducation.iam.application.port.out;

public interface PasswordHasher {
    String hash(String raw);
    boolean verify(String raw, String hash);
}
