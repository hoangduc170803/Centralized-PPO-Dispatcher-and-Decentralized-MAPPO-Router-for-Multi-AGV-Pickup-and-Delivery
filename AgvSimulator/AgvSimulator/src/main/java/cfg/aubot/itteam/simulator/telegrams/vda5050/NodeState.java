package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class NodeState {
    private String nodeDescription;
    private String nodeID;
    private NodePosition nodePosition;
    private boolean released;
    private long sequenceID;

    public NodeState() {
    }

    public NodeState(Node node) {
        this.nodeID = node.getNodeID();
        this.released = node.getReleased();
        this.sequenceID = node.getSequenceID();
    }

    /**
     * Verbose node description
     */
    @JsonProperty("nodeDescription")
    public String getNodeDescription() { return nodeDescription; }
    @JsonProperty("nodeDescription")
    public void setNodeDescription(String value) { this.nodeDescription = value; }

    /**
     * Unique node identification
     */
    @JsonProperty("nodeId")
    public String getNodeID() { return nodeID; }
    @JsonProperty("nodeId")
    public void setNodeID(String value) { this.nodeID = value; }

    /**
     * Node position. The object is defined in chapter 6.6. Optional: master control has this
     * information. Can be sent additionally, e.g. for debugging purposes.
     */
    @JsonProperty("nodePosition")
    public NodePosition getNodePosition() { return nodePosition; }
    @JsonProperty("nodePosition")
    public void setNodePosition(NodePosition value) { this.nodePosition = value; }

    /**
     * True: indicates that the node is part of the base. False: indicates that the node is part
     * of the horizon.
     */
    @JsonProperty("released")
    public boolean getReleased() { return released; }
    @JsonProperty("released")
    public void setReleased(boolean value) { this.released = value; }

    /**
     * sequenceId of the node.
     */
    @JsonProperty("sequenceId")
    public long getSequenceID() { return sequenceID; }
    @JsonProperty("sequenceId")
    public void setSequenceID(long value) { this.sequenceID = value; }
}
