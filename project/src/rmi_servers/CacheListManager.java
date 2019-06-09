package rmi_servers;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Interface for the cache level
 * @author Andrea Bugin and Ilie Sarpe
 */
public interface CacheListManager extends Remote
{
	/**
	 * Return the list of all L2 cache server available
	 * @return a HashMap containing the list of all L2 cache server available
	 * @throws RemoteException if any error occurs while retrieving the L2 list
	 */
	HashMap<String, Integer> getListL2() throws RemoteException;
}
