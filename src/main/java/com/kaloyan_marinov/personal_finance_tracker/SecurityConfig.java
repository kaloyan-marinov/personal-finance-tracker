package com.kaloyan_marinov.personal_finance_tracker;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainForBasicAuth(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/tokens")
            .authorizeHttpRequests((authorize) -> authorize
                .anyRequest().authenticated()
            )
            .csrf((csrf) -> csrf.disable())
            .httpBasic(Customizer.withDefaults())
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        ;

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChainForBearerAuth(HttpSecurity http) throws Exception {
        http
            // Notice that no call to `securityMatcher` is made here:
            // this «security filter chain» catches every request not matched by the other «security filter chain»,
            // so any endpoint (including ones added later) requires a Bearer token by default.
            .authorizeHttpRequests((authorize) -> authorize
                .anyRequest().authenticated()
            )
            .csrf((csrf) -> csrf.disable())
            // The following call also configures the `ExceptionTranslationFilter`
            // within this «security filter chain» to respond
            // via a `BearerTokenAuthenticationEntryPoint` (401 + `WWW-Authenticate: Bearer ...`)
            // and via a `BearerTokenAccessDeniedHandler` (403),
            // so there is no need to configure those explicitly by calling `.exceptionHandling`.
            .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        ;

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(
            userDetailsService
        );
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * - `${pft.security.user.name}` has no default value (there is no `:...` after it).
     * 
     *    So if the variable is missing, <u>the app fails at startup</u>.
     * 
     * - The property names are your own, so the `pft.` prefix is just a convention.
     * 
     * Spring Boot's «relaxed binding» maps the environment variable
     * `PFT_SECURITY_USER_NAME` to the property `pft.security.user.name`
     * (uppercase it, and replace `.` with `_`).
     */
    @Bean
    public UserDetailsService userDetailsService(
        PasswordEncoder pe,
        @Value("${pft.security.user.name}") String username,
        @Value("${pft.security.user.password}") String password
    ) {
        if (password.isBlank()) {
            throw new IllegalStateException("PFT_SECURITY_USER_PASSWORD may not be set to an empty string & may not be made only of spaces");
        }

        UserDetails userDetails = User
            .withUsername(username)
            .password(
                pe.encode(password)
            )
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecretKey secretKey(
        @Value("${pft.security.secret.key}") String secret
    ) {
        if (secret.isBlank()) {
            throw new IllegalStateException(
                "PFT_SECURITY_SECRET_KEY may not be set to an empty string & may not be made only of spaces"
            );
        }

        byte[] bytesThatRepresentSecret = secret.getBytes(StandardCharsets.UTF_8);
        if (bytesThatRepresentSecret.length < 32) {
            throw new IllegalStateException(
                "PFT_SECURITY_SECRET_KEY must be at least 256 bits (32 bytes) long, as required by HS256"
            );
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(
            bytesThatRepresentSecret,
            "HmacSHA256"
        );

        return secretKeySpec;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(
            new ImmutableSecret<>(secretKey)
        );
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
        ;
    }

}
