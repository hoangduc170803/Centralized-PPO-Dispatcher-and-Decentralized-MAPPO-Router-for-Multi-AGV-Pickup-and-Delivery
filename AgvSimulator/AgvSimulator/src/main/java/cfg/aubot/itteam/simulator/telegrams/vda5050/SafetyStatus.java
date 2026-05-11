package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Object that holds information about the safety status
 */
public class SafetyStatus {
    private EStop eStop;
    private boolean fieldViolation;

    public SafetyStatus(EStop eStop, boolean fieldViolation) {
//        this.eStop = EStop.MANUAL;  // estop for testing
        this.eStop = eStop;
        this.fieldViolation = fieldViolation;
    }

    /**
     * Acknowledge type of eStop.
     * AUTOACK: auto-acknowledgeable e-stop is activated e.g. by bumper or protective field.
     * MANUAL: e-stop has to be acknowledged manually at the vehicle.
     * REMOTE: facility e-stop has to be acknowledged remotely.
     * NONE: no e-stop activated.
     */
    @JsonProperty("eStop")
    public EStop getEStop() { return eStop; }
    @JsonProperty("eStop")
    public void setEStop(EStop value) { this.eStop = value; }

    /**
     * Protective field violation. true: field is violated. false: field is not violated.
     */
    @JsonProperty("fieldViolation")
    public boolean getFieldViolation() { return fieldViolation; }
    @JsonProperty("fieldViolation")
    public void setFieldViolation(boolean value) { this.fieldViolation = value; }
}
