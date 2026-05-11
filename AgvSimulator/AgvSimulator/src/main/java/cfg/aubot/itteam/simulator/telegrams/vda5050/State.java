package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.time.Instant;

/**
 * All encompassing state of the AGV.
 */
public class State {
    private long headerID;
    private String timestamp;
    private String version;
    private String manufacturer;
    private String serialNumber;
    private String orderID;
    private long orderUpdateID;
    private String lastNodeID;
    private long lastNodeSequenceID;
    private NodeState[] nodeStates;
    private EdgeState[] edgeStates;
    private boolean driving;
    private ActionState[] actionStates;
    private BatteryState batteryState;
    private AgvPosition agvPosition;
    private OperatingMode operatingMode;
    private Error[] errors;
    private SafetyStatus safetyState;
    private Velocity velocity;
    private Double distanceSinceLastNode;
    private Information[] information;
    private Load[] loads;
    private Object[] maps;
    private Boolean newBaseRequest;
    private Boolean paused;
    private String zoneSetID;

    public State(long headerId, String manufacturer, String serialNumber, String orderID, long orderUpdateID,
                 String lastNodeID, long lastNodeSequenceID, NodeState[] nodeStates, EdgeState[] edgeStates,
                 boolean driving, ActionState[] actionStates, BatteryState batteryState, double velocity, double distanceSinceLastNode) {
        this.headerID = headerId;
        this.timestamp = Instant.now().toString();
        this.version = "2.0.0";
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.orderID = orderID;
        this.orderUpdateID = orderUpdateID;
        this.lastNodeID = lastNodeID;
        this.lastNodeSequenceID = lastNodeSequenceID;
        this.nodeStates = nodeStates;
        this.edgeStates = edgeStates;
        this.driving = driving;
        this.actionStates = actionStates;
        this.batteryState = batteryState;
        this.operatingMode = OperatingMode.AUTOMATIC;
        this.errors = new Error[0];
        this.safetyState = new SafetyStatus(EStop.NONE, false);
        this.velocity = new Velocity(velocity);
        this.distanceSinceLastNode = distanceSinceLastNode;
        this.paused = false;
        this.information = createSampleInformation();
    }

    public State refresh(long headerID, String lastNodeId, NodeState[] nodeStates, EdgeState[] edgeStates, ActionState[] actionStates, double distanceSinceLastNode, int energyLevel) {
        this.headerID = headerID;
        this.timestamp = Instant.now().toString();
        this.lastNodeID = lastNodeId;
        this.nodeStates = nodeStates;
        this.edgeStates = edgeStates;
        this.actionStates = actionStates;
        this.distanceSinceLastNode = distanceSinceLastNode;
        this.batteryState.setBatteryCharge(energyLevel);
        return this;
    }

    private Information[] createSampleInformation() {
        Information[] info = new Information[5];
        
        // _sporadicUpdate info
        Information sporadicUpdate = new Information();
        sporadicUpdate.setInfoType("_sporadicUpdate");
        sporadicUpdate.setInfoLevel(InfoLevel.DEBUG);
        sporadicUpdate.setInfoReferences(new InfoReference[]{
            createInfoReference("number", "0")
        });
        info[0] = sporadicUpdate;
        
        // current info
        Information current = new Information();
        current.setInfoType("current");
        current.setInfoLevel(InfoLevel.INFO);
        current.setInfoReferences(new InfoReference[]{
            createInfoReference("value", "-1.100000023841858")
        });
        info[1] = current;
        
        // liftSensorStatus info
        Information liftSensorStatus = new Information();
        liftSensorStatus.setInfoType("liftSensorStatus");
        liftSensorStatus.setInfoLevel(InfoLevel.INFO);
        liftSensorStatus.setInfoReferences(new InfoReference[]{
            createInfoReference("head", "true"),
            createInfoReference("tail", "true"),
            createInfoReference("up", "false"),
            createInfoReference("down", "true")
        });
        info[2] = liftSensorStatus;
        
        // laserShortStop info
        Information laserShortStop = new Information();
        laserShortStop.setInfoType("laserShortStop");
        laserShortStop.setInfoLevel(InfoLevel.INFO);
        laserShortStop.setInfoReferences(new InfoReference[]{
            createInfoReference("count", "0"),
            createInfoReference("avg duration", "0")
        });
        info[3] = laserShortStop;
        
        // bms info
        Information bms = new Information();
        bms.setInfoType("bms");
        bms.setInfoLevel(InfoLevel.INFO);
        bms.setInfoReferences(new InfoReference[]{
            createInfoReference("soc", "98.2")
        });
        info[4] = bms;
        
        return info;
    }
    
