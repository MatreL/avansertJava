package no.kristiania.http;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class HttpClientTest{

    @Test
    void shouldReturnSuccessfulStatusCode() throws IOException {
        HttpClient client = new HttpClient("urlecho.appspot.com", 80, "/echo");
        assertEquals(200, client.getStatusCode());
    }
    @Test
    void shouldReturnErrorfulStatusCode() throws IOException {
        HttpClient client = new HttpClient("urlecho.appspot.com", 80, "/echo?status=404");
        assertEquals(404, client.getStatusCode());
    }
    @Test
    void shouldReadResponseHeader() throws IOException {
        HttpClient client = new HttpClient("urlecho.appspot.com", 80, "/echo?body=Kristiania");
        assertEquals("10", client.getResponseHeader("Content-Length"));
    }
    @Test
    void shouldReadResponseBody() throws IOException {
        HttpClient client = new HttpClient("urlecho.appspot.com", 80, "/echo?body=Kristiania");
        assertEquals("Kristiania", client.getResponseBody());
    }
}
