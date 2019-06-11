package database_servers;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

/**
 * Utility class to populate the relational database using an external file (media//populate_db//entry1.txt in this case)
 * @author Andrea Bugin and Ilie Sarpe
 */
public class PopulateDb
{
	public static void main(String args[])
	{
		final String clusterAdd = "127.0.0.1";
		String filePop = "entry1";
		
		try(Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/king", "postgres", "postgres");
			Scanner scan = new Scanner(new FileReader("media//populate_db//" + filePop));
			Cluster cluster = Cluster.builder().addContactPoint(clusterAdd).build();
			Session session = cluster.connect();)
		{
			session.execute("USE streaming;");
			
			String currentLine = scan.nextLine();
			if(!currentLine.equals("vid_path"))
			{
				System.out.println("Table not supported!");
			}
			while(scan.hasNextLine())//(currentLine = scan.readLine()) != null)
			{
				String line = scan.nextLine();
				String[] lineArray = line.split(" ");
				
				String id_vid = lineArray[0];
				String path_vid = lineArray[1];
				String ip = lineArray[2];
				int port = Integer.parseInt(lineArray[3]);
				
				String query = UtilitiesDb.insertUrlPath(id_vid, path_vid);
				System.out.println(query);
				//conn.createStatement().execute(query);
				
				query = UtilitiesDb.insertLocDb(id_vid, ip, port);
				session.execute(query);
				System.out.println(query);
			}
		}
		catch(SQLException sqle) 
		{
			sqle.printStackTrace();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}

