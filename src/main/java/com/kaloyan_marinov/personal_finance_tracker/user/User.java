package com.kaloyan_marinov.personal_finance_tracker.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_users_username",
            columnNames = { "username" }
        ),
        @UniqueConstraint(
            name = "uq_users_email",
            columnNames = { "email" }
        )
    }
    /*
    There is no need to include a property such as:
    ```
    indexes = @Index(
        name = "idx_email",
        columnList = "email"
    )
    ```

    MySQL (InnoDB) backs every `UNIQUE` constraint with a unique index of the same name.
    Lookups by `email` will use that index.
    (If the above-mentioned property were added,
    a 2nd, redundant index on the same column would be created.)
    */
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(
        length = 32,
        nullable = false
    )
    private String username;

    @NotBlank
    @Column(
        length = 128,
        nullable = false
    )
    private String email;

    @NotBlank
    @Column(
        name = "password_hash",
        length = 128,
        nullable = false
    )
    private String passwordHash;

    // JPA requires a no-argument constructor.
    protected User() {
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

}
