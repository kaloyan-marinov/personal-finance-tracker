package com.kaloyan_marinov.personal_finance_tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class TransactionController {
    
    private static final List<Transaction> TRANSACTIONS = new ArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> retrieveAll() {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(TRANSACTIONS)
        ;
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> create(
        @RequestBody
        TransactionDTO transactionDTO
    ) {
        Transaction t = new Transaction(
            counter.incrementAndGet(),
            transactionDTO.year(),
            transactionDTO.amount()
        );

        TRANSACTIONS.add(t);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(t)
        ;
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> retrieveOne(
        @PathVariable Long id
    ) {
        Transaction transaction = TRANSACTIONS
            .stream()
            .filter((t) -> t.id() == id)
            .findFirst()
            .orElse(null)
        ;

        if (transaction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(transaction)
        ;
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Transaction> delete(
        @PathVariable Long id
    ) {
        Transaction transaction = TRANSACTIONS
            .stream()
            .filter((t) -> t.id() == id)
            .findFirst()
            .orElse(null)
        ;

        if (transaction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Mutate the original list in-place.
        TRANSACTIONS.removeIf((t) -> t.id() == id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> update(
        @PathVariable Long id,
        @RequestBody TransactionDTO transactionDTO
    ) {
        int idxOfTransaction = 
            IntStream.range(0, TRANSACTIONS.size())
            .filter((i) -> TRANSACTIONS.get(i).id() == id)
            .findFirst()
            .orElse(-1)
        ;

        if (idxOfTransaction == -1) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Transaction updatedTransaction = new Transaction(
            id,
            transactionDTO.year(),
            transactionDTO.amount()
        );
        TRANSACTIONS.set(idxOfTransaction, updatedTransaction);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(updatedTransaction)
        ;
    }
}
