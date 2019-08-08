import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class LoadPhoenix {

    private static final int RANGE = 500000;
    private static final int BATCH = 10000;

    public static void main(String[] args) throws SQLException {
        Statement stmt;
        ResultSet rset = null;
        int[] countWithoutException;

        try (Connection con = DriverManager.getConnection("jdbc:phoenix:hadoop.example.com:2181:/hbase")) {
            stmt = con.createStatement();

            stmt.executeUpdate("create table if not exists LARGETBL (mykey integer "
                    + "not null primary key, mycolumn varchar) salt_buckets = 10");

            for (int i = 1; i <= RANGE; i++) {
                stmt.addBatch("upsert into LARGETBL values (" + i + ",'" + UUID.randomUUID().toString() + "')");

                if ((i % BATCH) == 0) {
                    countWithoutException = stmt.executeBatch();
                    System.out.printf("loaded: %d total: %d\n", countWithoutException.length, i);
                }
            }
            con.commit();
        }
    }
}
