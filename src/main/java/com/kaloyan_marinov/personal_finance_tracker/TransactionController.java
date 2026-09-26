package com.kaloyan_marinov.personal_finance_tracker;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController 
public class TransactionController {

    private TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> create(
        @Valid @RequestBody Transaction transactionDTO
    ) {
        Transaction createdTransaction = transactionRepository.save(transactionDTO);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdTransaction)
        ;
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> retrieveOne(
        @PathVariable Long id
    ) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);

        Transaction transaction = optionalTransaction.orElse(null);

        if (transaction == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(transaction)
        ;
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<Iterable<Transaction>> retrieveAll() {
        Iterable<Transaction> transactions = transactionRepository.findAll();

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(transactions)
        ;
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> update(
        @PathVariable Long id,
        @Valid @RequestBody Transaction transactionDTO
    ) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);

        if (optionalTransaction.isPresent()) {
            Transaction t = optionalTransaction.get();
            
            t.setYear(transactionDTO.getYear());
            t.setAmount(transactionDTO.getAmount());
            transactionRepository.save(t);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(t)
            ;
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(id);

        if (optionalTransaction.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Transaction t = optionalTransaction.get();
        transactionRepository.delete(t);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
