package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class Position {
    private Double theta;
    private double x;
    private double y;

    /**
     * orientation of wheel in AGV-coordinate system Necessary for fixed wheels
     */
    @JsonProperty("theta")
    public Double getTheta() { return theta; }
    @JsonProperty("theta")
    public void setTheta(Double value) { this.theta = value; }

    /**
     * [m] x-position in AGV-coordinate system
     */
    @JsonProperty("x")
    public double getX() { return x; }
    @JsonProperty("x")
    public void setX(double value) { this.x = value; }

    /**
     * y-position in AGV-coordinate system
     */
    @JsonProperty("y")
    public double getY() { return y; }
    @JsonProperty("y")
    public void setY(double value) { this.y = value; }
}
