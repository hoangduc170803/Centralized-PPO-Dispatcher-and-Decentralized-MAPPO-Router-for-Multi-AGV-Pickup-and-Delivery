package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * These parameters generally specify the class and the capabilities of the AGV
 */
public class TypeSpecification {
    private AgvClass agvClass;
    private AgvKinematic agvKinematic;
    private LocalizationType[] localizationTypes;
    private double maxLoadMass;
    private NavigationType[] navigationTypes;
    private String seriesDescription;
    private String seriesName;

    /**
     * Simplified description of AGV class.
     */
    @JsonProperty("agvClass")
    public AgvClass getAgvClass() { return agvClass; }
    @JsonProperty("agvClass")
    public void setAgvClass(AgvClass value) { this.agvClass = value; }

    /**
     * simplified description of AGV kinematics-type.
     */
    @JsonProperty("agvKinematic")
    public AgvKinematic getAgvKinematic() { return agvKinematic; }
    @JsonProperty("agvKinematic")
    public void setAgvKinematic(AgvKinematic value) { this.agvKinematic = value; }

    /**
     * simplified description of localization type
     */
    @JsonProperty("localizationTypes")
    public LocalizationType[] getLocalizationTypes() { return localizationTypes; }
    @JsonProperty("localizationTypes")
    public void setLocalizationTypes(LocalizationType[] value) { this.localizationTypes = value; }

    /**
     * maximum loadable mass
     */
    @JsonProperty("maxLoadMass")
    public double getMaxLoadMass() { return maxLoadMass; }
    @JsonProperty("maxLoadMass")
    public void setMaxLoadMass(double value) { this.maxLoadMass = value; }

    /**
     * List of path planning types supported by the AGV, sorted by priority
     */
    @JsonProperty("navigationTypes")
    public NavigationType[] getNavigationTypes() { return navigationTypes; }
    @JsonProperty("navigationTypes")
    public void setNavigationTypes(NavigationType[] value) { this.navigationTypes = value; }

    /**
     * Free text human readable description of the AGV type series
     */
    @JsonProperty("seriesDescription")
    public String getSeriesDescription() { return seriesDescription; }
    @JsonProperty("seriesDescription")
    public void setSeriesDescription(String value) { this.seriesDescription = value; }

    /**
     * Free text generalized series name as specified by manufacturer
     */
    @JsonProperty("seriesName")
    public String getSeriesName() { return seriesName; }
    @JsonProperty("seriesName")
    public void setSeriesName(String value) { this.seriesName = value; }
}
