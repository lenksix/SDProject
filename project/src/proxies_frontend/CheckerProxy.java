package proxies_frontend;

public class CheckerProxy
{
	String idVid;
	boolean isCorrect;
	int type;
	
	CheckerProxy(String idVid, boolean isCorrect, int type)
	{
		this.idVid = idVid;
		this.isCorrect = isCorrect;
		this.type = type;
	}
	
	public CheckerProxy(String malformedRequest)
	{
		this(malformedRequest, false, -1);
	}
	
	public String getId()
	{
		return idVid;
	}
	
	public boolean isCorrect()
	{
		return isCorrect;
	}
	
	public int getType()
	{
		return type;
	}
}
