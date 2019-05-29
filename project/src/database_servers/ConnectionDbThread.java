package database_servers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import rmi_servers.SessionManager;

class ConnectionDbThread implements Runnable
{
	private Socket cliSock = null;
	private String request = null;
	private String response = null;
	private String query = null;
	private ResultSet queryResult = null;
	//private Session session = null;
	
	private final static String notFound = "404 NOT FOUND";
	private final static int CHUNK_SIZE = 1000;
	private int rmi_port;
	private String rmi_name;

	public ConnectionDbThread(Socket cliSocket, int rmi_port, String rmi_name)
	{
		this.cliSock = cliSocket;
		this.rmi_port = rmi_port;
		this.rmi_name = rmi_name;
	}

	public void run()
	{
		boolean done = false;
		Scanner clientReq = null;
		PrintWriter clientResp = null;
		// connect to the cluster
		// TODO: Spostare su RMI
		//session.execute("USE streaming;");
		CheckerDB check = null;
		try
		{
			Registry registry = LocateRegistry.getRegistry(rmi_port);
			SessionManager server = (SessionManager) registry.lookup(rmi_name);
	
			try
			{
				clientReq = new Scanner(cliSock.getInputStream());
				clientResp = new PrintWriter(cliSock.getOutputStream());
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
	
			while(!done && clientReq.hasNextLine())
			{
				request = clientReq.nextLine();
	
				// NEW PROTOCOL
				// Remember the format of the possible requests:
				// 1. GET SP ALL SP CHANNEL_NAME or
				// 2. GET SP VIDEO SP URL (where URL is the url of the video)
	
				check = UtilitiesDb.checkQuery(request);
				if (!check.isCorrect())
				{
					response = check.getMessage();
					clientResp.println(response);
					clientResp.flush();
				} 
				else
				{
					query = check.getMessage();
					System.out.println(query);
					// TODO: Spostare su RMI
					String json = server.searchQuery(query);
	
					// manage the response for the query to the database
					// we have to decide the appropriate response
					response = "";
					String resourcePath = "";
					/*for (Row row : queryResult)
					{
						resourcePath += row.getString(0);
						System.out.println(resourcePath);
					}*/
					JSONArray array = new JSONArray(json);
					for(int j = 0; j < array.length(); j++)
					{
						JSONObject jsonObj = array.getJSONObject(j);
						String ip = jsonObj.getString("path");
						resourcePath += ip;
					}
	
					// if the query has no results 404 NOT FOUND is sent, else 200 OK plus the
					// result of the query
					if(resourcePath.isEmpty())
						response = notFound;
					else
					{
						response = "200 OK ";// + response;
						clientResp.println(response);
						clientResp.flush();
	
						try
						{
							DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(cliSock.getOutputStream()));
							FileInputStream fileStream = new FileInputStream(new File(resourcePath));
							int n = 0;
							byte[] chunck = new byte[CHUNK_SIZE];
							while ((n = fileStream.read(chunck)) != -1)
							{
								dos.write(chunck, 0, n);
								dos.flush();
							}
							dos.close();
							fileStream.close();
						} 
						catch (IOException ioe3)
						{
							ioe3.printStackTrace();
						}
	
					}
					// response = UtilitiesDb.getResponse(queryResult);
					clientResp.println(response);
					clientResp.flush();
					done = true;
					System.out.println(response);
				}
			}
			System.out.println("Closing the connection with a server L2.");
			try
			{
				clientReq.close();
				clientResp.close();
				cliSock.close();
			} 
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		catch(RemoteException | NotBoundException re )
		{
			re.printStackTrace();
		}
	}
}
