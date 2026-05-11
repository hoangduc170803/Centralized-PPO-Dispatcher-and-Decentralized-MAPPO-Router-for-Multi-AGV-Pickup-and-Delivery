package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class EdgeState {
    private String edgeDescription;
    private String edgeID;
    private boolean released;
    private long sequenceID;
    private Trajectory trajectory;

    public EdgeState(Edge edge) {
        this.edgeID = edge.getEdgeID();
        this.released = edge.getReleased();
        this.sequenceID = edge.getSequenceID();
    }

    /**
     * Verbose Edge description
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
     * True: Edge is part of base. False: Edge is part of horizon.
     */
    @JsonProperty("released")
    public boolean getReleased() { return released; }
    @JsonProperty("released")
    public void setReleased(boolean value) { this.released = value; }

    /**
     * sequenceId of the edge.
     */
    @JsonProperty("sequenceId")
    public long getSequenceID() { return sequenceID; }
    @JsonProperty("sequenceId")
    public void setSequenceID(long value) { this.sequenceID = value; }

    /**
     * The trajectory is to be communicated as a NURBS and is defined in chapter 6.4.
     * Trajectory segments are from the point where the AGV starts to enter the edge until the
     * point where it reports that the next node was traversed.
     */
    @JsonProperty("trajectory")
    public Trajectory getTrajectory() { return trajectory; }
    @JsonProperty("trajectory")
    public void setTrajectory(Trajectory value) { this.trajectory = value; }
}
