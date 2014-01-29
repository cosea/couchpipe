package at.cosea.couchpipe;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.cosea.couchpipe.model.Connection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * Starts the couch pipe server.
 * 
 * @author Florian Westreicher aka meredrica
 * @since Jan 2, 2014
 */
public class CouchPipe {

	private static Logger logger = Logger.getLogger(CouchPipe.class.getSimpleName());

	/**
	 * 
	 * @param args
	 *          A string pointing to the config.json
	 */
	public static void main(final String[] args) {
		if (args.length != 1) {
			System.out.println("path to config.json is required.");
			System.exit(1);
		}

		File config = new File(args[0]);
		if (config.exists() == false || config.isDirectory()) {
			logger.severe("config file missing or directory, expected a .json file");
			System.exit(1);
		}
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			List<Connection> connections = objectMapper.readValue(config, new TypeReference<List<Connection>>() {
			});
			if (connections.isEmpty()) {
				logger.severe("could not find any connections specified in " + config.getAbsolutePath());
				System.exit(1);
			}
			// now we got a list of connections, start a dispatcher for each of them
			for (Connection connection : connections) {
				URL from = new URL(connection.getFrom());
				URL to = new URL(connection.getTo());
				new Dispatcher(from, connection.getFromAuth(), to, connection.getToAuth(), connection.getTimeout()).start();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "could not parse connections file", e);
			System.exit(1);
		}

	}
}
