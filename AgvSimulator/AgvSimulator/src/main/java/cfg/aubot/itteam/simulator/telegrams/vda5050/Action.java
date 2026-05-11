package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

/**
 * Instant Action Object
 *
 * Edge Action Object
 *
 * Node Action Object
 */
public class Action {
    private String actionDescription;
    private String actionID;
    private ActionActionParameter[] actionParameters;
    private String actionType;
    private BlockingType blockingType;

    /**
     * Additional information on the action.
     */
    @JsonProperty("actionDescription")
    public String getActionDescription() { return actionDescription; }
    @JsonProperty("actionDescription")
    public void setActionDescription(String value) { this.actionDescription = value; }

    /**
     * ID to distinguish between multiple actions, either instant or with the same type on the
     * same node/edge.
     */
    @JsonProperty("actionId")
    public String getActionID() { return actionID; }
    @JsonProperty("actionId")
    public void setActionID(String value) { this.actionID = value; }

    /**
     * Array of actionParameter objects for the indicated action e.g. deviceId, loadId, external
     * triggers.
     */
    @JsonProperty("actionParameters")
    public ActionActionParameter[] getActionParameters() { return actionParameters; }
    @JsonProperty("actionParameters")
    public void setActionParameters(ActionActionParameter[] value) { this.actionParameters = value; }

    /**
     * Name of action as described in the first column of "Actions and Parameters"
     * Identifies the function of the action.
     */
    @JsonProperty("actionType")
    public String getActionType() { return actionType; }
    @JsonProperty("actionType")
    public void setActionType(String value) { this.actionType = value; }

    /**
     * Regulates if the action is allowed to be executed during movement and/or parallel to
     * other actions.
     * NONE: action can happen in parallel with others, including movement.
     * SOFT: action can happen simultaneously with others, but not while moving.
     * HARD: no other actions can be performed while this action is running.
     */
    @JsonProperty("blockingType")
    public BlockingType getBlockingType() { return blockingType; }
    @JsonProperty("blockingType")
    public void setBlockingType(BlockingType value) { this.blockingType = value; }
}
