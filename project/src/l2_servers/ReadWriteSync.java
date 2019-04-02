package l2_servers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteSync
{
	private ReentrantReadWriteLock rwl;
	private AtomicInteger readCounter;
	public ReadWriteSync(ReentrantReadWriteLock rwl)
	{
		this.rwl = rwl;
		this.readCounter = new AtomicInteger();
	}
	public int incrementCounter()
	{
		return readCounter.incrementAndGet();
	}
	public int decrementCounter()
	{
		return readCounter.decrementAndGet();
	}
	public ReentrantReadWriteLock getLock()
	{
		return rwl;
	}
	
	
}
