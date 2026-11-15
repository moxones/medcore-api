package com.medical.medcore.shared.security.recaptcha;

import com.medical.medcore.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RecaptchaVerifierTest {

    private MockRestServiceServer server;
    private RecaptchaVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new RecaptchaVerifier(builder.build(), true, "site", "secret", 0.5);
    }

    @Test
    void disabledVerifierAcceptsAnythingWithoutCallingGoogle() {
        RecaptchaVerifier disabled = new RecaptchaVerifier(RestClient.create(), false, "", "", 0.5);

        assertThatCode(() -> disabled.verify(null, "login", "1.2.3.4")).doesNotThrowAnyException();
        assertThat(disabled.siteKey()).isNull();
    }

    @Test
    void enabledWithoutKeysFailsAtStartup() {
        assertThatThrownBy(() -> new RecaptchaVerifier(RestClient.create(), true, "site", "", 0.5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingTokenIsRejected() {
        assertThatThrownBy(() -> verifier.verify(" ", "login", null))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getCode()).isEqualTo(RecaptchaVerifier.CODE_REQUIRED));
    }

    @Test
    void validTokenWithMatchingActionPasses() {
        expectGoogle("{\"success\":true,\"score\":0.9,\"action\":\"login\"}");

        assertThatCode(() -> verifier.verify("tok", "login", "1.2.3.4")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void lowScoreIsRejected() {
        expectGoogle("{\"success\":true,\"score\":0.1,\"action\":\"login\"}");

        assertRejected("login");
    }

    @Test
    void tokenIssuedForAnotherActionIsRejected() {
        expectGoogle("{\"success\":true,\"score\":0.9,\"action\":\"booking\"}");

        assertRejected("login");
    }

    @Test
    void invalidTokenIsRejected() {
        expectGoogle("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");

        assertRejected("login");
    }

    @Test
    void googleUnavailableFailsClosed() {
        server.expect(requestTo(RecaptchaVerifier.VERIFY_URL)).andRespond(withServerError());

        assertRejected("login");
    }

    private void expectGoogle(String json) {
        server.expect(requestTo(RecaptchaVerifier.VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(java.util.Map.of("secret", "secret", "response", "tok")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void assertRejected(String action) {
        assertThatThrownBy(() -> verifier.verify("tok", action, null))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getCode()).isEqualTo(RecaptchaVerifier.CODE_FAILED));
    }
}
