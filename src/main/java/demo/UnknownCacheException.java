package demo;

public class UnknownCacheException extends RuntimeException {
    public UnknownCacheException(String cacheName) {
        super("Unknown cache: " + cacheName);
    }
}
