package rmi_servers;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for the manage of the database "session"
 * @author andrea
 */
public interface SessionManager extends Remote
{
	/**
	 * Execute a search query specified in the input
	 * @param query the query to execute
	 * @return the result of the execution of the query parsed in JSON format
	 * @throws RemoteException if any error occurs while executing the query
	 */
	String searchQuery(String query) throws RemoteException;

	/**
	 * Execute a register query specified in the input
	 * @param query the query to execute
	 * @return 0 if no error, 1 otherwise
	 * @throws RemoteException if any error occurs while executing the query
	 */
	int registerQuery(String query) throws RemoteException; // 0 if no error, 1 otherwise
	
	/**
	 * Execute a delete query specified in the input
	 * @param query the query to execute
	 * @return 0 if no error, 1 otherwise
	 * @throws RemoteException if any error occurs while executing the query
	 */
	int deleteQuery(String query) throws RemoteException; // 0 if no error, 1 otherwise
}
