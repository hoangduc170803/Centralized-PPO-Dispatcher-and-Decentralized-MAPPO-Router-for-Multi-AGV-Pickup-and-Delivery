package cfg.aubot.itteam.simulator.telegrams.predefined;

import com.google.gson.annotations.SerializedName;

public abstract class RequestMessage extends MqttTelegram {

  @SerializedName("orderId")
  protected long requestId;

  public RequestMessage(String thingName, String type, long requestId) {
    super(thingName, type);
    this.requestId = requestId;
  }

  public long getRequestId() {
    return requestId;
  }
}
