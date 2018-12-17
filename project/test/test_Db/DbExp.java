package test_Db;

import com.datastax.driver.core.Cluster;
//import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
//import com.datastax.driver.core.schemabuilder.SchemaBuilder;

/* Programma per verificare se funziona la creazione di un keyspace,
 * tabella, indice e query su cassandra
 */
public class DbExp {    

   public static void main(String[] args) {
      // TODO Auto-generated method stub
      Cluster cluster;
      Session session;
      try{
    	 
         cluster = Cluster.builder().addContactPoint("127.0.0.1").build();

         session = cluster.connect();
     
         
         // Insert one record into the users table
         session.execute("CREATE KEYSPACE IF NOT EXISTS demo WITH replication = {"
               + " 'class': 'SimpleStrategy', "
               + " 'replication_factor': '3' "
               + "};" );
         session.execute("USE demo;");
         session.execute("CREATE TABLE IF NOT EXISTS users("
               + " id uuid PRIMARY KEY,"
               + "firstName text,"
               + "lastName text);");
         session.execute("INSERT INTO users (id, firstName, lastName)"
               + " VALUES ( now(), 'Walter', 'White')");
         session.execute("CREATE INDEX IF NOT EXISTS lastnameUsers"
               + " ON demo.users(lastname);");
         //ResultSet results = session.execute("SELECT * FROM users WHERE lastname='White'");
         ResultSet results = session.execute("SELECT * FROM users WHERE lastname='White'");
         for(Row row: results){
            System.out.format("%s\n", row.getString("firstname"));
         }
      }
      catch(NoHostAvailableException nhae){
         nhae.printStackTrace();
      }
      catch(NullPointerException npe){
    	  npe.printStackTrace();
      }
   }

}
