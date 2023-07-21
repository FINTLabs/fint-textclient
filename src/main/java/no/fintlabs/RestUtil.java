package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class RestUtil {
    private final WebClient webClient;

    public RestUtil(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T> Mono<T> get(Class<T> clazz, String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(clazz)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(retrySignal -> log.error("Retrying")))
                .doOnError(this::handleErrorResponse);
    }

    private void handleErrorResponse(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();

            if (statusCode == 404) {
                log.error("Endpoint not found (404):");
            } else if (statusCode == 403) {
                log.error("Forbidden (403): ");
            } else if (statusCode == 503) {
                log.error("Servive Unavalieble (503)");

            } else {
                log.error("An error occurred: " + responseException.getResponseBodyAsString());
            }
        } else {
            log.error("An unexpected error occurred: ", throwable);
        }
    }
}