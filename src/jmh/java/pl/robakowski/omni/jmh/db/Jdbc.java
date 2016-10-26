package pl.robakowski.omni.jmh.db;

import org.openjdk.jmh.annotations.*;

import java.sql.*;
import java.util.concurrent.ExecutionException;

public class Jdbc {

    @State(Scope.Thread)
    public static class TestState {
        private Connection conn;
        public PreparedStatement stmt;

        @Setup
        public void setup() throws ClassNotFoundException, SQLException {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:./build/jdbc");
            Statement statement = conn.createStatement();

            try {
                statement.executeUpdate("CREATE TABLE PERSON (age INTEGER)");
                statement.executeUpdate("INSERT INTO PERSON VALUES (0)");
            } catch (Exception e) {
            }

            stmt = conn.prepareStatement("update Person set age = age + 1");
        }

        @TearDown
        public void tearDown() throws SQLException {
            stmt.close();
            conn.close();
        }
    }

    @Benchmark
    public void test(TestState s) throws ExecutionException, InterruptedException, SQLException {
        s.stmt.executeUpdate();
    }
}