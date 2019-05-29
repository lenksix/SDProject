package rmi_example;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class AdditionBinder
{
	public static void main(String[] args)
	{
		try
		{
			Addition server = new AdditionImpl();
			//Addition stub = (Addition) UnicastRemoteObject.exportObject(server, 0);
			Registry registry = LocateRegistry.createRegistry(11099);
			registry.rebind("Addition", server);
		}
		catch(RemoteException re)
		{
			re.printStackTrace();
		}
	}
}
