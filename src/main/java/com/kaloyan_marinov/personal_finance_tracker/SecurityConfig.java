package com.kaloyan_marinov.personal_finance_tracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Spring Security turns «CSRF protection» on by default.
            // For a REST API called by non-browser clients, you can disable it as follows:
            .csrf(csrf -> csrf.disable())
            // https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html#stateless-authentication
            // Sometimes there is no need to create and maintain [an] `HttpSession`
            // [in order] to persist the authentication across requests.
            // Some authentication mechanisms (like HTTP Basic)
            // are stateless and, therefore, re-authenticates the user on every request.
            .sessionManagement(
                s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests((authorize) ->
                authorize
                    .requestMatchers("/api/v1/tokens").authenticated()
                    .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());

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

}
