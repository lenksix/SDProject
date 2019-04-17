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
import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import javafx.util.Pair;

public class CacheServicesThread implements Runnable
{
	private Socket cacheServerSock = null;
	private Session session = null;
	private static final String registerMe = "REGISTER ME";
	private static final String registrationOK = "210 REGISTRATION DONE";
	private static final String registrationError = "620 REGISTRATION ERROR";
	private static final String listServer = "LIST SERVER";
	private static final String unknownRequest = "UNKNOWN REQUEST";
	
	public CacheServicesThread(Socket cacheServerSock, Session session)
	{
		this.cacheServerSock = cacheServerSock;
		this.session = session;
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
			registerPair = (Pair<String, Integer>) oisRequest.readObject();
			String ip = registerPair.getKey();
			int port = registerPair.getValue();
			
			String registerQuery = UtilitiesDb.insertIPCache(ip, port);
			session.execute("USE streaming;");
			session.execute(registerQuery);
			
			oosResponse.writeObject(registrationOK);
			oosResponse.flush();
				
			System.out.println("Server cache at " + ip + " " + port + " registered");
		} 
		catch(ClassNotFoundException | IOException e) 
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
		
		String listServerQuery = UtilitiesDb.selectFromIPCache();
		ResultSet rSet = session.execute(listServerQuery);
		
		rSet.forEach(row -> {
			String ip = row.getString("ip");
			int port = row.getInt("port");
			listActiveServer.add(new Pair<>(ip, port));
		});
		
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
}
