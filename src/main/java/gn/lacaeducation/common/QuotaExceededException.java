package gn.lacaeducation.common;
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) { super(message); }
}
