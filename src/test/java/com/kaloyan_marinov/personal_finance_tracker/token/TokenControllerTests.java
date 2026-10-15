package com.kaloyan_marinov.personal_finance_tracker.token;

import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
    }
)
@AutoConfigureRestTestClient
/*
The following annotation is required,
because it creates a `MockMvc` bean that has the security filter chain,
and then the `RestTestClient` binds to the created `MockMvc` bean.
*/
@AutoConfigureMockMvc
class TokenControllerTests {
    
    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_1() {
        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().doesNotExist("Set-Cookie")
        ;
    }

    /*
    The last assertion within `create_1` cannot detect a session,
    because `MockMvc` has no «servlet container» to write `Set-Cookie: JSESSIONID=...`.

    So this test inspects the server-side request instead.
    */
    @Test
    void create_2() throws Exception {
        // Act.
        MvcResult mvcResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/tokens")
            )
            .andReturn()
        ;

        // Assert.
        Assertions.assertEquals(
            HttpStatus.UNAUTHORIZED.value(),
            mvcResult.getResponse().getStatus()
        );
        Assertions.assertNull(
            mvcResult.getRequest().getSession(false)
        );
    }

    @Test
    void create_3() {
        // Arrange.
        String wrongUsername = "wrong-u";
        String wrongPassword = "wrong-p";

        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    wrongUsername,
                    wrongPassword
                )
            )
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void create_4() {
        // Arrange.
        String wrongPassword = "wrong-p";

        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    "application-user-for-test-suite-u",
                    wrongPassword
                )
            )
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void create_5() {
        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    "application-user-for-test-suite-u",
                    "application-user-for-test-suite-p"
                )
            )
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty()
        ;
    }

}
