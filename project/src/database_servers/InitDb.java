package database_servers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/* creates the table we need for querying the urls */
class InitDb {

   public static void main(String[] args) {
      Cluster cluster;
      Session session;
      
      try 
      {
         cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
         session = cluster.connect();
         
         //Instantiate the keyspace, with standard parameters
         session.execute("CREATE KEYSPACE IF NOT EXISTS streaming WITH replication = {"
               + " 'class': 'SimpleStrategy', "
               + " 'replication_factor': '3' "
               + "};" );
         //Connect to the keyspace already instatiated
         session.execute("USE streaming;");
         //Create the table 
         session.execute("CREATE TABLE IF NOT EXISTS channel_vids("
               //+ "id_channel uuid,"
               + "channel_name text,"
               + "vids map<text,text>, " //where map is <url, path>
               + "PRIMARY KEY(channel_name));");  
      }
      catch(NoHostAvailableException nhae) {
         System.out.println("Build failed: <"+ nhae.getMessage() +">");
         nhae.printStackTrace();
      }
      catch(com.datastax.driver.core.exceptions.SyntaxError Dse) {
         Dse.printStackTrace();
      }
   } 
}
