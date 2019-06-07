/**
 * ManageDb class: it manage the request from clients and perform the query to the database sending the response using the protocol developed
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import rmi_example.Addition;
import rmi_servers.SessionManager;

public class ManageDb
{
	private static final int SOCKETPORT = 8765;
	private static final int NUMARGS = 3;
	private static final String errorMsg = "600 GENERIC ERROR";
	private static final String clusterAdd = "127.0.0.1";
	private static final int RMI_PORT = 11099; // suppose this class can get this port!
	private static final String RMI_NAME = "SessionManager"; // suppose this class can get this name with some protocol!

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
		// TODO: Spostare su RMI l'inizializzazione
		/*Cluster cluster = Cluster.builder().addContactPoint(clusterAdd).build();
		Session session = cluster.connect();*/
		try
		{
			Registry registry = LocateRegistry.getRegistry(11099);
			SessionManager server = (SessionManager) registry.lookup(RMI_NAME);
	
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
		catch(RemoteException | NotBoundException re)
		{
			re.printStackTrace();
		}
	}	
}
