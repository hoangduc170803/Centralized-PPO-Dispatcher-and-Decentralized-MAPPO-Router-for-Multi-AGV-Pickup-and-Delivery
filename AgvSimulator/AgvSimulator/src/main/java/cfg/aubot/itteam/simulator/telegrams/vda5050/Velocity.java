package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * The AGVs velocity in vehicle coordinates.
 */
public class Velocity {
    private Double omega;
    private Double vx;
    private Double vy;

    public Velocity(Double vx) {
        this.vx = 10.0;
    }

    /**
     * The AGVs turning speed around its z axis.
     */
    @JsonProperty("omega")
    public Double getOmega() { return omega; }
    @JsonProperty("omega")
    public void setOmega(Double value) { this.omega = value; }

    /**
     * The AGVs velocity in its x direction.
     */
    @JsonProperty("vx")
    public Double getVx() { return vx; }
    @JsonProperty("vx")
    public void setVx(Double value) { this.vx = value; }

    /**
     * The AGVs velocity in its y direction.
     */
    @JsonProperty("vy")
    public Double getVy() { return vy; }
    @JsonProperty("vy")
    public void setVy(Double value) { this.vy = value; }
}
