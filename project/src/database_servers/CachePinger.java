/**
 * CachePinger class: it retrieves the list of active cache servers and "pings" them one by one. If a server doesn't at least one of three times
 * with a given delay of time, the CachePinger removes the pair ip-port from the ip_cache table.
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CachePinger extends Thread
{
	private static final int TIMEOUT_RESPONSE = 3000;
	private static final int REPETITIONS = 3;
	private static final long SLEEP_TIME = 10000L;
	private static final long RELOAD_TIME = 100L;
	private Session session;
	
	public CachePinger(Session session)
	{
		this.session = session;
	}
	
	@Override
	public void run()
	{
		try 
		{
			while(!isInterrupted())
			{
				ResultSet rSet = session.execute(UtilitiesDb.selectFromIPCache());
				
				for(Row row : rSet)
				{
					int rep = 0;
					String ip = row.getString("ip");
					int port = row.getInt("port");
					
					for(int i = 0; i < REPETITIONS; i++)
					{
						if(!isAddressReachable(ip, port, TIMEOUT_RESPONSE))
						{
							rep++;
							sleep(RELOAD_TIME);
						}
						else 
						{
							System.out.println("Server " + ip + " " + port + " is alive.");
							break;
						}
					}
					if(rep >= REPETITIONS)
					{
						session.execute(UtilitiesDb.deleteIPCache(ip, port));
						System.out.println("Server " + ip + " " + port + " did not respond " + rep + " times; deletion in the table done");
					}
				}
				sleep(SLEEP_TIME);
			}
		}
		catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	/**
	 * Verify if an address specified by the pair ip-port is reachable.
	 * @param ip the ip to verify the reachability 
	 * @param port the port to verify the reachability
	 * @param timeout the timeout value to be used in milliseconds
	 * @return true if the address is reachable, false otherwise
	 */
	private static boolean isAddressReachable(String ip, int port, int timeout) 
	{
		try 
		{
			try(Socket sock = new Socket()) 
			{
				// Connects this socket to the server with a specified timeout value.
				sock.connect(new InetSocketAddress(ip, port), timeout);
			}
			// Return true if connection success
			return true;
		}
		catch(IOException ioe) 
		{
			// Return false if connection fails
			return false;
		}
	}
}
