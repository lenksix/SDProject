package l2_servers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FilenameUtils;

import javafx.util.Pair;


public class ServerL2
{
	private static int dbPort = -1;
	private final static int OK = 200;	
	private final static int VERBOSE = 100;
	private final static int CACHE_SERVER_MANAGER_PORT = 10000;
	private final static int CHUNK_SIZE = 1000; // size of each chunk of the file in bytes
	
	private final static String localhost = "localhost";
	private final static String registrationError = "620 REGISTRATION ERROR";
	private final static String registrationOK = "210 REGISTRATION DONE";
	private final static String notFound = "404 NOT FOUND";
	private final static String hosts = "src//l2_servers//db_host.txt";
	private final static String notInCache = "701 NOT IN_CACHE";
	private final static String notUpdated = "702 NOT UPDATED";
	private final static String cache_default_path = "media//media_cache//";
	private final static String[] formats = {".mp4", ".avi", ".mkv"};

	private static Set<Integer> usedPort = null;
	private static CacheCleaner cc = null;
	private static String dbAddress = null;
	private static HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache = null; // map <ID_VID, TUPLE_VID> = <ID_VID, <PATH, TIME_STAMP>>
	private static ReentrantLock lockMap = null;
	
	public static void main(String[] args)
	{
		(new ServerL2()).exec(args);
	}

	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;
		int SOCKET_PORT;
		usedPort = new CopyOnWriteArraySet<Integer>();
		
		// search an unused port and then add it in the usedPort set
		do
		{
			SOCKET_PORT = (int)(1024 + (1 + 49151 - 1024) * Math.random()); // the range of port we can use is [1024; 49151]
		}
		while(usedPort.contains(SOCKET_PORT));
		
		usedPort.add(SOCKET_PORT);
		
