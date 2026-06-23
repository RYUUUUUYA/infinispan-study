package demo;

import java.util.LinkedHashSet;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class CacheRegistry {
    private static final Logger log = LoggerFactory.getLogger(CacheRegistry.class);

    private final AppSettings settings;
    private final DefaultCacheManager cacheManager;
    private final Set<String> cacheNames;

    public CacheRegistry(AppSettings settings) {
        this.settings = settings;
        this.cacheNames = new LinkedHashSet<>(settings.cacheNames());
        this.cacheManager = createCacheManager(settings);
        defineCaches();
    }

    public boolean hasCache(String cacheName) {
        return cacheNames.contains(cacheName);
    }

    public Cache<String, String> cache(String cacheName) {
        if (!hasCache(cacheName)) {
            throw new UnknownCacheException(cacheName);
        }
        return cacheManager.getCache(cacheName);
    }

    public Set<String> cacheNames() {
        return Set.copyOf(cacheNames);
    }

    public int owners() {
        return settings.owners();
    }

    @PreDestroy
    public void stop() {
        cacheManager.stop();
    }

    private void defineCaches() {
        var cacheConfig = new ConfigurationBuilder()
            .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash().numOwners(settings.owners())
            .build();

        for (String cacheName : cacheNames) {
            cacheManager.defineConfiguration(cacheName, cacheConfig);
            cacheManager.getCache(cacheName);
            log.info("event=cache_initialized cache={} owners={} node={}",
                cacheName, settings.owners(), settings.nodeId());
        }
    }

    private static DefaultCacheManager createCacheManager(AppSettings settings) {
        applyJGroupsProperties(settings);

        var global = new GlobalConfigurationBuilder();
        global.transport()
            .defaultTransport()
            .clusterName(settings.clusterName())
            .nodeName(settings.nodeId())
            .addProperty("configurationFile", settings.stackFile());

        log.info("event=infinispan_start cluster={} node={} stack={}",
            settings.clusterName(), settings.nodeId(), settings.stackFile());
        return new DefaultCacheManager(global.build());
    }

    private static void applyJGroupsProperties(AppSettings settings) {
        System.setProperty("jgroups.bind.address", settings.bindAddress());
        System.setProperty("jgroups.bind.port", settings.bindPort());
        System.setProperty("jgroups.tcpping.initial_hosts", settings.tcppingInitialHosts());
        System.setProperty("jgroups.aws.s3.bucket_name", settings.s3PingBucket());
        System.setProperty("jgroups.aws.s3.bucket_prefix", settings.s3PingPrefix());
        System.setProperty("jgroups.aws.s3.region_name", settings.awsRegion());
    }
}
