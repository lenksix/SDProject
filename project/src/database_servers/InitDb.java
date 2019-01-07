/**
 * InitDb class: it creates the database "streaming" and creates the two tables.
 * 
 * @author Andrea Bugin ad Ilie Sarpe
 *
 */


package database_servers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.*;

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
               + " 'replication_factor': '1' "
               + "};" );
         //Connect to the keyspace already instatiated
         session.execute("USE streaming;");
         //Create the table 
         session.execute("CREATE TABLE IF NOT EXISTS channel_vids("
               //+ "id_channel uuid,"
               + "channel_name text,"
               + "vids set<text>, " //where list is a list of url
               + "PRIMARY KEY(channel_name));"); 
         
         // create the table of url-path for each video
         session.execute("CREATE TABLE IF NOT EXISTS vid_path("
         		+ "url text,"
         		+ "path text,"
         		+ "PRIMARY KEY(url));");
      }
      catch(NoHostAvailableException nhae) 
      {
         System.out.println("Build failed: <"+ nhae.getMessage() +">");
         nhae.printStackTrace();
      }
      catch(com.datastax.driver.core.exceptions.SyntaxError Dse) 
      {
         Dse.printStackTrace();
      }
   } 
}
