/**
 * UtilitiesDb class: it contains few methods that are used frequently by the database managers.
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class UtilitiesDb
{
	final static int NUMARGS = 3;
	final static ArrayList<String> methods = new ArrayList<String>(Arrays.asList("GET"));
	final static ArrayList<String> options = new ArrayList<String>(Arrays.asList("ALL", "VIDEO"));

	/**
	 * To retrieve a path of a single video in the vid_path table
	 * 
	 * @param id_vid the id of the video
	 * @return the query string to use to retrieve the path of the corresponding
	 *         video
	 */
	public static String getPath(String id_vid)
	{
		final String q = "SELECT path FROM vid_path WHERE id_vid = '" + id_vid + "';";
		return q;
	}
	
	
	/**
	 * Retrieve the ip and the port of the video in the vid_location table
	 * @param id_vid the id of the video
	 * @return the query string to use to retrieve the id and the port of the corresponding video
	 */
	public static String getLocDb(String id_vid)
	{
		final String q = "SELECT ip, port FROM vid_location WHERE id_vid = '" + id_vid + "';";
		return q;
	}
	
	/**
	 * Insert the id of a video and the ip and the port of the server which owns that video in the vid_location table
	 * @param id_vid the id of the video
	 * @param ip the ip of the server
	 * @param port the port of the server
	 * @return the query string to use to insert the new video into the vid_location table
	 */
	public static String insertLocDb(String id_vid, String ip, int port)
	{
		final String q = "INSERT INTO vid_location(id_vid, ip, port) VALUES ('" + id_vid + "', '" + ip + "', " + port + ");";
		return q;
	}


	/**
	 * To insert a new video to the vid_path table.
	 * 
	 * @param url  the url of the video
	 * @param path the path where the video is stored
	 * @return the query string to use to insert the new video into the vid_path
	 *         table.
	 */
	public static String insertUrlPath(String url, String path)
	{
		final String q = "INSERT INTO vid_path(id_vid, path) VALUES ('" + url + "', '" + path + "');";
		return q;
	}
	
	/**
	 * To insert a new ip-port record in the database ip_cache table if it's not already present.
	 * For the deletion see @see #deleteIPCache(String, int)
	 * @param ip the ip of the cache server
	 * @param port the port of the cache server 
	 * @return the query string to use to insert the record in the table.
	 */
	public static String insertIPCache(String ip, int port)
	{
		final String q = "INSERT INTO ip_cache(ip, port) VALUES ('"
			+ ip + "', " + port + ") IF NOT EXISTS;";
		return q;
	}
	
	/**
	 * To delete a ip-port record in the database ip_cache table. 
	 * For the insertion see @see #insertIPCache(String, int)
	 * @param ip the ip of the cache server
	 * @param port the port of the cache server 
	 * @return the query string to use to delete the record in the table.
	 */
	public static String deleteIPCache(String ip, int port)
	{
		final String q = "DELETE FROM ip_cache WHERE ip='" + ip +"' AND port=" + port + ";";
		return q;
	}
	
	/**
	 * To select all the record inside the ip_cache table
	 * @return the query string to select all the record inside the ip_cache table.
	 */
	public static String selectFromIPCache()
	{
		final String q = selectFromIPCache(null, 0);
		return q;
	}
	
	/**
	 * To select a specific record inside the ip_cache table given the ip and the port
	 * @param ip the ip of the cache server
	 * @param port the port of the cache server
	 * @return the query string to use to select a specific record in the table.
	 */
	public static String selectFromIPCache(String ip, int port)
	{
		String where = "WHERE ip = '" + ip + "' AND port = " + port + ";";
		if(ip == null) 
		{
			where = ";";
		}
			
		final String q = "SELECT * FROM ip_cache" + where;
		return q;
	}

	/**
	 * To check the syntax of the query received and identify the service a.k.a
	 * method, requested
	 * 
	 * @param request the string to be checked
	 * @return an object of type Checker with informations about the correctness of
	 *         the request
	 */
	public static CheckerDB checkQuery(String request)
	{
		String[] args = request.split(" ");

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
							if (j == 1)
							{ // GET SP VIDEO SP ID_VIDEO
								return new CheckerDB(true, args[NUMARGS - 1]);
							}
						}
					}
					return new CheckerDB(false, "612 ERROR OPTION"); // Option doesn't exists
				}
			}
			return new CheckerDB(false, "611 ERROR METHOD"); // Method doesn't exists
		} 
		else
		{
			return new CheckerDB(false, "610 ERROR NUM_PARAMS"); // Error on the number of parameters
		}

	}
}