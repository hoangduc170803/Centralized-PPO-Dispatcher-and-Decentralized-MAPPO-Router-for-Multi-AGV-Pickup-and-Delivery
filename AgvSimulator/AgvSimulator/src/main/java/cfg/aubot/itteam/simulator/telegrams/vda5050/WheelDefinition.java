package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class WheelDefinition {
    private Double centerDisplacement;
    private String constraints;
    private double diameter;
    private boolean isActiveDriven;
    private boolean isActiveSteered;
    private Position position;
    private Type type;
    private double width;

    /**
     * nominal displacement of the wheel’s center to the rotation point (necessary for caster
     * wheels). If the parameter is not defined, it is assumed to be 0
     */
    @JsonProperty("centerDisplacement")
    public Double getCenterDisplacement() { return centerDisplacement; }
    @JsonProperty("centerDisplacement")
    public void setCenterDisplacement(Double value) { this.centerDisplacement = value; }

    /**
     * free text: can be used by the manufacturer to define constraints
     */
    @JsonProperty("constraints")
    public String getConstraints() { return constraints; }
    @JsonProperty("constraints")
    public void setConstraints(String value) { this.constraints = value; }

    /**
     * nominal diameter of wheel
     */
    @JsonProperty("diameter")
    public double getDiameter() { return diameter; }
    @JsonProperty("diameter")
    public void setDiameter(double value) { this.diameter = value; }

    /**
     * True: wheel is actively driven (de: angetrieben)
     */
    @JsonProperty("isActiveDriven")
    public boolean getIsActiveDriven() { return isActiveDriven; }
    @JsonProperty("isActiveDriven")
    public void setIsActiveDriven(boolean value) { this.isActiveDriven = value; }

    /**
     * True: wheel is actively steered (de: aktiv gelenkt)
     */
    @JsonProperty("isActiveSteered")
    public boolean getIsActiveSteered() { return isActiveSteered; }
    @JsonProperty("isActiveSteered")
    public void setIsActiveSteered(boolean value) { this.isActiveSteered = value; }

    @JsonProperty("position")
    public Position getPosition() { return position; }
    @JsonProperty("position")
    public void setPosition(Position value) { this.position = value; }

    /**
     * wheel type. DRIVE, CASTER, FIXED, MECANUM
     */
    @JsonProperty("type")
    public Type getType() { return type; }
    @JsonProperty("type")
    public void setType(Type value) { this.type = value; }

    /**
     * nominal width of wheel
     */
    @JsonProperty("width")
    public double getWidth() { return width; }
    @JsonProperty("width")
    public void setWidth(double value) { this.width = value; }
}
