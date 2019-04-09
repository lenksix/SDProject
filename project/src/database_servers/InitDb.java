/**
 * InitDb class: it creates the database "streaming" and creates the two tables.
 * 
 * @author Andrea Bugin ad Ilie Sarpe
 *
 */

package database_servers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/* creates the table we need for querying the urls */
class InitDb
{
	public static void main(String[] args)
	{
		Cluster cluster;
		Session session;

		// run cassandra in background if it is not (useful in Windows for example)
		/*try
		{
			Runtime.getRuntime().exec("cassandra -f");
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}*/

		try
		{
			cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			session = cluster.connect();

			// Instantiate the keyspace, with standard parameters
			session.execute("CREATE KEYSPACE IF NOT EXISTS streaming WITH replication = {"
					+ " 'class': 'SimpleStrategy', " + " 'replication_factor': '1' " + "};");
			// Connect to the keyspace already instatiated
			session.execute("USE streaming;");
			// Create the table
			session.execute("CREATE TABLE IF NOT EXISTS channel_vids("
					+ "channel_name text," 
					+ "vids set<text>, " // where list is a list of url
					+ "PRIMARY KEY(channel_name));");

			// create the table of url-path for each video
			session.execute("CREATE TABLE IF NOT EXISTS vid_path("
					+ "url text," 
					+ "path text," 
					+ "PRIMARY KEY(url));");
			
			session.execute("CREATE TABLE IF NOT EXISTS ip_cache("
					+ "ip text,"
					+ "port int,"
					+ "PRIMARY KEY(ip, port));");
			
			// System.out.println("Table vid_path created!");
			session.close();
			cluster.close();
		} 
		catch (NoHostAvailableException nhae)
		{
			System.out.println("Build failed: <" + nhae.getMessage() + ">");
			nhae.printStackTrace();
			System.exit(1);
		}
	}
}
