package proxies_frontend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyFrontend
{
	final static int DEFAULT_PORT = 9856;
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
				System.out.println("Connection accepted, a new thread is going to be created.");
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

}
