package com.medical.medcore.shared.security.recaptcha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medical.medcore.shared.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class RecaptchaVerifier {

    static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    static final String CODE_REQUIRED = "RECAPTCHA_REQUIRED";
    static final String CODE_FAILED = "RECAPTCHA_FAILED";

    private final boolean enabled;
    private final String siteKey;
    private final String secretKey;
    private final double minScore;
    private final RestClient restClient;

    @Autowired
    public RecaptchaVerifier(RestClient.Builder builder,
                             @Value("${app.recaptcha.enabled:false}") boolean enabled,
                             @Value("${app.recaptcha.site-key:}") String siteKey,
                             @Value("${app.recaptcha.secret-key:}") String secretKey,
                             @Value("${app.recaptcha.min-score:0.5}") double minScore) {
        this(builder.requestFactory(timeouts()).build(), enabled, siteKey, secretKey, minScore);
    }

    RecaptchaVerifier(RestClient restClient, boolean enabled, String siteKey, String secretKey, double minScore) {
        if (enabled && (siteKey.isBlank() || secretKey.isBlank())) {
            throw new IllegalStateException(
                    "RECAPTCHA_ENABLED=true requiere RECAPTCHA_SITE_KEY y RECAPTCHA_SECRET_KEY");
        }
        this.restClient = restClient;
        this.enabled = enabled;
        this.siteKey = siteKey;
        this.secretKey = secretKey;
        this.minScore = minScore;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String siteKey() {
        return enabled ? siteKey : null;
    }

    public void verify(String token, String expectedAction, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new BadRequestException(CODE_REQUIRED,
                    "No pudimos verificar que eres una persona. Recarga la página e inténtalo de nuevo");
        }
        SiteVerifyResponse result = call(token, remoteIp);
        boolean accepted = result != null
                && result.success()
                && expectedAction.equals(result.action())
                && result.score() != null
                && result.score() >= minScore;
        if (!accepted) {
            log.info("reCAPTCHA rechazado: action={} esperado={} score={} errores={}",
                    result != null ? result.action() : null, expectedAction,
                    result != null ? result.score() : null, result != null ? result.errorCodes() : null);
            throw new BadRequestException(CODE_FAILED,
                    "No pudimos verificar que eres una persona. Inténtalo de nuevo en unos segundos");
        }
    }

    private SiteVerifyResponse call(String token, String remoteIp) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.add("remoteip", remoteIp);
        }
        try {
            return restClient.post()
                    .uri(VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);
        } catch (RestClientException e) {
            log.warn("No se pudo contactar a reCAPTCHA: {}", e.getMessage());
            return null;
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    record SiteVerifyResponse(boolean success,
                              Double score,
                              String action,
                              String hostname,
                              @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
