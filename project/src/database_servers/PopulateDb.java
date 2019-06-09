package database_servers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

public class PopulateDb
{
	final static String clusterAdd = "127.0.0.1";
	final static String keyspace = " streaming;";
	
	public static void main(String args[])
	{
		// pass the name of the file that specifies the entries to be put in the database i.e. "entry1" without quotes.
		// The file is structured like this : <name_table> e.g. <name_table> = vid_path
		// id_i value_i\n for each i=1,...,n (observe that for now only vid_path is supported)

		String filePop = "entry1"; 
		try
		{
			Cluster cluster = null;
			Session session = null;
			String currentLine = null;
			
			File input = new File("media//populate_db//" + filePop);
			BufferedReader reader = new BufferedReader(new FileReader(input));
			currentLine = reader.readLine().trim();
			
			if(currentLine != null)
			{
				try 
				{
					// Initialize connection 
					cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
					session = cluster.connect();
					String entry = null;
					session.execute("USE" + keyspace);
					if(currentLine.equals("vid_path"))
					{
						while((currentLine = reader.readLine()) != null)
						{
							entry = UtilitiesDb.insertUrlPath(currentLine.split(" ")[0].trim(), currentLine.split(" ")[1].trim());
							session.execute(entry);
							System.out.println(entry);
						}
					}
					else
					{
						System.out.println("Table not supported!");
					}
					//once inserting is done we can close the connection
					session.close();
					cluster.close();
					
				}
				catch (NoHostAvailableException nhae)
				{
					nhae.printStackTrace();
				} 
				catch (NullPointerException npe)
				{
					npe.printStackTrace();
				}
				finally 
				{
					reader.close();
				}
			}
			else
			{
				System.out.println("Format not respected!");
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}

