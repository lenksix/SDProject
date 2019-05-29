package rmi_example;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class AdditionClient 
{
	public static void main(String[] args)
	{
		try
		{
			Registry registry = LocateRegistry.getRegistry(11099);
			Addition server = (Addition) registry.lookup("Addition");
			int sum = server.add(10, 20);
			System.out.println("Result is: " + sum);
			Scanner rI = new Scanner(System.in);
			while(rI.hasNext())
			{
				server.sumCounter(rI.nextInt());
				System.out.println("Counter is: " + server.getCounter());
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
