package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * This point describes the loads position on the AGV in the vehicle coordinates. The
 * boundingBoxReference point is in the middle of the footprint of the load, so length/2 and
 * width/2.
 */
public class LoadBoundingBoxReference {
    private Double theta;
    private double x;
    private double y;
    private double z;

    /**
     * Orientation of the loads bounding box. Important for tugger trains etc.
     */
    @JsonProperty("theta")
    public Double getTheta() { return theta; }
    @JsonProperty("theta")
    public void setTheta(Double value) { this.theta = value; }

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
