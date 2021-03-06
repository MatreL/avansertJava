package no.kristiania.http;

import no.kristiania.database.Worker;
import no.kristiania.database.WorkerDao;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    private JdbcDataSource dataSource;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        Flyway.configure().dataSource(dataSource).load().migrate();
        server = new HttpServer(0, dataSource);
    }


    @Test
    void shouldReturnSuccessfulStatusCode() throws IOException {
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo");
        assertEquals(200, client.getStatusCode());
    }

    @Test
    void shouldReturnUnsuccessfulStatusCode() throws IOException{
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?status=404");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldReturnContentLength() throws IOException{
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("10", client.getResponseHeader("Content-Length"));
    }

    @Test
    void shouldReturnResponseBody() throws IOException{
        HttpClient client = new HttpClient("localhost", server.getPort(), "/echo?body=HelloWorld");
        assertEquals("HelloWorld", client.getResponseBody());
    }

    @Test
    void shouldReturnFileFromDisk() throws IOException{
        File contentRoot = new File("target/test-classes");

        String fileContent = "Hello World " + new Date();
        Files.writeString(new File(contentRoot, "test.txt").toPath(), fileContent);

        HttpClient client = new HttpClient("localhost", server.getPort(), "/test.txt");
        assertEquals(fileContent, client.getResponseBody());
        assertEquals("text/plain", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturnCorrectContentType() throws IOException{
        File contentRoot = new File("target/test-classes");

        Files.writeString(new File(contentRoot, "index.html").toPath(), "<h2>Hello World</h2>");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/index.html");
        assertEquals("text/html", client.getResponseHeader("Content-Type"));
    }

    @Test
    void shouldReturn404IfFileNotFound() throws IOException{
        new File("target/test-classes");

        HttpClient client = new HttpClient("localhost", server.getPort(), "/notFound.txt");
        assertEquals(404, client.getStatusCode());
    }

    @Test
    void shouldPostNewWorkers() throws IOException, SQLException {
        String requestBody = "first_name=Even&last_name=Larsen&email_address=even@even.no";
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/newWorker", "POST", requestBody);
        assertEquals(200, client.getStatusCode());
        assertThat(server.getFirstName())
                .filteredOn(worker -> worker.getFirstName().equals("Even"))
                .isNotEmpty()
                .satisfies(w -> assertThat(w.get(0).getEmail()).isEqualTo("even@even.no"));

    }
    @Test
    void shouldReturnExistingWorkers() throws IOException, SQLException {
        WorkerDao workerDao = new WorkerDao(dataSource);
        Worker worker = new Worker();
        worker.setFirstName("Even");


        worker.setLastName("Larsen");
        worker.setEmail("even@even.no");
        workerDao.insert(worker);
        HttpClient client = new HttpClient("localhost", server.getPort(), "/api/workers");
        assertThat(client.getResponseBody()).contains("<li>Name: Even Larsen</li><li>Email: even@even.no</li>");
    }

    @Test
    void shouldPostNewTask() throws IOException, SQLException {
        String requestBody = "taskName=urgent&color=red";
        HttpClient postClient = new HttpClient("localhost", server.getPort(), "/api/newTask", "POST", requestBody);
        assertEquals(200, postClient.getStatusCode());

        HttpClient getClient = new HttpClient("localhost", server.getPort(), "/api/tasks");
        assertThat(getClient.getResponseBody()).contains("<li>urgent</li>");
    }

}