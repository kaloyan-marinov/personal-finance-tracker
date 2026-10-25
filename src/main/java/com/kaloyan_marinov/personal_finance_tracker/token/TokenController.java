package com.kaloyan_marinov.personal_finance_tracker.token;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaloyan_marinov.personal_finance_tracker.user.User;
import com.kaloyan_marinov.personal_finance_tracker.user.UserRepository;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {

    private final UserRepository userRepository;

    private final JwtEncoder jwtEncoder;

    public TokenController(UserRepository userRepository, JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("")
    public ResponseEntity<TokenResponse> create(
        Authentication authentication
    ) {
        Instant instantNow = Instant.now();
        long secondsOfValidity = Duration.ofHours(1).toSeconds();
        Instant instantOfExpiration = instantNow.plusSeconds(secondsOfValidity);

        String email = authentication.getName();

        User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException(email))
        ;

        // Without making this explicit,
        // the `jwtEncoder` bean would use the default header for `NimbusJwtEncoder`,
        // which asks for the RS256 (RSA) algorithm.
        JwsHeader jwsHeader = JwsHeader
            .with(MacAlgorithm.HS256)
            .build()
        ;
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet
            .builder()
            .expiresAt(instantOfExpiration)
            // «Spring Security»'s resource server reads the principal name from `sub` by default;
            // consequently, if the following claim were omitted,
            // the `Authentication` of Bearer-authenticated requests would have a `null` name.
            .subject(user.getUsername())
            .claim("userId", user.getId())
            .build()
        ;
        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
            jwsHeader,
            jwtClaimsSet
        );

        String accessToken = this.jwtEncoder
            .encode(jwtEncoderParameters)
            .getTokenValue()
        ;
        TokenResponse tokenResponse = new TokenResponse(accessToken);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(tokenResponse)
        ;
    }

}
