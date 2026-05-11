package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Instant actions that the AGV is to execute as soon as they arrive.
 */
public class InstantActions {
    private long headerID;
    private String manufacturer;
    private String serialNumber;
    private String timestamp;
    private String version;
    private Action[] actions;

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
     * Array of actions that need to be performed immediately and are not part of the regular
     * order.
     */
    @JsonProperty("actions")
    public Action[] getActions() { return actions; }
    @JsonProperty("actions")
    public void setActions(Action[] value) { this.actions = value; }
}
