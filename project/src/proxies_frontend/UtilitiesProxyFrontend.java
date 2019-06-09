package proxies_frontend;

/**
 * UtilitiesProxyFrontend class: it contains few methods that are used frequently by the proxies.
 * @author Andrea Bugin and Ilie Sarpe
 */
public class UtilitiesProxyFrontend
{
	/**
	 * @param request String to be syntax checked
	 * @return A {@link proxies_frontend.CheckerProxy} with parameters set the properly. 
	 * In particular type = 1 if the query is: GET VIDEO IDVID
	 */
	public static CheckerProxy parseRequest(String request)
	{
		// Supported requests are:
		// GET VIDEO ID_VIDEO
		// END THE CONNECTION
		
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
					return new CheckerProxy("803 ERROR PARAMETER");
				}
			}
			else if(chunks[0].toLowerCase().equals("end"))
			{
				if(chunks[1].toLowerCase().equals("the"))
				{
					if(chunks[2].toLowerCase().equals("connection"))
					{
						return new CheckerProxy(chunks[2], true, 99);
					}
					else
					{
						return new CheckerProxy("804 ERROR PARAMETER_SPECIFIER");
					}
				}
				else
				{
					return new CheckerProxy("803 ERROR PARAMETER");
				}
			}
			else
			{
				return new CheckerProxy("802 ERROR METHOD");
			}
		}
	}
}
