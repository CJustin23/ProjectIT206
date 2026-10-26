package vn.ptit.ffa.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import vn.ptit.ffa.cache.LoggingCacheErrorHandler;

import java.time.Duration;

/**
 * Hai tang cache theo DUAN-57 va muc 5 bao cao:
 *   L2 Redis  = so lieu thong ke (reports, dashboard) dung chung moi instance.
 *   L1 Caffeine = danh muc / vi it doi, doc nhanh trong pham vi mot node.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_REPORTS = "reports";
    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_CATALOG = "catalog";

    @Bean
    @Primary
    public org.springframework.cache.CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper());
        var base = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .prefixCacheNameWith("budglee:")
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(base)
            .withCacheConfiguration(CACHE_DASHBOARD, base.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(CACHE_CATALOG, base.entryTtl(Duration.ofHours(1)))
            .transactionAware()
            .build();
    }

    /**
     * Redis luu record DTO (LocalDate, BigDecimal) nen can JavaTimeModule va
     * default typing de doc lai dung kieu.
     */
    private ObjectMapper cacheObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("vn.ptit.ffa.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_CATALOG);
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30)));
        return manager;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(log);
    }
}
