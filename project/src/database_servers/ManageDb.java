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
	final static int NUMARGS = 3;
	final static String clusterAdd = "127.0.0.1";
	final static String notFound = "404 NOT FOUND";

	final static int CHUNK_SIZE = 1000;

	Cluster cluster = null;
	Session session = null;

	// Usual main method
	public static void main(String[] argv)
	{
		// Just for test
		/*
		 * Checker che = null; Scanner sc = new Scanner(System.in); String line = null;
		 * 
		 * while((line = sc.nextLine()) != null) { che = UtilitiesDb.checkQuery(line);
		 * boolean b = che.isCorrect(); if (b == true) System.out.println("<true>");
		 * else System.out.println("<false>"); System.out.println("<"+ che.getMessage()
		 * + ">"); }
		 */
		(new ManageDb()).exec(argv);
	}

	private void exec(String[] argv)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;

		// run cassandra in background if it is not already running (useful in Windows
		// for example)
		try
		{
			if (os.contains("linux"))
			{
				Runtime.getRuntime().exec("sh cassandra -f");
			} else if (os.startsWith("win"))
			{
				Runtime.getRuntime().exec("cmd /c start cassandra -f");
			} else
			{
				System.out.println("Operating system not supported.");
				return;
			}
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}

		try
		{
			// Instantiate the server socket
			serverSock = new ServerSocket(SOCKETPORT);
			System.out.println("Ok, Serversocket created!");
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}

		while (true)
		{
			try
			{
				// accept a connection, when a client is connected, a new thread is created to
				// manage the connection
				// (no thread pooling)
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to be created.");
				ConnectionThread ct = new ConnectionThread(clientSock);
				ct.start();
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	private class ConnectionThread extends Thread
	{
		private Socket cliSock = null;
		String request = null;
		String response = null;
		String query = null;
		ResultSet queryResult = null;

		public ConnectionThread(Socket s)
		{
			cliSock = s;
		}

		public void run()
		{
			boolean done = false;
			Scanner clientReq = null;
			PrintWriter clientResp = null;
			// connect to the cluster
			cluster = Cluster.builder().addContactPoint(clusterAdd).build();
			session = cluster.connect();
			session.execute("USE streaming;");
			CheckerDB check = null;

			try
			{
				clientReq = new Scanner(cliSock.getInputStream());
				clientResp = new PrintWriter(cliSock.getOutputStream());
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}

			while (!done && clientReq.hasNextLine())
			{
				request = clientReq.nextLine();

				// NEW PROTOCOL
				// Remember the format of the possible requests:
				// 1. GET SP ALL SP CHANNEL_NAME or
				// 2. GET SP VIDEO SP URL (where URL is the url of the video)

				check = UtilitiesDb.checkQuery(request);
				if (!check.isCorrect())
				{
					response = check.getMessage();
					clientResp.println(response);
					clientResp.flush();
				} 
				else
				{
					query = check.getMessage();
					System.out.println(query);
					queryResult = session.execute(query);

					// manage the response for the query to the database
					// we have to decide the appropriate response
					response = "";
					String resourcePath = "";
					for (Row row : queryResult)
					{
						resourcePath += row.getString(0);
						System.out.println(resourcePath);
					}

					// if the query has no results 404 NOT FOUND is sent, else 200 OK plus the
					// result of the query
					if (resourcePath.isEmpty())
						response = notFound;
					else
					{
						response = "200 OK ";// + response;
						clientResp.println(response);
						clientResp.flush();

						try
						{
							DataOutputStream dos = new DataOutputStream(
									new BufferedOutputStream(cliSock.getOutputStream()));
							FileInputStream fileStream = new FileInputStream(new File(resourcePath));
							int n = 0;
							byte[] chunck = new byte[CHUNK_SIZE];
							while ((n = fileStream.read(chunck)) != -1)
							{
								dos.write(chunck, 0, n);
								dos.flush();
							}
							dos.close();
							fileStream.close();
						} 
						catch (IOException ioe3)
						{
							ioe3.printStackTrace();
						}

					}
					// response = UtilitiesDb.getResponse(queryResult);
					clientResp.println(response);
					clientResp.flush();
					done = true;
					System.out.println(response);
				}
			}
			System.out.println("Closing the connection.");
			try
			{
				clientReq.close();
				clientResp.close();
				cliSock.close();
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
}
