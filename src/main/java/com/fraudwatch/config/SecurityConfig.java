package com.fraudwatch.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.fraudwatch.auth.TokenService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who can call what.
 *
 * <ul>
 *   <li>Stateless API: no server session, every request carries a JWT in the Authorization header.</li>
 *   <li>CSRF protection is disabled because there is no cookie-based session to attack.</li>
 *   <li>The JWT contains a "roles" claim, turned into Spring authorities ROLE_USER, ROLE_ANALYST, ROLE_ADMIN.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Static demo UI, API docs and health check are public
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // Role-based access
                        .requestMatchers("/api/alerts/**").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")
                        .requestMatchers("/api/accounts/**").hasRole("USER")
                        .requestMatchers("/error").permitAll()
                        // Everything else: deny by default
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:"))
                        .frameOptions(Customizer.withDefaults()));
        return http.build();
    }

    /** Reads the "roles" claim of the token and prefixes each value with ROLE_. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** BCrypt: slow on purpose and salted, so stolen hashes are hard to crack. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Validates the signature, the expiry date (with the application clock) and the issuer of every token.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator expiry = new JwtTimestampValidator();
        expiry.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(expiry, new JwtIssuerValidator(TokenService.ISSUER)));
        return decoder;
    }
}
