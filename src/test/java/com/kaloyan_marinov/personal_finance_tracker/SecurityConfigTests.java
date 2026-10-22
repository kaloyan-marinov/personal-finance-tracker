package com.kaloyan_marinov.personal_finance_tracker;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.env.StandardEnvironment;

class SecurityConfigTests {

    private static final List<String> PROPERTIES_EXCEPT_SECRET_KEY = List.of(
        "spring.datasource.url=jdbc:h2:mem:test-security-config",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "server.port=0",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
    );

    /*
    The «system environment» property source is removed,
    so that a `PFT_SECURITY_SECRET_KEY` set in the shell running the test suite
    cannot stand in for the property this test class is about.
    */
    private static Throwable startApplication(List<String> properties) {
        StandardEnvironment standardEnv = new StandardEnvironment();
        standardEnv.getPropertySources().remove(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME
        );

        RuntimeException runtimeException = Assertions.assertThrows(
            RuntimeException.class,
            () -> new SpringApplicationBuilder(PersonalFinanceTrackerApplication.class)
                .environment(standardEnv)
                .properties(properties.toArray(String[]::new))
                .run()
                .close()
        );

        return NestedExceptionUtils.getMostSpecificCause(runtimeException);
    }

    @Test
    void secretKey_1() {
        // Arrange.
        SecurityConfig securityConfig = new SecurityConfig();
        String secret = "a".repeat(31);

        // Act + Assert.
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> securityConfig.secretKey(secret)
        );
    }

    @Test
    void secretKey_2() {
        // Arrange.
        SecurityConfig securityConfig = new SecurityConfig();
        String secret = "a".repeat(32);

        // Act.
        SecretKey secretKey = securityConfig.secretKey(secret);

        // Assert.
        Assertions.assertEquals(
            "HmacSHA256",
            secretKey.getAlgorithm()
        );

        Assertions.assertEquals(
            32,
            secretKey.getEncoded().length
        );
    }

    @Test
    void secretKey_3() {
        // Arrange.
        SecurityConfig securityConfig = new SecurityConfig();
        String secret = " ".repeat(32);

        // Act + Assert.
        IllegalStateException illegalStateException = Assertions.assertThrows(
            IllegalStateException.class,
            () -> securityConfig.secretKey(secret)
        );

        Assertions.assertTrue(
            illegalStateException.getMessage().contains("only of spaces"),
            illegalStateException.getMessage()
        );
    }

    @Test
    void startup_1() {
        // Act.
        Throwable cause = startApplication(PROPERTIES_EXCEPT_SECRET_KEY);

        // Assert.
        Assertions.assertTrue(
            cause.getMessage().contains("pft.security.secret.key"),
            cause.getMessage()
        );
    }

    @Test
    void startup_2() {
        // Arrange.
        List<String> properties = new ArrayList<>(PROPERTIES_EXCEPT_SECRET_KEY);
        properties.add("pft.security.secret.key=" + "a".repeat(31));

        // Act.
        Throwable cause = startApplication(properties);

        // Assert.
        Assertions.assertInstanceOf(
            IllegalStateException.class,
            cause
        );

        Assertions.assertTrue(
            cause.getMessage().contains("PFT_SECURITY_SECRET_KEY"),
            cause.getMessage()
        );
    }

}
