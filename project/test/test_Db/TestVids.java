package test_Db;

import java.util.*;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.*;
import com.google.common.reflect.TypeToken;

public class TestVids 
{
   public static void main(String[] args) 
   {
      
      Cluster cluster;
      Session session;
       
      cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
        
      session = cluster.connect();
         
      session.execute("USE streaming;");
      //If it's the first time you execute the class, uncomment the following two lines
      //session.execute("INSERT INTO channel_vids(id_channel, channel_name, vids)"
      //      + " VALUES (now(), 'dellimellow', {'https://www.youtube.com/watch?v=gVpYcpmNRb4':'path da specificare'})");
         
      //query to select all the records in the channel_vids
      ResultSet results = session.execute("SELECT * FROM channel_vids WHERE channel_name='dellimellow';");
         
      Map<String, String> m = null;
      for(Row row: results)
      {
      	 m = row.getMap("vids", TypeToken.of(String.class), TypeToken.of(String.class)); //this returns a Map for each row in the table
      	 //this lambda expression is used, in this case, to print all the values 
       	 m.forEach((key, value) ->
       	 {
       		 System.out.println(key);
       		 System.out.println(value);
       	 });
      }
   }
}
