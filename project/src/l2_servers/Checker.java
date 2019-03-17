package l2_servers;

public class Checker
{
	boolean result = false;
	int queryType = -1;
	String idResource = null;

	public Checker(boolean bool, int type, String id)
	{
		result = bool;
		queryType = type;
		idResource = id;
	}

	public boolean isCorrect()
	{
		return result;
	}

	public int getType()
	{
		return queryType;
	}

	public String getResource()
	{
		return idResource;
	}
}
