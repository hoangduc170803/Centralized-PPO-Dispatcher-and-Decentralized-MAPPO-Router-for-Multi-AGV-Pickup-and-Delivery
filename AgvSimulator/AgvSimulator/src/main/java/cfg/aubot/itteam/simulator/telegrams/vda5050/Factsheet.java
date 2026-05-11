package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * The factsheet provides basic information about a specific AGV type series. This
 * information allows comparison of different AGV types and can be applied for the planning,
 * dimensioning and simulation of an AGV system. The factsheet also includes information
 * about AGV communication interfaces which are required for the integration of an AGV type
 * series into a VD[M]A-5050-compliant master control.
 */
public class Factsheet {
    private long headerID;
    private String manufacturer;
    private String serialNumber;
    private String timestamp;
    private String version;
    private AgvGeometry agvGeometry;
    private LoadSpecification loadSpecification;
    private PhysicalParameters physicalParameters;
    private ProtocolFeatures protocolFeatures;
    private ProtocolLimits protocolLimits;
    private TypeSpecification typeSpecification;
    private VehicleConfig vehicleConfig;

    /**
     * headerId of the message. The headerId is defined per topic and incremented by 1 with each
     * sent (but not necessarily received) message.
     */
    @JsonProperty("headerId")
    public long getHeaderID() { return headerID; }
    @JsonProperty("headerId")
    public void setHeaderID(long value) { this.headerID = value; }

    /**
     * Manufacturer of the AGV
     */
    @JsonProperty("manufacturer")
    public String getManufacturer() { return manufacturer; }
    @JsonProperty("manufacturer")
    public void setManufacturer(String value) { this.manufacturer = value; }

    /**
     * Serial number of the AGV
     */
    @JsonProperty("serialNumber")
    public String getSerialNumber() { return serialNumber; }
    @JsonProperty("serialNumber")
    public void setSerialNumber(String value) { this.serialNumber = value; }

    /**
     * Timestamp (ISO8601, UTC); YYYY-MM-DDTHH:mm:ss.ssZ; e.g. 2017-04-15T11:40:03.12Z
     */
    @JsonProperty("timestamp")
    public String getTimestamp() { return timestamp; }
    @JsonProperty("timestamp")
    public void setTimestamp(String value) { this.timestamp = value; }

    /**
     * Version of the protocol [Major].[Minor].[Patch], e.g. 1.3.2
     */
    @JsonProperty("version")
    public String getVersion() { return version; }
    @JsonProperty("version")
    public void setVersion(String value) { this.version = value; }

    /**
     * Detailed definition of AGV geometry
     */
    @JsonProperty("agvGeometry")
    public AgvGeometry getAgvGeometry() { return agvGeometry; }
    @JsonProperty("agvGeometry")
    public void setAgvGeometry(AgvGeometry value) { this.agvGeometry = value; }

    /**
     * Abstract specification of load capabilities
     */
    @JsonProperty("loadSpecification")
    public LoadSpecification getLoadSpecification() { return loadSpecification; }
    @JsonProperty("loadSpecification")
    public void setLoadSpecification(LoadSpecification value) { this.loadSpecification = value; }

    /**
     * These parameters specify the basic physical properties of the AGV
     */
    @JsonProperty("physicalParameters")
    public PhysicalParameters getPhysicalParameters() { return physicalParameters; }
    @JsonProperty("physicalParameters")
    public void setPhysicalParameters(PhysicalParameters value) { this.physicalParameters = value; }

    /**
     * Supported features of VDA5050 protocol
     */
    @JsonProperty("protocolFeatures")
    public ProtocolFeatures getProtocolFeatures() { return protocolFeatures; }
    @JsonProperty("protocolFeatures")
    public void setProtocolFeatures(ProtocolFeatures value) { this.protocolFeatures = value; }

    /**
     * This JSON-object describes the protocol limitations of the AGV. If a parameter is not
     * defined or set to zero then there is no explicit limit for this parameter.
     */
    @JsonProperty("protocolLimits")
    public ProtocolLimits getProtocolLimits() { return protocolLimits; }
    @JsonProperty("protocolLimits")
    public void setProtocolLimits(ProtocolLimits value) { this.protocolLimits = value; }

    /**
     * These parameters generally specify the class and the capabilities of the AGV
     */
    @JsonProperty("typeSpecification")
    public TypeSpecification getTypeSpecification() { return typeSpecification; }
    @JsonProperty("typeSpecification")
    public void setTypeSpecification(TypeSpecification value) { this.typeSpecification = value; }

    @JsonProperty("vehicleConfig")
    public VehicleConfig getVehicleConfig() { return vehicleConfig; }
    @JsonProperty("vehicleConfig")
    public void setVehicleConfig(VehicleConfig value) { this.vehicleConfig = value; }
}
