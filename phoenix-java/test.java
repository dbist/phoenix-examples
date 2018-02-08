import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class test {

	public static void main(String[] args) throws SQLException {
		Statement stmt = null;
		ResultSet rset = null;

		Connection con = DriverManager.getConnection("jdbc:phoenix:hadoop.example.com:2181:/hbase");
		stmt = con.createStatement();

		stmt.executeUpdate("create table if not exists test (mykey integer not null primary key, mycolumn varchar)");
		stmt.executeUpdate("upsert into test values (1,'Hello')");
		stmt.executeUpdate("upsert into test values (2,'World!')");
		con.commit();

		PreparedStatement statement = con.prepareStatement("select * from test");
		rset = statement.executeQuery();
		while (rset.next()) {
			System.out.println(rset.getString("mycolumn"));
		}
		statement.close();
		con.close();
	}
}
