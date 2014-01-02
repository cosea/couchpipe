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

/**
 * A dispatcher for a specific connection
 * 
 * @author Florian Westreicher aka meredrica
 * @since Jan 2, 2014
 */
public class Dispatcher extends Thread {

	private long timeout;
	private URL to;
	private URL from;
	private long lastHeartbeat = 0;
	private Timer timer = new Timer();
	private BufferedReader br;
	private String toAuth;
	private String fromAuth;
	private TimerTask task = new TimerTask() {
		@Override
		public void run() {
			// check timeout
			if (System.currentTimeMillis() - lastHeartbeat > timeout) {
				// we have a timeout. reconnect
				System.err.println("timeout detected, reconnecting");
				new Dispatcher(from, fromAuth, to, toAuth, timeout).start();
				running = false;
				interrupt();
			} else {
				// timeout check pass
			}
		}
	};
	private boolean running;

	/**
	 * Closes all ressources
	 */
	private void closeAll() {
		try {
			if (br != null) {
				br.close();
			}
		} catch (IOException e) {
			System.out.println("exception");
		}
		if (task != null) {
			task.cancel();
		}
		if (timer != null) {
			timer.cancel();
		}
		task = null;
		timer = null;
	}

	/**
	 * Creates the dispatcher
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
	public Dispatcher(final URL from, final String fromAuth, final URL to, final String toAuth, final long timeout) {
		this.from = from;
		this.fromAuth = fromAuth;
		this.to = to;
		this.toAuth = toAuth;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		super.run();
		InputStream is = null;
		InputStreamReader isr = null;
		try {
			URLConnection conn = from.openConnection();
			conn.setDoInput(true);
			conn.setReadTimeout(0);
			if (fromAuth != null) {
				conn.setRequestProperty("Authorization", fromAuth);
			}
			conn.connect();
			is = conn.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line;
			// connect to the stream
			// start the watchdog timer
			timer.schedule(task, 1000, timeout / 10);
			running = true;
			while (running) {
				if (br.ready()) {
					line = br.readLine();
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
							int responseCode = out.getResponseCode();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							if (dos != null) {
								dos.close();
							}
							if (stream != null) {
								stream.close();
							}
						}
					}
				}
				sleep(10);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
			}
			try {
				if (isr != null) {
					isr.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
			}
		}
		closeAll();
	}
}
