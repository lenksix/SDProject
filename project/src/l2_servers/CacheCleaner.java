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
	private int time_limit;
	private long sleep_time = 2048L; // Milliseconds 
	private final File root_dir = new File("/");
	
	public CacheCleaner(HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache, ReentrantLock vcLock, int time_limit)
	{
		this.vidsCache = vidsCache;
		this.vcLock = vcLock;
		this.time_limit = time_limit;
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
				Thread.sleep(sleep_time);
				System.out.println("I woke up at :<" + System.currentTimeMillis() + ">");
				System.out.println("Disk usage = " + root_dir.getTotalSpace());
				System.out.println("Disk free = " + root_dir.getFreeSpace());
				
				// Lock of vidsCache
				vcLock.lock();
				try
				{
					vidsCache.forEach((id_vid, pair)->{
						try 
						{
							pair.getValue().writeLock().lock();	// lock of the element inside the vidsCache
							long insert_time = pair.getKey().getTimeStamp();
							
							// if the timestamp of the current tuple is zero then another thread is retrieving the resource from the database
							// so the cleaner does not have to remove it since other thread can wait without asking again in the database
							if((insert_time != 0) && (insert_time < System.currentTimeMillis()-time_limit))
							{
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
					
					// check how much memory is free
					// TODO: decide if this can be done without keeping the lock of the map
					// now the policy will be 80%-20%
					double free_space_percent = root_dir.getFreeSpace() / root_dir.getTotalSpace() * 100;
					if(free_space_percent > 80)
					{
						time_limit *= 2;
						sleep_time *= 2;
					}
					else if(free_space_percent < 20)
					{
						time_limit = (time_limit == 1)? time_limit : (time_limit/2);
						sleep_time = (sleep_time == 1)? sleep_time : (sleep_time/2);
					}
				}
				finally
				{
					vcLock.unlock();
				}
			}
		}
		catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
}
