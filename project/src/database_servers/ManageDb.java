/**
 * ManageDb class: it manage the request from clients and perform the query to the database sending the response using the protocol developed
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ManageDb
{
	private static final int SOCKETPORT = 8765;
	private static final int RMI_PORT = 11099; // suppose this class can get this port!
	private static final String RMI_NAME = "SessionManager"; // suppose this class can get this name with some protocol!

	// Usual main method
	public static void main(String[] argv)
	{
		(new ManageDb()).exec(argv);
	}

	private void exec(String[] argv)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;
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
		
		CachePinger cachePingerThread = new CachePinger(RMI_PORT, RMI_NAME);
		cachePingerThread.start();
		
		try 
		{
			while(true)
			{
				try
				{
					// accept a connection, when a client is connected, a new thread is created to
					// manage the connection
					// (no thread pooling)
					clientSock = serverSock.accept();
					System.out.println("Connection accepted, a new thread is going to be created.");
					Thread connectionThread = new Thread(new ConnectionDbThread(clientSock, RMI_PORT, RMI_NAME));
					connectionThread.start();
				} 
				catch (IOException ioe)
				{
					ioe.printStackTrace();
					try
					{
						serverSock.close();
					} 
					catch (IOException ioe2)
					{
						System.out.println("Failed to close serverSocket Database!");
						ioe2.printStackTrace();
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}	
}
