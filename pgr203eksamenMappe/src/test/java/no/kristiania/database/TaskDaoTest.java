package no.kristiania.database;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskDaoTest {
    private WorkerTaskDao TaskDao;
    private static Random random = new Random();

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).load().migrate();
        TaskDao = new WorkerTaskDao(dataSource);
    }
    @Test
    void shouldListAllTasks() throws SQLException {
        WorkerTask task1= exampleTask();
        WorkerTask task2= exampleTask();
        TaskDao.insert(task1);
        TaskDao.insert(task2);
        assertThat(TaskDao.list())
                .extracting(WorkerTask::getName)
                .contains(task1.getName(), task2.getName());
    }


    @Test
    void shouldRetrieveAllTaskProperties() throws SQLException {
        TaskDao.insert(exampleTask());
        TaskDao.insert(exampleTask());
        WorkerTask task = exampleTask();
        TaskDao.insert(task);
        assertThat(task).hasNoNullFieldsOrProperties();

        assertThat(TaskDao.retrieve(task.getId()))
                .usingRecursiveComparison()
                .isEqualTo(task);


    }

    public static WorkerTask exampleTask() {
        WorkerTask task = new WorkerTask();
        task.setName(exampleTaskName());
        return task;
    }

    private static String exampleTaskName(){
        String[] options = {"urgent", "started", "finished"};
        return options[random.nextInt(options.length)];
    }

}
