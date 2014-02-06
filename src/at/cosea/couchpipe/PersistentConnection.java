package at.cosea.couchpipe;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dispatcher for a specific connection
 * 
 * @author Florian Westreicher aka meredrica
 * @since Jan 2, 2014
 */
public class PersistentConnection extends Thread {

	private long timeout;
	private URL to;
	private URL from;
	private long lastHeartbeat = 0;
	private Timer timer = new Timer();
	private String toAuth;
	private String fromAuth;

	private Logger logger = Logger.getLogger(getClass().getSimpleName());

	private TimerTask task = new TimerTask() {
		private int counter;

		@Override
		public void run() {
			// check timeout
			if (System.currentTimeMillis() - lastHeartbeat > timeout) {
				// we have a timeout. restart
				logger.warning("timeout detected, restarting");
				restart();
			} else if (counter++ == 10) {
				// timeout check pass
				logger.info("10 timeout checks passed");
				counter = 0;
			}
		}
	};
	private boolean running;
	private InputStream persistentInputStream;
	private InputStreamReader persistentStreamReader;
	private BufferedReader persistentBufferedReader;

	/**
	 * Restarts all connections.
	 */
	private void restart() {
		logger.info("restarting");
		// set the initial hearbeat
		lastHeartbeat = System.currentTimeMillis();
		persistentInputStream = null;
		persistentStreamReader = null;
		persistentBufferedReader = null;
		try {
			URLConnection conn = from.openConnection();
			conn.setDoInput(true);
			conn.setReadTimeout(0);
			if (fromAuth != null) {
				conn.setRequestProperty("Authorization", fromAuth);
			}
			conn.connect();
			persistentInputStream = conn.getInputStream();
			persistentStreamReader = new InputStreamReader(persistentInputStream);
			persistentBufferedReader = new BufferedReader(persistentStreamReader);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "exception in restart()", e);
			running = false;
			closeAll();
		}
	}

	private void closeAll() {
		try {
			if (persistentBufferedReader != null) {
				persistentBufferedReader.close();
			}
		} catch (IOException e1) {
		}
		try {
			if (persistentStreamReader != null) {
				persistentStreamReader.close();
			}
		} catch (IOException e1) {
		}
		try {
			if (persistentInputStream != null) {
				persistentInputStream.close();
			}
		} catch (IOException e1) {
		}
	}

	/**
	 * Creates the persistent connection
	 * 
	 * @param from
	 *          Url where the connection should be opened
	 * @param fromAuth
	 *          Base64 string to use for http basic auth (or null if no auth needed)
	 * @param to
	 *          Url where the packages should be delivered
	 * @param toAuth
	 *          Base64 string to use for http basic auth (or null if no auth needed)
	 * @param timeout
	 *          Timeout to use for checking
	 * 
	 */
	public PersistentConnection(final URL from,
			final String fromAuth,
			final URL to,
			final String toAuth,
			final long timeout) {
		this.from = from;
		this.fromAuth = fromAuth;
		this.to = to;
		this.toAuth = toAuth;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		super.run();
		// it's not a real restart, but it works the same way
		restart();
		// start the watchdog timer
		timer.schedule(task, 1000, timeout / 10);
		// connect to the stream
		running = true;
		while (running) {
			try {
				if (persistentBufferedReader != null && persistentBufferedReader.ready()) {
					String line = persistentBufferedReader.readLine();
					logger.fine("got: " + line);
					lastHeartbeat = System.currentTimeMillis();
					if (line.isEmpty()) {
						// this is a heartbeat.
					} else {
						// deliver the response
						HttpURLConnection out = (HttpURLConnection) to.openConnection();
						out.setRequestMethod("POST");
						if (toAuth != null) {
							out.setRequestProperty("Authorization", toAuth);
						}
						out.setDoOutput(true);
						OutputStream stream = null;
						DataOutputStream dos = null;
						try {
							stream = out.getOutputStream();
							dos = new DataOutputStream(stream);
							dos.writeBytes(line);
							dos.flush();
							int code = out.getResponseCode(); // keep this line! it executes the whole http connection
							logger.fine("response code:" + code);
						} catch (Exception e) {
							logger.log(Level.WARNING, "could not write to stream", e);
						} finally {
							if (dos != null) {
								try {
									dos.close();
								} catch (IOException e) {
								}
							}
							if (stream != null) {
								try {
									stream.close();
								} catch (IOException e) {
								}
							}
						}
					}
				}
				sleep(10);
			} catch (InterruptedException | IOException ex) {
				logger.log(Level.WARNING, "error in run", ex);
			}
		}
	}
}
