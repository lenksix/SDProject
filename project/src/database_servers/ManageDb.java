/**
 * ManageDb class: it manage the request from clients and perform the query to the database sending the response using the protocol developed
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.*;
import java.net.*;
import java.util.*;

import com.datastax.driver.core.*;

public class ManageDb 
{
	private static final String os = System.getProperty("os.name").toLowerCase();
	final static int SOCKETPORT = 8765;
	final static String errorMsg = "600 GENERIC ERROR";
	
	Cluster cluster = null;
	Session session = null;
	
	// Usual main method
	public static void main(String[] argv)
	{
		(new ManageDb()).exec(argv);
	}
	
	private void exec(String[] argv) 
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;
		
		// run cassandra in background if it is not (useful in Windows for example)
		try
		{
			if(os.contains("linux"))
			{
				Runtime.getRuntime().exec("sh cassandra -f");
			}
			else if(os.startsWith("win"))
			{
				Runtime.getRuntime().exec("cmd /c start cassandra -f");
			}
			else
			{
				System.out.println("Operating system not supported.");
				return;
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
		
		try 
		{
			//Instantiate the server socket
			serverSock = new ServerSocket(SOCKETPORT);
			System.out.println("Ok, serversocket created!");
		}
		catch(IOException ioe) 
		{
			ioe.printStackTrace();         
		}
		
		while(true) 
		{
			try 
			{
				// accept a connection, when a client is connected, a new thread is created to manage the connection
				// (no thread pooling)
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to be created.");
				ConnectionThread ct = new ConnectionThread(clientSock);
				ct.start();
			}
			catch(IOException ioe) 
			{
				ioe.printStackTrace();
			}
		}
	}
	
	private class ConnectionThread extends Thread
	{
		private static final int NUMARGS = 3;
		private Socket s = null;
		String request = null;
		String response = null;
		String query = null;
		ResultSet queryResult = null;
		
		public ConnectionThread(Socket s)
		{
			this.s = s;
		}
		
		public void run()
		{
			Scanner clientReq = null;
			PrintWriter clientResp = null;
			//connect to the cluster
			cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			session = cluster.connect();
			session.execute("USE streaming;");
			
			try
			{
				clientReq = new Scanner(s.getInputStream());
				clientResp = new PrintWriter(s.getOutputStream());
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			
			while(clientReq.hasNextLine()) 
			{
				request = clientReq.nextLine();
				
				// NEW PROTOCOL 
				// Remember the format of the possible requests: 
				// GET SP ALL SP CHANNEL_NAME or
				// GET SP VIDEO SP URL (where URL is the url of the video)
				
				String[] args = request.split(" "); 
				//form the query for the db
				if((args.length == NUMARGS) && args[0].equals("GET") && (args[1].equals("ALL") || args[1].equals("VIDEO"))) 
				{
					if(args[1].equals("ALL"))
						query = UtilitiesDb.getAllVideos(args[2]);
					else
						query = UtilitiesDb.getPath(args[2]);
				}
				else 
				{
					// if the request is not well formed
					response = errorMsg;
					clientResp.println(response);
					clientResp.flush();
					continue;
				}
				System.out.println(query);
				queryResult = session.execute(query);
				
				// manage the response for the query to the database
				// we have to decide the appropriate response
				response = "";
				for(Row row : queryResult)
				{
					response += row.toString();
				}
				
				// if the query has no results 404 NOT FOUND is sent, else 200 OK plus the result of the query
				if(response.isEmpty())
					response = "404 NOT FOUND";
				else
					response = "200 OK " + response;
				//response = UtilitiesDb.getResponse(queryResult);
				clientResp.println(response);
				clientResp.flush();
			}
			System.out.println("Closing the connection.");
			try
			{
				clientReq.close();
				clientResp.close();
				s.close();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
}
