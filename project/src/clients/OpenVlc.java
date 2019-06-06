package clients;

import java.io.IOException;

public class OpenVlc 
{
	public static void main(String[] args) throws IOException
	{
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("vlc");
	}
}
