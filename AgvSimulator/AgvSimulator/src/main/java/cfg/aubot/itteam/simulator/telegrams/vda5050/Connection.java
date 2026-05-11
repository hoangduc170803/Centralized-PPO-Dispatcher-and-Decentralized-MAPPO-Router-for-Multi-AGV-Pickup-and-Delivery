package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.time.Instant;

/**
 * AGV connection state reported as a last will message. Has to be sent with retain flag.
 * Once the AGV comes online, it has to send this message on its connect topic, with the
 * connectionState enum set to "ONLINE".
 * The last will message is to be configured with the connection state set to
 * "CONNECTIONBROKEN".
 * Thus, if the AGV disconnects from the broker, master control gets notified via the topic
 * "connection".
 * If the AGV is disconnecting in an orderly fashion (e.g. shutting down, sleeping), the AGV
 * is to publish a message on this topic with the connectionState set to "OFFLINE".
 */
public class Connection {
    private long headerID;
    private String manufacturer;
    private String serialNumber;
    private String timestamp;
    private String version;
    private ConnectionState connectionState;

    public Connection(long headerID, String manufacturer, String serialNumber) {
        this.headerID = headerID;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.timestamp = Instant.now().toString();
        this.version = "2.0.0";
        this.connectionState = ConnectionState.ONLINE;
    }

    public Connection(long headerID, String manufacturer, String serialNumber, ConnectionState connectionState) {
        this(headerID, manufacturer, serialNumber);
        this.connectionState = connectionState;
    }

    /**
     * headerId of the message. The headerId is defined per topic and incremented by 1 with each
     * sent (but not necessarily received) message.
     */
    @JsonProperty("headerId")
    public long getHeaderID() { return headerID; }
    @JsonProperty("headerId")
    public void setHeaderID(long value) { this.headerID = value; }

    /**
     * Manufacturer of the AGV
     */
    @JsonProperty("manufacturer")
    public String getManufacturer() { return manufacturer; }
    @JsonProperty("manufacturer")
    public void setManufacturer(String value) { this.manufacturer = value; }

    /**
     * Serial number of the AGV
     */
    @JsonProperty("serialNumber")
    public String getSerialNumber() { return serialNumber; }
    @JsonProperty("serialNumber")
    public void setSerialNumber(String value) { this.serialNumber = value; }

    /**
     * Timestamp (ISO8601, UTC); YYYY-MM-DDTHH:mm:ss.ssZ; e.g. 2017-04-15T11:40:03.12Z
     */
    @JsonProperty("timestamp")
    public String getTimestamp() { return timestamp; }
    @JsonProperty("timestamp")
    public void setTimestamp(String value) { this.timestamp = value; }

    /**
     * Version of the protocol [Major].[Minor].[Patch], e.g. 1.3.2
     */
    @JsonProperty("version")
    public String getVersion() { return version; }
    @JsonProperty("version")
    public void setVersion(String value) { this.version = value; }

    /**
     * Connection state.
     * ONLINE: connection between AGV and broker is active.
     * OFFLINE: connection between AGV and broker has gone offline in a coordinated way.
     * CONNECTIONBROKEN: The connection between AGV and broker has unexpectedly ended.
     */
    @JsonProperty("connectionState")
    public ConnectionState getConnectionState() { return connectionState; }
    @JsonProperty("connectionState")
    public void setConnectionState(ConnectionState value) { this.connectionState = value; }
}
