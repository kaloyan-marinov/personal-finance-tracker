package com.kaloyan_marinov.personal_finance_tracker.user;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestCreate(
    @NotBlank
    @Size(max = 32)
    String username,

    @NotBlank
    @Size(max = 128)
    @Email
    String email,

    /**
     * In «Spring Security» 7,
     * `BCryptPasswordEncoder.encode` rejects passwords over 72 bytes
     * with an `IllegalArgumentException`.
     * 
     * If a `@Size(max = 72)` annotation were added here,
     * it would validate the number of characters (not bytes);
     * that is why that annotation is not added here.
     */
    @NotBlank
    String password
) {

    /**
     * The following method implements a custom validation,
     * which serves as a safeguard against
     * the above-mentioned limitation within `BCryptPasswordEncoder.encode`.
     * 
     * (
     * A few remarks are in order:
     * 
     * - A missing password is reported by `@NotBlank` instead,
     *   so it is not reported here too.
     * 
     * - `@JsonIgnore` prevents this method
     *   from being serialized as a `passwordAtMost72Bytes` property.
     * )
     */
    @JsonIgnore
    @AssertTrue(message = "password must be at most 72 bytes in UTF-8")
    public boolean isPasswordAtMost72Bytes() {
        if (password == null) {
            return true;
        }

        byte[] passwordAsBytesInUTF8 = password.getBytes(StandardCharsets.UTF_8);

        return passwordAsBytesInUTF8.length <= 72;
    }
}
