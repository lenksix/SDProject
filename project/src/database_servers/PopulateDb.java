package database_servers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class to populate the relational database using an external file (media//populate_db//entry1.txt in this case)
 * @author Andrea Bugin and Ilie Sarpe
 */
public class PopulateDb
{	
	public static void main(String args[])
	{
		String filePop = "entry1"; 
		try(Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/king", "postgres", "postgres");
			BufferedReader reader = new BufferedReader(new FileReader("media//populate_db//" + filePop));)
		{
			String currentLine = reader.readLine().trim();
			
			if(currentLine != null)
			{
				if(currentLine.equals("vid_path"))
				{
					while((currentLine = reader.readLine()) != null)
					{
						String query = UtilitiesDb.insertUrlPath(currentLine.split(" ")[0].trim(), currentLine.split(" ")[1].trim());
						conn.createStatement().execute(query);
						System.out.println(query);
					}
				}
				else
				{
					System.out.println("Table not supported!");
				}
			}
			else
			{
				System.out.println("Format not respected!");
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

