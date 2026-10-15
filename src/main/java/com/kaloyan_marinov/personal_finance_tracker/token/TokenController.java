package com.kaloyan_marinov.personal_finance_tracker.token;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {

    @PostMapping("")
    public ResponseEntity<Void> create() {

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
            ;
    }

}
