package proxies_frontend;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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


import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaViewer;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.demos.VideoImage;
import com.xuggle.xuggler.io.DataInputOutputHandler;
import com.xuggle.xuggler.io.XugglerIO;

import javafx.util.Pair;




public class ProxyFrontend implements Runnable
{
	final static int DEFAULT_PORT = 9856;
	final static int WAIT_TIME = 15000;
	private final static int C_SIZE = 1000; // size of each chunk of the file in bytes
	
	private static HashMap<String, Integer> l2Map; // list of L2 servers <IP, PORT>
	private static ReentrantLock mapLock; // need the lock for the updates on l2 list
	private static List<Socket> sockAlive;
	private static ReentrantLock lockSockets;
	private static Condition socketAvailable;
	private static ManageCacheList manager;
	
	public static void main(String[] args)
	{
		(new ProxyFrontend()).exec(args);
	}
	
	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		
		sockAlive = new LinkedList<Socket>();
		l2Map = new HashMap<String, Integer>(); 
		mapLock = new ReentrantLock();
		lockSockets = new ReentrantLock();
		socketAvailable = lockSockets.newCondition();
		
		try
		{
			serverSock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Ok, Serversocket <proxy> created!");

			//TODO: need a distributed structure of l2 servers!!
			
			manager = new ManageCacheList(l2Map, mapLock);
			new Thread(manager).start();
			//l2Map.put("localhost", 28517); // just for test
			
			//TODO: need to instantiate a class that updates the map.. pings etc..
			
			
		}
		catch(IOException ioe)
		{
			System.err.println("Failed to instatiate the <proxy> serversocket ");
			ioe.printStackTrace();
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

	@SuppressWarnings("deprecation")
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
							mapLock.lock();
							try
							{
								for (Map.Entry<String, Integer> entry : l2Map.entrySet())
								{
									// make a copy
									updatedlist.add(new Pair<String, Integer>(entry.getKey(), entry.getValue()));
								}
							}
							finally
							{
								mapLock.unlock();
							}
							
							// Select servers at random -> not the best method but ok for our purposes..
							Collections.shuffle(updatedlist);
							String cacheResponse;
							
							for(Pair<String, Integer>pair : updatedlist)
							{
								try
								{
									// Ask each proxy if it has the resource
									cacheConn = new Socket(pair.getKey(), pair.getValue());
									
									scannerCache = new Scanner(cacheConn.getInputStream());
									pwCache = new PrintWriter(cacheConn.getOutputStream());
									
									pwCache.println("GET IN_CACHE " + check.getId());
									pwCache.flush();
									
									cacheResponse = scannerCache.nextLine(); // Assume it is alive
									System.out.println("Prima del parsing..");
									if(cacheResponse.trim().toLowerCase().equals("200 ok"))
									{
										// I have found the resource!!
										pwClient.println("200 OK");
										pwClient.flush();
										
										dis = new DataInputStream(new BufferedInputStream(cacheConn.getInputStream()));
										dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
										int n = -1; // default out of range value
										long bytesRead = 0L; // just for debug puposes TODO: remove this in the final version
										while((n = dis.read(buffer)) != -1)
										{
											dos.write(buffer, 0, n);
											dos.flush();
											bytesRead += n; 
										}
										System.out.println("Number of bytes read: <" + bytesRead + ">");
										done = true;
										break;
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
										
										dis = new DataInputStream(new BufferedInputStream(cacheConn.getInputStream()));
										dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
										int n = -1; // default out of range value
										long bytesRead = 0L; // just for debug puposes TODO: remove this in the final version
										while((n = dis.read(buffer)) != -1)
										{
											dos.write(buffer, 0, n);
											dos.flush();
											bytesRead += n; 
										}
										System.out.println("Number of bytes read: <" + bytesRead + ">");
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



}
