package database_servers;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.util.Pair;
import rmi_servers.ListL2Manager;
import rmi_servers.SessionManager;

/**
 * CacheServicesThread class: it manages requests from cache servers or proxies. In the first case the only allowed request is the registration
 * in the ip_cache table because the server is online; on the other hand when the request comes from a proxy, it receives the list of active cache 
 * servers, retrieved from the ip_cache table. All the requests and the responses are handled by appropriate protocols.
 * @author Andrea Bugin and Ilie Sarpe
 */
public class CacheServicesThread extends UnicastRemoteObject implements ListL2Manager
{
	private static final String registrationOK = "210 REGISTRATION DONE";
	private static final String registrationError = "620 REGISTRATION ERROR";
	private int RMI_PORT = 11099; // suppose this class can get this port!
	private static final String RMI_NAME = "SessionManager"; // suppose this class can get this name with some protocol!
	
	/**
	 * Create a CacheServicesThread
	 * @throws RemoteException if any error occurs while creating the CacheServicesThread
	 */
	public CacheServicesThread() throws RemoteException
	{
		super();
	}
	
	@Override
	public String serverRegister(String ip, int port) 
	{
		try 
		{
			Registry registry = LocateRegistry.getRegistry(RMI_PORT);
			SessionManager server = (SessionManager) registry.lookup(RMI_NAME);
			
			
			String registerQuery = UtilitiesDb.insertIPCache(ip, port);
			int result = server.registerQuery(registerQuery);
			
			if(result == 0)
			{	
				System.out.println("Server cache at " + ip + " " + port + " registered");
				return registrationOK;	
			}
			else
			{
				System.err.println("Something wrong during the registration");
				return registrationError;	
			}
		} 
		catch(IOException | NotBoundException e) 
		{
			e.printStackTrace();
			return registrationError;
		}		
	}

	@Override
	public ArrayList<Pair<String, Integer>> listServers() 
	{
		ArrayList<Pair<String, Integer>> listActiveServer = new ArrayList<>();
		try
		{
			Registry registry = LocateRegistry.getRegistry(RMI_PORT);
			SessionManager server = (SessionManager) registry.lookup(RMI_NAME);
			
			String listServerQuery = UtilitiesDb.selectFromIPCache();
			String json = server.searchQuery(listServerQuery);
			
			JSONArray array = new JSONArray(json); 

			for(int j = 0; j < array.length(); j++)
			{
				JSONObject jsonObj = array.getJSONObject(j);
				String ip = jsonObj.getString("ip");
				int port = jsonObj.getInt("port");
				listActiveServer.add(new Pair<>(ip, port));
			}
			return listActiveServer;
		}
		catch(RemoteException | NotBoundException re)
		{
			re.printStackTrace();
		}
		return null;
		
	}
	
	public static void main(String[] args)
	{
		try
		{
			// Binding to the RMI registry
			ListL2Manager server = new CacheServicesThread();
			Registry registry = LocateRegistry.createRegistry(11300);
			registry.rebind("ListL2Manager", server);
			System.out.println("Binded to RMI registry, Server is Alive, CST!");
		}
		catch(RemoteException re)
		{
			re.printStackTrace();
		}
	}
}
