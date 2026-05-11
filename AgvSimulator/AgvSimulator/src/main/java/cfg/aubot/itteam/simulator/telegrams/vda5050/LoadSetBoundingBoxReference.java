package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * bounding box reference as defined in parameter loads[] in state-message
 */
public class LoadSetBoundingBoxReference {
    private Long theta;
    private double x;
    private double y;
    private double z;

    /**
     * Orientation of the loads bounding box. Important for tugger trains, etc.
     */
    @JsonProperty("theta")
    public Long getTheta() { return theta; }
    @JsonProperty("theta")
    public void setTheta(Long value) { this.theta = value; }

    /**
     * x-coordinate of the point of reference.
     */
    @JsonProperty("x")
    public double getX() { return x; }
    @JsonProperty("x")
    public void setX(double value) { this.x = value; }

    /**
     * y-coordinate of the point of reference.
     */
    @JsonProperty("y")
    public double getY() { return y; }
    @JsonProperty("y")
    public void setY(double value) { this.y = value; }

    /**
     * z-coordinate of the point of reference.
     */
    @JsonProperty("z")
    public double getZ() { return z; }
    @JsonProperty("z")
    public void setZ(double value) { this.z = value; }
}
