/**
 * CachePinger class: it retrieves the list of active cache servers and "pings" them one by one. If a server doesn't at least one of three times
 * with a given delay of time, the CachePinger removes the pair ip-port from the ip_cache table.
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.json.JSONArray;
import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import rmi_servers.SessionManager;

public class CachePinger extends Thread
{
	private static final int TIMEOUT_RESPONSE = 3000;
	private static final int REPETITIONS = 3;
	private static final long SLEEP_TIME = 10000L;
	private static final long RELOAD_TIME = 100L;
	//private Session session;
	private int rmi_port;
	private String rmi_name;
	
	public CachePinger(int rmi_port, String rmi_name)
	{
		this.rmi_port = rmi_port;
		this.rmi_name = rmi_name;
	}
	
	@Override
	public void run()
	{
		try 
		{
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			SessionManager server = (SessionManager) registry.lookup(rmi_name);
			
			while(!isInterrupted())
			{
				String json = server.searchQuery(UtilitiesDb.selectFromIPCache());
				
				JSONArray array = new JSONArray(json); 

				for(int j = 0; j < array.length(); j++)
				{
					int rep = 0;
					JSONObject jsonObj = array.getJSONObject(j);
					String ip = jsonObj.getString("ip");
					int port = jsonObj.getInt("port");
					
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
						int error = server.deleteQuery(UtilitiesDb.deleteIPCache(ip, port));
						if(error == 0)
						{
							System.out.println("Server " + ip + " " + port + " did not respond " + rep + " times; deletion in the table done");
						}
						else
						{
							System.err.println("Something went wrong.. not deleted");
						}
					}
				}
				sleep(SLEEP_TIME);
			}
		}
		catch(InterruptedException | RemoteException | NotBoundException ie )
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
