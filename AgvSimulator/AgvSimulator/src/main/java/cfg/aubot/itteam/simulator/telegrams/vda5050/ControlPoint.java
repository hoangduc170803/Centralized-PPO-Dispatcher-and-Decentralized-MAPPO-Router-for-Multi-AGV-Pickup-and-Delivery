package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class ControlPoint {
    private double x;
    private double y;

    /**
     * X coordinate described in the world coordinate system.
     */
    @JsonProperty("x")
    public double getX() { return x; }
    @JsonProperty("x")
    public void setX(double value) { this.x = value; }

    /**
     * Y coordinate described in the world coordinate system.
     */
    @JsonProperty("y")
    public double getY() { return y; }
    @JsonProperty("y")
    public void setY(double value) { this.y = value; }
}
