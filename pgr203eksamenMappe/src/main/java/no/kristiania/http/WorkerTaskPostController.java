package no.kristiania.http;


import no.kristiania.database.WorkerTask;
import no.kristiania.database.WorkerTaskDao;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class WorkerTaskPostController implements HttpController {
    private WorkerTaskDao workerTaskDao;
    public WorkerTaskPostController(WorkerTaskDao workerTaskDao) {
        this.workerTaskDao = workerTaskDao;
    }

    @Override
    public void handle(HttpMessage request, Socket clientSocket) throws IOException, SQLException {
        QueryString requestParameter = new QueryString(request.getBody());

        WorkerTask task = new WorkerTask();
        task.setName(requestParameter.getParameter("taskName"));
        workerTaskDao.insert(task);

        String body = "Okay";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }
}
