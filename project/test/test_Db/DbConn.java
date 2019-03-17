package test_Db;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/*Semplice programma per verificare la connettività a cassandra*/
public class DbConn
{

	public static void main(String[] args)
	{
		Cluster cluster = null;
		try
		{
			cluster = Cluster.builder() 
					.addContactPoint("127.0.0.1").build(); // (1)
			Session session = cluster.connect(); // (2)

			ResultSet rs = session.execute("select release_version from system.local"); // (3)
			Row row = rs.one();
			System.out.println(row.getString("release_version")); // (4)
			System.out.println("Ciao");
		} 
		finally
		{
			if (cluster != null)
			{
				cluster.close(); // (5)
			}
		}
	}

}