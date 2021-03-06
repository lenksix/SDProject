package l2_servers;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * UtilitiesL2 class: it contains few methods that are used frequently by the L2 servers.
 * @author Andrea Bugin and Ilie Sarpe
 */
public class UtilitiesL2
{

	final static int NUMARGS = 3;
	final static ArrayList<String> methods = new ArrayList<String>(Arrays.asList("GET"));
	final static ArrayList<String> options = new ArrayList<String>(Arrays.asList("IN_CACHE", "IN_DATABASE"));

	/*
	 * We need to ensure that the request from L1 is formed correctly moreover we
	 * have to check what kind of request is required from L1. RECALL the protocol
	 * specifies the following requests: 1.GET SP IN_CACHE SP ID_RISORSA 2.GET SP
	 * IN_DATABASE SP ID_RISORSA
	 */
	/**
	 * @param query a string that specifies a query for the cache
	 * @return an object of type CheckerL2 that specifies the correctness and the meaning of the query
	 */
	public static CheckerL2 checkQuery(String query)
	{
		String[] args = query.split(" ");

		// This works only for the request of a video!

		if (args.length == NUMARGS)
		{
			for (int i = 0; i < methods.size(); i++)
			{
				if (methods.get(i).equals(args[0].toUpperCase()) && (i == 0))
				{
					for (int j = 0; j < options.size(); j++)
					{
						if (options.get(j).equals(args[1].toUpperCase()))
						{
							if (j == 0)
							{ // GET SP IN_CACHE SP ID_RISORSA
								return new CheckerL2(true, 1, args[NUMARGS - 1]);
							} 
							else if (j == 1)
							{ // GET SP IN_DATABASE SP ID_RISORSA
								return new CheckerL2(true, 2, args[NUMARGS - 1]);
							}
						}
					}
					return new CheckerL2(false, -1, "622 ERROR OPTION"); // Option doesn't exists
				}
			}
			return new CheckerL2(false, -1, "621 ERROR METHOD"); // Method doesn't exists
		} 
		else
		{
			return new CheckerL2(false, -1, "620 ERROR NUM_PARAMS"); // Error on the number of parameters
		}

	}

	/**
	 * @param video id of the video to be retrieved from the database
	 * @return query format for the database to retrieve a given resource
	 */
	public static String queryVid(String video)
	{
		return "GET VIDEO " + video;
	}
}
