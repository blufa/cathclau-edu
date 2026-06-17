package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * TEMPORARY diagnostic interceptor — logs the raw Djomy request/response (Bearer token + JSON body
 * out, status + JSON body in). Wired only on the Djomy RestClient. Remove after testing.
 */
class DjomyWireLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger("DJOMY-WIRE");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info(">>> {} {}", request.getMethod(), request.getURI());
        log.info(">>> Authorization: {}", request.getHeaders().getFirst("Authorization"));
        log.info(">>> X-API-KEY:     {}", request.getHeaders().getFirst("X-API-KEY"));
        log.info(">>> body:          {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);
        byte[] respBytes = response.getBody().readAllBytes();
        log.info("<<< status: {}", response.getStatusCode());
        log.info("<<< body:   {}", new String(respBytes, StandardCharsets.UTF_8));
        return new BufferedResponse(response, respBytes);
    }

    /** Re-serves the already-consumed response body so downstream deserialization still works. */
    private record BufferedResponse(ClientHttpResponse delegate, byte[] body) implements ClientHttpResponse {
        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public void close() { delegate.close(); }
        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public HttpHeaders getHeaders() { return delegate.getHeaders(); }
    }
}
