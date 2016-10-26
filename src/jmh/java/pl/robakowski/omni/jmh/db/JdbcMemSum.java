package pl.robakowski.omni.jmh.db;

import org.openjdk.jmh.annotations.*;

import java.sql.*;
import java.util.concurrent.ExecutionException;

public class JdbcMemSum {

    @State(Scope.Thread)
    public static class TestState {
        public Connection conn;
        public PreparedStatement stmt;

        @Setup
        public void setup() throws ClassNotFoundException, SQLException {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:mem:test2");
            Statement statement = conn.createStatement();

            try {
                statement.executeUpdate("CREATE TABLE PERSON (age INTEGER)");
                statement.executeUpdate("INSERT INTO PERSON VALUES (0)");
            } catch (Exception e) {
            }

            stmt = conn.prepareStatement("insert into Person values(1)");
        }

        @TearDown
        public void tearDown() throws SQLException {
            stmt.close();
            conn.close();
        }
    }

    @Benchmark
    public int test(TestState s) throws ExecutionException, InterruptedException, SQLException {
        s.stmt.executeUpdate();

        Statement st = s.conn.createStatement();
        ResultSet result = st.executeQuery("select sum(age) from person");

        result.next();
        int i = result.getInt(1);

        result.close();
        st.close();

        return i;
    }
}