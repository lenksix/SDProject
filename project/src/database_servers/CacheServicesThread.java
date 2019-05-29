/**
 * CacheServicesThread class: it manages requests from cache servers or proxies. In the first case the only allowed request is the registration
 * in the ip_cache table because the server is online; on the other hand when the request comes from a proxy, it receives the list of active cache 
 * servers, retrieved from the ip_cache table. All the requests and the responses are handled by appropriate protocols.
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import javafx.util.Pair;
import rmi_servers.SessionManager;

public class CacheServicesThread implements Runnable
{
	private Socket cacheServerSock = null;
	//private Session session = null;
	private static final String registerMe = "REGISTER ME";
	private static final String registrationOK = "210 REGISTRATION DONE";
	private static final String registrationError = "620 REGISTRATION ERROR";
	private static final String listServer = "LIST SERVER";
	private static final String unknownRequest = "UNKNOWN REQUEST";
	private int rmi_port;
	private String rmi_name;
	
	public CacheServicesThread(Socket cacheServerSock, int rmi_port, String rmi_name)
	{
		this.cacheServerSock = cacheServerSock;
		// TODO: modify constructor RMI
		this.rmi_port = rmi_port;
		this.rmi_name = rmi_name;
	}
	
	@Override
	public void run()
	{		
		ObjectInputStream oisRequest = null;
		ObjectOutputStream oosResponse = null;
		
		try
		{
			oisRequest = new ObjectInputStream(cacheServerSock.getInputStream());
			oosResponse = new ObjectOutputStream(cacheServerSock.getOutputStream());
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		String request = null;
		try
		{
			request = (String) oisRequest.readObject();
			if(request.equalsIgnoreCase(registerMe))
			{
				serverRegister(oisRequest, oosResponse);
			}
			else if(request.equalsIgnoreCase(listServer))
			{
				listServer(oosResponse);
			}
		}
		catch(ClassNotFoundException | IOException e)
		{
			try
			{
				oosResponse.writeObject(unknownRequest);
				oosResponse.flush();
			}
			catch(IOException ioe) 
			{
				ioe.printStackTrace();
			}
			e.printStackTrace();
		}
		finally 
		{
			try
			{
				oisRequest.close();
				oosResponse.close();
				cacheServerSock.close();
			} 
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	
	private void serverRegister(ObjectInputStream oisRequest, ObjectOutputStream oosResponse) 
	{
		Pair<String, Integer> registerPair = null;
		try 
		{
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			SessionManager server = (SessionManager) registry.lookup(rmi_name);
			registerPair = (Pair<String, Integer>) oisRequest.readObject();
			String ip = registerPair.getKey();
			int port = registerPair.getValue();
			
			String registerQuery = UtilitiesDb.insertIPCache(ip, port);
			// TODO: Spostare su RMI la registrazioe, serve un metodo!
			/*session.execute("USE streaming;");
			session.execute(registerQuery);*/
			int result = server.registerQuery(registerQuery);
			
			if(result == 0)
			{
				oosResponse.writeObject(registrationOK);
				oosResponse.flush();
					
				System.out.println("Server cache at " + ip + " " + port + " registered");
			}
			else
			{
				System.err.println("Something wrong during the registration");
			}
		} 
		catch(ClassNotFoundException | IOException | NotBoundException e) 
		{
			try 
			{
				oosResponse.writeObject(registrationError);
				oosResponse.flush();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			e.printStackTrace();
		}		
	}
	
	/**
	 * This method write a list object of active cache servers in the given ObjectOutputStream passed by reference.
	 * @param oosResponse the stream where to write the list of active cache servers
	 */
	private void listServer(ObjectOutputStream oosResponse) 
	{
		List<Pair<String, Integer>> listActiveServer = new ArrayList<>();
		try
		{
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			SessionManager server = (SessionManager) registry.lookup(rmi_name);
			
			String listServerQuery = UtilitiesDb.selectFromIPCache();
			//TODO: Spostare su RMI sta roba!!
			//ResultSet rSet = session.execute(listServerQuery);
			String json = server.searchQuery(listServerQuery);
			
			JSONArray array = new JSONArray(json); 

			for(int j = 0; j < array.length(); j++)
			{
				JSONObject jsonObj = array.getJSONObject(j);
				String ip = jsonObj.getString("ip");
				int port = jsonObj.getInt("port");
				listActiveServer.add(new Pair<>(ip, port));
			}
			
			/*rSet.forEach(row -> {
				String ip = row.getString("ip");
				int port = row.getInt("port");
				listActiveServer.add(new Pair<>(ip, port));
			});*/
			
			try
			{
				oosResponse.writeObject(listServer);
				oosResponse.flush();
				oosResponse.writeObject(listActiveServer);
				oosResponse.flush();
			} 
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		catch(RemoteException | NotBoundException re)
		{
			re.printStackTrace();
		}
		
	}
}
