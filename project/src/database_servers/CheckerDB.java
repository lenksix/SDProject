package database_servers;

public class CheckerDB
{
	boolean result = false;
	String query = null;

	/**
	 * @param bool boolean indicating the correctness of the check
	 * @param str if bool is true is the resource otherwise an error code
	 */
	public CheckerDB(boolean bool, String str)
	{
		result = bool;
		query = str;
	}

	/**
	 * @return true if the syntax of the check is correct, false otherwise
	 */
	public boolean isCorrect()
	{
		return result;
	}

	/**
	 * @return id of the resource if the check is correct, an error message otherwise
	 */
	public String getMessage()
	{
		return query;
	}
	
	public String getResource()
	{
		return query.split(" ")[2];
	}
}
