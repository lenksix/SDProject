package proxies_frontend;

/**
 * Class that is useful to check properly a query.
 * @author Andrea Bugin and Ilie Sarpe
 *
 */
public class CheckerProxy
{
	private String idVid;
	private boolean isCorrect;
	private int type;
	
	/**
	 * Constructor
	 * @param idVid ID of the given video 
	 * @param isCorrect Boolean for the correctness of the request
	 * @param type Specifies the type of query according to the request
	 */
	CheckerProxy(String idVid, boolean isCorrect, int type)
	{
		this.idVid = idVid;
		this.isCorrect = isCorrect;
		this.type = type;
	}
	
	/**
	 * Constructor for malformed query
	 * @param malformedRequest Error string
	 */
	public CheckerProxy(String malformedRequest)
	{
		this(malformedRequest, false, -1);
	}
	
	/**
	 * @return A string that is: 1. An error message if the query was malformed 2. the resource if the quer was correct
	 */
	public String getId()
	{
		return idVid;
	}
	
	/**
	 * @return A boolean indicating the correctness of the query
	 */
	public boolean isCorrect()
	{
		return isCorrect;
	}
	
	/**
	 * @return The type of the query if the query is correct, -1 otherwise
	 */
	public int getType()
	{
		return type;
	}
}
