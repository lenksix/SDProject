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
	private long time_limit;
	private long sleep_time = 2048L; // Milliseconds 
	private final File root_dir = new File("/");
	
	public CacheCleaner(HashMap<String, Pair<TupleVid, ReentrantReadWriteLock>> vidsCache, ReentrantLock vcLock, long time_limit)
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
				System.out.println("I slept: <" + sleep_time + ">");
				System.out.println("Time limit is: <" + time_limit + ">");
				
				// Lock of vidsCache
				vcLock.lock();
				try
				{
					Iterator<Map.Entry<String, Pair<TupleVid, ReentrantReadWriteLock>>> it = vidsCache.entrySet().iterator();
					while(it.hasNext())
					{
						Map.Entry<String, Pair<TupleVid, ReentrantReadWriteLock>> entry = it.next();
						boolean updated = true;
						try 
						{
							entry.getValue().getValue().writeLock().lock();
							
							long insert_time = entry.getValue().getKey().getTimeStamp();
							System.out.println("Rresource is in cache from : <" + (System.currentTimeMillis() - insert_time) + ">");
							if((insert_time < System.currentTimeMillis()-time_limit))
							{
								updated = false;
								File file = new File(entry.getValue().getKey().getPath());
								System.out.println("Path of the resource is: <" +entry.getValue().getKey().getPath()+ ">");
								file.delete();
							}
						}
						finally
						{
							entry.getValue().getValue().writeLock().unlock();
						}
						if(!updated)
						{
							System.out.println("Deleted key <" + entry.getKey() +">");
							vidsCache.remove(entry.getKey());
						}
					}
					/*
					vidsCache.forEach((id_vid, pair)->{
						try 
						{
							pair.getValue().writeLock().lock();	// lock of the element inside the vidsCache
							long insert_time = pair.getKey().getTimeStamp();
							
							// if the timestamp of the current tuple is zero then another thread is retrieving the resource from the database
							// so the cleaner does not have to remove it since other thread can wait without asking again in the database
							//if((insert_time != 0) && (insert_time < System.currentTimeMillis()-time_limit))
							if((insert_time < System.currentTimeMillis()-time_limit))
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
					});*/
					
					// check how much memory is free
					// TODO: decide if this can be done without keeping the lock of the map
					// now the policy will be 80%-20%
					/*
					long first = root_dir.getTotalSpace();
					long second = root_dir.getFreeSpace();
					double ratio =  (double) second / first;
					System.out.println("Ratio is <" + ratio + ">"); */
					double free_space_percent = ((double)root_dir.getFreeSpace() /root_dir.getTotalSpace()) * 100;
					System.out.println("Free space is <" + free_space_percent + ">");
					if(free_space_percent > 70)
					{
						time_limit *= 1.5;
						sleep_time *= 1.5;
					}
					else if(free_space_percent < 30)
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
	
	public synchronized long getTimeLimit()
	{
		return time_limit;
	}
}
