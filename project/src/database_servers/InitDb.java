/**
 * InitDb class: it creates the NOSql database "streaming" and the two tables "ip_cache" and "vid_location".
 * It creates also the relational database "king" and the only table "vid_path"
 * 
 * @author Andrea Bugin ad Ilie Sarpe
 *
 */

package database_servers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/* creates the table we need for querying the urls */
public class InitDb
{
	public static void main(String[] args)
	{
		Cluster cluster;
		Session session;
		
		Connection conn = null;

		try
		{
			cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			session = cluster.connect();

			// Instantiate the keyspace, with standard parameters
			session.execute("CREATE KEYSPACE IF NOT EXISTS streaming WITH replication = {"
					+ " 'class': 'SimpleStrategy', " + " 'replication_factor': '1' " + "};");
			
			// Connect to the keyspace already instatiated
			session.execute("USE streaming;");
			
			session.execute("CREATE TABLE IF NOT EXISTS ip_cache("
					+ "ip text,"
					+ "port int,"
					+ "PRIMARY KEY(ip, port));");
			
			session.execute("CREATE TABLE IF NOT EXISTS vid_location("
					+ "id_vid text,"
					+ "ip text,"
					+ "port int,"
					+ "PRIMARY KEY(id_vid));");
			
			session.close();
			cluster.close();
		} 
		catch (NoHostAvailableException nhae)
		{
			System.out.println("Build failed: <" + nhae.getMessage() + ">");
			nhae.printStackTrace();
			System.exit(1);
		}
		
		try
		{
			Class.forName("org.postgresql.Driver");
			System.out.println("JDBC Driver for PostreSQL loaded succesfully.");
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
			System.err.println("Driver not loaded!");
			System.exit(1);
		}

		try
		{
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/postgres", "postgres", "postgres"); 
			conn.createStatement().execute("CREATE DATABASE king;");
			
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/king", "postgres", "postgres");
			
			conn.createStatement().execute("CREATE TABLE vid_path(" + 
					"	id_vid VARCHAR(100)," + 
					"	path VARCHAR(1000) NOT NULL," + 
					"	PRIMARY KEY(id_vid)" + 
					");");
			conn.close();
		}
		catch(SQLException e)
		{
			System.err.println("Something went wrong with your SQL query. Here is the exception code: \n");
			e.printStackTrace();
		}
	}
}
