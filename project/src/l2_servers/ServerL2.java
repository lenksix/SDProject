package l2_servers;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.HashMap;
import org.apache.commons.io.FilenameUtils;
import javafx.util.*;
import java.util.concurrent.locks.*;


public class ServerL2
{
	private final static int SOCKETPORT = 8860;
	private final static String hosts = "src//l2_servers//db_host.txt";
	private final static String notInCache = "701 NOT IN_CACHE";
	private final static String CACHE_DEFAULT_PATH = "media//media_cache//";
	private final static String[] formats = {".mp4", ".avi", ".mkv"};
	private final static int CHUNKSIZE = 1000; // size of each chunk of the file in bytes

	private static String dbAddress = null;
	private static int dbPort = -1;
	private static HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache = null; // map <ID_VID, TUPLE_VID> = <ID_VID, <PATH, TIME_STAMP>>
	private static ReentrantLock lockMap = null;
	// private static HashMap<String, String[]> namesCache = null; // map <ID_CH,
	// Video[]> future implementation

	public static void main(String[] args)
	{
		(new ServerL2()).exec(args);
	}

	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		Socket clientSock = null;

		try
		{
			// Instantiate the server socket
			/*
			 * PROBLEM: We need to add the ip address of this L2 server to a common data
			 * structure for the L1 servers
			 */
			serverSock = new ServerSocket(SOCKETPORT);
			System.out.println("Ok, Serversocket created!");

			// Need the address of the "db manager", assume this file is just for the boot,
			// then is modified according to some protocol
			BufferedReader file = new BufferedReader(new FileReader(hosts.trim()));
			String line = file.readLine();
			file.close();

			dbAddress = line.split(" ")[0];
			dbPort = Integer.parseInt(line.split(" ")[1]);

			System.out.println("Ok, file read!" + " <" + dbAddress + "> " + " <" + dbPort + "> ");
			// initialize the local cache
			vidsCache = new HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>>();
			lockMap = new ReentrantLock();
			//***** TEST *******
			//CacheCleaner cc = new CacheCleaner(vidsCache, 1000);
			//cc.run();
			//System.exit(1);
			
			// if the cache goes down we have to restore in vidsCache all the videos
			// TODO: decide how to restore the videos. For now we put in the vidsCache all the videos with timestamp equal to the moment when they are found.
			// Open the default folder of the cache
			File folder = new File("media/media_cache");
			for(File fileFound : folder.listFiles()) 
			{
				if(fileFound.isDirectory()) 
				{ } 	// TODO: need to decide if we find a directory
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
			
			//TODO: we have to implement the garbage collection

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

		while (true)
		{
			try
			{
				// accept a connection, when a client is connected, a new thread is created to
				// manage the connection
				// (no thread pooling) for now
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to be created.");
				ConnectionThread ct = new ConnectionThread(clientSock);
				ct.start();
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	private class ConnectionThread extends Thread
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

			// boolean connectStatus = connectDB(dbAddress, dbPort);
			try
			{
				String query = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());

				while (scannerClient.hasNextLine())
				{
					// Check syntax of the request
					query = scannerClient.nextLine();
					check = UtilitiesL2.checkQuery(query);

					if (check.isCorrect())
					{
						// 1.GET SP IN_CACHE SP ID_RISORSA
						// where ID_RISORSA is a video, in further development we should add a code to
						// request also a
						// list of the videos of a specific channel
						if (check.getType() == 1)
						{
							boolean ownRes = false;
							Pair<TupleVid, ReentrantReadWriteLock> resource = null;
							String videoCachePath = null;
							Lock lockfileRead = null;
							// Verify if the cache contains the resource
							// Brand new Section!!
							try
							{
								lockMap.lock();
								if (vidsCache.containsKey(check.getResource()))
								{ 	// NOTE!!
									// WE need to update this code by sending the video at the location
									ownRes = true;
									resource = vidsCache.get(check.getResource());
									//get the read lock to the file
									lockfileRead = resource.getValue().readLock();
									videoCachePath = resource.getKey().getPath();
									resource.getKey().setTimeStamp(System.currentTimeMillis());
									// don't need to reinsert the same object, the timestamp is already changed!
									//Not so sure...
									//vidsCache.replace(check.getResource(), new TupleVid(resource, System.currentTimeMillis()));
								}
								else
								{
									// We send that we don't have the resource
									pwClient.println(notInCache);
									pwClient.flush();
								}
								
							}
							finally
							{
								lockMap.unlock();
							}
							//trying to send the video if i have it in cache
							if(ownRes)
							{
								try
								{
									lockfileRead.lock();
									
									//Now that i have the read lock i can send the file!
									pwClient.println("200 OK");
									pwClient.flush();
									dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
									fileStream = new FileInputStream(new File(videoCachePath));
									int n = 0;
									byte[] chunck = new byte[CHUNKSIZE];
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
								finally
								{
									lockfileRead.unlock();
								}
							}
						} 
						else
						{
							// Retrieve the resource and send it to the L1 server
							// and bring it in the cache -> we need to know the most recent

							// First: connect to db Manager
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
								if (Integer.parseInt((response.split(" ")[0])) == 200)
								{
									/* LEGGI VIDEO, SALVARLO IN CACHE E MANDARLO A L1 - NON COMPLETO!! */
									pwClient.println(response);
									pwClient.flush();
									DataInputStream dis = null;
									FileOutputStream fos = null;

									try
									{
										String path = CACHE_DEFAULT_PATH + check.getResource();

										// WRONGGGGGG!!!!!!!!!!!!!!!!!
										// CONCURRENCY
										video = new File(path);
										fos = new FileOutputStream(video + ".mp4");
										dis = new DataInputStream(new BufferedInputStream(dbSock.getInputStream()));
										dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
										byte[] chunck = new byte[CHUNKSIZE];
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
										try
										{
											lockMap.lock();
											//Insert the video in the "cache"
											TupleVid newRes = new TupleVid(check.getResource(), System.currentTimeMillis());
											ReentrantReadWriteLock rwlRes = new ReentrantReadWriteLock(true);
											if(vidsCache.put(check.getResource(), new Pair<TupleVid, ReentrantReadWriteLock>(newRes, rwlRes)) != null)
											{
												System.out.println("Some concurrency problem, the resource was already present in the map!!");
											}
										}
										finally
										{
											lockMap.unlock();
										}
									} 
									catch (IOException ioe)
									{
										ioe.printStackTrace();
									}
								} 
								else
								{
									pwClient.println("404 NOT FOUND"); /* Video non in DB */
								}
								dbSock.close();
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
					else
					{
						// Malformed request
						pwClient.println(check.getResource()); // send error code
						pwClient.flush();
						continue;
					}
				}
				try
				{
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
}
