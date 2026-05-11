package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

public class Edge {
    private Action[] actions;
    private Corridor corridor;
    private String direction;
    private String edgeDescription;
    private String edgeID;
    private String endNodeID;
    private Double length;
    private Double maxHeight;
    private Double maxRotationSpeed;
    private Double maxSpeed;
    private Double minHeight;
    private Double orientation;
    private OrientationType orientationType;
    private boolean released;
    private Boolean rotationAllowed;
    private long sequenceID;
    private String startNodeID;
    private Trajectory trajectory;

    /**
     * Array of action objects with detailed information.
     */
    @JsonProperty("actions")
    public Action[] getActions() { return actions; }
    @JsonProperty("actions")
    public void setActions(Action[] value) { this.actions = value; }

    /**
     * Definition of boundaries in which a vehicle can deviate from its trajectory, e. g. to
     * avoid obstacles.
     */
    @JsonProperty("corridor")
    public Corridor getCorridor() { return corridor; }
    @JsonProperty("corridor")
    public void setCorridor(Corridor value) { this.corridor = value; }

    /**
     * Sets direction at junctions for line-guided vehicles, to be defined initially
     * (vehicle-individual). Can be descriptive (left, right, middle, straight) or a frequency
     * ("433MHz").
     */
    @JsonProperty("direction")
    public String getDirection() { return direction; }
    @JsonProperty("direction")
    public void setDirection(String value) { this.direction = value; }

    /**
     * Verbose description of the edge.
     */
    @JsonProperty("edgeDescription")
    public String getEdgeDescription() { return edgeDescription; }
    @JsonProperty("edgeDescription")
    public void setEdgeDescription(String value) { this.edgeDescription = value; }

    /**
     * Unique edge identification
     */
    @JsonProperty("edgeId")
    public String getEdgeID() { return edgeID; }
    @JsonProperty("edgeId")
    public void setEdgeID(String value) { this.edgeID = value; }

    /**
     * The nodeId of the end node.
     */
    @JsonProperty("endNodeId")
    public String getEndNodeID() { return endNodeID; }
    @JsonProperty("endNodeId")
    public void setEndNodeID(String value) { this.endNodeID = value; }

    /**
     * Distance of the path from startNode to endNode in meters.
     * Optional: This value is used by line-guided AGVs to decrease their speed before reaching
     * a stop position.
     */
    @JsonProperty("length")
    public Double getLength() { return length; }
    @JsonProperty("length")
    public void setLength(Double value) { this.length = value; }

    /**
     * Permitted maximum height of the vehicle, including the load, on edge. In meters.
     */
    @JsonProperty("maxHeight")
    public Double getMaxHeight() { return maxHeight; }
    @JsonProperty("maxHeight")
    public void setMaxHeight(Double value) { this.maxHeight = value; }

    /**
     * Maximum rotation speed in rad/s
     */
    @JsonProperty("maxRotationSpeed")
    public Double getMaxRotationSpeed() { return maxRotationSpeed; }
    @JsonProperty("maxRotationSpeed")
    public void setMaxRotationSpeed(Double value) { this.maxRotationSpeed = value; }

    /**
     * permitted maximum speed of the agv on the edge in m/s. Speed is defined by the fastest
     * point of the vehicle.
     */
    @JsonProperty("maxSpeed")
    public Double getMaxSpeed() { return maxSpeed; }
    @JsonProperty("maxSpeed")
    public void setMaxSpeed(Double value) { this.maxSpeed = value; }

    /**
     * Permitted minimal height of the edge measured at the bottom of the load. In meters.
     */
    @JsonProperty("minHeight")
    public Double getMinHeight() { return minHeight; }
    @JsonProperty("minHeight")
    public void setMinHeight(Double value) { this.minHeight = value; }

    /**
     * Orientation of the AGV on the edge relative to the map coordinate origin (for holonomic
     * vehicles with more than one driving direction).
     * Example: orientation Pi/2 rad will lead to a rotation of 90 degrees.
     * If AGV starts in different orientation, rotate the vehicle on the edge to the desired
     * orientation if rotationAllowed is set to "true".
     * If rotationAllowed is "false", rotate before entering the edge.
     * If that is not possible, reject the order.
     * If a trajectory with orientation is defined, follow the trajectories orientation. If a
     * trajectory without orientation and the orientation field here is defined, apply the
     * orientation to the tangent of the trajectory.
     */
    @JsonProperty("orientation")
    public Double getOrientation() { return orientation; }
    @JsonProperty("orientation")
    public void setOrientation(Double value) { this.orientation = value; }

    /**
     * Enum {GLOBAL, TANGENTIAL}:
     * "GLOBAL"- relative to the global project specific map coordinate system;
     * "TANGENTIAL"- tangential to the edge.
     * If not defined, the default value is "TANGENTIAL".
     */
    @JsonProperty("orientationType")
    public OrientationType getOrientationType() { return orientationType; }
    @JsonProperty("orientationType")
    public void setOrientationType(OrientationType value) { this.orientationType = value; }

    /**
     * If true, the edge is part of the base plan. If false, the edge is part of the horizon
     * plan.
     */
    @JsonProperty("released")
    public boolean getReleased() { return released; }
    @JsonProperty("released")
    public void setReleased(boolean value) { this.released = value; }

    /**
     * If true, rotation is allowed on the edge.
     */
    @JsonProperty("rotationAllowed")
    public Boolean getRotationAllowed() { return rotationAllowed; }
    @JsonProperty("rotationAllowed")
    public void setRotationAllowed(Boolean value) { this.rotationAllowed = value; }

    /**
     * Id to track the sequence of nodes and edges in an order and to simplify order updates.
     * The variable sequenceId runs across all nodes and edges of the same order and is reset
     * when a new orderId is issued.
     */
    @JsonProperty("sequenceId")
    public long getSequenceID() { return sequenceID; }
    @JsonProperty("sequenceId")
    public void setSequenceID(long value) { this.sequenceID = value; }

    /**
     * The nodeId of the start node.
     */
    @JsonProperty("startNodeId")
    public String getStartNodeID() { return startNodeID; }
    @JsonProperty("startNodeId")
    public void setStartNodeID(String value) { this.startNodeID = value; }

    /**
     * Trajectory JSON-object for this edge as a NURBS. Defines the curve on which the AGV
     * should move between startNode and endNode.
     * Optional: Can be omitted if AGV cannot process trajectories or if AGV plans its own
     * trajectory.
     */
    @JsonProperty("trajectory")
    public Trajectory getTrajectory() { return trajectory; }
    @JsonProperty("trajectory")
    public void setTrajectory(Trajectory value) { this.trajectory = value; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Edge edge)) return false;
      return sequenceID == edge.sequenceID && Objects.equals(edgeID, edge.edgeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edgeID, sequenceID);
    }
}
