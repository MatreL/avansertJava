package no.kristiania.database;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkerTaskDao extends AbstractDao<WorkerTask>{

    public WorkerTaskDao(DataSource dataSource) {
        super (dataSource);
    }

    public void insert(WorkerTask task) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO worker_tasks (name) VALUES(?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, task.getName());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    generatedKeys.next();
                    task.setId(generatedKeys.getInt("id"));
                }
            }
        }
    }

    public WorkerTask retrieve(Integer id) throws SQLException {
        return retrieve(id, "SELECT * FROM worker_tasks WHERE id = ?");

    }

    public List<WorkerTask> list() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("select * from worker_tasks")) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<WorkerTask> workers = new ArrayList<>();
                    while (rs.next()) {
                        workers.add(mapRow(rs));
                    }
                    return workers;
                }
            }
        }
    }

    @Override
    protected WorkerTask mapRow(ResultSet rs) throws SQLException {
        WorkerTask task = new WorkerTask();
        task.setId(rs.getInt("id"));
        task.setName(rs.getString("name"));

        return task;
    }
}
