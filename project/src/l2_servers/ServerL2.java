/**
 * Server Cache class: it manages the requests of the proxies. When there is a proxy request, according to the protocol, 
 * it retrieve the given video from the database, with a request to the ManageDb, or from the cache if the resource is update.
 * @author Andrea Bugin and Ilie Sarpe
 */

package l2_servers;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FilenameUtils;

import javafx.util.Pair;
import rmi_servers.ListL2Manager;

public class ServerL2
{
	private static int dbPort = -1;
	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 49151;
	private static final int OK = 200;	
	private static final int VERBOSE = 100;
	private static final int CACHE_SERVER_MANAGER_PORT = 10000;
	private static final int CHUNK_SIZE = 1000; // size of each chunk of the file in bytes
	
	private static final String localhost = "localhost";
	private static final String registerMe = "REGISTER ME";
	private static final String registrationError = "620 REGISTRATION ERROR";
	private static final String registrationOK = "210 REGISTRATION DONE";
	private static final String notFound = "404 NOT FOUND";
	private static final String hosts = "src//l2_servers//db_host.txt";
	private static final String notInCache = "701 NOT IN_CACHE";
	private static final String notUpdated = "702 NOT UPDATED";
	private static final String cache_default_path = "media//media_cache//";
	private static final String[] formats = {".mp4", ".avi", ".mkv"};

	private static Set<Integer> usedPort = null;
	private static CacheCleaner cc = null;
	private static String dbAddress = null;
	private static HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache = null; // map <ID_VID, TUPLE_VID> = <ID_VID, <PATH, TIME_STAMP>>
	private static ReentrantLock lockMap = null;
	
	private static int STREAM_PORT = 23456; // Default streaming port for http responses (VIDS)
	
	private static final int RMI_REGISTER_SERVICE_PORT = 11300;
	private static final String RMI_REGISTER_SERVICE_NAME = "ListL2Manager";
	
	public static void main(String[] args)
	{
		(new ServerL2()).exec(args);
	}

	private void exec(String[] args)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;
		int SOCKET_PORT;
		usedPort = new CopyOnWriteArraySet<Integer>();
		
