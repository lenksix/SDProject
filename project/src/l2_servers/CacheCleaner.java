/*********************************
 * This class will act as a "garbage collector" for the cache 
 * we still have to decide the policy, by the way, once reached a certain timeout or a certain 
 * load of the cache this class wakes up, keeps the lock of the cache and cleans all the videos not more useful
 * according to some policy.
 * @author Andrea Bugin and Ilie Sarpe
 *
 */

package l2_servers;

import java.io.*;
import java.util.*;
import javafx.util.*;
import java.util.concurrent.locks.*;

public class CacheCleaner implements Runnable
{
	private HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache = null;
	private Lock vcLock = null;
	private int TIMELIMIT;
	private final long SLEEP_TIME = 2000L; // Milliseconds 
	private final File root = new File("/");
	
	public CacheCleaner(HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache, ReentrantLock vcLock, int TIMELIMIT)
	{
		this.vidsCache = vidsCache;
		this.vcLock = vcLock;
		this.TIMELIMIT = TIMELIMIT;
	}
	
	@Override
	public void run() 
	{
		try
		{
			while(true)
			{	
				// Problem: DRIFT we have to fix it
				//System.out.println("i'm going to sleep at :<" + System.currentTimeMillis() + ">");
				Thread.currentThread();
				Thread.sleep(SLEEP_TIME);
				System.out.println("I woke up at :<" + System.currentTimeMillis() + ">");
				System.out.println("Disk usage = " + root.getTotalSpace());
				System.out.println("Disk free = " + root.getFreeSpace());
				
				// Lock of vidsCache
				/*
				vcLock.lock();
				try
				{
					vidsCache.forEach((id_vid, pair)->{
						try 
						{
							pair.getValue().writeLock().lock();	// lock of the element inside the vidsCache
							vcLock.unlock();					// now I can unlock the vidsCache
							TupleVid actual_tv = pair.getKey();
							if(actual_tv.getTimeStamp() < System.currentTimeMillis()-TIMELIMIT)
							{
								// QUA SE VOGLIO RIMUOVERE DA vidsCache DEVO RIPRENDEMI IL LOCK !?
								File file = new File(pair.getKey().getPath());
								file.delete();
								vidsCache.remove(id_vid);
							}
							
						}
						finally
						{
							pair.getValue().writeLock().unlock();
						}
						
					});
				}
				finally
				{
					vcLock.unlock();
				}
				*/
				
			}
		}
		catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
}
