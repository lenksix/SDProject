package l2_servers;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.HashMap;


public class ServerL2 
{

	private final static int SOCKETPORT = 8860;
	private final static String hosts = "src//l2_servers//db_host.txt";
	private final static String notInCache = "701 NOT IN_CACHE";
	private final static String CACHE_DEFAULT_PATH = "media//media_cache//";
	
	private static String dbAddress = null;
	private static int dbPort = -1;
	private static HashMap<String,String> vidsCache = null; // map <ID_VID, /../localpath>
	private static HashMap<String, String[]> namesCache = null; // map <ID_CH, Video[]> future implementation
	
	
	public static void main(String[] args) {
		(new ServerL2()).exec(args);
	}
	
	public void exec(String[] args) {
		ServerSocket serverSock = null;
		Socket clientSock = null;
		
		
		try 
		{
			//Instantiate the server socket
			/*PROBLEM: We need to add the ip address of this L2 server to a common data structure for the L1 servers*/
			serverSock = new ServerSocket(SOCKETPORT); 
			System.out.println("Ok, Serversocket created!");
         
			//Need the address of the "db manager", assume this file is just for the boot, then is modified according to some protocol
			BufferedReader file = new BufferedReader(new FileReader(hosts.trim()));
			String line = file.readLine();
			file.close();
			
			dbAddress = line.split(" ")[0];
			dbPort = Integer.parseInt(line.split(" ")[1]);
			
			System.out.println("Ok, file read!" + " <"+ dbAddress + "> "+ " <" + dbPort + "> ");
			//initialize the local cache
			vidsCache = new HashMap<String,String>();
			namesCache = new HashMap<String,String[]>();
			
		} 
		catch(IOException ioe) {
			try {
				serverSock.close();
			}
			catch(IOException ioe2) {
				System.out.println("Failed to close serverSocket");
				ioe2.printStackTrace();
			}
			ioe.printStackTrace();         
		}
		
		
		while(true) {
			try {
				// accept a connection, when a client is connected, a new thread is created to manage the connection
				// (no thread pooling) for now
				clientSock = serverSock.accept();
				System.out.println("Connection accepted, a new thread is going to be created.");
				ConnectionThread ct = new ConnectionThread(clientSock);
				ct.start();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
   
	private class ConnectionThread extends Thread 
	{
		Socket clientSock = null;
		Socket dbSock = null;
		
		public ConnectionThread(Socket s) {
			this.clientSock = s;
		}
		
		public void run() 
		{
			OutputStream osClient = null;
			InputStream isClient = null;
			Scanner scannerClient = null;
			PrintWriter pwClient = null;
			Checker check = null;
			
			OutputStream osDb = null;
			InputStream isDb = null;
			Scanner scannerDb = null;
			PrintWriter pwDb = null;
			
			//boolean connectStatus = connectDB(dbAddress, dbPort);
			try 
			{
				String query = null;
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream());
				
				//outer:
				while(scannerClient.hasNextLine()) 
				{
					// Check syntax of the request
					query = scannerClient.nextLine();
					check = UtilitiesL2.checkQuery(query);
					
					if(check.isCorrect()) 
					{
						// 1.GET SP IN_CACHE SP ID_RISORSA 
						// where ID_RISORSA is a video, in further development we should add a code to request also a
						// list of the videos of a specific channel
						if(check.getType()==1) 
						{
							boolean ownRes = false; 
							String resource = null;
							// Verify if the cache contains the resource
							synchronized(vidsCache) 
							{
								if(vidsCache.containsKey(check.getResource()))
								{	// NOTE!!
									// WE need to update this code by sending the video at the location
									ownRes = true;
									resource = vidsCache.get(check.getResource());
								}
							}
							if(ownRes)
							{
								pwClient.println("VIDEO A " + resource + "NON VA FATTO COSI'");
								pwClient.flush();
							}
							else
							{
								// We send that we don't have the resource
								pwClient.println(notInCache);
								pwClient.flush();
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
								int n =-1;
								//while(scannerDb.hasNextLine())
								//{
									response = scannerDb.nextLine();
									if(Integer.parseInt((response.split(" ")[0])) == 200)
									{
										/* LEGGI VIDEO, SALVARLO IN CACHE E MANDARLO A L1 - NON COMPLETO!!*/
										pwClient.println(response);
										DataInputStream dis = null;
										FileOutputStream fos = null;
										
										try 
										{
											String path = CACHE_DEFAULT_PATH + check.getResource();
										   File video = new File(path);
										   fos = new FileOutputStream(video);
										   dis = new DataInputStream(new BufferedInputStream(dbSock.getInputStream()));  
										   byte[] chunck = new byte[1000];
										   while((n = dis.read(chunck)) != -1)
										   {
										      fos.write(chunck,0,n);
										      fos.flush();
										   }
										   fos.close();
										   synchronized(vidsCache)
										   {
										      vidsCache.put(check.getResource(), path);
										   }
										   
										}
										catch(IOException ioe)
										{
										   ioe.printStackTrace();
										}
									}
									
									else
									{
										pwClient.println("404 NOT FOUND"); /* Video non in DB */
									}
									pwClient.flush();
									dbSock.close();
								//}
							}
							catch(UnknownHostException uhe) 
							{
								// NOTE need to define
								pwClient.println("AN ERROR MESSAGE");
								pwClient.flush();
								uhe.printStackTrace();
							}
						}
					}
					else {
						// Malformed request
						pwClient.println(check.getResource()); // send error code
						pwClient.flush();
						continue;
					}
					
					// Ask for the resource to the manager of the database
					
					/*
	               	System.out.println("Il client mi ha detto <" + line + ">");
	               	pw.println("Grazie di avermi detto " + line);
	               	pw.flush();
					 */
					
				}
				try 
				{
					clientSock.close();
	            }
	            catch(IOException ioe2) {
	            	ioe2.printStackTrace();
	            	}
	         }
	         catch(IOException ioe) {
	         	ioe.printStackTrace();
	         }
		}
	}
}
