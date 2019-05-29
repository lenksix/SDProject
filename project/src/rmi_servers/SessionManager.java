package rmi_servers;
import java.rmi.*;

import com.datastax.driver.core.ResultSet;

public interface SessionManager extends Remote
{
	String searchQuery(String query) throws RemoteException;
	int registerQuery(String query) throws RemoteException; // 0 if no error, 1 otherwise
	int deleteQuery(String query) throws RemoteException; // 0 if no error, 1 otherwise
}
