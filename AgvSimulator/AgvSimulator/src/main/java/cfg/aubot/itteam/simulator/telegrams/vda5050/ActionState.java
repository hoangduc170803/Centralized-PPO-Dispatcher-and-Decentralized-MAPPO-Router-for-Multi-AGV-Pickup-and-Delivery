package cfg.aubot.itteam.simulator.telegrams.vda5050;

import com.fasterxml.jackson.annotation.*;

import java.util.UUID;

public class ActionState {
    private String actionDescription;
    private String actionID;
    private ActionStatus actionStatus;
    private String actionType;
    private String resultDescription;

    public ActionState(Action action) {
        this.actionID = action.getActionID();
        this.actionType = action.getActionType();
        this.actionStatus = ActionStatus.WAITING;
    }

    /**
     * Additional information on the action.
     */
    @JsonProperty("actionDescription")
    public String getActionDescription() { return actionDescription; }
    @JsonProperty("actionDescription")
    public void setActionDescription(String value) { this.actionDescription = value; }

    /**
     * Unique actionId, e.g. blink_123jdaimoim234
     */
    @JsonProperty("actionId")
    public String getActionID() { return actionID; }
    @JsonProperty("actionId")
    public void setActionID(String value) { this.actionID = value; }

    /**
     * Action status.
     * WAITING: Action was received by AGV but the node where it triggers was not yet reached or
     * the edge where it is active was not yet entered.
     * INITIALIZING: Action was triggered, preparatory measures are initiated.
     * RUNNING: The action is running.
     * PAUSED: The action is paused because of a pause instantAction or external trigger (pause
     * button on AGV).
     * FINISHED: The action is finished. A result is reported via the resultDescription.
     * FAILED: Action could not be finished for whatever reason.
     */
    @JsonProperty("actionStatus")
    public ActionStatus getActionStatus() { return actionStatus; }
    @JsonProperty("actionStatus")
    public void setActionStatus(ActionStatus value) { this.actionStatus = value; }

    /**
     * actionType of the action.
     * Optional: Only for informational or visualization purposes. Order knows the type.
     */
    @JsonProperty("actionType")
    public String getActionType() { return actionType; }
    @JsonProperty("actionType")
    public void setActionType(String value) { this.actionType = value; }

    /**
     * Description of the result, e.g. the result of a rfid-read.
     */
    @JsonProperty("resultDescription")
    public String getResultDescription() { return resultDescription; }
    @JsonProperty("resultDescription")
    public void setResultDescription(String value) { this.resultDescription = value; }
}
