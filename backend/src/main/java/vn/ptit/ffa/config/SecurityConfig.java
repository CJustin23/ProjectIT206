package vn.ptit.ffa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.security.JwtAuthenticationFilter;
import vn.ptit.ffa.web.dto.ErrorResponse;

import java.util.Arrays;
import java.util.List;

/**
 * API stateless: chi /api/health, Swagger va nut xac thuc duoc mo cong
 * (DUAN-40). Moi request khac phai co access token hop le.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/api/health",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health"
    };

    private static final String[] PUBLIC_AUTH_PATHS = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh"
    };

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
                .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, cause) ->
                    writeError(request, response, objectMapper, 401))
                .accessDeniedHandler((request, response, cause) ->
                    writeError(request, response, objectMapper, 403)))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    private void writeError(jakarta.servlet.http.HttpServletRequest request,
                            HttpServletResponse response,
                            ObjectMapper objectMapper,
                            int status) throws java.io.IOException {
        Object attr = request.getAttribute("authError");
        ErrorResponse body;
        if (attr instanceof ApiException api && api.getStatus().value() == status) {
            body = ErrorResponse.of(status, api.getCode(), api.getMessage());
        } else if (status == 403) {
            body = ErrorResponse.of(403, "FORBIDDEN_ROLE", "Bạn không có quyền thực hiện thao tác này.");
        } else {
            body = ErrorResponse.of(401, "AUTH_INVALID_CREDENTIALS", "Cần đăng nhập để truy cập tài nguyên này.");
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Idempotency-Key", "If-Match"));
        config.setExposedHeaders(List.of("ETag"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
