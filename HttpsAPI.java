package customer.freescale.com.serus.workflow;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

public class HttpsAPI {
	protected SSLContext mSSLCtx;
	static Logger cat;
	
	public HttpsAPI(File keyStore, String password) throws Exception {
		KeyManager[] keyMgrs = { new HttpKeyManager(keyStore, password) };
		mSSLCtx = SSLContext.getInstance("TLS");
		mSSLCtx.init(keyMgrs, getTrustManagers(), new SecureRandom());
	}

	public HttpURLConnection post(URL url, File data) throws Exception {
		return post(url, loadFile(data));
	}

	public HttpURLConnection post(URL url, String data) throws Exception {
		return post(url, data.getBytes("UTF-8"));
	}

	public HttpURLConnection post(URL url, byte[] data) throws Exception {
		URLConnection con = url.openConnection();
		// cat.info(url);
		if (!(con instanceof HttpURLConnection)) {
			throw new Exception("Only HTTP related urls are supported " + url);
		}

		if (con instanceof HttpsURLConnection) {
			HttpsURLConnection httpsCon = (HttpsURLConnection) con;
			httpsCon.setSSLSocketFactory(mSSLCtx.getSocketFactory());
		}
		HttpURLConnection httpCon = (HttpURLConnection) con;
		httpCon.setDoInput(true);
		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("POST");
		httpCon.setRequestProperty("Connection", "close");
		httpCon.setRequestProperty("Content-Type",
				"application/xml; charset=utf-8");

		httpCon.setRequestProperty("Content-Length", "" + data.length);
		httpCon.setInstanceFollowRedirects(false);
		int connectionTimeout = 10000;
		int readTimeout = 10000;
		httpCon.setConnectTimeout(connectionTimeout);
		httpCon.setReadTimeout(readTimeout);
		DataOutputStream outputStream = null;
		try {
			outputStream = new DataOutputStream(httpCon.getOutputStream());
			outputStream.write(data);
			outputStream.flush();
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
		/*
		 * OutputStream outputStream = con.getOutputStream();
		 * outputStream.write(data);
		 */
		return httpCon;
	}

	public HttpURLConnection get(URL url) throws Exception {
		URLConnection con = url.openConnection();
//		cat.debug(url);
		if (!(con instanceof HttpURLConnection)) {
			throw new Exception("Only HTTP related urls are supported " + url);
		}
		if (con instanceof HttpsURLConnection) {
			HttpsURLConnection httpsCon = (HttpsURLConnection) con;
			httpsCon.setSSLSocketFactory(mSSLCtx.getSocketFactory());
		}
		int getConTimeout = 10000;
		int getReadTimeout = 10000;
		con.setConnectTimeout(getConTimeout);
		con.setReadTimeout(getReadTimeout);
		return (HttpURLConnection) con;
	}

	private static TrustManager[] getTrustManagers() throws Exception {
		InputStream is = null;
		try {
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SunX509");
			KeyStore keyStore = KeyStore.getInstance("JKS");
			File file = new File(System.getProperty("java.home")
					+ "/lib/security/cacerts");
			is = new FileInputStream(file);
			keyStore.load(is, "changeit".toCharArray());
			tmf.init(keyStore);
			return tmf.getTrustManagers();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Throwable tx) {
				}
			}
		}
	}

	class HttpKeyManager implements X509KeyManager {
		/** last modified time for Keystore */
		protected long mKeyStoreLastModified = 0;

		protected File mKeyStorePath;
		protected String mPassword;

		/** <code>mKeyManagers</code> cached KeyManagers from KeyStore */
		protected X509KeyManager[] mKeyManagers = null;

		public HttpKeyManager(File keyStore, String passwd) {
			mKeyStorePath = keyStore;
			mPassword = passwd;
		}

		/**
		 * Get the matching aliases for authenticating the client side of a
		 * secure * socket given the public key type and the list of certificate
		 * issuer authorities recognized by the peer (if any).
		 *
		 * @param keyType
		 *            the key algorithm type name
		 * @param issuers
		 *            the list of acceptable CA issuer subject names, or null if
		 *            it does not matter which issuers are used.
		 * @return an array of the matching alias names, or null if there were
		 *         no matches.
		 */
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			String aliases[] = null;
			for (X509KeyManager km : getKeyManagers()) {
				if ((aliases = km.getClientAliases(keyType, null)) != null)
					break;
			}

			return aliases;
		}

		/**
		 * Choose an alias to authenticate the client side of a secure socket
		 * given the public key type and the list of certificate issuer
		 * authorities recognized by the peer (if any).
		 *
		 * @param keyType
		 *            the key algorithm type name(s), ordered with the
		 *            most-preferred key type first.
		 * @param issuers
		 *            the list of acceptable CA issuer subject names or null if
		 *            it does not matter which issuers are used.
		 * @param socket
		 *            the socket to be used for this connection. This parameter
		 *            can be null, which indicates that implementations are free
		 *            to select an alias applicable to any socket.
		 * @return the alias name for the desired key, or null if there are no
		 *         matches.
		 */
		public String chooseClientAlias(String[] keyType, Principal[] issuers,
				Socket socket) {
			String alias = null;
			for (X509KeyManager km : getKeyManagers()) {
				if ((alias = km.chooseClientAlias(keyType, null, null)) != null)
					break;
			}

			return alias;
		}

		/**
		 * Get the matching aliases for authenticating the server side of a
		 * secure socket given the public key type and the list of certificate
		 * issuer authorities recognized by the peer (if any).
		 *
		 * @param keyType
		 *            the key algorithm type name
		 * @param issuers
		 *            the list of acceptable CA issuer subject names or null if
		 *            it does not matter which issuers are used.
		 * @return an array of the matching alias names, or null if there were
		 *         no matches.
		 */
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			String aliases[] = null;
			for (X509KeyManager km : getKeyManagers()) {
				if ((aliases = km.getServerAliases(keyType, issuers)) != null)
					break;
			}

			return aliases;
		}

		/**
		 * Choose an alias to authenticate the server side of a secure socket
		 * given the public key type and the list of certificate issuer
		 * authorities recognized by the peer (if any).
		 *
		 * @param keyType
		 *            the key algorithm type name.
		 * @param issuers
		 *            the list of acceptable CA issuer subject names or null if
		 *            it does not matter which issuers are used.
		 * @param socket
		 *            the socket to be used for this connection. This parameter
		 *            can be null, which indicates that implementations are free
		 *            to select an alias applicable to any socket.
		 * @return the alias name for the desired key, or null if there are no
		 *         matches.
		 */
		public String chooseServerAlias(String keyType, Principal[] issuers,
				Socket socket) {
			String alias = null;
			for (X509KeyManager km : getKeyManagers()) {
				alias = km.chooseServerAlias(keyType, issuers, socket);
				if (alias != null)
					break;
			}
			return alias;
		}

		/**
		 * Returns the certificate chain associated with the given alias.
		 *
		 * @param alias
		 *            the alias name
		 * @return the certificate chain (ordered with the user's certificate
		 *         first and the root certificate authority last), or null if
		 *         the alias can't be found.
		 */
		public X509Certificate[] getCertificateChain(String alias) {
			X509Certificate certs[] = null;
			for (X509KeyManager km : getKeyManagers()) {
				if ((certs = km.getCertificateChain(alias)) != null)
					break;
			}

			return certs;
		}

		/**
		 * Returns the key associated with the given alias.
		 *
		 * @param alias
		 *            the alias name
		 * @return the requested key, or null if the alias can't be found.
		 */
		public PrivateKey getPrivateKey(String alias) {
			PrivateKey pkey = null;
			for (X509KeyManager km : getKeyManagers()) {
				if ((pkey = km.getPrivateKey(alias)) != null)
					break;
			}

			return pkey;
		}

		// ==================================================================
		// Private Methods
		// ==================================================================

		private synchronized X509KeyManager[] getKeyManagers() {
			// Return mKeyManagers if not modified (or doesn't exist).
			if (!mKeyStorePath.exists()
					|| mKeyStoreLastModified == mKeyStorePath.lastModified()) {
				// Return the already cached KeyManagers
				return mKeyManagers;
			}

			InputStream is = null;
			try {
				// Create KeyStore and load contents from ksPath.
				KeyStore keystore = KeyStore.getInstance("PKCS12");

				// Use keystore to load cacerts
				is = new FileInputStream(mKeyStorePath);
				keystore.load(is, mPassword.toCharArray());

				// Initialize KeyManagers from the loaded KeyStore
				KeyManagerFactory kmfactory = KeyManagerFactory
						.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmfactory.init(keystore, mPassword.toCharArray());

				List<X509KeyManager> tmp = new LinkedList<X509KeyManager>();
				for (KeyManager km : kmfactory.getKeyManagers()) {
					if (km instanceof X509KeyManager)
						tmp.add((X509KeyManager) km);
				}

				// Convert managers list to an array...
				mKeyManagers = (X509KeyManager[]) tmp
						.toArray(new X509KeyManager[0]);
			} catch (Throwable tx) {
				tx.printStackTrace();
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (Exception ex) { /*--*/
					}
			}

			// Return the newly cached KeyManagers
			return mKeyManagers;
		}

	} 

	private static String loadFile(File file) throws Exception {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		try {
			byte[] b = new byte[(int) raf.length()];
			raf.readFully(b);
			return new String(b);
		} finally {
			if (raf != null)
				raf.close();
		}
	}

	/*
	 * private static void usage() {
	 * System.out.println("Usage : HttpsAPI get <url>");
	 * System.out.println("        HttpsAPI post <url> <file>"); System.exit(1);
	 * }
	 */

	public static HttpURLConnection getConnection(String conType,
			String urlString, String fileTopost, String certificate,
			String certPassword) throws Exception {
		// String env = System.getProperty("e2.env.subtype");
		HttpURLConnection con = null;
		try {
			String op = conType;
			if (op.equals("get")) {
				URL url = new URL(urlString);
				HttpsAPI api = new HttpsAPI(new File(certificate),
						certPassword);
				return con = api.get(url);

			} else if (op.equals("post")) {
				URL url = new URL(urlString);
				File file = new File(fileTopost);

				HttpsAPI api = new HttpsAPI(new File(certificate),
						certPassword);
				return con = api.post(url, file);
			}
		} catch (Exception e) {
			cat.info(e.toString());
			e.printStackTrace();
		}
		return con;
	}

}
