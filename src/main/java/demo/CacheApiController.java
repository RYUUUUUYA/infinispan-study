package demo;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheApiController {
    private static final Logger log = LoggerFactory.getLogger(CacheApiController.class);

    private final CacheRegistry cacheRegistry;
    private final ObjectMapper objectMapper;
    private final AppSettings settings;

    public CacheApiController(CacheRegistry cacheRegistry, ObjectMapper objectMapper, AppSettings settings) {
        this.cacheRegistry = cacheRegistry;
        this.objectMapper = objectMapper;
        this.settings = settings;
    }

    @GetMapping(value = "/api/get/{cacheName}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(@PathVariable String cacheName, @PathVariable String key) {
        Cache<String, String> cache = cacheRegistry.cache(cacheName);
        String value = cache.get(key);
        if (value == null) {
            log.info("event=cache_get cache={} key={} result=miss node={}", cacheName, key, settings.nodeId());
            return json(HttpStatus.NOT_FOUND, "{\"error\":\"key_not_found\"}");
        }

        log.info("event=cache_get cache={} key={} result=hit node={}", cacheName, key, settings.nodeId());
        return json(HttpStatus.OK, value);
    }

    @PostMapping(
        value = "/api/get/{cacheName}/{key}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> put(@PathVariable String cacheName, @PathVariable String key, @RequestBody String body) {
        Cache<String, String> cache = cacheRegistry.cache(cacheName);
        String canonicalJson = canonicalJson(body);
        boolean existed = cache.containsKey(key);
        cache.put(key, canonicalJson);

        String operation = existed ? "update" : "create";
        log.info("event=cache_put operation={} cache={} key={} result=success node={}",
            operation, cacheName, key, settings.nodeId());
        return json(existed ? HttpStatus.OK : HttpStatus.CREATED, canonicalJson);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        String body = writeJson(Map.of(
            "status", "UP",
            "node", settings.nodeId(),
            "owners", cacheRegistry.owners(),
            "caches", cacheRegistry.cacheNames()
        ));
        return json(HttpStatus.OK, body);
    }

    private String canonicalJson(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            throw new InvalidJsonException("Request body must be valid JSON", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write JSON response", e);
        }
    }

    private static ResponseEntity<String> json(HttpStatus status, String body) {
        return ResponseEntity.status(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body);
    }
}
