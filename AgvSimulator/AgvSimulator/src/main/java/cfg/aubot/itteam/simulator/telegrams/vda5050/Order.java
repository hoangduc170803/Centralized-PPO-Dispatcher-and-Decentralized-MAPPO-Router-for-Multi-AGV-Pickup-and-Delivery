package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.time.Instant;

/**
 * An order to be communicated from master control to the AGV.
 */
public class Order {
    private long headerID;
    private String manufacturer;
    private String serialNumber;
    private String timestamp;
    private String version;
    private Edge[] edges;
    private Node[] nodes;
    private String orderID;
    private long orderUpdateID;
    private String zoneSetID;

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
     * Base and Horizon Edges of the Order Graph.
     */
    @JsonProperty("edges")
    public Edge[] getEdges() { return edges; }
    @JsonProperty("edges")
    public void setEdges(Edge[] value) { this.edges = value; }

    /**
     * This list holds the base and the horizon nodes of the order graph.
     */
    @JsonProperty("nodes")
    public Node[] getNodes() { return nodes; }
    @JsonProperty("nodes")
    public void setNodes(Node[] value) { this.nodes = value; }

    /**
     * Unique order Identification.
     */
    @JsonProperty("orderId")
    public String getOrderID() { return orderID; }
    @JsonProperty("orderId")
    public void setOrderID(String value) { this.orderID = value; }

    /**
     * orderUpdate identification. Is unique per orderId. If an order update is rejected, this
     * field is to be passed in the rejection message.
     */
    @JsonProperty("orderUpdateId")
    public long getOrderUpdateID() { return orderUpdateID; }
    @JsonProperty("orderUpdateId")
    public void setOrderUpdateID(long value) { this.orderUpdateID = value; }

    /**
     * Unique identifier of the zone set that the AGV has to use for navigation or that was used
     * by MC for planning.
     * Optional: Some MC systems do not use zones. Some AGVs do not understand zones. Do not add
     * to message if no zones are used.
     */
    @JsonProperty("zoneSetId")
    public String getZoneSetID() { return zoneSetID; }
    @JsonProperty("zoneSetId")
    public void setZoneSetID(String value) { this.zoneSetID = value; }
}
