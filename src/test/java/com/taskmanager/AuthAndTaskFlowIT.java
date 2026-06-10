package com.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end happy/edge paths against real Postgres + Redis (Testcontainers). */
class AuthAndTaskFlowIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @LocalServerPort
    int port;

    /** Absolute URL incl. context-path, so TestRestTemplate's rootUri can't double-prepend it. */
    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private HttpHeaders json(String clientIp) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("X-Forwarded-For", clientIp);   // isolate rate-limit buckets per test
        return h;
    }

    private HttpHeaders auth(String token, String clientIp) {
        HttpHeaders h = json(clientIp);
        h.setBearerAuth(token);
        return h;
    }

    private String register(String email, String ip) {
        String body = """
                {"email":"%s","password":"Passw0rd!","fullName":"IT User"}""".formatted(email);
        ResponseEntity<JsonNode> res = rest.exchange(url("/v1/auth/register"), HttpMethod.POST,
                new HttpEntity<>(body, json(ip)), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return res.getBody().get("accessToken").asText();
    }

    @Test
    void registerLoginAndFetchProfile() {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        String ip = "1.1.1.1";
        String token = register(email, ip);
        assertThat(token).isNotBlank();

        // login
        String loginBody = """
                {"email":"%s","password":"Passw0rd!"}""".formatted(email);
        ResponseEntity<JsonNode> login = rest.exchange(url("/v1/auth/login"), HttpMethod.POST,
                new HttpEntity<>(loginBody, json(ip)), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().get("accessToken").asText()).isNotBlank();

        // /users/me with token
        ResponseEntity<JsonNode> me = rest.exchange(url("/v1/users/me"), HttpMethod.GET,
                new HttpEntity<>(auth(token, ip)), JsonNode.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email").asText()).isEqualTo(email);
    }

    @Test
    void requestWithoutTokenIsUnauthorized() {
        ResponseEntity<String> res = rest.exchange(url("/v1/users/me"), HttpMethod.GET,
                new HttpEntity<>(json("1.2.3.4")), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createCategoryThenListContainsIt() {
        String ip = "5.5.5.5";
        String token = register("it-" + UUID.randomUUID() + "@example.com", ip);

        String body = """
                {"name":"Việc nhà","color":"#26a69a"}""";
        ResponseEntity<JsonNode> created = rest.exchange(url("/v1/categories"), HttpMethod.POST,
                new HttpEntity<>(body, auth(token, ip)), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // duplicate name -> 409
        ResponseEntity<String> dup = rest.exchange(url("/v1/categories"), HttpMethod.POST,
                new HttpEntity<>(body, auth(token, ip)), String.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // list (served from cache on subsequent calls) contains the category
        ResponseEntity<JsonNode> list = rest.exchange(url("/v1/categories"), HttpMethod.GET,
                new HttpEntity<>(auth(token, ip)), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().toString()).contains("Việc nhà");
    }

    @Test
    void rateLimitReturns429AfterThreshold() {
        String ip = "9.9.9.9";   // dedicated bucket
        String body = """
                {"email":"nobody@example.com","password":"wrong"}""";
        HttpStatus last = null;
        boolean got429 = false;
        for (int i = 0; i < 14; i++) {
            ResponseEntity<String> res = rest.exchange(url("/v1/auth/login"), HttpMethod.POST,
                    new HttpEntity<>(body, json(ip)), String.class);
            last = HttpStatus.valueOf(res.getStatusCode().value());
            if (last == HttpStatus.TOO_MANY_REQUESTS) { got429 = true; break; }
        }
        assertThat(got429).as("should hit 429 within the window").isTrue();
        assertThat(last).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
