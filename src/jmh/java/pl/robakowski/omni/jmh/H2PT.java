package pl.robakowski.omni.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

@Fork( 0 )
@Warmup( time = 2, iterations = 5 )
@Measurement( time = 5, iterations = 3 )
@Threads( 4 )
public class H2PT
{
    @State( Scope.Thread )
    public static class TestState
    {
        private Connection conn;
        public PreparedStatement stmt;

        @Setup
        public void setup() throws ClassNotFoundException, SQLException
        {
            Class.forName( "org.h2.Driver" );
            conn = DriverManager.getConnection( "jdbc:h2:./build/test" );
            Statement statement = conn.createStatement();

            try
            {
                statement.executeUpdate( "CREATE TABLE PERSON (age INTEGER)" );
                statement.executeUpdate( "INSERT INTO PERSON VALUES (0)" );
            }
            catch( Exception e )
            {
            }

            stmt = conn.prepareStatement( "update Person set age = age + 1" );
        }

        @TearDown
        public void tearDown() throws SQLException
        {
            stmt.close();
            conn.close();
        }
    }

    //@Benchmark
    public void testMethod( TestState s ) throws ExecutionException, InterruptedException, SQLException
    {
        s.stmt.executeUpdate();
    }
}