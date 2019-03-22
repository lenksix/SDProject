package l2_servers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
/*********************************
 * This class will act as a "garbage collector" for the cache 
 * we still have to decide the policy, by the way, once reached a certain timeout or a certain 
 * load of the cache this class wakes up, keeps the lock of the cache and cleans all the videos not more useful
 * according to some policy.
 * @author Andrea Bugin and Ilie Sarpe
 *
 */

public class CacheCleaner implements Runnable
{	
	private static HashMap<String, TupleVid> vidsCache = null;
	private static int TIMELIMIT;
	private final static long SLEEP_TIME = 2000L; // Milliseconds 
	private static ArrayList<String> toRemove = null;
	
	public CacheCleaner(HashMap<String, TupleVid> vidsCache, int TIMELIMIT)
	{
		CacheCleaner.vidsCache = vidsCache;
		CacheCleaner.TIMELIMIT = TIMELIMIT;
	}
	
	public void run() 
	{
		while(true)
		{
			try
			{
				// Problem: DRIFT we have to fix it
				//System.out.println("i'm going to sleep at :<" + System.currentTimeMillis() + ">");
				Thread.currentThread();
				Thread.sleep(SLEEP_TIME);
				//System.out.println("I woke up at :<" + System.currentTimeMillis() + ">");
				synchronized(vidsCache)
				{
					Set<String> keys = vidsCache.keySet();
					for(String videoId: keys)
					{
						TupleVid video = vidsCache.get(videoId);
						long insertTime = video.getTimeStamp(); 
						if(System.currentTimeMillis() >= (insertTime + TIMELIMIT))
						{
							toRemove.add(video.getPath()); // we have to physically remove this video!
							vidsCache.remove(videoId); // first remove from the cache
						}
					}
				}
				// now truly removing the files from the cache.. maybe this code goes inside the sync.
				for(int i=0; i < toRemove.size(); ++i)
				{
					String pathToVideo = toRemove.get(i);
					File file = new File(pathToVideo);
					if(file.delete())
					{
						System.out.println("Correct deletion");
					}	
				}
				toRemove.clear(); // empty the files to remove
			} 
			catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
	}
}
