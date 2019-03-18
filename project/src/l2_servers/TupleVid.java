/**
 * TupleVid class: a simple class to store the path of a video and his timestamp in milliseconds. It's used to manage the cache.
 * @author Andrea Bugin and Ilie Sarpe
 */


package l2_servers;

class TupleVid
{
	private String path;	// TODO: decide if the path must be final 
	private long timeStamp;
	
	/**
	 * Creates a new TupleVid given the path of the video and his timestamp
	 * @param path the path of the video
	 * @param timeStamp the timestamp of the video in milliseconds
	 */
	public TupleVid(String path, long timeStamp)
	{
		this.path = path;
		this.timeStamp = timeStamp;
	}
	
	/**
	 * Return the path of the TupleVid
	 * @return the path of the video
	 */
	public String getPath()
	{
		return path;
	}
	
	/**
	 * Return the timestamp of the Tuple
	 * @return the timestamp of the video (in milliseconds)
	 */
	public long getTimeStamp()
	{
		return timeStamp;
	}
	
	/**
	 * Set the path of the Tuple
	 * @param path
	 */
	public void setPath(String path)
	{
		this.path = path;
	}
	
	/**
	 * Set the timestamp of the Tuple
	 * @param timeStamp
	 */
	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}
}
