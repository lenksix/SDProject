package database_servers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.datastax.driver.core.Session;

import javafx.util.Pair;

public class CacheRegisterThread implements Runnable
{
	private Socket cacheServerSock = null;
	private Session session = null;
	private final String registrationError = "620 REGISTRATION ERROR";
	private final String registrationOK = "210 REGISTRATION DONE";
	
	public CacheRegisterThread(Socket cacheServerSock, Session session)
	{
		this.cacheServerSock = cacheServerSock;
		this.session = session;
	}
	
	@Override
	public void run()
	{		
		ObjectInputStream request = null;
		PrintWriter response = null;
		
		try
		{
			request = new ObjectInputStream(cacheServerSock.getInputStream());
			response = new PrintWriter(cacheServerSock.getOutputStream());
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		Pair<String, Integer> registerPair;
		try
		{
			registerPair = (Pair<String, Integer>) request.readObject();
			String ip = registerPair.getKey();
			int port = registerPair.getValue();
			
			String registerQuery = UtilitiesDb.insertIPCache(ip, port);
			session.execute("USE streaming;");
			session.execute(registerQuery);
			
			response.println(registrationOK);
			response.flush();
				
			System.out.println("Server cache at " + ip + " " + port + " registered");
		}
		catch(ClassNotFoundException cnfe)
		{
			response.println(registrationError);
			response.flush();
		}
		catch(IOException ioe) 
		{
			response.println(registrationError);
			response.flush();
		}
		
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
