package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * maximum lengths of strings
 */
public class MaxStringLens {
    private Long enumLen;
    private Long idLen;
    private Boolean idNumericalOnly;
    private Long loadIDLen;
    private Long msgLen;
    private Long topicElemLen;
    private Long topicSerialLen;

    /**
     * maximum length of ENUM- and Key-Strings. Affected parameters: action.actionType,
     * action.blockingType, edge.direction, actionParameter.key, state.operatingMode,
     * load.loadPosition, load.loadType, actionState.actionStatus, error.errorType,
     * error.errorLevel, errorReference.referenceKey, info.infoType, info.infoLevel,
     * safetyState.eStop, connection.connectionState
     */
    @JsonProperty("enumLen")
    public Long getEnumLen() { return enumLen; }
    @JsonProperty("enumLen")
    public void setEnumLen(Long value) { this.enumLen = value; }

    /**
     * maximum length of ID-Strings. Affected parameters: order.orderId, order.zoneSetId,
     * node.nodeId, nodePosition.mapId, action.actionId, edge.edgeId, edge.startNodeId,
     * edge.endNodeId
     */
    @JsonProperty("idLen")
    public Long getIDLen() { return idLen; }
    @JsonProperty("idLen")
    public void setIDLen(Long value) { this.idLen = value; }

    /**
     * If true ID-strings need to contain numerical values only
     */
    @JsonProperty("idNumericalOnly")
    public Boolean getIDNumericalOnly() { return idNumericalOnly; }
    @JsonProperty("idNumericalOnly")
    public void setIDNumericalOnly(Boolean value) { this.idNumericalOnly = value; }

    /**
     * maximum length of loadId Strings
     */
    @JsonProperty("loadIdLen")
    public Long getLoadIDLen() { return loadIDLen; }
    @JsonProperty("loadIdLen")
    public void setLoadIDLen(Long value) { this.loadIDLen = value; }

    /**
     * maximum MQTT Message length
     */
    @JsonProperty("msgLen")
    public Long getMsgLen() { return msgLen; }
    @JsonProperty("msgLen")
    public void setMsgLen(Long value) { this.msgLen = value; }

    /**
     * maximum length of all other parts in MQTT-topics. Affected parameters: order.timestamp,
     * order.version, order.manufacturer, instantActions.timestamp, instantActions.version,
     * instantActions.manufacturer, state.timestamp, state.version, state.manufacturer,
     * visualization.timestamp, visualization.version, visualization.manufacturer,
     * connection.timestamp, connection.version, connection.manufacturer
     */
    @JsonProperty("topicElemLen")
    public Long getTopicElemLen() { return topicElemLen; }
    @JsonProperty("topicElemLen")
    public void setTopicElemLen(Long value) { this.topicElemLen = value; }

    /**
     * maximum length of serial-number part in MQTT-topics. Affected Parameters:
     * order.serialNumber, instantActions.serialNumber, state.SerialNumber,
     * visualization.serialNumber, connection.serialNumber
     */
    @JsonProperty("topicSerialLen")
    public Long getTopicSerialLen() { return topicSerialLen; }
    @JsonProperty("topicSerialLen")
    public void setTopicSerialLen(Long value) { this.topicSerialLen = value; }
}
