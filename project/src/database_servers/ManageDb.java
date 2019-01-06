package database_servers;

import java.io.*;
import java.net.*;
import java.util.*;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

/* returns the results of a given query.. in further development this will 
 * create different servants that will provide different services */
public class ManageDb {
   
   final static int SOCKETPORT = 8765;
   final static String errorMsg = "600 GENERIC ERROR";
   
   public static void main(String[] args) {
      
      ServerSocket serverSock = null;
      Socket clientSock = null;
      //OutputStream outStr = null;
      //InputStream inpStr = null;
      
      //instead of BufferedReader, use Scanner, it's easier
      Scanner clientReq = null;
      PrintWriter clientResp = null;
      Cluster cluster;
      Session session;
      
      
      try {
         //Instantiate the server socket
         serverSock = new ServerSocket(SOCKETPORT);
         System.out.println("Ok, serversocket created!");
      }
      catch(IOException ioe) {
         ioe.printStackTrace();         
      }
      catch(SecurityException se) {
         se.printStackTrace();
      }
      catch(IllegalArgumentException iae) {
         iae.printStackTrace();         
      }
      
      while(true) 
      {
         try 
         {
            //accept a connection
            clientSock = serverSock.accept();
            System.out.println("Connection accepted");
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
         }
         try {

            //connect to the cluster
            cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
            session = cluster.connect();
            
            String request = null;
            String response = null;
            String query = null;
            ResultSet queryResult = null;
            final int NUMARGS = 4;
            //UtilitiesDb utilities = new UtilitiesDb();
            
            clientReq = new Scanner(clientSock.getInputStream());
            clientResp = new PrintWriter(clientSock.getOutputStream());
            
            while(clientReq.hasNextLine()) 
            {
            	request = clientReq.nextLine();
            	String[] arguments = request.split(" "); // Recall the format of the request is GET SP DB SP ID_CH SP ID_RISORSA
            	//form the query for the db
            	if((arguments.length == NUMARGS) && arguments[0].equals("GET") && arguments[1].equals("DB")) 
            		query = UtilitiesDb.createQuery(arguments[2], arguments[3]); // (Channel, Url)
            	else 
            	{
            		response = errorMsg;
            		clientResp.println(response);
            		clientResp.flush();
            		continue;
            	}
            	queryResult = session.execute(query);
            	response = UtilitiesDb.getResponse(queryResult);
            	clientResp.println(response);
            	clientResp.flush();
            }
         }
         catch(IOException ioe1) {
            ioe1.printStackTrace();
            try {
               clientSock.close();
            }
            catch(IOException ioe2) {
               ioe2.printStackTrace();
            }
         }
         clientReq.close();
      }
   }
}
