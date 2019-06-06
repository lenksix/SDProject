package rmi_servers;
import java.rmi.*;
import java.util.HashMap;

public interface CacheListManager extends Remote
{
	// get the list of all L2 cache server available
	HashMap<String, Integer> getListL2() throws RemoteException;
}
