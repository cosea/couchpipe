package at.cosea.couchpipe.model;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Models a connection specified in the config.
 * 
 * @author Florian Westreicher aka meredrica
 * @since Jan 2, 2014
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {

	@JsonProperty("from")
	private String from;

	@JsonProperty("to")
	private String to;

	@JsonProperty("from_user")
	private String fromUser;

	@JsonProperty("to_user")
	private String toUser;

	@JsonProperty("from_pass")
	private String fromPassword;

	@JsonProperty("to_pass")
	private String toPassword;

	@JsonProperty("timeout")
	private long timeout;

	public Connection() {
		// keep for Jackson
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	/**
	 * @return the timeout in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}

	public String getFromAuth() {
		if (fromUser == null || fromPassword == null || fromUser.isEmpty() || fromPassword.isEmpty()) {
			return null;
		}
		return "Basic " + Base64.encodeBase64String((fromUser + ":" + fromPassword).getBytes());
	}

	public String getToAuth() {
		if (toUser == null || toPassword == null || toUser.isEmpty() || toPassword.isEmpty()) {
			return null;
		}
		return "Basic " + Base64.encodeBase64String((toUser + ":" + toPassword).getBytes());
	}
}
