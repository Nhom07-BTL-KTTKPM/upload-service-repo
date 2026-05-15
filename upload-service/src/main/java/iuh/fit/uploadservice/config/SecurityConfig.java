package iuh.fit.uploadservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::mapAuthorities);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = jwtSecretKey();
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey jwtSecretKey() {
        byte[] raw = jwtSecret == null
                ? new byte[0]
                : jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (raw.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }

        return new SecretKeySpec(raw, "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Công khai endpoint health check để Eureka hoặc Gateway có thể kiểm tra service
                        .requestMatchers("/api/v1/upload/health").permitAll()
                        // Mọi endpoint upload khác đều bắt buộc phải có Token hợp lệ
                        // (Đã dọn dẹp các dòng đơn lẻ vì anyRequest().authenticated() đã bao hàm tất cả)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Chuyển đổi các thông tin từ JWT thành Authorities (Roles/Scopes) cho Spring Security.
     * Giữ lại để phục vụ việc phân quyền chi tiết (hasRole) trong tương lai.
     */
    private static Collection<GrantedAuthority> mapAuthorities(Jwt jwt) {
        java.util.List<GrantedAuthority> out = new java.util.ArrayList<>();

        // Trích xuất từ claim "role" hoặc "roles"
        Object roleClaim = findClaim(jwt, new String[]{"role", "roles"});
        if (roleClaim instanceof String sRole) {
            String normalized = normalizeRole(sRole);
            if (normalized != null) out.add(new SimpleGrantedAuthority(normalized));
        } else if (roleClaim instanceof Collection<?> roleCols) {
            roleCols.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(SecurityConfig::normalizeRole)
                    .filter(Objects::nonNull)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(out::add);
        }

        // Trích xuất từ claim "authority" hoặc "authorities"
        Object authClaim = findClaim(jwt, new String[]{"authority", "authorities", "authoritiesList"});
        if (authClaim instanceof String sAuth) {
            out.add(new SimpleGrantedAuthority(sAuth));
        } else if (authClaim instanceof Collection<?> authCols) {
            authCols.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(out::add);
        }

        // Trích xuất từ "scope" hoặc "scp" (Thường dùng cho Gateway/Client)
        Object scopeClaim = findClaim(jwt, new String[]{"scope", "scp"});
        if (scopeClaim instanceof String sScope) {
            String[] parts = sScope.split("[\\s,]+");
            for (String p : parts) {
                if (p == null || p.isBlank()) continue;
                out.add(new SimpleGrantedAuthority("SCOPE_" + p));
                if (p.equalsIgnoreCase("upload") || p.toLowerCase().contains("upload")) {
                    out.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
            }
        } else if (scopeClaim instanceof Collection<?> scopeCols) {
            scopeCols.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .forEach(p -> {
                        out.add(new SimpleGrantedAuthority("SCOPE_" + p));
                        if (p.equalsIgnoreCase("upload") || p.toLowerCase().contains("upload")) {
                            out.add(new SimpleGrantedAuthority("ROLE_USER"));
                        }
                    });
        }

        return out.isEmpty() ? List.of() : out;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) return null;
        String upper = role.trim().toUpperCase(java.util.Locale.ROOT);
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }

    private static Object findClaim(Jwt jwt, String[] keys) {
        for (String k : keys) {
            if (jwt.getClaims().containsKey(k)) {
                return jwt.getClaims().get(k);
            }
        }
        return null;
    }
}