package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

public class AgvAction {
    private String actionDescription;
    private AgvActionActionParameter[] actionParameters;
    private ActionScope[] actionScopes;
    private String actionType;
    private String resultDescription;

    /**
     * free text: description of the action
     */
    @JsonProperty("actionDescription")
    public String getActionDescription() { return actionDescription; }
    @JsonProperty("actionDescription")
    public void setActionDescription(String value) { this.actionDescription = value; }

    /**
     * list of parameters. if not defined, the action has no parameters
     */
    @JsonProperty("actionParameters")
    public AgvActionActionParameter[] getActionParameters() { return actionParameters; }
    @JsonProperty("actionParameters")
    public void setActionParameters(AgvActionActionParameter[] value) { this.actionParameters = value; }

    /**
     * list of allowed scopes for using this action-type. INSTANT: usable as instantAction,
     * NODE: usable on nodes, EDGE: usable on edges.
     */
    @JsonProperty("actionScopes")
    public ActionScope[] getActionScopes() { return actionScopes; }
    @JsonProperty("actionScopes")
    public void setActionScopes(ActionScope[] value) { this.actionScopes = value; }

    /**
     * unique actionType corresponding to action.actionType
     */
    @JsonProperty("actionType")
    public String getActionType() { return actionType; }
    @JsonProperty("actionType")
    public void setActionType(String value) { this.actionType = value; }

    /**
     * free text: description of the resultDescription
     */
    @JsonProperty("resultDescription")
    public String getResultDescription() { return resultDescription; }
    @JsonProperty("resultDescription")
    public void setResultDescription(String value) { this.resultDescription = value; }
}
