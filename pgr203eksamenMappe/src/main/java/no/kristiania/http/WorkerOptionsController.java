package no.kristiania.http;

import no.kristiania.database.Worker;
import no.kristiania.database.WorkerDao;


import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class WorkerOptionsController implements HttpController{
    private WorkerDao workerDao;

    public WorkerOptionsController(WorkerDao workerDao) {
        this.workerDao = workerDao;
    }

    @Override
    public void handle(HttpMessage request, Socket clientSocket) throws IOException, SQLException {
        HttpMessage response = new HttpMessage(getBody());
        response.write(clientSocket);
    }

    public String getBody() throws SQLException {
        String body = "";
        for (Worker worker : workerDao.list()) {
            body += "<option value=" + worker.getId() + ">" + worker.getFirstName() + "</option>";
        }

        return body;
    }
}
