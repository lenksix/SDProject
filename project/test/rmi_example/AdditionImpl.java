package rmi_example;
import java.rmi.*;
import java.rmi.server.*;

public class AdditionImpl extends UnicastRemoteObject implements Addition
{
	static int counter = 0;
	protected AdditionImpl() throws RemoteException
	{
		super();
	}
	@Override
	public int add(int a, int b) throws RemoteException
	{
		return a+b;
	}

	public void sumCounter(int a)
	{
		counter += a;
	}
	
	public int getCounter()
	{
		return counter;
	}
}
