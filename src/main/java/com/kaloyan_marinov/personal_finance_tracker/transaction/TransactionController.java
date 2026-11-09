package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kaloyan_marinov.personal_finance_tracker.common.ConstantsForPagination;
import com.kaloyan_marinov.personal_finance_tracker.common.PaginatedResponse;
import com.kaloyan_marinov.personal_finance_tracker.common.PaginationInfo;
import com.kaloyan_marinov.personal_finance_tracker.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;

    public TransactionController(
        TransactionRepository transactionRepository,
        UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("")
    public ResponseEntity<?> create(
        @Valid @RequestBody TransactionRequest transactionRequest,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        if (!userRepository.existsById(userId)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    Map.of(
                        "error", "unauthorized to create a transaction"
                    )
                )
            ;
        }

        Transaction t = new Transaction();
        t.setUser(
            /*
            (The user's existence is checked separately above, via `existsById`.)

            The following returns a proxy that holds only the ID, without running a SELECT;
            that's all Hibernate needs to write `user_id`.
            */
            userRepository.getReferenceById(userId)
        );

        t.setYear(transactionRequest.year());
        t.setAmount(transactionRequest.amount());
        t.setCurrency(transactionRequest.currency());
        t.setDescription(transactionRequest.description());
        Transaction createdTransaction = transactionRepository.save(t);

        TransactionResponse transactionResponse = new TransactionResponse(
            createdTransaction.getId(),
            createdTransaction.getYear(),
            createdTransaction.getAmount(),
            createdTransaction.getCurrency(),
            createdTransaction.getDescription()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(transactionResponse)
        ;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> retrieveOne(
        @PathVariable Long id,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<Transaction> optionalTransaction = transactionRepository.findByIdAndUserId(id, userId);

        Transaction transaction = optionalTransaction.orElse(null);

        if (transaction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        TransactionResponse transactionResponse = new TransactionResponse(
            transaction.getId(),
            transaction.getYear(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getDescription()
        );
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(transactionResponse)
        ;
    }

    @GetMapping("")
    public ResponseEntity<PaginatedResponse<TransactionResponse>> retrieveAll(
        Authentication authentication,
        @RequestParam(defaultValue = "" + ConstantsForPagination.PAGE_NUMBER_DEFAULT)
        @Min(ConstantsForPagination.PAGE_NUMBER_MIN)
        int pageNumber,
        @RequestParam(defaultValue = "" + ConstantsForPagination.PAGE_SIZE_DEFAULT)
        @Min(ConstantsForPagination.PAGE_SIZE_MIN)
        @Max(ConstantsForPagination.PAGE_SIZE_MAX)
        int pageSize
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        String nameOfEntityProperty = "id";
        Pageable pageable = PageRequest.of(
            pageNumber,
            pageSize,
            Sort.by(nameOfEntityProperty)
        );
        Page<Transaction> pageOfTransactions = transactionRepository.findAllByUserId(
            userId,
            pageable
        );

        List<TransactionResponse> transactionResponses = new ArrayList<>();
        for (Transaction t : pageOfTransactions.getContent()) {
            TransactionResponse trR = new TransactionResponse(
                t.getId(),
                t.getYear(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription()
            );
            transactionResponses.add(trR);
        }
        PaginatedResponse<TransactionResponse> paginatedResponse = new PaginatedResponse<>(
            transactionResponses,
            new PaginationInfo(
                pageOfTransactions.getTotalElements(),
                pageNumber,
                pageSize
            )
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(paginatedResponse)
        ;
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody TransactionRequest transactionRequest,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<Transaction> optionalTransaction = transactionRepository.findByIdAndUserId(id, userId);

        if (optionalTransaction.isPresent()) {
            Transaction t = optionalTransaction.get();
            
            t.setYear(transactionRequest.year());
            t.setAmount(transactionRequest.amount());
            t.setCurrency(transactionRequest.currency());
            t.setDescription(transactionRequest.description());
            Transaction updatedTransaction = transactionRepository.save(t);

            TransactionResponse transactionResponse = new TransactionResponse(
                updatedTransaction.getId(),
                updatedTransaction.getYear(),
                updatedTransaction.getAmount(),
                updatedTransaction.getCurrency(),
                updatedTransaction.getDescription()
            );
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(transactionResponse)
            ;
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<Transaction> optionalTransaction = transactionRepository.findByIdAndUserId(id, userId);

        if (optionalTransaction.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Transaction t = optionalTransaction.get();

        transactionRepository.delete(t);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
