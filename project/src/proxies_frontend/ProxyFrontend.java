package proxies_frontend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//import clients.VlcjTrial.StreamMedia;
import javafx.util.Pair;





public class ProxyFrontend implements Runnable
{
	
	final static int DEFAULT_PORT = 9856;
	final static int WAIT_TIME = 15000;
	private final static int C_SIZE = 1000; // size of each chunk of the file in bytes
	
	
	final static int RMI_FRONTEND_REG_PORT = 11200; // default port for frontend rmi registry
	final static String RMI_NAMING_CL = "ManageCacheList"; // Name of the Cache List service
	rmi_servers.CacheListManager server;
	

	private static List<Socket> sockAlive;
	private static ReentrantLock lockSockets;
	private static Condition socketAvailable;
	
	public static void main(String[] args)
	{
		(new ProxyFrontend()).exec(args);
	}
	
	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		
		sockAlive = new LinkedList<Socket>();
		lockSockets = new ReentrantLock();
		socketAvailable = lockSockets.newCondition();
		
		try
		{
			serverSock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Ok, Serversocket <proxy> created!");
			
			// Server for requesting remote methods
			Registry registry = LocateRegistry.getRegistry(RMI_FRONTEND_REG_PORT);
			server = (rmi_servers.CacheListManager) registry.lookup(RMI_NAMING_CL);
			
		}
		catch(IOException ioe)
		{
			System.err.println("Failed to instatiate the <proxy> serversocket ");
			ioe.printStackTrace();
		} catch (NotBoundException nbe)
		{
			nbe.printStackTrace();
		}
		
		while (true)
		{
			Socket clientSock = null;
			try
			{
				// TODO: Accept the client only if the size of the list is <= 60K else redirect!
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to served by the proxy!");
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
			
			lockSockets.lock();
			try 
			{
				sockAlive.add(clientSock);
				if(!lockSockets.hasWaiters(socketAvailable))
				{
					(new Thread(this)).start();
				}
				else
				{
					socketAvailable.signal();
				}
			}
			finally
			{
				lockSockets.unlock();
			}
		}
	}

