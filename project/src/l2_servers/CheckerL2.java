package l2_servers;

public class CheckerL2
{
	boolean result = false;
	int queryType = -1;
	String idResource = null;

	/**
	 * @param bool true if the syntax of the query checked is correct, false otherwise
	 * @param type specifies the type of query, for now only 1: GET SP IN_CACHE SP ID_RISORSA, 2: GET SP IN_DATABASE SP ID_RISORSA
	 * @param id the id of the resource if query is correct, an error message otherwise
	 */
	public CheckerL2(boolean bool, int type, String id)
	{
		result = bool;
		queryType = type;
		idResource = id;
	}

	/**
	 * @return true if the syntax of the query is correct, false otherwise
	 */
	public boolean isCorrect()
	{
		return result;
	}

	/**
	 * @return type of the query 1 if the format GET SP IN_CACHE SP ID_RISORSA, 2 it format is GET SP IN_DATABASE SP ID_RISORSA
	 */
	public int getType()
	{
		return queryType;
	}

	/**
	 * @return id of the resource if the query is correct , an error message otherwise
	 */
	public String getResource()
	{
		return idResource;
	}
}
