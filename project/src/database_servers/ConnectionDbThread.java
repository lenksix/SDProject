package database_servers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import rmi_servers.SessionManager;

/**
 * A thread called when a connection from the L2 servers is created. 
 * @author Andrea Bugin and Ilie Sarpe
 */
class ConnectionDbThread implements Runnable
{
	private Socket cliSock = null;
	private String request = null;
	private String response = null;
	private String query = null;
	
	private final static String notFound = "404 NOT FOUND";
	private final static int CHUNK_SIZE = 1000;
	private int rmi_port;
	private String rmi_name;
	
	/**
	 * Create a ConnectionDbThread
	 * @param cliSocket the socket of the client
	 * @param rmi_port the port of the RMI registry
	 * @param rmi_name the name in the registry to link to
	 */
	public ConnectionDbThread(Socket cliSocket, int rmi_port, String rmi_name)
	{
		this.cliSock = cliSocket;
		this.rmi_port = rmi_port;
		this.rmi_name = rmi_name;
	}
	
	/**
	 * Manage the connection between the L2 servers and the Relational Database Manager
	 */
	@Override
	public void run()
	{
		boolean done = false;
		Scanner clientReq = null;
		PrintWriter clientResp = null;
		CheckerDB check = null;
		Socket rdbSocket = null;
		Scanner scannerRDB = null;
		PrintWriter pwRDB = null;
		try
		{
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			SessionManager server = (SessionManager) registry.lookup(rmi_name);

			clientReq = new Scanner(cliSock.getInputStream());
			clientResp = new PrintWriter(cliSock.getOutputStream(), true);
	
			while(!done && clientReq.hasNextLine())
			{
				request = clientReq.nextLine();
	
				// NEW PROTOCOL
				// Remember the format of the possible request:
				// 1. GET SP ALL SP CHANNEL_NAME
				
				check = UtilitiesDb.checkQuery(request);
				if (!check.isCorrect())
				{
					response = check.getMessage();
					clientResp.println(response);
				} 
				else
				{
					query = check.getMessage();
					System.out.println(query);
					String json = server.searchQuery(query);
	
					// manage the response for the query to the database
					// we have to decide the appropriate response
					response = "";
					String ipDb = null;
					int portDb = -1;
					
					JSONArray array = new JSONArray(json);
					for(int j = 0; j < array.length(); j++)
					{
						JSONObject jsonObj = array.getJSONObject(j);
						ipDb = jsonObj.getString("ip");
						portDb = jsonObj.getInt("port");
					}
	
					// if the query has no results 404 NOT FOUND is sent, else 200 OK plus the
					// result of the query
					if(portDb == -1)
					{
						response = notFound;
					}
					else
					{
						rdbSocket = new Socket(ipDb, portDb); 
						scannerRDB = new Scanner(rdbSocket.getInputStream());
						pwRDB = new PrintWriter(rdbSocket.getOutputStream(), true);
						
						pwRDB.println("GET VIDEO " + check.getResource());
						String responseRDB = scannerRDB.nextLine();
						if(!responseRDB.equals("200 OK"))
						{
							response = notFound;
							clientResp.println(response);
						}
						else 
						{
							response = "200 OK ";
							clientResp.println(response);
							try
							{
								DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(cliSock.getOutputStream()));
								DataInputStream dis = new DataInputStream(new BufferedInputStream(rdbSocket.getInputStream()));
								int n = 0;
								byte[] chunck = new byte[CHUNK_SIZE];
								while ((n = dis.read(chunck)) != -1)
								{
									dos.write(chunck, 0, n);
									dos.flush();
								}
								dos.close();
								dis.close();
							} 
							catch(IOException ioe2)
							{
								ioe2.printStackTrace();
							}
						}
					}
					done = true;
				}
			}
			System.out.println("Closing the connection with a server L2.");
		}
		catch(RemoteException | NotBoundException re )
		{
			re.printStackTrace();
		} 
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		} 
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally 
		{
			try
			{
				clientReq.close();
				clientResp.close();
				cliSock.close();
				rdbSocket.close();				
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
}
