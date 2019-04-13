package test_Db;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import database_servers.InitDb;


/**
 * THIS CLASS IS JUST FOR TEST!!!! IT DROPS AND INITS A CLEAN DATABASE 
 */
public class DropAndInitDb
{
	public static void main(String[] args)
	{
		Cluster cluster = null;
		Session session = null;
		try
		{
			cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			session = cluster.connect();
			session.execute("drop keyspace streaming;");
			InitDb.main(args);
		}
		finally 
		{
			session.close();
			cluster.close();
		}
	}
}
