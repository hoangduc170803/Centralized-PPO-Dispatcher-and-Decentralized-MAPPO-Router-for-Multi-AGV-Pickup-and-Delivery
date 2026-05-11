package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Contains all battery-related information.
 */
public class BatteryState {
    private double batteryCharge;
    private Double batteryHealth;
    private Double batteryVoltage;
    private boolean charging;
    private Double reach;

    public BatteryState(double batteryCharge, boolean charging) {
        this.batteryCharge = batteryCharge;
        this.charging = charging;
        this.batteryVoltage = 26.0;
    }

    /**
     * State of Charge in percent as a float value:
     * If AGV only provides values for good or bad battery levels, these will be indicated as
     * 20% (bad) and 80% (good).
     */
    @JsonProperty("batteryCharge")
    public double getBatteryCharge() { return batteryCharge; }
    @JsonProperty("batteryCharge")
    public void setBatteryCharge(double value) { this.batteryCharge = value; }

    /**
     * State of health in percent as an integer within range [0..100]
     */
    @JsonProperty("batteryHealth")
    public Double getBatteryHealth() { return batteryHealth; }
    @JsonProperty("batteryHealth")
    public void setBatteryHealth(Double value) { this.batteryHealth = value; }

    /**
     * Battery voltage
     */
    @JsonProperty("batteryVoltage")
    public Double getBatteryVoltage() { return batteryVoltage; }
    @JsonProperty("batteryVoltage")
    public void setBatteryVoltage(Double value) { this.batteryVoltage = value; }

    /**
     * If true: Charging in progress. If false: AGV is currently not charging.
     */
    @JsonProperty("charging")
    public boolean getCharging() { return charging; }
    @JsonProperty("charging")
    public void setCharging(boolean value) { this.charging = value; }

    /**
     * Estimated reach with current State of Charge (in meter as uint32)
     */
    @JsonProperty("reach")
    public Double getReach() { return reach; }
    @JsonProperty("reach")
    public void setReach(Double value) { this.reach = value; }
    
  /**
   * Decrease battery voltage by specified amount
   */
  public void decreaseVoltage(double amount) {
    // Disabled voltage decrease for simulator testing.
    // if (this.batteryVoltage != null) {
    //   this.batteryVoltage -= amount;
    //   // Prevent voltage from going below 0
    //   if (this.batteryVoltage < 0) {
    //     this.batteryVoltage = 0.0;
    //   }
    // }
  }
    
    /**
     * Increase battery voltage by specified amount
     */
    public void increaseVoltage(double amount) {
        if (this.batteryVoltage != null) {
            this.batteryVoltage += amount;
            // Prevent voltage from going above reasonable limit (e.g., 30V)
            if (this.batteryVoltage > 30.0) {
                this.batteryVoltage = 30.0;
            }
        }
    }
}