    private InfoReference createInfoReference(String key, String value) {
        InfoReference ref = new InfoReference();
        ref.setReferenceKey(key);
        ref.setReferenceValue(value);
        return ref;
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
     * Contains a list of the current actions and the actions which are yet to be finished. This
     * may include actions from previous nodes that are still in progress.
     * When an action is completed, an updated state message is published with actionStatus set
     * to finished and if applicable with the corresponding resultDescription. The actionStates
     * are kept until a new order is received.
     */
    @JsonProperty("actionStates")
    public ActionState[] getActionStates() { return actionStates; }
    @JsonProperty("actionStates")
    public void setActionStates(ActionState[] value) { this.actionStates = value; }

    /**
     * Current position of the AGV on the map.
     * Optional: Can only be omitted for AGVs without the capability to localize themselves,
     * e.g. line guided AGVs.
     */
    @JsonProperty("agvPosition")
    public AgvPosition getAgvPosition() { return agvPosition; }
    @JsonProperty("agvPosition")
    public void setAgvPosition(AgvPosition value) { this.agvPosition = value; }

    /**
     * Contains all battery-related information.
     */
    @JsonProperty("batteryState")
    public BatteryState getBatteryState() { return batteryState; }
    @JsonProperty("batteryState")
    public void setBatteryState(BatteryState value) { this.batteryState = value; }

    /**
     * Used by line guided vehicles to indicate the distance it has been driving past the
     * lastNodeId.
     * Distance is in meters
     */
    @JsonProperty("distanceSinceLastNode")
    public Double getDistanceSinceLastNode() { return distanceSinceLastNode; }
    @JsonProperty("distanceSinceLastNode")
    public void setDistanceSinceLastNode(Double value) { this.distanceSinceLastNode = value; }

    /**
     * True: indicates that the AGV is driving and/or rotating. Other movements of the AGV (e.g.
     * lift movements) are not included here.
     * False: indicates that the AGV is neither driving nor rotating
     */
    @JsonProperty("driving")
    public boolean getDriving() { return driving; }
    @JsonProperty("driving")
    public void setDriving(boolean value) { this.driving = value; }

    /**
     * Information about the edges the AGV still has to drive over. Empty list if the AGV is
     * idle.
     */
    @JsonProperty("edgeStates")
    public EdgeState[] getEdgeStates() { return edgeStates; }
    @JsonProperty("edgeStates")
    public void setEdgeStates(EdgeState[] value) { this.edgeStates = value; }

    /**
     * Array of error objects. All active errors of the AGV should be in the list. An empty
     * array indicates that the AGV has no active errors.
     */
    @JsonProperty("errors")
    public Error[] getErrors() { return errors; }
    @JsonProperty("errors")
    public void setErrors(Error[] value) { this.errors = value; }

    /**
     * Array of information objects. An empty array indicates that the AGV has no information.
     * This should only be used for visualization or debugging – it must not be used for logic
     * in master control. Objects are only for visualization/debugging. There's no specification
     * when these objects are deleted.
     */
    @JsonProperty("information")
    public Information[] getInformation() { return information; }
    @JsonProperty("information")
    public void setInformation(Information[] value) { this.information = value; }

    /**
     * nodeID of last reached node or, if AGV is currently on a node, current node (e. g.
     * node7). Empty string ("") if no lastNodeId is available.
     */
    @JsonProperty("lastNodeId")
    public String getLastNodeID() { return lastNodeID; }
    @JsonProperty("lastNodeId")
    public void setLastNodeID(String value) { this.lastNodeID = value; }

    /**
     * sequenceId of the last reached node or, if the AGV is currently on a node, sequenceId of
     * current node.
     * 0 if no lastNodeSequenceId is available.
     */
    @JsonProperty("lastNodeSequenceId")
    public long getLastNodeSequenceID() { return lastNodeSequenceID; }
    @JsonProperty("lastNodeSequenceId")
    public void setLastNodeSequenceID(long value) { this.lastNodeSequenceID = value; }

    /**
     * Array for information about the loads that an AGV currently carries, if the AGV has any
     * information about them. This array is optional: if an AGV cannot reason about its load
     * state, it shall not send this field. If an empty field is sent, MC is to assume that the
     * AGV can reason about its load state and that the AGV currently does not carry a load.
     */
    @JsonProperty("loads")
    public Load[] getLoads() { return loads; }
    @JsonProperty("loads")
    public void setLoads(Load[] value) { this.loads = value; }

    /**
     * Array of map-objects that are currently stored on the vehicle.
     */
    @JsonProperty("maps")
    public Object[] getMaps() { return maps; }
    @JsonProperty("maps")
    public void setMaps(Object[] value) { this.maps = value; }

    /**
     * True: AGV is almost at the end of the base and will reduce speed if no new base is
     * transmitted. Trigger for MC to send new base
     * False: no base update required
     */
    @JsonProperty("newBaseRequest")
    public Boolean getNewBaseRequest() { return newBaseRequest; }
    @JsonProperty("newBaseRequest")
    public void setNewBaseRequest(Boolean value) { this.newBaseRequest = value; }

    /**
     * Information about the nodes the AGV still has to drive over. Empty list if idle.
     */
    @JsonProperty("nodeStates")
    public NodeState[] getNodeStates() { return nodeStates; }
    @JsonProperty("nodeStates")
    public void setNodeStates(NodeState[] value) { this.nodeStates = value; }

    /**
     * Current operating mode of the AGV. For additional information, see the table
     * OperatingModes in chapter 6.10.6.
     */
    @JsonProperty("operatingMode")
    public OperatingMode getOperatingMode() { return operatingMode; }
    @JsonProperty("operatingMode")
    public void setOperatingMode(OperatingMode value) { this.operatingMode = value; }

    /**
     * Unique order identification of the current order or the previous finished order. The
     * orderId is kept until a new order is received. Empty string ("") if no previous orderId
     * is available.
     */
    @JsonProperty("orderId")
    public String getOrderID() { return orderID; }
    @JsonProperty("orderId")
    public void setOrderID(String value) { this.orderID = value; }

    /**
     * Order Update Identification to identify that an order update has been accepted by the
     * AGV. 0 if no previous orderUpdateId is available.
     */
    @JsonProperty("orderUpdateId")
    public long getOrderUpdateID() { return orderUpdateID; }
    @JsonProperty("orderUpdateId")
    public void setOrderUpdateID(long value) { this.orderUpdateID = value; }

    /**
     * True: AGV is currently in a paused state, either because of the push of a physical button
     * on the AGV or because of an instantAction. The AGV can resume the order.
     * False: The AGV is currently not in a paused state.
     */
    @JsonProperty("paused")
    public Boolean getPaused() { return paused; }
    @JsonProperty("paused")
    public void setPaused(Boolean value) { this.paused = value; }

    /**
     * Object that holds information about the safety status
     */
    @JsonProperty("safetyState")
    public SafetyStatus getSafetyState() { return safetyState; }
    @JsonProperty("safetyState")
    public void setSafetyState(SafetyStatus value) { this.safetyState = value; }

    /**
     * The AGVs velocity in vehicle coordinates.
     */
    @JsonProperty("velocity")
    public Velocity getVelocity() { return velocity; }
    @JsonProperty("velocity")
    public void setVelocity(Velocity value) { this.velocity = value; }

    /**
     * Unique ID of the zone set that the AGV currently uses for path planning. Must be the same
     * as the one used in the order, otherwise the AGV is to reject the order.
     * Optional: If the AGV does not use zones, this field can be omitted.
     */
    @JsonProperty("zoneSetId")
    public String getZoneSetID() { return zoneSetID; }
    @JsonProperty("zoneSetId")
    public void setZoneSetID(String value) { this.zoneSetID = value; }
}
