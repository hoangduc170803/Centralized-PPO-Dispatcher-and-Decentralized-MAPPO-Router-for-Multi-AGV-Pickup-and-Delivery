package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

public class Node {
    private Action[] actions;
    private String nodeDescription;
    private String nodeID;
    private NodePosition nodePosition;
    private boolean released;
    private long sequenceID;

    /**
     * Array of actions that are to be executed on the node. Their sequence in the list governs
     * their sequence of execution.
     */
    @JsonProperty("actions")
    public Action[] getActions() { return actions; }
    @JsonProperty("actions")
    public void setActions(Action[] value) { this.actions = value; }

    /**
     * Verbose Node Description.
     */
    @JsonProperty("nodeDescription")
    public String getNodeDescription() { return nodeDescription; }
    @JsonProperty("nodeDescription")
    public void setNodeDescription(String value) { this.nodeDescription = value; }

    /**
     * Unique node identification. For example: pumpenhaus_1, MONTAGE
     */
    @JsonProperty("nodeId")
    public String getNodeID() { return nodeID; }
    @JsonProperty("nodeId")
    public void setNodeID(String value) { this.nodeID = value; }

    /**
     * Defines the position on a map in world coordinates. Each floor has its own map. Precision
     * is up to the specific implementation.
     */
    @JsonProperty("nodePosition")
    public NodePosition getNodePosition() { return nodePosition; }
    @JsonProperty("nodePosition")
    public void setNodePosition(NodePosition value) { this.nodePosition = value; }

    /**
     * If true, the node is part of the base plan. If false, the node is part of the horizon
     * plan.
     */
    @JsonProperty("released")
    public boolean getReleased() { return released; }
    @JsonProperty("released")
    public void setReleased(boolean value) { this.released = value; }

    /**
     * Id to track the sequence of nodes and edges in an order and to simplify order updates.
     * The main purpose is to distinguish between a node which is passed more than once within
     * one orderId. The variable sequenceId can run across all nodes and edges of the same order
     * and is reset when a new orderId is issued.
     */
    @JsonProperty("sequenceId")
    public long getSequenceID() { return sequenceID; }
    @JsonProperty("sequenceId")
    public void setSequenceID(long value) { this.sequenceID = value; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node node)) return false;
      return sequenceID == node.sequenceID && Objects.equals(nodeID, node.nodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeID, sequenceID);
    }
}
