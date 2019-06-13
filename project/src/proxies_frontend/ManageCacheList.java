package proxies_frontend;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.util.Pair;
import rmi_servers.ListL2Manager;

/**
 * Manage the list of the active servers in the proxy level
 * @author Andrea Bugin and Ilie Sarpe
 */
public class ManageCacheList extends UnicastRemoteObject implements rmi_servers.CacheListManager
{
	private static HashMap<String, Integer> l2Map; // list of L2 servers <IP, PORT>
	private static ReentrantReadWriteLock mapLock; // need the lock for the updates on l2 list
	private static long SLEEP = 2000L;
	private static final String localhost = "localhost";
	private static final int CACHE_SERVER_MANAGER_PORT = 10000;
	private static final String listServer = "LIST SERVER";
	
	private static final int RMI_REGISTER_SERVICE_PORT = 11300;
	private static final String RMI_REGISTER_SERVICE_NAME = "ListL2Manager";
	
	protected ManageCacheList() throws RemoteException
	{
		super();
	}

	public static void main(String[] args)
	{
		//Initialize structures 
		l2Map = new HashMap<String, Integer>();
		mapLock = new ReentrantReadWriteLock(true);
		
		try
		{
			// Binding to the RMI registry
			rmi_servers.CacheListManager server = new ManageCacheList();
			Registry registry = LocateRegistry.createRegistry(11200); // if no registry exists
			registry.rebind("ManageCacheList", server);
			System.out.println("Binded to RMI registry, Server is Alive!");
		}
		catch(RemoteException re)
		{
			re.printStackTrace();
		}

		ArrayList<Pair<String, Integer>> servers = null;
		
		System.err.println("Ci passo");
		// ONLY FOR TEST--------------------------
		/*mapLock.writeLock().lock();
		try
		{
			l2Map.put("localhost", 28517); // just for test
		}
		finally 
		{
			mapLock.writeLock().unlock();
		}*/
		// ---------------------------------------
		
		while(true)
		{
			try
			{
				Thread.currentThread();
				Thread.sleep(SLEEP);
				//System.out.println("I am awake");
				try
				{
					Registry registry = LocateRegistry.getRegistry(RMI_REGISTER_SERVICE_PORT);
					ListL2Manager server = (ListL2Manager) registry.lookup(RMI_REGISTER_SERVICE_NAME);
					servers = server.listServers();
					//System.out.println("Free the map" + Thread.currentThread().getId());
					mapLock.writeLock().lock();
					try
					{
						l2Map.clear();
						for(Pair<String, Integer> serv : servers)
						{
							l2Map.put(serv.getKey(), serv.getValue());
							//System.out.println("updated + < " + serv.getKey() + " > and < " + serv.getValue() + " >");
						}
						
					}
					finally 
					{
						mapLock.writeLock().unlock();
					}
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				} catch (NotBoundException nbe)
				{
					nbe.printStackTrace();
				}
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
		//*/
	}
	
	@Override
	public HashMap<String, Integer> getListL2() throws RemoteException
	{
		mapLock.readLock().lock();
		try
		{
			return l2Map;
		}
		finally
		{
			mapLock.readLock().unlock();
		}
	}

}
