package cfg.aubot.itteam.simulator;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class VirtualAgv extends Thread {
  protected String position = "0005";
  protected String nextPosition = "0000";
  protected char operationState = 'I';
  protected char previousStateBeforeError;
  protected char loadState = 'E';
  protected int energyLevel = 99;
  protected double current = 5.425f;  //(dòng điện i=5.5V)
  protected double voltage = 12.732f;  //(điện áp = 12.7W)
  protected long PROCESS_TIME = 5000;
  protected double distance = 0;
  protected boolean isLoop = false;
  protected WorkingRoutes routes = new WorkingRoutes();
  protected WorkingRoutes tempRoutes = new WorkingRoutes();
  protected PositionSubscriber positionSubscriber;
  protected Map<VirtualAgv, String> sharedPositionMap;
  private static final Logger logger = LogManager.getLogger(VirtualAgv.class.getName());

  protected ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

  public VirtualAgv() {}

  public VirtualAgv(PositionSubscriber positionSubscriber) {}
  
  public VirtualAgv(PositionSubscriber positionSubscriber, Map<VirtualAgv, String> sharedPositionMap) {
    this.positionSubscriber = positionSubscriber;
    this.sharedPositionMap = sharedPositionMap;
  }

  @Override
  public void run() {
    //xu li order
    executorService.scheduleWithFixedDelay(this::processRequest, 0, 100, TimeUnit.MILLISECONDS);

    //pub
    try {
      setupAgv();
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  protected abstract void setupAgv() throws MqttException;

  protected abstract void processRequest();

  public void open() throws  Exception{
    initialize(this);
  }
  public abstract void initialize(Runnable onSuccess) throws Exception;
  
  /**
   * Check if a position is currently occupied by another AGV
   * @param targetPosition The position to check
   * @return true if occupied by another AGV, false otherwise
   */
  protected boolean isPositionOccupiedByOtherAgv(String targetPosition) {
    if (sharedPositionMap == null) {
      return false; // No collision detection if map not provided
    }
    
    return sharedPositionMap.entrySet().stream()
        .anyMatch(entry -> {
          VirtualAgv otherAgv = entry.getKey();
          String occupiedPosition = entry.getValue();
          // Check if another AGV (not this one) is at the target position
          logger.info("{} - Checking if position {} is occupied by another AGV", this.getName(), targetPosition);
          logger.info("{} - Other AGV: {}", otherAgv.getName(), occupiedPosition);
          return !otherAgv.equals(this) && occupiedPosition.equals(targetPosition);
        });
  }

  public interface PositionSubscriber {
    void onPositionChange(VirtualAgv agv, String position);
  }
}
