package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;
import lombok.Builder;

/**
 * An error object.
 */
@Builder
public class Error {
    private String errorDescription;
    private String errorHint;
    private ErrorLevel errorLevel;
    private ErrorReference[] errorReferences;
    private String errorType;

    /**
     * Verbose description of error.
     */
    @JsonProperty("errorDescription")
    public String getErrorDescription() { return errorDescription; }
    @JsonProperty("errorDescription")
    public void setErrorDescription(String value) { this.errorDescription = value; }

    /**
     * Hint on how to approach or solve the reported error.
     */
    @JsonProperty("errorHint")
    public String getErrorHint() { return errorHint; }
    @JsonProperty("errorHint")
    public void setErrorHint(String value) { this.errorHint = value; }

    /**
     * Error level.
     * WARNING: AGV is ready to start (e.g. maintenance cycle expiration warning).
     * FATAL: AGV is not in running condition, user intervention required (e.g. laser scanner is
     * contaminated).
     */
    @JsonProperty("errorLevel")
    public ErrorLevel getErrorLevel() { return errorLevel; }
    @JsonProperty("errorLevel")
    public void setErrorLevel(ErrorLevel value) { this.errorLevel = value; }

    /**
     * Array of references to identify the source of the error (e. g. headerId, orderId,
     * actionId, ...).
     * For additional information see "Best Practice" chapter 7.
     */
    @JsonProperty("errorReferences")
    public ErrorReference[] getErrorReferences() { return errorReferences; }
    @JsonProperty("errorReferences")
    public void setErrorReferences(ErrorReference[] value) { this.errorReferences = value; }

    /**
     * Type / name of error.
     */
    @JsonProperty("errorType")
    public String getErrorType() { return errorType; }
    @JsonProperty("errorType")
    public void setErrorType(String value) { this.errorType = value; }
}
