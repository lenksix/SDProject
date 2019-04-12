package database_servers;

import java.io.IOException;
import java.net.Socket;

import com.datastax.driver.core.Session;

public class CacheRegisterThread extends Thread
{
	private Socket cacheServerSock = null;
	private Session session = null;
	
	public CacheRegisterThread(Socket cacheServerSock, Session session)
	{
		this.cacheServerSock = cacheServerSock;
		this.session = session;
	}
	
	@Override
	public void run()
	{
		String ip = cacheServerSock.getInetAddress().getHostAddress();
		int port = cacheServerSock.getPort();
		String registerQuery = UtilitiesDb.insertIPCache(ip, port);
		try
		{
			synchronized(session)
			{
				session.execute("USE streaming;");
				session.execute(registerQuery);
			}
			System.out.println("Server cache at " + ip + " " + port + " registered");
		}
		finally
		{
			session.close();
			try
			{
				cacheServerSock.close();
			} 
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
}
