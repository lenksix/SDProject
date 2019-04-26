package proxies_frontend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import javafx.util.Pair;

public class ManageCacheList implements Runnable
{
	private static HashMap<String, Integer> l2Map;
	private static ReentrantLock mapLock;
	private static long SLEEP = 2000L;
	private static final String localhost = "localhost";
	private static final int CACHE_SERVER_MANAGER_PORT = 10000;
	private static final String listServer = "LIST SERVER";

	
	public ManageCacheList(HashMap<String, Integer> l2Map, ReentrantLock mapLock)
	{
		this.l2Map = l2Map;
		this.mapLock = mapLock;
	}
	
	@Override
	public void run()
	{
		Socket listSocket = null;
		ObjectInputStream listScanner = null;
		ObjectOutputStream listStream = null;
		ArrayList<Pair<String, Integer>> servers = null;
		
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
						System.out.println("Free the map");
						mapLock.lock();
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
							mapLock.unlock();
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
		}
	}

}
