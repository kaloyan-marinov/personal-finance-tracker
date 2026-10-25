package com.kaloyan_marinov.personal_finance_tracker;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.kaloyan_marinov.personal_finance_tracker.user.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainForBasicAuth(HttpSecurity http) throws Exception {
        http
            .securityMatchers(
                (matchers) -> matchers
                    .requestMatchers("/api/v1/tokens")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}")
            )
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
                /*
                When a controller - or «Spring MVC» itself (e.g. on a malformed request body) - calls `response.sendError(...)`,
                the servlet container forwards to `/error` (an «ERROR dispatch»),
                which passes through this «security filter chain» again;
                without a rule for it, a request without a token gets a 401 instead of the real status.

                (
                    For example,

                    a `POST /api/v1/users` with a malformed JSON body (and no token, as permitted below)
                    fails with an `HttpMessageNotReadableException`
                    before the controller method is even invoked;

                    «Spring MVC»'s `DefaultHandlerExceptionResolver` turns that into `response.sendError(400)`,
                    but the ensuing ERROR dispatch carries no token,
                    so - without a rule for it - the client would get a 401 instead of the 400.
                )

                There are 2 ways of permitting it:

                1. `.requestMatchers("/error")` matches the path,
                   so it also lets clients call `GET /error` directly without a token,
                   and it stops matching if `server.error.path` is changed.

                2. `.dispatcherTypeMatchers(DispatcherType.ERROR)` matches only the container's own error dispatches,
                   so a direct `GET /error` still requires a Bearer token,
                   and it keeps matching wherever the error page lives.

                The latter is preferable: it permits exactly what is needed (and nothing more),
                which keeps the "requires a Bearer token by default" promise above;
                it is also the approach shown in the «Spring Security» reference documentation.

                [Caveat]

                `MockMvc` does not perform the ERROR dispatch,
                so tests based on it cannot tell the 2 options apart (nor catch the 401 in the 1st place);
                only a running server can (e.g. `@SpringBootTest(webEnvironment = RANDOM_PORT)`).
                */
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
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

    @Bean
    public UserDetailsService userDetailsService(
        UserRepository userRepository
    ) {
        return email -> userRepository
            .findByEmail(email)
            .map(
                (user) -> User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .build()
            )
            // `DaoAuthenticationProvider` expects exactly this exception for an unknown user
            // (any other exception is treated as an internal error).
            .orElseThrow(() -> new UsernameNotFoundException(email))
        ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * - `${pft.security.secret.key}` has no default value (there is no `:...` after it).
     * 
     *    So if the variable is missing, <u>the app fails at startup</u>.
     * 
     * - The property names are your own, so the `pft.` prefix is just a convention.
     * 
     * Spring Boot's «relaxed binding» maps the environment variable
     * `PFT_SECURITY_SECRET_KEY` to the property `pft.security.secret.key`
     * (uppercase it, and replace `.` with `_`).
     */
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
