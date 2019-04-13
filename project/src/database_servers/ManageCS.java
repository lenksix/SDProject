package database_servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.datastax.driver.core.Session;

/**
 * The class ManageCS (Manage cache servers) is used to manage the active server cache, 
 * adding them in the database as ip-port record and ping periodically to verify how many of them are active,
 * removing the record whether one of them is not.
 * 
 * @author Andrea Bugin and Ilie Sarpe
 */
public class ManageCS extends Thread
{
	private final static int CACHE_SERVER_MANAGER_PORT = 10000;
	private Session session = null;
	
	public ManageCS(Session session)
	{
		this.session = session;
	}
	
	@Override
	public void run()
	{
		ServerSocket manageCSSock = null;
		Socket cacheServerSock = null;
		session.execute("USE streaming;");
		
		//------------------- for test purpose, just to create some records in the table.
		/* 
		String q = UtilitiesDb.insertIPCache("192.168.1.1", 12345);
		session.execute(q);
		
		q = UtilitiesDb.insertIPCache("192.168.1.1", 12346);
		session.execute(q);
		
		q = UtilitiesDb.selectFromIPCache();
		ResultSet rSet = session.execute(q);
		
		System.out.println(q);
		//rSet.forEach(System.out::println);
		
		//List<ColumnDefinitions.Definition> column = rSet.getColumnDefinitions().asList();
		//column.forEach(x->System.out.println(x.getName()));
		
		rSet.forEach(row -> System.out.println(row.getString("ip") + " " + row.getInt("port")));
		*/
		//------------------------------------------------------------------------------------------
		
		try
		{
			// Instantiate the server socket
			manageCSSock = new ServerSocket(CACHE_SERVER_MANAGER_PORT);
			System.out.println("Ok, cache server manager created!");
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		// starting the cache pinger
		CachePinger cachePingerThread = new CachePinger(session);
		cachePingerThread.start();
		
		while(!isInterrupted())
		{
			try
			{
				// accept a connection, when a client is connected, a new thread is created to
				// manage the connection
				// (no thread pooling)
				cacheServerSock = manageCSSock.accept();
				String ip_port = cacheServerSock.getInetAddress().getHostAddress() + " " + cacheServerSock.getPort();
				System.out.println("Connection accepted from a server cache at " + ip_port);
				System.out.println("A new cache register thread is going to be created.");
				
				// TODO: decide the protocol of the connection between the L2 server and the CacheRegisterThread
				Thread cacheRegisterThread = new Thread(new CacheRegisterThread(cacheServerSock, session));
				cacheRegisterThread.start();
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				try
				{
					manageCSSock.close();
				}
				catch (IOException ioe2)
				{
					ioe2.printStackTrace();
				}
			}
		}	
	}
	
}
