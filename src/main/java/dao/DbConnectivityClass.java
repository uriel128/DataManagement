package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Major;
import model.Person;
import service.MyLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbConnectivityClass {
    private static final String DEFAULT_AZURE_JDBC = "jdbc:sqlserver://csc311server.database.windows.net:1433;database=CSC311DB;user=vasqf@csc311server;password=Uriel0128;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    private final String azureJdbcUrl;
    private final String derbyPath;

    private DatabaseMode databaseMode;

    private enum DatabaseMode {
        AZURE_SQL_SERVER,
        DERBY
    }

    public DbConnectivityClass() {
        this.azureJdbcUrl = sanitizeConnectionString(readConfig("app.azure.jdbc", "APP_AZURE_JDBC", DEFAULT_AZURE_JDBC));
        this.derbyPath = readConfig("app.derby.path", "APP_DERBY_PATH",
                System.getProperty("user.home") + "/Documents/CSC311_DERBY_DB");
        this.databaseMode = parseMode(readConfig("app.db.mode", "APP_DB_MODE", "AZURE"));
    }

    public ObservableList<Person> getData() {
        connectToDatabase();
        ObservableList<Person> data = FXCollections.observableArrayList();

        String sql = "SELECT * FROM users";
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String department = resultSet.getString("department");
                Major major = Major.fromValue(resultSet.getString("major"));
                String email = resultSet.getString("email");
                String imageUrl = resultSet.getString("imageURL");
                data.add(new Person(id, firstName, lastName, department, major, email, imageUrl));
            }

        } catch (SQLException e) {
            MyLogger.makeLog("Error loading users: " + e.getMessage());
        }

        return data;
    }

    public boolean connectToDatabase() {
        boolean hasRegisteredUsers = false;

        try {
            ensureDriverLoaded();
            if (databaseMode == DatabaseMode.AZURE_SQL_SERVER) {
                hasRegisteredUsers = initAzureSchema();
            } else {
                hasRegisteredUsers = initDerbySchema();
            }
        } catch (Exception e) {
            if (databaseMode == DatabaseMode.AZURE_SQL_SERVER) {
                MyLogger.makeLog("Azure SQL unavailable, switching to Derby: " + e.getMessage());
                databaseMode = DatabaseMode.DERBY;
                try {
                    ensureDriverLoaded();
                    hasRegisteredUsers = initDerbySchema();
                } catch (Exception derbyError) {
                    MyLogger.makeLog("Derby setup failed: " + derbyError.getMessage());
                }
            } else {
                MyLogger.makeLog("Database setup failed: " + e.getMessage());
            }
        }

        return hasRegisteredUsers;
    }

    public int insertUser(Person person) {
        connectToDatabase();

        if (databaseMode == DatabaseMode.AZURE_SQL_SERVER) {
            String sql = "INSERT INTO users (first_name, last_name, department, major, email, imageURL) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = openConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

                preparedStatement.setString(1, person.getFirstName());
                preparedStatement.setString(2, person.getLastName());
                preparedStatement.setString(3, person.getDepartment());
                preparedStatement.setString(4, person.getMajor().name());
                preparedStatement.setString(5, person.getEmail());
                preparedStatement.setString(6, person.getImageURL());

                try (ResultSet keys = preparedStatement.executeQuery()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            } catch (SQLException e) {
                MyLogger.makeLog("Insert failed: " + e.getMessage());
            }
            return -1;
        }

        String derbySql = "INSERT INTO users (first_name, last_name, department, major, email, imageURL) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(derbySql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, person.getFirstName());
            preparedStatement.setString(2, person.getLastName());
            preparedStatement.setString(3, person.getDepartment());
            preparedStatement.setString(4, person.getMajor().name());
            preparedStatement.setString(5, person.getEmail());
            preparedStatement.setString(6, person.getImageURL());

            int row = preparedStatement.executeUpdate();
            if (row > 0) {
                try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            MyLogger.makeLog("Insert failed: " + e.getMessage());
        }

        return -1;
    }

    public boolean editUser(int id, Person person) {
        connectToDatabase();
        String sql = "UPDATE users SET first_name=?, last_name=?, department=?, major=?, email=?, imageURL=? WHERE id=?";

        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, person.getFirstName());
            preparedStatement.setString(2, person.getLastName());
            preparedStatement.setString(3, person.getDepartment());
            preparedStatement.setString(4, person.getMajor().name());
            preparedStatement.setString(5, person.getEmail());
            preparedStatement.setString(6, person.getImageURL());
            preparedStatement.setInt(7, id);

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            MyLogger.makeLog("Update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRecord(Person person) {
        connectToDatabase();
        String sql = "DELETE FROM users WHERE id=?";

        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setInt(1, person.getId());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            MyLogger.makeLog("Delete failed: " + e.getMessage());
            return false;
        }
    }

    public int countByMajor(Major major) {
        connectToDatabase();
        String sql = "SELECT COUNT(*) FROM users WHERE major = ?";

        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setString(1, major.name());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            MyLogger.makeLog("Count by major failed: " + e.getMessage());
        }

        return 0;
    }

    public String getActiveDatabaseMode() {
        return databaseMode.name();
    }

    private boolean initAzureSchema() throws SQLException {
        String sql = "IF OBJECT_ID('dbo.users', 'U') IS NULL "
                + "CREATE TABLE users ("
                + "id INT IDENTITY(1,1) PRIMARY KEY,"
                + "first_name NVARCHAR(200) NOT NULL,"
                + "last_name NVARCHAR(200) NOT NULL,"
                + "department NVARCHAR(200) NOT NULL,"
                + "major NVARCHAR(20) NOT NULL CHECK (major IN ('CS','CPIS','ENGLISH')),"
                + "email NVARCHAR(200) NOT NULL UNIQUE,"
                + "imageURL NVARCHAR(300)"
                + ")";

        try (Connection conn = DriverManager.getConnection(azureJdbcUrl);
             Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        }

        return hasRows(true);
    }

    private boolean initDerbySchema() throws SQLException {
        String sql = "CREATE TABLE users ("
                + "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,"
                + "first_name VARCHAR(200) NOT NULL,"
                + "last_name VARCHAR(200) NOT NULL,"
                + "department VARCHAR(200) NOT NULL,"
                + "major VARCHAR(20) NOT NULL,"
                + "email VARCHAR(200) NOT NULL UNIQUE,"
                + "imageURL VARCHAR(300),"
                + "CONSTRAINT major_allowed CHECK (major IN ('CS','CPIS','ENGLISH'))"
                + ")";

        try (Connection conn = DriverManager.getConnection(getDerbyUrl());
             Statement statement = conn.createStatement()) {
            if (!tableExists(conn, "USERS")) {
                statement.executeUpdate(sql);
            }
        }

        return hasRows(false);
    }

    private boolean hasRows(boolean azure) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Connection conn = azure
                ? DriverManager.getConnection(azureJdbcUrl)
                : DriverManager.getConnection(getDerbyUrl());
             PreparedStatement preparedStatement = conn.prepareStatement(countSql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)) {
            return resultSet.next();
        }
    }

    private Connection openConnection() throws SQLException {
        if (databaseMode == DatabaseMode.AZURE_SQL_SERVER) {
            return DriverManager.getConnection(azureJdbcUrl);
        }
        return DriverManager.getConnection(getDerbyUrl());
    }

    private String getDerbyUrl() {
        return "jdbc:derby:" + derbyPath + ";create=true";
    }

    private void ensureDriverLoaded() throws ClassNotFoundException {
        if (databaseMode == DatabaseMode.AZURE_SQL_SERVER) {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } else {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        }
    }

    private static DatabaseMode parseMode(String value) {
        if ("AZURE".equalsIgnoreCase(value)
                || "AZURE_SQL_SERVER".equalsIgnoreCase(value)
                || "MSSQL".equalsIgnoreCase(value)
                || "SQLSERVER".equalsIgnoreCase(value)) {
            return DatabaseMode.AZURE_SQL_SERVER;
        }
        return DatabaseMode.DERBY;
    }

    private static String readConfig(String propertyKey, String envKey, String defaultValue) {
        String fromProperty = System.getProperty(propertyKey);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }

        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        return defaultValue;
    }

    private static String sanitizeConnectionString(String jdbc) {
        if (jdbc == null) {
            return "";
        }
        return jdbc.replace('“', '"')
                .replace('”', '"')
                .replace("\"", "")
                .trim();
    }
}
