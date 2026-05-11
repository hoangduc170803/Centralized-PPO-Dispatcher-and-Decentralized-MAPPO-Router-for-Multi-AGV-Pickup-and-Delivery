package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;
import java.util.Map;

public class Envelopes3D {
    private Map<String, Object> data;
    private Long description;
    private String format;
    private String set;
    private String url;

    /**
     * 3D-envelope curve data, format specified in ‚format‘
     */
    @JsonProperty("data")
    public Map<String, Object> getData() { return data; }
    @JsonProperty("data")
    public void setData(Map<String, Object> value) { this.data = value; }

    /**
     * free text: description of envelope curve set
     */
    @JsonProperty("description")
    public Long getDescription() { return description; }
    @JsonProperty("description")
    public void setDescription(Long value) { this.description = value; }

    /**
     * format of data e.g. DXF
     */
    @JsonProperty("format")
    public String getFormat() { return format; }
    @JsonProperty("format")
    public void setFormat(String value) { this.format = value; }

    /**
     * name of the envelope curve set
     */
    @JsonProperty("set")
    public String getSet() { return set; }
    @JsonProperty("set")
    public void setSet(String value) { this.set = value; }

    /**
     * protocol and url-definition for downloading the 3D-envelope curve data e.g.
     * ftp://xxx.yyy.com/ac4dgvhoif5tghji
     */
    @JsonProperty("url")
    public String getURL() { return url; }
    @JsonProperty("url")
    public void setURL(String value) { this.url = value; }
}
