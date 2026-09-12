package gn.lacaeducation.communication.adapters.out.gateway;

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
 * Intercepteur de diagnostic TEMPORAIRE — journalise la requête/réponse brute envoyée à NimbaSMS
 * (en-tête Authorization + corps JSON sortant, statut + corps entrant), sur le même modèle que
 * {@code DjomyWireLoggingInterceptor}. Sert à diagnostiquer l'erreur « sender_name/to obligatoire »
 * observée en test réel : permet de voir exactement ce qui part sur le fil plutôt que de deviner.
 * À retirer une fois le diagnostic terminé.
 */
class NimbaWireLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger("NIMBA-WIRE");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info(">>> {} {}", request.getMethod(), request.getURI());
        log.info(">>> Content-Type: {}", request.getHeaders().getFirst("Content-Type"));
        log.info(">>> Authorization: {}", request.getHeaders().getFirst("Authorization"));
        log.info(">>> body:          {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);
        byte[] respBytes = response.getBody().readAllBytes();
        log.info("<<< status: {}", response.getStatusCode());
        log.info("<<< body:   {}", new String(respBytes, StandardCharsets.UTF_8));
        return new BufferedResponse(response, respBytes);
    }

    /** Re-sert le corps de réponse déjà consommé pour que la désérialisation en aval fonctionne toujours. */
    private record BufferedResponse(ClientHttpResponse delegate, byte[] body) implements ClientHttpResponse {
        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public void close() { delegate.close(); }
        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public HttpHeaders getHeaders() { return delegate.getHeaders(); }
    }
}
