package no.kristiania.database;

import no.kristiania.http.HttpMessage;
import no.kristiania.http.UpdateWorkerController;
import no.kristiania.http.WorkerOptionsController;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkerDaoTest {

    private WorkerDao workerDao;
    private static Random random = new Random();
    private WorkerTaskDao taskDao;

    @BeforeEach
    void setUp(){
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).load().migrate();
        workerDao = new WorkerDao(dataSource);
        taskDao = new WorkerTaskDao(dataSource);
    }
    @Test
    void shouldListInsertedProducts() throws SQLException {
        Worker worker1 = exampleWorker();
        Worker worker2 = exampleWorker();
        workerDao.insert(worker1);
        workerDao.insert(worker2);
        assertThat(workerDao.list())
                .extracting(Worker::getFirstName)
                .contains(worker1.getFirstName(), worker2.getFirstName());
    }

    @Test
    void shouldRetrieveAllWorkerProperties() throws SQLException{
        workerDao.insert(exampleWorker());
        workerDao.insert(exampleWorker());
        Worker worker = exampleWorker();
        workerDao.insert(worker);
        assertThat(worker).hasNoNullFieldsOrPropertiesExcept("taskId");
        assertThat(workerDao.retrieve(worker.getId()))
                .usingRecursiveComparison()
                .isEqualTo(worker);
    }

    @Test
    void shouldReturnWorkersAsOptions() throws SQLException {
        WorkerOptionsController controller = new WorkerOptionsController(workerDao);
        Worker worker = WorkerDaoTest.exampleWorker();
        workerDao.insert(worker);

        assertThat(controller.getBody())
                .contains("<option value=" + worker.getId() + ">" + worker.getFirstName() + "</option>");
    }

    @Test
    void shouldUpdateExistingWorkerWithTask() throws IOException, SQLException {
        UpdateWorkerController controller = new UpdateWorkerController(workerDao);

        Worker worker = exampleWorker();
        workerDao.insert(worker);

        WorkerTask task = TaskDaoTest.exampleTask();
        taskDao.insert(task);

        String body = "workerId=" + worker.getId() +"&taskId=" + task.getId();

        HttpMessage response = controller.handle(new HttpMessage(body));
        assertThat(workerDao.retrieve(worker.getId()).getTaskId())
                .isEqualTo(task.getId());
        assertThat(response.getStartLine())
                .isEqualTo("HTTP/1.1 302 Redirect");
        assertThat(response.getHeaders().get("Location"))
                .isEqualTo("http://localhost:8080/index.html");

    }


    public static Worker exampleWorker(){
        Worker worker = new Worker();
        worker.setFirstName(exampleWorkerName());
        worker.setLastName(exampleWorkerName());
        worker.setEmail("even@even.no");
        return worker;
    }

    private static String exampleWorkerName(){
        String[] options = {"even", "matre"};
        return options[random.nextInt(options.length)];
    }
}
