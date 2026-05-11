package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Definition of boundaries in which a vehicle can deviate from its trajectory, e. g. to
 * avoid obstacles.
 */
public class Corridor {
    private CorridorRefPoint corridorRefPoint;
    private double leftWidth;
    private double rightWidth;

    /**
     * Defines whether the boundaries are valid for the kinematic center or the contour of the
     * vehicle.
     */
    @JsonProperty("corridorRefPoint")
    public CorridorRefPoint getCorridorRefPoint() { return corridorRefPoint; }
    @JsonProperty("corridorRefPoint")
    public void setCorridorRefPoint(CorridorRefPoint value) { this.corridorRefPoint = value; }

    /**
     * Defines the width of the corridor in meters to the left related to the trajectory of the
     * vehicle.
     */
    @JsonProperty("leftWidth")
    public double getLeftWidth() { return leftWidth; }
    @JsonProperty("leftWidth")
    public void setLeftWidth(double value) { this.leftWidth = value; }

    /**
     * Defines the width of the corridor in meters to the right related to the trajectory of the
     * vehicle.
     */
    @JsonProperty("rightWidth")
    public double getRightWidth() { return rightWidth; }
    @JsonProperty("rightWidth")
    public void setRightWidth(double value) { this.rightWidth = value; }
}
