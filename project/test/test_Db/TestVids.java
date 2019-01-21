/**
 * TestVids class: used to test the queries to the database
 * @author Andrea Bugin and Ilie Sarpe
 */

package test_Db;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import database_servers.UtilitiesDb;

class TestVids
{
	public static void main(String[] args) 
	{
		Cluster cluster;
		Session session;
		
		cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		
		session = cluster.connect();
		
		String query = null;
		
		session.execute("USE streaming;");
		
		
		// if it's the first time you execute the class, uncomment the following lines
		query = UtilitiesDb.insertChannelVids("dellimellow", "https://www.youtube.com/watch?v=gVpYcpmNRb4");
		session.execute(query);
		System.out.println(query);
		
		// this query is used to update a row in the database  
		query = UtilitiesDb.updateChannelVids("dellimellow", "https://www.youtube.com/watch?v=gVpYcpmNRb5");
		
		session.execute(query);
		
		//query to select all the records in the channel_vids
		ResultSet results = session.execute("SELECT vids FROM channel_vids WHERE channel_name='dellimellow';");
		System.out.println("Results = " + results);
		
		for(Row row: results)
		{
			System.out.println(row);
		}
		
		
		query = UtilitiesDb.insertUrlPath("prova", "path da specificare");
      	System.out.println(query);
      	
      	session.execute(query);
      	results = session.execute("SELECT * FROM vid_path;");
      	
      	for(Row row: results)
      	{
      		System.out.println(row);
      	} 
      	
      	/*
      	Map<String, String> m = null;
      	
      	for(Row row: results)
      	{
      		System.out.println(row);
      		m = row.getMap("vids", TypeToken.of(String.class), TypeToken.of(String.class)); //this returns a Map for each row in the table
      		//this lambda expression is used, in this case, to print all the values 
      		m.forEach((key, value) ->
      		{
      			System.out.println(key);
      			System.out.println(value);
      		});
      	}
      	*/
      	session.close();
      	cluster.close();
	}	
}
