package l2_servers;

import java.util.ArrayList;
import java.util.Arrays;


public class UtilitiesL2 {

	final static int NUMARGS = 3;
	final static ArrayList<String> methods = new ArrayList<String>(Arrays.asList("GET"));
	final static ArrayList<String> options = new ArrayList<String>(Arrays.asList("IN_CACHE", "IN_DATABASE"));
	/*
	 * We need to ensure that the request from L1 is formed correctly
	 * moreover we have to check what kind of request is required from L1.
	 * RECALL the protocol specifies the following requests:
	 * 1.GET SP IN_CACHE SP ID_RISORSA
	 * 2.GET SP IN_DATABASE SP ID_RISORSA
	 * */
	public static Checker checkQuery(String query) {
		String[] args = query.split(" ");
		
		// This works only for the request of a video!
		
		if(args.length == NUMARGS) {
			for(int i = 0; i< methods.size(); i++) {
				if(methods.get(i).equals(args[0].toUpperCase()) && (i == 0)) {
					for(int j = 0; j < options.size(); j++) {
						if(options.get(j).equals(args[1].toUpperCase())) {
							if(j == 0){ // GET SP IN_CACHE SP ID_RISORSA
								return new Checker(true, 1, args[NUMARGS-1]);
							}	
							else if(j == 1) { // GET SP IN_DATABASE SP ID_RISORSA
								return new Checker(true, 2, args[NUMARGS-1]);
							}
						}
					}
					return new Checker(false, -1, "622 ERROR OPTION"); // Option doesn't exists
				}  
			}
			return new Checker(false, -1 , "621 ERROR METHOD"); // Method doesn't exists
		}
		else {
			return new Checker(false, -1, "620 ERROR NUM_PARAMS"); // Error on the number of parameters
		}
		
	}
   
	public static String queryVid(String video) {
		return "GET VIDEO " + video;
	}  
}