		try
		{
			// Instantiate the server socket
			SOCKET_PORT = 28517; // TODO: remove this is only for test
			serverSock = new ServerSocket(SOCKET_PORT);
			System.out.println("Ok, Server L2 created at " + SOCKET_PORT );

			// Need the address of the "db manager", assume this file is just for the boot,
			// then is modified according to some protocol
			Scanner file = new Scanner(new FileReader(hosts.trim()));
			String line = file.nextLine();
			file.close();

			dbAddress = line.split(" ")[0];
			dbPort = Integer.parseInt(line.split(" ")[1]);

			System.out.println("Ok, file read!" + " <" + dbAddress + "> " + " <" + dbPort + "> ");
			
			// initialize the local cache
			vidsCache = new HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>>();
			lockMap = new ReentrantLock();
			long time_limit = 20000L;
			
			// if the cache goes down we have to restore in vidsCache all the videos
			// TODO: decide how to restore the videos. For now we put in the vidsCache all the videos with timestamp equal to the moment when they are found.
			// Open the default folder of the cache
			File folder = new File("media/media_cache");
			for(File fileFound : folder.listFiles()) 
			{
				System.out.println(fileFound);
				if(fileFound.isDirectory()) {} 	// TODO: need to decide if we find a directory
				else
				{
					for(String format : formats) // for every file check if it is a video (if it ends with one the formats defined above)
					{
						if(fileFound.getName().endsWith(format))
						{
							String id_vid = FilenameUtils.removeExtension(fileFound.getName());
							TupleVid video_tup = new TupleVid(fileFound.getPath(), System.currentTimeMillis());
							ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
							Pair< TupleVid, ReentrantReadWriteLock> toInsert = new Pair<TupleVid, ReentrantReadWriteLock>(video_tup, rwl);
							vidsCache.put(id_vid, toInsert);
						}
					}
		        }
		    }
			vidsCache.forEach((x,y)->
			{
				// Accessing the TupleViod in the pair <TupleVid, rwl>
				System.out.println("Filename = " + FilenameUtils.removeExtension(x));
				System.out.println("Path = " + y.getKey().getPath());
				System.out.println("Timestamp = " + y.getKey().getTimeStamp());
			});
			
			//System.exit(1);
			
			//***** TEST *******
			cc = new CacheCleaner(vidsCache, lockMap, time_limit);
			new Thread(cc).start();
		} 
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			try
			{
				serverSock.close();
			} 
			catch (IOException ioe2)
			{
				System.out.println("Failed to close serverSocket");
				ioe2.printStackTrace();
			}
		}
		/*
		// I have to register this server on the ip_cache table
		try
		{
			try(Socket registerSocket = new Socket(localhost, CACHE_SERVER_MANAGER_PORT);
				Scanner registerScanner = new Scanner(registerSocket.getInputStream());
				ObjectOutputStream registerStream = new ObjectOutputStream(registerSocket.getOutputStream()); ) // try-with-resources
			{
				registerStream.writeObject(new Pair<>(localhost, SOCKET_PORT));
				registerStream.flush();
				String registerResponse = registerScanner.nextLine();
				
				if(registerResponse.equalsIgnoreCase(registrationOK)) {}
				else if(registerResponse.equalsIgnoreCase(registrationError))
				{
					throw new UnregisteredServerException();
				}
				else
				{
					System.out.println("Undefined contol sequence in registration response: quit.");
					System.exit(1);
				}
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}*/
		
		try 
		{
			while(true)
			{
				try
				{
					// accept a connection, when a client is connected, a new thread is created to
					// manage the connection
					// (no thread pooling) for now
					clientSock = serverSock.accept();
					System.out.println("Connection accepted, a new thread is going to be created.");
					Thread connectionThread = new Thread(new ConnectionThread(clientSock));
					connectionThread.start();
				} 
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
		finally 
		{
			usedPort.remove(SOCKET_PORT);
		}
	}

	private class ConnectionThread implements Runnable
	{
		Socket clientSock = null;
		Socket dbSock = null;

		public ConnectionThread(Socket s)
		{
			this.clientSock = s;
		}

		public void run()
		{
			Scanner scannerClient = null;
			PrintWriter pwClient = null;
			CheckerL2 check = null;

			Scanner scannerDb = null;
			PrintWriter pwDb = null;

			File video = null;
			DataOutputStream dos = null;
			FileInputStream fileStream = null;

			Pair<TupleVid, ReentrantReadWriteLock> resource = null;
			String videoCachePath = null;
			try
			{
				String query = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());

				while(scannerClient.hasNextLine())
				{
					// Check syntax of the request
					query = scannerClient.nextLine();
					check = UtilitiesL2.checkQuery(query);

					if(!check.isCorrect())
					{
						// Malformed request
						pwClient.println(check.getResource()); // send error code
						pwClient.flush();
					}
					else
					{
						// 1.GET SP IN_CACHE SP ID_RISORSA
						// where ID_RISORSA is a video, in further development we should add a code to
						// request also a list of the videos of a specific channel
						if (check.getType() == 1)
						{
							// Verify if the cache contains the resource
							// Brand new Section!!
							lockMap.lock();
							try
							{
								if(!vidsCache.containsKey(check.getResource()))
								{
									if(VERBOSE >= 50)
									{
										System.out.println("The resource: <" + check.getResource() + "> is not in cache");
									}
									// We send that we don't have the resource
									pwClient.println(notInCache);
									pwClient.flush();
									continue;
								}
								// get the resource since it is in cache
								resource = vidsCache.get(check.getResource());
								if(VERBOSE >= 50)
								{
									System.out.println("Already accessed map for resource: <" + check.getResource() + ">");
								}
								// check if the resource is still updated 
								// TODO: improve the way cc is accessed
								if (!(resource.getKey().getTimeStamp() + cc.getTimeLimit() > System.currentTimeMillis())) 
								{
									if(VERBOSE >= 50)
									{
										System.out.println("The resource: <" + check.getResource() + "> is not updated");
									}
									// We send that the resource is not updated
									pwClient.println(notUpdated);
									pwClient.flush();
									continue;
								}
								else
								{ 	
									if(VERBOSE >= 50)
									{
										System.out.println("Getting: <" + check.getResource() + "> from cache");
									}
									//get the path of the resource
									videoCachePath = resource.getKey().getPath();
									// need to read the resource so need the read lock
									resource.getValue().readLock().lock();
								}
							}
							finally
							{
								if(VERBOSE >= 50)
								{
									System.out.println("Unlocked map");
								}
								// i don't need the lock on the map since i have the lock on the resource
								lockMap.unlock();
							}
							try 
							{
								if(VERBOSE >= 50)
								{
									System.out.println("reading the file: <" + videoCachePath + "> and sending it");
								}
								//Now that i have the read lock and I know file is readable, I can send the file!
								pwClient.println("200 OK");
								pwClient.flush();
								// !! TRIAL- don't know if needed 
								//if(dos == null)
								//{
									dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
								//}
								fileStream = new FileInputStream(new File(videoCachePath));
								int n = 0;
								byte[] chunck = new byte[CHUNK_SIZE];
								long readBytes = 0;
								while ((n = fileStream.read(chunck)) != -1)
								{
									dos.write(chunck, 0, n);
									dos.flush();
									readBytes += n;
								}
								System.out.println("Bytes read from cache = " + readBytes);
								fileStream.close();
										
										
							}
							catch(IOException ioe)
							{
								System.err.println("For some reason file was not found");
								ioe.printStackTrace();
							}	
							finally
							{
								if(VERBOSE >= 50)
								{
									System.out.println("Unlocking the readlock on resource");
								}
								// unlocking the read on the resource
								resource.getValue().readLock().unlock();
							}
							
						}
						else
						{
							// Retrieve the resource and send it to the L1 server
							// and bring it in the cache -> we need to know the most recent

							// First: check if the resource is in database
							lockMap.lock();
							try
							{
								if(VERBOSE >= 50)
								{
									System.out.println("Took the lock on Map - Get in_cache");
								}
								if(vidsCache.containsKey(check.getResource()))
								{
									boolean updatedResource = false;
									try 
									{
										resource = vidsCache.get(check.getResource());
										if(VERBOSE >= 50)
										{
											System.out.println("Getting the timestamp - Get in_cache" + resource.getKey().getPath() + ">");
										}
										// check if the resource is still updated 
										if ((resource.getKey().getTimeStamp() + cc.getTimeLimit() >= System.currentTimeMillis())) 
										{
											updatedResource = true;
											//get the path of the resource
											videoCachePath = resource.getKey().getPath();
											// need to read the resource so need the read lock
											resource.getValue().readLock().lock();
										}
										else
										{
											updatedResource = false;
											videoCachePath = resource.getKey().getPath();
											// need to read the resource so need the read lock
											resource.getValue().writeLock().lock();
											resource.getKey().setTimeStamp(System.currentTimeMillis());
										}
									}
									finally
									{
										if(VERBOSE >= 50)
										{
											System.out.println("Unlock the map - Get in_cache");
										}
										lockMap.unlock();
									}
									if(updatedResource)
									{
										try
										{
											//Now that i have the read lock and I know file is readable, I can send the file!
											pwClient.println("200 OK");
											pwClient.flush();
											//if(dos==null)
											//{
												dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
											//}
											fileStream = new FileInputStream(new File(videoCachePath));
											int n1 = 0;
											byte[] chunck = new byte[CHUNK_SIZE];
											long readBytes = 0;
											while ((n1 = fileStream.read(chunck)) != -1)
											{
												dos.write(chunck, 0, n1);
												dos.flush();
												readBytes += n1;
											}
											System.out.println("Bytes read from cache = " + readBytes);
											fileStream.close();
										}	
										catch(IOException ioe)
										{
											System.err.println("For some reason file was not found");
											ioe.printStackTrace();
										}	
										finally
										{
											// unlocking the read on the resource
											resource.getValue().readLock().unlock();
										}
									}
									// resource in cache but not updated need let's retrieve it from the db
									else
									{
										try
										{
											dbSock = new Socket(dbAddress, dbPort);
											
											scannerDb = new Scanner(dbSock.getInputStream());
											pwDb = new PrintWriter(dbSock.getOutputStream());
			
											String request = UtilitiesL2.queryVid(check.getResource());
											pwDb.println(request);
											pwDb.flush();
			
											String response = null;
											int n = -1;
											response = scannerDb.nextLine();
											if (Integer.parseInt((response.split(" ")[0])) == OK)
											{
												pwClient.println(response);
												pwClient.flush();
												DataInputStream dis = null;
												FileOutputStream fos = null;
												String path = cache_default_path + check.getResource();
												
												/* LEGGI VIDEO, SALVARLO IN CACHE E MANDARLO A L1 - NON COMPLETO!! */
												video = new File(path);
												fos = new FileOutputStream(video + ".mp4");
												dis = new DataInputStream(new BufferedInputStream(dbSock.getInputStream()));
												//if(dos == null)
												//{
													dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
												//}
												byte[] chunck = new byte[CHUNK_SIZE];
												long readBytes = 0;
												while ((n = dis.read(chunck)) != -1)
												{
													fos.write(chunck, 0, n);
													fos.flush();
													dos.write(chunck, 0, n);
													dos.flush();
													readBytes += n;
												}
													
												System.out.println("Bytes read from database = " + readBytes);
												fos.close();	
											}
											else
											{
												System.err.println("Resource was not updated in cache and disappeared from the database!!");
											}
										}
										catch(IOException ioe)
										{
											ioe.printStackTrace();
										}
										finally
										{
											resource.getValue().writeLock().unlock();
										}
									}
								}
								//resource was not in cache hence retrieve it from the db
								else
								{
									//Connect to the database
									dbSock = new Socket(dbAddress, dbPort);
	
									scannerDb = new Scanner(dbSock.getInputStream());
									pwDb = new PrintWriter(dbSock.getOutputStream());
	
									String request = UtilitiesL2.queryVid(check.getResource());
									pwDb.println(request);
									pwDb.flush();
	
									String response = null;
									int n = -1;
									response = scannerDb.nextLine();
									if (Integer.parseInt((response.split(" ")[0])) == OK)
									{
										ReentrantReadWriteLock rwlRes;
										
										pwClient.println(response);
										pwClient.flush();
										DataInputStream dis = null;
										FileOutputStream fos = null;
										String path = cache_default_path + check.getResource() ;

										try 
										{
											//Insert the video in the "cache"
											TupleVid newRes = new TupleVid((path + ".mp4"), System.currentTimeMillis());
											rwlRes = new ReentrantReadWriteLock(true); 
											if(vidsCache.put(check.getResource(), new Pair<TupleVid, ReentrantReadWriteLock>(newRes, rwlRes)) != null)
											{
												System.err.println("Some concurrency problem, the resource was already present in the map!!");
											}
											// get the write lock
											rwlRes.writeLock().lock();
											
										}
										finally
										{
											//release the lock on the cache
											lockMap.unlock();
										}
										/* LEGGI VIDEO, SALVARLO IN CACHE E MANDARLO A L1 - NON COMPLETO!! */
										
										try
										{
											video = new File(path);
											fos = new FileOutputStream(video + ".mp4");
											dis = new DataInputStream(new BufferedInputStream(dbSock.getInputStream()));
											//if(dos==null)
											//{
												dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
											//}
											byte[] chunck = new byte[CHUNK_SIZE];
											long readBytes = 0;
											while ((n = dis.read(chunck)) != -1)
											{
												fos.write(chunck, 0, n);
												fos.flush();
												dos.write(chunck, 0, n);
												dos.flush();
												readBytes += n;
											}
											
											System.out.println("Bytes read from database = " + readBytes);
											fos.close();
											
										}
										catch (IOException ioe)
										{
											ioe.printStackTrace();
										}
										finally
										{
											rwlRes.writeLock().unlock();
										}
										// Video know is in cache we have to set the right timestamp
										//vidsCache.get(check.getResource()).getKey().setTimeStamp(System.currentTimeMillis());
										
									} 
									else
									{
										pwClient.println(notFound); /* Video not in DB */
										pwClient.flush();
									}
									dbSock.close();
								} 
							}
							catch (UnknownHostException uhe)
							{
								// NOTE need to define
								pwClient.println("AN ERROR MESSAGE");
								pwClient.flush();
								uhe.printStackTrace();
							}
						}
					} 
				}
				try
				{
					if(dos != null)
						dos.close();
					clientSock.close();
				} 
				catch (IOException ioe2)
				{
					ioe2.printStackTrace();
				}
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
	
	private class UnregisteredServerException extends RuntimeException
	{
		public UnregisteredServerException()
		{
			super();
		}
	}
}
