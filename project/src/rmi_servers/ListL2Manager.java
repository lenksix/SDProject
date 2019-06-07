package rmi_servers;
import java.rmi.*;
import java.util.ArrayList;

import javafx.util.Pair;

public interface ListL2Manager
{
	String serverRegister(String ip, int port) throws RemoteException;
	ArrayList<Pair<String, Integer>> listServers() throws RemoteException;
}