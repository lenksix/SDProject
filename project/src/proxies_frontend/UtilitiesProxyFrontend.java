package proxies_frontend;

public class UtilitiesProxyFrontend
{
	public static CheckerProxy parseRequest(String request)
	{
		// Supported requests are:
		// GET VIDEO ID_VIDEO
		
		String[] chunks = request.split(" ");
		if(chunks.length != 3)
		{
			return new CheckerProxy("801 ERROR NUM_PARAMETERS");
		}
		else
		{
			if(chunks[0].toLowerCase().equals("get"))
			{
				if(chunks[1].toLowerCase().equals("video"))
				{
					return new CheckerProxy(chunks[2], true, 1);
				}
				else
				{
					return new CheckerProxy("802 ERROR PARAMETER");
				}
			}
			else
			{
				return new CheckerProxy("802 ERROR METHOD");
			}
		}
	}
}
