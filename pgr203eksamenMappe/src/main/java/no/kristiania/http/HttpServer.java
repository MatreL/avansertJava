package no.kristiania.http;

import no.kristiania.database.Worker;
import no.kristiania.database.WorkerDao;
import no.kristiania.database.WorkerTaskDao;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private Map<String, HttpController> controllers;

    private WorkerDao workerDao;
    private final ServerSocket serverSocket;

    public HttpServer(int port, DataSource dataSource) throws IOException {
        workerDao = new WorkerDao(dataSource);
        WorkerTaskDao workerTaskDao = new WorkerTaskDao(dataSource);
        controllers = Map.of(
                "/api/newTask", new WorkerTaskPostController(workerTaskDao),
                "/api/tasks", new WorkerTaskGetController(workerTaskDao),
                "/api/taskOptions", new WorkerTaskOptionsController(workerTaskDao),
                "/api/workersOptions", new WorkerOptionsController(workerDao),
                "/api/updateWorker", new UpdateWorkerController(workerDao)
        );
        // Opens a entry point to our program for network clients
        serverSocket = new ServerSocket(port);
        logger.warn("Server startet on port {}", serverSocket.getLocalPort());

        // new Threads executes the code in a separate "thread", that is: In parallel
        new Thread(() -> { // anonymous function with code that will be executed in parallel
            while (true) {
                try(Socket clientSocket = serverSocket.accept()) {
                    // accept waits for a client to try to connect - blocks
                    handleRequest(clientSocket);
                } catch (IOException | SQLException e) {
                    // If something went wrong - print out exception and try again
                    e.printStackTrace();
                }
            }
        }).start(); // Start the threads, so the code inside executes without block the current thread

    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    // This code will be executed for each client
    private void handleRequest(Socket clientSocket) throws IOException, SQLException {

        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println("REQUEST " + requestLine);
        // Example "GET /echo?body=hello HTTP/1.1"

        // Example GET, POST, PUT, DELETE etc
        String requestMethod = requestLine.split(" ")[0];

        String requestTarget = requestLine.split(" ")[1];
        // Example "/echo?body=hello"

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            if (requestPath.equals("/api/newWorker")){
                handlePostWorker(clientSocket, request);
            }else{
                getController(requestPath).handle(request, clientSocket);
            }
        } else {
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else if (requestPath.equals("/api/workers")) {
                handleGetWorkers(clientSocket);
            } else {
                HttpController controller =  controllers.get(requestPath);
                if (controller != null ){
                    controller.handle(request, clientSocket);
                }else{
                    handleFileRequest(clientSocket, requestPath);
                }
            }
        }
    }

    private HttpController getController(String requestPath) {
        return controllers.get(requestPath);
    }

    private void handlePostWorker(Socket clientSocket, HttpMessage request) throws SQLException, IOException {
        QueryString requestParameter = new QueryString(request.getBody());

        Worker worker = new Worker();
        worker.setFirstName(requestParameter.getParameter("first_name"));
        worker.setLastName(requestParameter.getParameter("last_name"));
        worker.setEmail(requestParameter.getParameter("email_address"));
        workerDao.insert(worker);
        String body = "Okay";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        clientSocket.getOutputStream().write(response.getBytes());
    }

    private void handleFileRequest(Socket clientSocket, String requestPath) throws IOException{
        try(InputStream inputStream = getClass().getResourceAsStream(requestPath)){
            if (inputStream == null){
                String body = requestPath = "does not exist";
                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        body;
                clientSocket.getOutputStream().write(response.getBytes());
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            inputStream.transferTo(buffer);

            String contentType = "text/plain";
            if (requestPath.endsWith(".html")) {
                contentType = "text/html";
            }if(requestPath.endsWith(".css")) {
                contentType = "text/css";
            }
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + buffer.toByteArray().length + "\r\n" +
                    "Connection: close\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";
            clientSocket.getOutputStream().write(response.getBytes());
            clientSocket.getOutputStream().write(buffer.toByteArray());
        }
    }

    private void handleGetWorkers (Socket clientSocket) throws IOException, SQLException {
        String body = "<ul>";
        for (Worker worker : workerDao.list()) {
            body += "<li>" + "Name: "+ worker.getFirstName() + " " + worker.getLastName()+ "</li>" + "<li>" + "Email: " + worker.getEmail() + "</li>";
        }
        body += "</ul>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
    }

    private void handleEchoRequest(Socket clientSocket, String requestTarget, int questionPos) throws IOException {
        String statusCode = "200";
        String body = "Hello <strong>World</strong>!";
        if (questionPos != -1) {
            // body=hello
            QueryString queryString = new QueryString(requestTarget.substring(questionPos + 1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                body;

        // Write the response back to the client
        clientSocket.getOutputStream().write(response.getBytes());
    }

    public static void main(String[] args) throws IOException, SQLException {
        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader("pgr203.properties")){
            properties.load(fileReader);
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(properties.getProperty("dataSource.url"));
        dataSource.setUser(properties.getProperty("dataSource.username"));
        dataSource.setPassword(properties.getProperty("dataSource.password"));
        logger.info("Using database {}", dataSource.getUrl());
        Flyway.configure().dataSource(dataSource).load().migrate();


        HttpServer server = new HttpServer(8080, dataSource);
        logger.info("Started on http://localhost:{}/index.html", 8080);
    }

    public List<Worker> getFirstName() throws SQLException{
        return workerDao.list();
    }
}