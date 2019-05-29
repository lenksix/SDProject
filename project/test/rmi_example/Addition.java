package rmi_example;
import java.rmi.*;

public interface Addition extends Remote
{
	public int add(int a, int b) throws RemoteException;
	public void sumCounter(int a) throws RemoteException;
	public int getCounter() throws RemoteException;
}
