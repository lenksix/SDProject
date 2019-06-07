package proxies_frontend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.util.Pair;

public class ManageCacheList extends UnicastRemoteObject implements rmi_servers.CacheListManager
{
	private static HashMap<String, Integer> l2Map; // list of L2 servers <IP, PORT>
	private static ReentrantReadWriteLock mapLock; // need the lock for the updates on l2 list
	private static long SLEEP = 2000L;
	private static final String localhost = "localhost";
	private static final int CACHE_SERVER_MANAGER_PORT = 10000;
	private static final String listServer = "LIST SERVER";
	
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
			//Registry registry = LocateRegistry.getRegistry(11099); if already instatiated by the back-end 
			registry.rebind("ManageCacheList", server);
			System.out.println("Binded to RMI registry, Server is Alive!");
		}
		catch(RemoteException re)
		{
			re.printStackTrace();
		}
		
		Socket listSocket = null;
		ObjectInputStream listScanner = null;
		ObjectOutputStream listStream = null;
		ArrayList<Pair<String, Integer>> servers = null;
		
		System.err.println("Ci passo");
		// ONLY FOR TEST--------------------------
		mapLock.writeLock().lock();
		try
		{
			l2Map.put("localhost", 28517); // just for test
		}
		finally 
		{
			mapLock.writeLock().unlock();
		}
		// ---------------------------------------
		/*
		while(true)
		{
			try
			{
				Thread.currentThread();
				Thread.sleep(SLEEP);
				System.out.println("I am awake");
				try
				{
					listSocket = new Socket(localhost, CACHE_SERVER_MANAGER_PORT);
					listStream = new ObjectOutputStream(listSocket.getOutputStream());
					listScanner = new ObjectInputStream(listSocket.getInputStream());
					listStream.writeObject(listServer);
					listStream.flush();
	
					String listResponse = (String) listScanner.readObject();
					if(listResponse.equalsIgnoreCase(listServer)) 
					{
						servers = (ArrayList<Pair<String, Integer>>) listScanner.readObject();
						System.out.println("Free the map" + Thread.currentThread().getId());
						mapLock.writeLock().lock();
						try
						{
							l2Map.clear();
							for(Pair<String, Integer> server : servers)
							{
								l2Map.put(server.getKey(), server.getValue());
								System.out.println("updated + < " + server.getKey() + " > and < " + server.getValue() + " >");
							}
							
						}
						finally 
						{
							mapLock.writeLock().unlock();
						}
					}
					else
					{
						System.out.println("Undefined contol sequence in requesting the list of active servers: quit.");
						System.exit(1);
					}
				}
				catch(ClassNotFoundException | IOException ioe)
				{
					ioe.printStackTrace();
				}
				finally
				{
					try
					{
						listSocket.close();
					}
					catch(IOException ioe)
					{
						ioe.printStackTrace();
					}
				}
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}*/
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
