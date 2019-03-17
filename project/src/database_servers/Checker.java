package database_servers;

public class Checker
{
	boolean result = false;
	String query = null;

	public Checker(boolean bool, String str)
	{
		result = bool;
		query = str;
	}

	public boolean isCorrect()
	{
		return result;
	}

	public String getMessage()
	{
		return query;
	}
}
