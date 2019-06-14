package rmi_servers;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class is the only known implementation of the interface SessionManager.
 * It instantiates the cluster and session objects and Cassandra.
 * Offers the functionalities of querying, reading and deleting on database as remote methods.
 * @author Andrea Bugin and Ilie Sarpe
 *
 */
public class SessionManagerImpl extends UnicastRemoteObject implements SessionManager
{
	private static final String clusterAdd = "127.0.0.1";
	private static Cluster cluster;
	private static Session session;
	
	public SessionManagerImpl() throws RemoteException
	{
		SessionManagerImpl.cluster = Cluster.builder().addContactPoint(clusterAdd).build();
		SessionManagerImpl.session = cluster.connect();
	}

	@Override
	public String searchQuery(String query)
	{
		//System.out.println("Executing searchQuery");
		session.execute("USE streaming;");
		
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(ResultSet.class, new ResultSetSerializer());
		mapper.registerModule(module);
		
		ResultSet queryResult = session.execute(query);
		
		try
		{
			String json = mapper.writeValueAsString(queryResult);

			// Trial for decoding
			/*
			JSONArray array = new JSONArray(json);
			System.out.println("Size is " + array.length());
			for (int i = 0; i < array.length(); i++)
			{
				JSONObject jsonObj = array.getJSONObject(i);
				System.out.println(jsonObj.toString());
				System.out.println(jsonObj.getString("ip"));
				System.out.println(jsonObj.getInt("port"));
			}
			*/
			return json;

		} catch (JsonProcessingException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int registerQuery(String query)
	{
		//System.out.println("Executing registerQuery");
		try
		{
			session.execute("USE streaming;");
			session.execute(query);
			return 0;
		}
		catch(Exception e)
		{
			//return 1; //should be this
			e.printStackTrace();
		}
		return 1;
	}

	@Override
	public int deleteQuery(String query)
	{
		//System.out.println("Executing deleteQuery");
		return registerQuery(query);
	}
	
	public static void main(String[] args)
	{
		try
		{
			// Binding to the RMI registry
			SessionManager server = new SessionManagerImpl();
			Registry registry = LocateRegistry.createRegistry(11099);
			registry.rebind("SessionManager", server);
			System.out.println("Binded to RMI registry, Server is Alive!");
		}
		catch(RemoteException re)
		{
			re.printStackTrace();
			try 
			{
				session.close();
				cluster.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}


