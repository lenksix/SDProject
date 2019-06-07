package database_servers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.xml.internal.ws.addressing.ProblemAction;

public class RDBManager implements Runnable
{
	private final static String notFound = "404 NOT FOUND";
	private final static int CHUNK_SIZE = 1000;
	final static int DEFAULT_PORT = 13222;
	final static int WAIT_TIME = 15000;
	
	private static List<Socket> sockAlive;
	private static ReentrantLock lockSockets;
	private static Condition socketAvailable;
	private static Connection conn;
	
	public static void main(String[] args)
	{
		(new RDBManager()).exec(args);
	}
	
	public void exec(String[] args)
	{
		ServerSocket serverSock = null;
		
		sockAlive = new LinkedList<Socket>();
		lockSockets = new ReentrantLock();
		socketAvailable = lockSockets.newCondition();
		
		try
		{			
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/king", "postgres", "postgres");
		}
		catch(SQLException e)
		{
			System.err.println("Something went wrong with your SQL query. Here is the exception code: \n");
			e.printStackTrace();
		}
		try
		{
			serverSock = new ServerSocket(DEFAULT_PORT);
			System.out.println("Ok, Serversocket <proxy> created!");
					
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
	
	@Override
	public void run()
	{
		Socket clientSock;
		Scanner scannerClient = null;
		PrintWriter pwClient = null;
		String myname = Thread.currentThread().getName();
		boolean alive = true;
		String response = null;
		
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
				scannerClient = new Scanner(clientSock.getInputStream());
				pwClient = new PrintWriter(clientSock.getOutputStream(), true);
				CheckerDB check = UtilitiesDb.checkQuery(scannerClient.nextLine());
				
				if(!check.isCorrect())
				{
					response = notFound;
					pwClient.println(response);
				}
				else
				{
					ResultSet rs = conn.createStatement().executeQuery(UtilitiesDb.getPath(check.getResource()));
					String resourcePath = rs.getString("path");
					
					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(clientSock.getOutputStream()));
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
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			catch(SQLException sqle) 
			{
				sqle.printStackTrace();
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
