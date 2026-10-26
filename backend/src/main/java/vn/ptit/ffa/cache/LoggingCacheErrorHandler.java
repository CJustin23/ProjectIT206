package vn.ptit.ffa.cache;

import org.slf4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.lang.NonNull;

/**
 * Redis là tầng tăng tốc, không phải nguồn dữ liệu đúng. Mất Redis (lưới nội bộ,
 * container khởi động chậm hơn app) chỉ làm chậm truy vấn, không được làm fail API.
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private final Logger log;

    public LoggingCacheErrorHandler(Logger log) {
        this.log = log;
    }

    @Override
    public void handleCacheGetError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
        log.warn("cache GET loi (cache={}, key={}): {} — goi thang xuong nguon", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key, Object value) {
        log.warn("cache PUT bo qua (cache={}, key={}): {}", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
        log.warn("cache EVICT bo qua (cache={}, key={}): {}", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException e, @NonNull Cache cache) {
        log.warn("cache CLEAR bo qua (cache={}): {}", cache.getName(), e.getMessage());
    }
}