		// search an unused port and then add it in the usedPort set
		do
		{
			Random random = new Random();
			SOCKET_PORT = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT + 1); // the range of port we can use is [1024; 49151]
		}
		while(usedPort.contains(SOCKET_PORT));
		
		usedPort.add(SOCKET_PORT);
		
		try
		{
			// Instantiate the server socket
			//SOCKET_PORT = 28517; // TODO: remove this is only for test
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
			long time_limit = 2000000L;
			
			// if the cache goes down we have to restore in vidsCache all the videos
			// TODO: decide how to restore the videos. For now we put in the vidsCache all the videos with timestamp equal to the moment when they are found.
			// Open the default folder of the cache
			File folder = new File("media/media_cache");
			for(File fileFound : folder.listFiles()) 
			{
				System.out.println(fileFound);
				if(fileFound.isDirectory()) {}
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
				System.out.println("Key is " + x);
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
		
		// I have to register this server on the ip_cache table
		
		try
		{
			Registry registry = LocateRegistry.getRegistry(RMI_REGISTER_SERVICE_PORT);
			ListL2Manager server = (ListL2Manager) registry.lookup(RMI_REGISTER_SERVICE_NAME);
			
			String registerResponse = server.serverRegister(localhost, SOCKET_PORT);
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
		catch(IOException e) 
		{
			e.printStackTrace();
		} catch (NotBoundException nbe)
		{
			nbe.printStackTrace();
		}
		
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
							System.err.println("Ci passo 1");
							lockMap.lock();
							try
							{
								if(!vidsCache.containsKey(check.getResource()))
								{
									System.err.println("Ci passo 2");
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
								//if(!true)
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
								System.err.println("Ci passo 3");
								
								String url = createUrl(STREAM_PORT, videoCachePath);
								pwClient.println("STREAMING AT " + url);
								pwClient.flush();
								
								System.err.println("Prima dello stream");
								sendVideo(videoCachePath);
								System.err.println("Dopo lo stream");
								// stream the video
								/*if(!sendVideo(videoCachePath))
								{
									System.err.println("Error in sending the video from cache updated");
								}*/		
										
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

							// First: check if the resource is in cache
							ReentrantReadWriteLock lockResource = null;
							lockMap.lock();
							try
							{
								if(VERBOSE >= 50)
								{
									System.out.println("Took the lock on Map - Get in_cache");
								}
								// I was asked to retrieve the resource in database but i have it in cache
								if(vidsCache.containsKey(check.getResource()))
								{
									boolean updatedResource = false;
									
									try 
									{
										resource = vidsCache.get(check.getResource());
										lockResource = resource.getValue(); // This is for performances
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
											//resource.getValue().readLock().lock();
										}
										else
										{
											updatedResource = false;
											videoCachePath = resource.getKey().getPath();
											
											
											// need to read the resource so need the read lock
											//resource.getValue().writeLock().lock();
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
										lockResource.readLock().lock();
										try
										{
											//Now that i have the read lock and I know file is readable, I can send the file!
											pwClient.println("200 OK");
											pwClient.flush();
											System.err.println("Ci passo 4");
											
											String url = createUrl(STREAM_PORT, videoCachePath);
											pwClient.println("STREAMING AT " + url);
											pwClient.flush();
											
											// stream the video
											if(!sendVideo(videoCachePath))
											{
												System.err.println("Error in sending the video from cache when asked in database");
											}
										}	
										finally
										{
											// unlocking the read on the resource
											lockResource.readLock().unlock();
										}
									}
									// resource in cache but not updated need let's retrieve it from the db
									else
									{
										lockResource.writeLock().lock();
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
												//pwClient.println(response); // TODO: Why is this??.. should recall
												//pwClient.flush();
												DataInputStream dis = null;
												FileOutputStream fos = null;
												String path = cache_default_path + check.getResource();
												
												long readBytes = receiveVideofromDB(video, fos, dis, dbSock, path);
												
												System.out.println("Bytes read from database = " + readBytes);		
											}
											else
											{
												pwClient.println(notFound);
												System.err.println("Resource was not updated in cache and disappeared from the database!!");
											}
										}
										catch(IOException ioe)
										{
											ioe.printStackTrace();
										}
										finally
										{
											lockResource.writeLock().unlock();
										}
										
										// Now that i hve already updated the resource i can send the video
										lockResource.readLock().lock();
										try
										{
											// Now that the video is in cache I can send it
											pwClient.println("200 OK");
											pwClient.flush();
											
											String url = createUrl(STREAM_PORT, videoCachePath);
											pwClient.println("STREAMING AT " + url);
											pwClient.flush();
											
											// stream the video
											if(!sendVideo(videoCachePath))
											{
												System.err.println("Error in sending the video from brand new cache");
											}
										}
										finally
										{
											lockResource.readLock().unlock();
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

										rwlRes = new ReentrantReadWriteLock(true); 
										try 
										{
											//Insert the video in the "cache"
											TupleVid newRes = new TupleVid((path + ".mp4"), System.currentTimeMillis());		
											if(vidsCache.put(check.getResource(), new Pair<TupleVid, ReentrantReadWriteLock>(newRes, rwlRes)) != null)
											{
												System.err.println("Some concurrency problem, the resource was already present in the map!!");
											}									
										}
										finally
										{
											//release the lock on the cache
											lockMap.unlock();
										}
										
										// get the write lock
										rwlRes.writeLock().lock();
										try
										{
											long readBytes = receiveVideofromDB(video, fos, dis, dbSock, path);
				
											System.out.println("Bytes read from database = " + readBytes);
										}
										finally
										{
											rwlRes.writeLock().unlock();
										}
										
										// Now that the video is in cache i can send it
										rwlRes.readLock().lock();
										try
										{
											// Now that the video is in cache I can send it
											pwClient.println("200 OK");
											pwClient.flush();
											
											String url = createUrl(STREAM_PORT, videoCachePath);
											pwClient.println("STREAMING AT " + url);
											pwClient.flush();
											
											// stream the video
											if(!sendVideo(videoCachePath))
											{
												System.err.println("Error in sending the video from brand new cache");
											}
										}
										finally
										{
											rwlRes.readLock().unlock();
										}
										
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
					break; //TODO: think if this is useful... i think not
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
	
	private String createUrl(int port,  String path)
	{
		String[] chunks = path.split("/");
		String id = chunks[chunks.length - 1]; // ID of the video
		return "http://localhost:" + STREAM_PORT + "/" + id;
	}
	
	private boolean sendVideo(String videoPath)
	{
		Process pr;
		try
		{
			String[] chunks = videoPath.split("/");
			String id = chunks[chunks.length - 1]; // ID of the video
			Runtime rt = Runtime.getRuntime();
			
			String command = "cvlc -vvv " + videoPath + " --sout '#transcode{vcodec=h264,acodec=mp3,samplerate44100}:std{access=http,mux=ffmpeg{mux=flv},dst=localhost:"
					+ STREAM_PORT + "/" + id +"}'";
			System.err.println("Creating the stream\n" + command);
			pr = rt.exec(new String[]{"bash","-c",command});
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			return false;
		}
		return true;
	}
	
	// return the size of the bytes read
	private long receiveVideofromDB(File video, FileOutputStream fos, DataInputStream dis, Socket dbSock, String path)
	{
		
		long readBytes = 0;
		try
		{
			video = new File(path);
			fos = new FileOutputStream(video + ".mp4");
			dis = new DataInputStream(new BufferedInputStream(dbSock.getInputStream()));

			byte[] chunck = new byte[CHUNK_SIZE];
			
			int n;
			while ((n = dis.read(chunck)) != -1)
			{
				fos.write(chunck, 0, n);
				fos.flush();
				readBytes += n;
			}
			fos.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		return readBytes;
	}
	
	/**
	 * This is an exception thrown when the current server is not able to get register in the ip_cache table.
	 * @author Andrea Bugin and Ilie Sarpe
	 */
	private class UnregisteredServerException extends RuntimeException
	{
		public UnregisteredServerException()
		{
			super();
		}
	}
}
