package customer.freescale.com.serus.workflow;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.Scanner;


public class WebserviceMessageHandler {
	static Logger threadCat;

	public static void postRequest (String httpRequestType, String url, String fileFolder, String cert, String password)
			throws Exception {

		try {
			// get the file to POST 
			
			Date startTime = new Date();
			ExecutorService executor = Executors
					.newFixedThreadPool(3);
			if(httpRequestType.equalsIgnoreCase("POST"))
			{
				File folder = new File(fileFolder);
				File[] listOfFiles = folder.listFiles();
				// Overriding the comparator for sorting with respect to modified
				// date.
				Arrays.sort(listOfFiles, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(
								f2.lastModified());
					}
				});
				for (int i = 0; i < listOfFiles.length; i++) {
	
					if (listOfFiles[i].isFile()) {
						File postedFile = listOfFiles[i];
						Runnable worker = new MSPostMessageExecutor(url,postedFile, cert, password);
						System.out.println("Invoking thread for the file "
								+ postedFile.getName());
						executor.execute(worker);
					}
				}
			}
			else
			{
				
				Runnable worker = new MSPostMessageExecutor(url, null,cert, password);
				executor.execute(worker);
				
			}
			executor.shutdown();

			// checkForResponseErrors(businessExceptionList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	 

	public static void main(String[] args) throws Exception {
		// SSL debugging
		// System.setProperty("javax.net.debug", "all");
		
		//HTTP Get or HTTP Post
		Scanner in = new Scanner(System.in);
		System.out.println("--Enter the Request Type (POST/GET)--");
		String reqType = in.nextLine();
		System.out.println("--Enter the URL--");
		String url = in.nextLine();
		String fileFolder = null;
		if(null != reqType && reqType.equalsIgnoreCase("POST"))
		{
		System.out.println("--Enter the  Location of file to POST--");
		fileFolder = in.nextLine();
		}
		
		System.out.println("--Enter the Certificate Location if its need a cert to trust--");
		String cert = in.nextLine();
	    System.out.println("--Enter the password--");
		String password = in.nextLine();
		postRequest(reqType,url,fileFolder,cert,password);
	}

	public static boolean wildCardMatch(String text, String pattern) {
		// Create the cards by splitting using a RegEx. If more speed
		// is desired, a simpler character based splitting can be done.
		String[] cards = pattern.split("\\*");

		// Iterate over the cards.
		for (String card : cards) {
			int idx = text.indexOf(card);

			// Card not detected in the text.
			if (idx == -1) {
				return false;
			}

			// Move ahead, towards the right of the text.
			text = text.substring(idx + card.length());
		}

		return true;
	}
}
