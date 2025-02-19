package de.swm.lhm.geoportal.gateway.shared;

import de.swm.lhm.geoportal.gateway.shared.exceptions.LoggedResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;


@ExtendWith(OutputCaptureExtension.class)
class ErrorHandlingServiceTest extends ErrorHandlingService {

    private RuntimeException genericException;

    @BeforeEach
    void setUp() {
        genericException = new RuntimeException("Test General Throwable");
    }

    @Test
    void handleExceptionShouldLogAndReturnLoggedResponseStatusException(CapturedOutput output) {
        LoggedResponseStatusException loggedException = new LoggedResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test Logged Exception", false);

        Mono<String> result = Mono.error(() -> handleException("Test Logged Exception", loggedException));

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
                    assertThat(exception.getReason(), containsString("Test Logged Exception"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test Logged Exception"));
        assertThat(output.toString(), containsString("Internal Server Error"));
        assertThat(output.toString(), containsString(LoggedResponseStatusException.class.getName()));
    }

    @Test
    void handleExceptionShouldReturnLoggedResponseStatusExceptionWhenInstanceOfResponseStatusException(CapturedOutput output) {
        ResponseStatusException responseStatusException = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test ResponseStatusException");

        Mono<String> result = Mono.error(() -> handleException("Test ResponseStatusException", responseStatusException));

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(exception.getReason(), containsString("Test ResponseStatusException"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test ResponseStatusException"));
        assertThat(output.toString(), containsString("Bad Request"));
        assertThat(output.toString(), containsString(ResponseStatusException.class.getName()));
    }

    @Test
    void handleExceptionShouldReturnLoggedResponseStatusExceptionWhenInstanceOfWebClientResponseException(CapturedOutput output) {
        WebClientResponseException webClientResponseException = WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", null, null, null, null);

        Mono<String> result = Mono.error(() -> handleException("Test WebClientResponseException", webClientResponseException));

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
                    assertThat(exception.getReason(), containsString("Test WebClientResponseException"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test WebClientResponseException"));
        assertThat(output.toString(), containsString("NOT_FOUND"));
        assertThat(output.toString(), containsString(WebClientResponseException.class.getName()));
    }

    @Test
    void handleExceptionShouldReturnLoggedResponseStatusExceptionForGeneralThrowable(CapturedOutput output) {
        Mono<String> result = Mono.error(() -> handleException("Test General Throwable", genericException));

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
                    assertThat(exception.getReason(), containsString("Test General Throwable"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test General Throwable"));
        assertThat(output.toString(), containsString("Internal Server Error"));
    }

    @Test
    void handleErrorResponseShouldReturnLoggedResponseStatusExceptionForClientErrors(CapturedOutput output) {
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body("{\"error\":\"bad_request\",\"error_description\":\"Bad Request Error\"}")
                .build();

        Mono<Throwable> result = handleErrorResponse(HttpStatus.BAD_REQUEST, "Test Client Error", clientResponse, "testContext");

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(exception.getReason(), containsStringIgnoringCase("Test Client Error"));
                    assertThat(exception.getReason(), containsStringIgnoringCase("bad_request"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test Client Error"));
        assertThat(output.toString(), containsString("bad_request"));
        assertThat(output.toString(), containsString("[Context: testContext]"));

    }

    @Test
    void handleErrorResponseShouldReturnLoggedResponseStatusExceptionForServerError(CapturedOutput output) {
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body("{\"error\":\"internal_error\",\"error_description\":\"Internal Server Error\"}")
                .build();

        Mono<Throwable> result = handleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Test Server Error", clientResponse, "testContext");

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
                    assertThat(exception.getReason(), containsStringIgnoringCase("Test Server Error"));
                    assertThat(exception.getReason(), containsStringIgnoringCase("internal_error"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test Server Error"));
        assertThat(output.toString(), containsString("internal_error"));
        assertThat(output.toString(), containsString("[Context: testContext]"));
    }

    @Test
    void handleErrorResponseShouldReturnLoggedResponseStatusExceptionForPlainTextError(CapturedOutput output) {
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain")
                .body("Plain text error")
                .build();

        Mono<Throwable> result = handleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Test Plain Text Error", clientResponse, "testContext");

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
                    assertThat(exception.getReason(), containsString("Test Plain Text Error"));
                    assertThat(exception.getReason(), containsString("Plain text error"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test Plain Text Error"));
        assertThat(output.toString(), containsString("Plain text error"));
        assertThat(output.toString(), containsString("[Context: testContext]"));

    }

    @Test
    void handleErrorResponseShouldLogAppropriateMessageForUnknownStatus(CapturedOutput output) {
        ClientResponse clientResponse = ClientResponse.create(HttpStatusCode.valueOf(999))
                .header("Content-Type", "application/json")
                .body("{\"error\":\"unknown_error\",\"error_description\":\"Unknown error\"}")
                .build();

        Mono<Throwable> result = handleErrorResponse(HttpStatusCode.valueOf(999), "Test Unknown Status Error", clientResponse, "testContext");

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable, instanceOf(LoggedResponseStatusException.class));
                    LoggedResponseStatusException exception = (LoggedResponseStatusException) throwable;
                    assertThat(exception.getStatusCode(), is(HttpStatusCode.valueOf(999)));
                    assertThat(exception.getReason(), containsStringIgnoringCase("Test Unknown Status Error"));
                    assertThat(exception.getReason(), containsStringIgnoringCase("unknown_error"));
                })
                .verify();

        assertThat(output.toString(), containsString("Test Unknown Status Error"));
        assertThat(output.toString(), containsString("unknown_error"));
        assertThat(output.toString(), containsString("[Context: testContext]"));
        assertThat(output.toString(), containsString("Unknown Status"));
    }

}