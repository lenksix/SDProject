package rmi_servers;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javafx.util.Pair;

/**
 * Interface for the L2 level
 * @author Andrea Bugin and Ilie Sarpe
 *
 */
public interface ListL2Manager
{
	/**
	 * Register the server specifying its ip and its port 
	 * @param ip the ip of the server
	 * @param port the port of the server
	 * @return a string which says if the registration was successful or not
	 * @throws RemoteException if any error occurs while registering the server
	 */
	String serverRegister(String ip, int port) throws RemoteException;
	
	/**
	 * Return a list of all the servers registered
	 * @return an ArrayList containing all the active servers
	 * @throws RemoteException if any error occurs while retrieving the list
	 */
	ArrayList<Pair<String, Integer>> listServers() throws RemoteException;
}