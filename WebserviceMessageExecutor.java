package customer.freescale.com.serus.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.logging.Logger;

public class WebserviceMessageExecutor implements Runnable {

	private String posturl;
	private File postedFile;
	private String certificate;
	private String password;
	
	public WebserviceMessageExecutor(String posturl, File postedFile,
			String certificate,
			String password) {
		this.posturl = posturl;
		this.postedFile = postedFile;
		this.certificate = certificate;
		this.password = password;
	}

	public void run() {
		HttpURLConnection getCon = null;
		Date startTime = new Date( );
		try{
		
		int statusCode = 100;
		try {
				
				if (certificate == null || password == null) {
					System.out.println("Unable to retrieve the cert path and password  ");
					return;
				}
				System.out.println("url used to connect : " + posturl);
				    //Decide its a GET r POST request
					if(null !=postedFile)
					{
						getCon = MSHttpsAPI.getConnection("post",
							posturl, postedFile.getName(), certificate,
							password);
					}
					else
						getCon = MSHttpsAPI.getConnection("get",
								posturl, null, certificate,
								password);
					if (getCon != null)
						statusCode = getCon.getResponseCode();
					
			
			} 
		catch (ConnectException ce) 
		{
				//"MS web service  Exception caught - ConnectException : ";
				ce.printStackTrace();

				return;
			} 
		catch (SocketTimeoutException se) {
				// "MS web service  Exception caught - SocketTimeoutException : ";
				se.printStackTrace();
				return;
			} 
		catch (RemoteException re) {
				// "MS web service  Exception caught - RemoteException : ";
				re.printStackTrace();
				return;
			} 
		catch (Exception e) {
				//"MS web service Exception caught : ";
				e.printStackTrace();
				return;
			}
		if (statusCode == HttpURLConnection.HTTP_OK) {
				prepareResponseLog(getCon);
				
			} else {

				//"Bad response code while posting the file into Microsoft web service: "
						
			switch (statusCode) {
				case 400: {
					// --Bad request. The request was not properly formatted, according to the format documented in the individual web service request format specification, or a validation error on the data provided with the request."
				}
				case 401: {
					// " --Unauthorized. User authentication has failed.";
					break;
				}
				case 404: {
					///"Typically, this occurs when the request URI does not correctly address a resource by name or format.";
					break;
				}
				case 405: {
					//--"Method Not Allowed. The HTTP method used is unsupported by the web service.";
					break;
				}
				case 500: {
					// --Internal Server Error. An error occurred while processing the request.";
					break;
				}
				default:
					//--Web service  call returned an unexpected HTTP status of: ";
				}
				InputStream is = getCon.getErrorStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String string = null;
				if (br != null) {
					while ((string = br.readLine()) != null) {
						System.out.println(string);
					}
					
					is.close();
				}
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (getCon != null) {
				getCon.disconnect();
			}
		}
		 Date stopTime = new Date();
		 	System.out.println("Execution End Time for the file "+postedFile.getName() +": "+stopTime);
			long diff = stopTime.getTime() - startTime.getTime();
			long diffMinutes = diff / 1000;
			System.out.println("Total Time Taken for the file "+postedFile.getName() +": "+diffMinutes+" Seconds");

	}


	static void prepareResponseLog(HttpURLConnection con)
			throws IOException {


		InputStream is = con.getInputStream();
		is = con.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String string = null;
		//System.out.println(br.readLine());
		if (br != null) {
			while ((string = br.readLine()) != null) {
				System.out.println(string);
			}

		}
	}

}
