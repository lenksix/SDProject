package database_servers;

/**
 * Utility class to manage the request and response message according to the protocol
 * @author Andrea Bugin and Ilie Sarpe
 */
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
	 * @return the resource if the check is correct, an error message otherwise
	 */
	public String getMessage()
	{
		return query;
	}
	
	/**
	 * @return the id of teh resource according to the protocol, an error message otherwise
	 */
	public String getResource()
	{
		if(isCorrect())
		{
			return query.split(" ")[2];
		}
		return query;
	}
}