	@Override
	public void run()
	{
		Socket clientSock;
		Scanner scannerClient = null;
		PrintWriter pwClient = null;
		CheckerProxy check;
		String myname = Thread.currentThread().getName();
		boolean alive = true;
		
		Scanner scannerCache = null;
		PrintWriter pwCache = null;
		
		while(true)
		{
			System.out.println("I am alive  <" + myname + ">");
			lockSockets.lock();
			try
			{
				while(sockAlive.isEmpty())
				{
					
					try
					{
						if(!socketAvailable.await(WAIT_TIME, TimeUnit.MILLISECONDS))
						{
							// I have to die
							System.out.println("I terminated my existence " + myname);
							alive = false; 
							break;
						}
					}
					catch(InterruptedException ie)
					{
						ie.printStackTrace();
					}
				}
				if(!alive)
				{
					System.out.println("Exiting all " + myname);
					break;
				}
				clientSock = sockAlive.remove(0); // This must exist at this point
				System.out.println("I am handling a connection " + myname);
			}
			finally
			{
				lockSockets.unlock();
			}
			
			try
			{
				String request = null;
				Socket cacheConn = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());
				List<Pair<String, Integer>> updatedlist = new ArrayList<Pair<String, Integer>>(); 
				DataInputStream dis = null;
				DataOutputStream dos = null;
				byte[] buffer = new byte[C_SIZE];
				boolean done = false;
				HashMap<String, Integer> l2Map;

				// get requests 
				while (scannerClient.hasNextLine())
				{
					request = scannerClient.nextLine();
					check = UtilitiesProxyFrontend.parseRequest(request);
					
					if(!check.isCorrect())
					{
						// Malformed request
						pwClient.println(check.getId()); // send error code
						pwClient.flush();
					}
					else
					{
						
						if(check.getType() == 1)
						{
							// GET VIDEO ID_VID
							
							//need to know the available caches
							try
							{
								l2Map = server.getListL2();
								for (Map.Entry<String, Integer> entry : l2Map.entrySet())
								{
									// make a copy
									updatedlist.add(new Pair<String, Integer>(entry.getKey(), entry.getValue()));
								}
							}
							catch(RemoteException re)
							{
								re.printStackTrace();
							}
							
							// Select servers at random -> not the best method but ok for our purposes..
							Collections.shuffle(updatedlist);
							String cacheResponse;
							
							for(Pair<String, Integer>pair : updatedlist)
							{
								try
								{
									System.err.println("Ci passo 1");
									// Ask each proxy if they have the resource
									cacheConn = new Socket(pair.getKey(), pair.getValue());
									
									scannerCache = new Scanner(cacheConn.getInputStream());
									pwCache = new PrintWriter(cacheConn.getOutputStream());
									
									pwCache.println("GET IN_CACHE " + check.getId());
									pwCache.flush();
									
									cacheResponse = scannerCache.nextLine(); // Assume it is alive
									if(cacheResponse.trim().toLowerCase().equals("200 ok"))
									{
										System.err.println("Ci passo 2");
										// I have found the resource!!
										pwClient.println("200 OK");
										pwClient.flush();
										
										// Expected STREAMING AT <URL>
										String[] streaming = scannerCache.nextLine().split(" ");
										if(streaming[0].equals("STREAMING") && streaming[1].equals("AT"))
										{
											String url = streaming[2];
											readVideo(url);
											System.err.println("Video letto, tutto ok");
											done = true;
											break;
										}
										else
										{
											System.err.println("Protocol was ot respected");
											continue;
										}
										
									}
									else
									{
										// Cleanup since we are good persons..
										try
										{
											cacheConn.close();
											scannerCache = null;
											pwCache = null;
											dis = null;
											dos = null;
										}
										catch(IOException ioe)
										{
											ioe.printStackTrace();
										}
										// this server does not have the resource
										continue;
									}
								}				
								catch(IOException ioe)
								{
									ioe.printStackTrace();
								}
							}
							if(!done)
							{
								//No server of L2 had the resource, try to ask in database
								Random generator = new Random(System.currentTimeMillis());
								int cacheRand = generator.nextInt(updatedlist.size()); // int in [0,numCaches)
								Pair<String,Integer> cacheSelected = updatedlist.get(cacheRand);
								
								try
								{
									// ask the selected server for the resource
									cacheConn = new Socket(cacheSelected.getKey(), cacheSelected.getValue());
									
									scannerCache = new Scanner(cacheConn.getInputStream());
									pwCache = new PrintWriter(cacheConn.getOutputStream());
									
									pwCache.println("GET IN_DATABASE " + check.getId());
									pwCache.flush();
									
									cacheResponse = scannerCache.nextLine(); 
									
									if(cacheResponse.trim().toLowerCase().equals("200 ok"))
									{
										// I have found the resource!!
										pwClient.println("200 OK");
										pwClient.flush();
										
										// Expected STREAMING AT <URL>
										String[] streaming = scannerCache.nextLine().split(" ");
										if(streaming[0].equals("STREAMING") && streaming[1].equals("AT"))
										{
											String url = streaming[2];
											readVideo(url);
										}
										else
										{
											System.err.println("Protocol was not respected");
											continue;
										}
									}
									else if(cacheResponse.toLowerCase().equals("404 not found"))
									{
										// The resource does not exists in the database
										pwClient.println("404 NOT FOUND");
										pwClient.flush();
									}
									else
									{
										// I dunno what is happened
										System.err.println("The response was: <" + cacheResponse + ">");
										pwClient.println("ERROR RETRY LATER");
										pwClient.flush();
									}	
									
								}
								catch(IOException ioe)
								{
									ioe.printStackTrace();
								}						
							}
							else
							{
								// CLosing the connection since I served you my lord
								System.out.println("You already have what you need");
								break;
							}
						}
						else if(check.getType() == 99)
						{
							// END THE CONNECTION
							System.out.println("Ending the connection");
							break;
						}
					}
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			finally
			{
				try 
				{
					clientSock.close();
				}
				catch(IOException ioe2)
				{
					System.out.println("Failed to close the connection");
					ioe2.printStackTrace();
				}
			}
			
		}
	}

	private boolean readVideo(String url)
	{
		Process pr;
		try
		{
			Runtime rt = Runtime.getRuntime();
			String command = "vlc " + url;
			pr = rt.exec(command);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			return false;
		}
		return true;
	}
}
