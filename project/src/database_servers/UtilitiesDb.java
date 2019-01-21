/**
 * UtilitiesDb class: it contains few methods that are used frequently.
 * @author Andrea Bugin and Ilie Sarpe
 */

package database_servers;

public class UtilitiesDb 
{
	/*
   public static String createQuery(String channel, String url) {
      return null;
   }
   
   public static String getResponse(ResultSet queryResult) {
      return null;
   }
   */
   
	/**
	 * To retrieve the list of all videos for a channel in the database
	 * 
	 * @param ch_name the name of the channel
	 * @return the query string to use to retrieve the list of videos
	 */
	public static String getAllVideos(String ch_name)
	{
		final String q = "SELECT vids FROM channel_vids WHERE channel_name='" + ch_name + "';";
		return q;
	}
   
	/**
	 * To retrieve a path of a single video in the vid_path table 
	 * 
	 * @param url the url of the video 
	 * @return the query string to use to retrieve the path of the corresponding video
	 */
	public static String getPath(String url)
	{
		final String q = "SELECT path FROM vid_path WHERE url='" + url + "';";
		return q;
	}
	
	
	/**
	 * To insert a (channel_name, url)-tuple into the channel_vids table of the DB
	 * 
	 * @param ch_name the name of the channel
	 * @param url the url of the corresponding video
	 * @return the query string to use to insert the tuple in the channel_vids table 
	 */
	public static String insertChannelVids(String ch_name, String url)
   	{
		final String q = "INSERT INTO channel_vids(channel_name, vids) VALUES ('"
			+ ch_name + "', {'" + url + "'});";
		return q;
   	}
   
	/**
	 * To update the channel_vids table with a new video url.
	 * Since vids is a set, duplicate values will not be stored distinctly. 
	 * 
	 * @param ch_name the name of the channel 
	 * @param url the url of the corresponding video
	 * @return the query string to use to update the channel_vids table 
	 */
	public static String updateChannelVids(String ch_name, String url)
	{
		final String q = "UPDATE channel_vids SET "
			+ "vids = vids + {'" + url + "'}"
			+ " WHERE "
			+ "channel_name = '" + ch_name + "';";
		return q;
   	}
   
	/**
	 * To insert a new video to the vid_path table.
	 * NOTE: duplicate values will be overwritten because the IF NOT EXIST parameters return errors (to fix)
	 * 
	 * @param url the url of the video
	 * @param path the path where the video is stored
	 * @return the query string to use to insert the new video into the vid_path table 
	 */
	public static String insertUrlPath(String url, String path)
	{
		final String q = "INSERT INTO vid_path(url, path) VALUES ('"
			+ url + "', '" + path + "')"
	   		+ "	IF NOT EXISTS;";
		return q;
	}
}