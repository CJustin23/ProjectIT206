package vn.ptit.ffa.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint xác nhận Sprint 1: container app sống + nối được MySQL qua Flyway migration.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("app", "family-finance-api");
        body.put("version", System.getenv().getOrDefault("APP_VERSION", "dev"));

        Map<String, Object> db = new LinkedHashMap<>();
        try {
            db.put("connected", true);
            db.put("tables", jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database()", Long.class));
            db.put("flywayVersion", jdbcTemplate.queryForObject(
                    "select version from flyway_schema_history order by installed_rank desc limit 1", String.class));
        } catch (Exception e) {
            db.put("connected", false);
            db.put("error", e.getMessage());
        }
        body.put("db", db);
        return body;
    }
}
