package proxies_frontend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;




public class ProxyFrontend
{
	final static int DEFAULT_PORT = 9856;
	
	private static HashMap<String, Integer> l2List; // list of L2 servers <IP, PORT>
	private static ReentrantLock listLock; // need the lock for the updates on l2 list
	
	public static void main(String[] args)
	{
		(new ProxyFrontend()).exec(args);
	}
	
	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;
		
		try
		{
			serverSock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Ok, Serversocket <proxy> created!");
			
			l2List = new HashMap<String, Integer>(); 
			
			//TODO: need a distributed structure of l2 servers!!
			
			l2List.put("localhost",8860); // just for test
			
			//TODO: need to instantiate a class that updates the map.. pings etc..
			
			
		}
		catch(IOException ioe)
		{
			System.err.println("Failed to instatiate the <proxy> serversocket ");
			ioe.printStackTrace();
		}
		
		while (true)
		{
			try
			{
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to served by the proxy!");
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
		Socket clientSock;

		public ConnectionThread(Socket clientSock)
		{
			this.clientSock = clientSock;
		}
		
		public void run()
		{
			Scanner scannerClient = null;
			PrintWriter pwClient = null;
			
			CheckerProxy check;
			
			try
			{
				String request = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());

				while (scannerClient.hasNextLine())
				{
					request = scannerClient.nextLine();
					check = UtilitiesProxyFrontend.parseRequest(request);
					
					if(!check.isCorrect())
					{
						// Malformed request
						pwClient.println(check.getId()); // send error code
						pwClient.flush();
					}
					else
					{
						;
					}
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		
	}

}
