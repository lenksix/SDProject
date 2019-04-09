package database_servers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

/**
 * The class ManageCS (Manage cache servers) is used to manage the active server cache, 
 * adding them in the database as ip-port record and ping periodically to verify how many of them are active,
 * removing the record whether one of them is not.
 * 
 * @author Andrea Bugin and Ilie Sarpe
 */
public class ManageCS 
{
	final static String clusterAdd = "127.0.0.1";
	Cluster cluster = null;
	Session session = null;
	
	public static void main(String[] args)
	{
		(new ManageCS()).exec(args);
	}

	void exec(String[] args) 
	{
		try
		{
			cluster = Cluster.builder().addContactPoint(clusterAdd).build();
			session = cluster.connect();
			session.execute("USE streaming;");
			
			String q = UtilitiesDb.selectFromIPCache();
			ResultSet rSet = session.execute(q);
			
			System.out.println(q);
			//rSet.forEach(System.out::println);
			
			//List<ColumnDefinitions.Definition> column = rSet.getColumnDefinitions().asList();
			//column.forEach(x->System.out.println(x.getName()));
			
			rSet.forEach(row -> {
				System.out.println(row.getString("ip") + " " + row.getInt("port"));
			});
		}
		finally 
		{
			cluster.close();
			session.close();
		}
	}
}
