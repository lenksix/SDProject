package test_Db;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

public class TestVids {
   
   public static void main(String[] args) {
      
      Cluster cluster;
      Session session;
      try{
       
         cluster = Cluster.builder().addContactPoint("127.0.0.1").build();

         session = cluster.connect();
     
         
         
         session.execute("USE streaming;");
         session.execute("INSERT INTO channel_vids(id, channel_name, lastName)" //Da qui devi completare la classe di test della nuova tabella channel_vids creata nella cartella src datatase_servers, basta che inserisci una tupla, fai una query e vedi se va.
               + " VALUES ( now(), 'Walter', 'White')");
         /*session.execute("CREATE INDEX IF NOT EXISTS lastnameUsers"
               + " ON demo.users(lastname);");
         //ResultSet results = session.execute("SELECT * FROM users WHERE lastname='White'");
         ResultSet results = session.execute("SELECT * FROM users WHERE lastname='White'");
         for(Row row: results){
            System.out.format("%s\n", row.getString("firstname"));
         }
         session.close();
         cluster.close();
      }
      catch(NoHostAvailableException nhae){
         nhae.printStackTrace();
      }
      catch(NullPointerException npe){
        npe.printStackTrace();
      }*/
   }
}
