package com.kaloyan_marinov.personal_finance_tracker.user;

import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRepository;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TransactionRepository transactionRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public UserController(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        TransactionRepository transactionRepository,
        ExchangeRateRepository exchangeRateRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        this.transactionRepository = transactionRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @PostMapping("")
    public ResponseEntity<?> create(
        @Valid @RequestBody UserRequestCreate userRequestCreate
    ) {
        boolean usernameTaken = userRepository.existsByUsername(
            userRequestCreate.username()
        );
        if (usernameTaken) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "username is already taken"))
            ;
        }

        boolean emailExists = userRepository.existsByEmail(
            userRequestCreate.email()
        );
        if (emailExists) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "email is already taken"))
            ;
        }

        String passwordHash = this.passwordEncoder.encode(
            userRequestCreate.password()
        );
        User u = new User(
            userRequestCreate.username(),
            userRequestCreate.email(),
            passwordHash
        );

        User createdUser;
        try {
            createdUser = userRepository.save(u);
        } catch (DataIntegrityViolationException e) {
            /*
            This block of code is executed
            upon every violation of data integrity,
            which includes but is not limited to
            a `NOT NULL` violation;
            two concurrent attempts to insert the same username or email;
            etc.
            */
            if (!isUniqueConstraintViolation(e)) {
                // Any other kind of integrity violation is a bug,
                // so it is re-thrown.
                throw e;
            }

            /*
            This is reached
            only if another attempt to insert the same username or email is made
            *after* all of the `.existsBy...` checks above have passed.
            (
            For example,

            if two requests attempting to insert the same username or email
            reach the application concurrently/simultaneously (= at the same time),
            each of those requests passes the `.existsBy...` checks above;

            but the `.save` call from only one request will have been accepted by the database,
            whereas the other one will have fallen through to this block of code.
            )
            */

            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "username or email is already taken"))
            ;
        }

        UserResponseCreate userResponseCreate = new UserResponseCreate(
            createdUser.getId(),
            createdUser.getUsername(),
            createdUser.getEmail()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userResponseCreate)
        ;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
        @PathVariable Long id,
        Authentication authentication
    ) {
        String email = authentication.getName();

        User authenticatedUser = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException(email))
        ;

        if (!authenticatedUser.getId().equals(id)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                    Map.of(
                        "error", "not allowed to delete a user other than the authenticated one"
                    )
                )
            ;
        }

        if (transactionRepository.existsByUserId(id)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    Map.of(
                        "error", "unable to delete your user, because you still have transactions"
                    )
                )
            ;
        }

        if (exchangeRateRepository.existsByUserId(id)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    Map.of(
                        "error", "unable to delete your user, because you still have exchange rates"
                    )
                )
            ;
        }

        userRepository.delete(authenticatedUser);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private static boolean isUniqueConstraintViolation(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof ConstraintViolationException cve) {
                return cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
            }
        }
        return false;
    }
}
