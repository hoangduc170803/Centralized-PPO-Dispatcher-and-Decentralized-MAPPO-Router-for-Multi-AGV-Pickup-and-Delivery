package cfg.aubot.itteam.simulator;

import cfg.aubot.itteam.simulator.telegrams.vda5050.*;
import cfg.aubot.itteam.simulator.telegrams.vda5050.Error;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;

public class VirtualMqttAgv extends VirtualAgv implements MovingListener, MqttCallbackExtended {

  private static final Logger logger = LogManager.getLogger(VirtualMqttAgv.class.getName());
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_MQTT_LOAD_TYPE = "EURO_PALLET";
  // VDA5050: theta=0 = facing +X, counter-clockwise positive.
  private static final double DEFAULT_INITIAL_THETA_RAD = 0.0;
  private static final String DEFAULT_MAP_ID = "map";

  private static final List<String> WARNING_ERROR_TYPES = List.of(
      "validationError",
      "orderError",
      "orderUpdateError",
      "noRouteError");

  static {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private MqttClient mqttClient;
  private final String name;
  private boolean isError;

  private String manufacturer = "AUBOT";
  private String serialNumber;
  private long idCounter = 0;
  private String topicPrefix;

  // Resource management
  private Timer statePublishTimer;
  private Timer chargingTimer;
  private ScheduledExecutorService chargingExecutor;
  private ScheduledExecutorService actionExecutor;
  private ExecutorService messageProcessingExecutor;

  private cfg.aubot.itteam.simulator.telegrams.vda5050.State state;
  private List<Node> nodes = Collections.synchronizedList(new ArrayList<>());
  private List<Edge> edges = Collections.synchronizedList(new ArrayList<>());
  private Map<Node, List<ActionState>> nodeActionStatesMap = new LinkedHashMap<>();
  private Map<Edge, List<ActionState>> edgeActionStatesMap = new LinkedHashMap<>();
  private List<ActionState> instantActionStates = new ArrayList<>();

  // Deceleration logging flag - reset khi AGV đổi vị trí
  private boolean hasLoggedDeceleration = false;
  private final Map<String, NodePosition> knownNodePositions = new ConcurrentHashMap<>();
  private volatile double lastKnownThetaRad = 0.0;
  private volatile boolean hasKnownTheta = false;

  public VirtualMqttAgv(String name, String initialPosition, PositionSubscriber positionSubscriber) {
    this(name, initialPosition, positionSubscriber, null);
  }

  public VirtualMqttAgv(String name, String initialPosition, PositionSubscriber positionSubscriber,
      Map<VirtualAgv, String> sharedPositionMap) {
    // khởi tạo positionSubscriber và sharedPositionMap trong super để VirtualAgv để
    // phát hiện va chạm
    super(positionSubscriber, sharedPositionMap);
    // Gán danh tính xe
    this.name = name; // "V01" — tên hiển thị trong log
    this.serialNumber = name; // "V01" — dùng trong MQTT topic và VDA5050 state
    this.position = initialPosition; // "3027" — xe đang đứng ở đâu lúc khởi động
    // topicPrefix sẽ là "aubotagv/2.0.0/AUBOT/V01" — dùng làm prefix cho các topic
    // MQTT của xe này
    // Topic format must match OpenTCS opentcs-commadapter-vda5050:
    // <interfaceName>/v<MAJOR>/<manufacturer>/<serialNumber>
    // (see CommAdapterImpl.setTopicPrefix in v2_0).
    // The version segment is hardcoded as "v2" on both sides.
    this.topicPrefix = "aubotagv/v2/" + this.manufacturer + "/" + this.serialNumber;
    loadKnownNodePositionsFromPlantModel();

    // Initialize executors
    // Chạy quá trình sạc pin (có thể kéo dài 60 giây) mà không block luồng chính
    this.chargingExecutor = Executors.newSingleThreadScheduledExecutor();
    // Lên lịch cho stopCharging sau 3 giây để đảm bảo startCharging được xử lý
    // trước, tránh tình trạng stopCharging đến trước startCharging do message xử lý
    // bất đồng bộ
    this.actionExecutor = Executors.newSingleThreadScheduledExecutor();
    // Xử lý MQTT message nhận được — tách ra để không block callback thread của
    // Paho
    this.messageProcessingExecutor = Executors.newSingleThreadExecutor();

    // Khởi tạo Mqtt client
    // dev:mqict20130516@171.244.5.185
    try {
      // "tcp://localhost:1883" — địa chỉ brokername ("V01") — MQTT clientId, phải là
      // duy nhất trên broker
      mqttClient = new MqttClient("tcp://127.0.0.1:1883", name, new MemoryPersistence()); // lưu các message QoS 1/2
                                                                                          // đang chờ xử lý trong RAM
                                                                                          // (thay vì file)
      mqttClient.setCallback(this);
    } catch (MqttException e) {
      e.printStackTrace();
    }

    // Ngay khi xe được tạo ra, nó báo với agvPointMap rằng: "xe V01 đang đứng tại
    // 3027". Điều này quan trọng để các xe khác biết điểm 3027 đã có người, tránh
    // va chạm từ đầu.
    if (positionSubscriber != null) {
      logger.info("Initializing position map for AGV {}", name);
      positionSubscriber.onPositionChange(this, initialPosition);
    }

    // this.position = "";
    // ScheduledExecutorService executorService =
    // Executors.newScheduledThreadPool(1);
    // executorService.schedule(() -> this.position = initialPosition, 5,
    // TimeUnit.SECONDS);

    // this.energyLevel = 30;
  }

  public VirtualMqttAgv setErrorManager(AgvVirtualError errorManager) {
    errorManager.setListener(this);
    return this;
  }

  @Override
  public void initialize(Runnable onSuccess) throws Exception {
    // Cấu hình kết nối
    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    options.setUserName("fms");
    options.setPassword("Aubot@2025".toCharArray());
    options.setKeepAliveInterval(300);

    // Bước 2: Đặt "di chúc" (Last Will Testament)

    // Xe nói với Broker lúc connect:
    // "Nếu tôi mất kết nối đột ngột (crash, mất mạng...),
    // anh hãy tự động publish tin nhắn CONNECTIONBROKEN
    // vào topic .../V01/connection thay tôi"

    // Broker ghi nhớ "di chúc" này.
    // Khi xe mất kết nối đột ngột → Broker tự publish → FMS biết ngay.
    options.setWill(topicPrefix.concat("/connection"),
        objectMapper.writeValueAsBytes(
            new Connection(idCounter++, manufacturer, serialNumber, ConnectionState.CONNECTIONBROKEN)),
        1, true);

    mqttClient.connect(options); // thực sự kết nối tới broker

    // Bước 3: Kết nối và công bố "tôi online"

    MqttMessage connection = new MqttMessage(
        objectMapper.writeValueAsBytes(new Connection(idCounter++, manufacturer, serialNumber))); // Tạo message
                                                                                                  // CONNECTION với
                                                                                                  // trạng thái mặc định
                                                                                                  // (CONNECTED)
    connection.setRetained(true); // Retained = true để broker lưu lại message này, bất kỳ client nào subscribe
                                  // vào topic này sau đó cũng sẽ nhận được message CONNECTION mới nhất, biết ngay
                                  // AGV đang online
    connection.setQos(1); // QoS 1: đảm bảo message được deliver ít nhất một lần, quan trọng để FMS chắc
                          // chắn nhận được thông báo trạng thái kết nối của AGV
    mqttClient.publish(topicPrefix.concat("/connection"), connection);
    onSuccess.run();
  }

  @Override
  public void onDistanceChange(float distance) {
    this.distance = distance;
  }

  @Override
  public void onError(Map<String, String> errorMap) {
    List<Error> errors = new ArrayList<>(List.of(this.state.getErrors()));
    errors.removeIf(error -> error.getErrorLevel() == ErrorLevel.FATAL);
    isError = !errors.isEmpty();
    errors.addAll(errorMap.entrySet().stream().map(error -> Error.builder()
        .errorType(error.getKey())
        .errorDescription(error.getValue())
        .errorLevel(ErrorLevel.FATAL)
        .build()).toList());
    state.setErrors(errors.toArray(new Error[0]));
  }

  @Override
  public void onError(int errors) {

  }

  @Override
  protected void setupAgv() throws MqttException {
    this.state = new cfg.aubot.itteam.simulator.telegrams.vda5050.State(idCounter, manufacturer, serialNumber,
        "", 0, position, 0,
        new NodeState[0], new EdgeState[0], false, new ActionState[0],
        new BatteryState(energyLevel, false), 0, distance);
    setStateUnloaded();
    updateAgvPositionAtNode(position);

    // Mỗi 200ms (5Hz) publish vào topic agvs/state/<name>

    statePublishTimer = new Timer(name + "-state-publisher");
    statePublishTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          if (mqttClient == null || !mqttClient.isConnected()) {
            return;
          }
          NodeState[] nodeStates;
          if (nodes.isEmpty()) {
            nodeStates = new NodeState[0];
          } else {
            nodeStates = nodes.stream().map(NodeState::new).toList().toArray(new NodeState[0]);
          }
          var edgeStates = edges.stream().map(EdgeState::new).toList().toArray(new EdgeState[0]);
          var nodeEdgeActionStates = Stream.concat(
              nodeActionStatesMap.values().stream().flatMap(List::stream),
              edgeActionStatesMap.values().stream().flatMap(List::stream));
          var allActionStates = Stream.concat(nodeEdgeActionStates, instantActionStates.stream())
              .toList().toArray(new ActionState[0]);
          MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(
              state.refresh(idCounter++, position, nodeStates, edgeStates, allActionStates, distance, energyLevel)));
          message.setQos(0);
          mqttClient.publish(topicPrefix.concat("/state"), message);
        } catch (MqttException e) {
          if (mqttClient != null && mqttClient.isConnected()) {
            logger.warn("{} - Could not publish MQTT state: {}", name, e.toString());
          }
        } catch (Exception e) {
          logger.warn("{} - State publish failed: {}", name, e.toString());
        }
      }
    }, 0, 200);

    // Timer để tăng batteryVoltage, batteryCharge và energyLevel khi sạc (mỗi 1
    // giây)
    chargingTimer = new Timer(name + "-charging");
    chargingTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          BatteryState batteryState = state.getBatteryState();
          if (batteryState.getCharging()) {
            // Nếu đang sạc thì tăng voltage, charge và energyLevel mỗi 1 giây
            batteryState.increaseVoltage(0.1);
            batteryState.setBatteryCharge(batteryState.getBatteryCharge() + 1.0);
            energyLevel = Math.min(energyLevel + 1, 100);
            // Giới hạn batteryCharge không vượt quá 100%
            if (batteryState.getBatteryCharge() > 100.0) {
              batteryState.setBatteryCharge(100.0);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }, 1000, 1000); // Bắt đầu sau 1 giây, lặp lại mỗi 1 giây

    // Timer để giảm batteryVoltage, batteryCharge và energyLevel khi không sạc (mỗi
    // 6 giây)
    // Timer dischargingTimer = new Timer(name + "-discharging");
    // dischargingTimer.schedule(new TimerTask() {
    // @Override
    // public void run() {
    // try {
    // BatteryState batteryState = state.getBatteryState();
    // if (!batteryState.getCharging()) {
    // // Nếu không sạc thì giảm voltage, charge và energyLevel mỗi 6 giây
    // batteryState.decreaseVoltage(0.1);
    // batteryState.setBatteryCharge(batteryState.getBatteryCharge() - 1.0);
    // energyLevel = Math.max(energyLevel - 1, 0);
    // // Giới hạn batteryCharge không xuống dưới 0%
    // if (batteryState.getBatteryCharge() < 0.0) {
    // batteryState.setBatteryCharge(0.0);
    // }
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // }, 6000, 6000); // Bắt đầu sau 6 giây, lặp lại mỗi 6 giây

    // Subscribe vào topic agvs/req/<name>
    mqttClient.subscribe(topicPrefix.concat("/order"), 2);
    mqttClient.subscribe(topicPrefix.concat("/instantActions"), 2);
  }

  private int traversedEdgeCount = 0;

  // Hàm xử lý hàng đợi message của xe
  @Override
  protected void processRequest() {
    if (nodes.isEmpty()) {
      if (!edges.isEmpty()) {
        logger.warn("{} - Clearing {} stale edge(s) without pending nodes.", name, edges.size());
        edges.clear();
        edgeActionStatesMap.clear();
      }
      return;
    }
    // Nếu AGV có tên là VAGV4, không cho process
    // if ("VAGV4".equals(name)) {
    // return;
    // }
    try {
      // var nodeIterator = nodes.iterator();
      // var edgeIterator = edges.iterator();
      var destination = nodes.getFirst();
      if (!destination.getReleased()) {
        return;
      }

      // Nếu xe đang paused và chưa bắt đầu di chuyển, không cho bắt đầu edge tiếp
      // theo
      if (state.getPaused() && !state.getDriving()) {
        logger.info("{} - Vehicle is paused, waiting for stopPause before processing next node", name);
        return;
      }

      if (!destination.getNodeID().equals(state.getLastNodeID())) {
        // Collision detection: Check if destination is occupied by another AGV
        if (positionSubscriber != null && !state.getDriving()) {
          boolean isDestinationOccupied = isPositionOccupiedByOtherAgv(destination.getNodeID());
          if (isDestinationOccupied) {
            logger.info("{} - Cannot move to position {}: occupied by another AGV. Waiting...",
                name, destination.getNodeID());
            return;
          }
        }
        if (edges.isEmpty()) {
          logger.warn("State has node {} to arrive but no edge", destination.getNodeID());
        } else {
          state.setDriving(true);
          Edge nextEdge = edges.getFirst();
          String sourceNodeId = state.getLastNodeID();
          updateAgvPositionAlongPath(sourceNodeId, destination.getNodeID(), 0.0, nextEdge);
          List<ActionState> actionStates = edgeActionStatesMap.getOrDefault(nextEdge, new ArrayList<>());
          if (!actionStates.isEmpty()) {
            for (ActionState actionState : actionStates) {
              if (actionState.getActionStatus() == ActionStatus.FINISHED
                  || actionState.getActionStatus() == ActionStatus.FAILED) {
                continue;
              }
              logger.info("{} - {}", position, actionState.getActionType());
              actionState.setActionStatus(ActionStatus.RUNNING);
              actionState.setActionStatus(ActionStatus.FINISHED);
            }
          }
          logger.info("{} - New edge: {}", name, nextEdge.getEdgeID());
          // Cho phép hoàn thành edge hiện tại ngay cả khi paused (để xe đi nốt edge đang
          // di chuyển)
          while (distance < 1) {
            loopTime(100);
            distance += Math.abs(nextEdge.getMaxSpeed()) * 0.005;
            updateAgvPositionAlongPath(sourceNodeId, destination.getNodeID(), distance, nextEdge);

            // Log thời điểm AGV cần giảm tốc (chỉ log 1 lần cho mỗi vị trí)
            if (!hasLoggedDeceleration && nodes.size() <= 1 && distance >= 0.7) {
              logger.info("{} - DECELERATION POINT: nodes remaining = {}, distance = {:.3f}, position = {}",
                  name, nodes.size(), distance, position);
              hasLoggedDeceleration = true;
            }
          }
          traversedEdgeCount++;
          // Disabled automatic battery/voltage drain while moving.
          // if (traversedEdgeCount % 10 == 0) {
          // BatteryState batteryState = state.getBatteryState();
          // this.energyLevel = Math.max(this.energyLevel - 1, 0);
          // batteryState.setBatteryCharge(this.energyLevel);
          // batteryState.decreaseVoltage(0.1);
          // }
          edges.removeFirst();
        }
      }

      // finished - đã đến node đích
      distance = 0;
      position = destination.getNodeID();
      if (positionSubscriber != null) {
        positionSubscriber.onPositionChange(this, position);
      }
      logger.info("{} - New position: {}", name, position);
      state.setLastNodeID(position);
      state.setLastNodeSequenceID(destination.getSequenceID());
      state.setDriving(false);
      updateAgvPositionAtNode(position);

      // Reset deceleration log flag khi AGV đổi vị trí
      hasLoggedDeceleration = false;
      instantActionStates.stream().filter(actionState -> actionState.getActionType().equals("cancelOrder")
          && actionState.getActionStatus() == ActionStatus.RUNNING)
          .forEach(actionState -> actionState.setActionStatus(ActionStatus.FINISHED));
      List<ActionState> actionStates = nodeActionStatesMap.getOrDefault(destination, new ArrayList<>());
      if (!actionStates.isEmpty()) {
        for (ActionState actionState : actionStates) {
          if (actionState.getActionStatus() == ActionStatus.FINISHED
              || actionState.getActionStatus() == ActionStatus.FAILED) {
            continue;
          }
          logger.info("{} - {}", position, actionState.getActionType());
          actionState.setActionStatus(ActionStatus.RUNNING);
          if ("liftUp".equals(actionState.getActionType())) {
            loopTime(3000);
            setStateLoaded();
          } else if ("liftDown".equals(actionState.getActionType())
              || "leaveGoods".equals(actionState.getActionType())) {
            loopTime(3000);
            setStateUnloaded();
          } else if (actionState.getActionType().equals("startCharging")) {
            state.getBatteryState().setCharging(true);
            actionState.setActionStatus(ActionStatus.FINISHED);
            // Use executor service instead of raw thread
            chargingExecutor.submit(() -> {
              try {
                loopTime(60000, time -> {
                  // energyLevel được xử lý bởi chargingTimer riêng biệt
                  return !state.getBatteryState().getCharging();
                });
                state.getBatteryState().setCharging(false);
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
          }
          actionState.setActionStatus(ActionStatus.FINISHED);
        }
      }

      // Nếu xe đang paused, không remove node để không process node tiếp theo
      // Điều này ngăn xe bắt đầu edge tiếp theo
      if (state.getPaused()) {
        logger.info("{} - Vehicle is paused at node {}, will not process next node until stopPause", name, position);
        return;
      }

      nodes.removeFirst();
      if (nodes.isEmpty() && !edges.isEmpty()) {
        logger.warn("{} - Reached final pending node {}, clearing {} stale edge(s).",
            name, position, edges.size());
        edges.clear();
        edgeActionStatesMap.clear();
      }
      logger.info("nodes size: {} edge size: {}", nodes.size(), edges.size());
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  private void mergeOrderUpdate(Node updateStartNode, Edge[] updateEdges) {
    long updateSequenceId = updateStartNode.getSequenceID();
    if (updateEdges != null && updateEdges.length > 0) {
      updateSequenceId = Math.min(updateSequenceId, updateEdges[0].getSequenceID());
    }
    final long firstSequenceId = updateSequenceId;

    nodes.removeIf(node -> node.getSequenceID() >= firstSequenceId);
    edges.removeIf(edge -> edge.getSequenceID() >= firstSequenceId);
  }

  private void loopTime(long timeInMillis) throws Exception {
    loopTime(timeInMillis, null);
  }

  private void loopTime(long timeInMillis, Predicate<Integer> condition) throws Exception {
    for (int i = 0; i < timeInMillis / 100; i++) {
      try {
        if (isError) {
          continue;
        }
        Thread.sleep(100);
        if (condition != null && condition.test(i * 100)) {
          break;
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void rememberNodePositions(Node[] orderNodes) {
    if (orderNodes == null) {
      return;
    }
    for (Node node : orderNodes) {
      if (node == null || node.getNodeID() == null || node.getNodePosition() == null) {
        continue;
      }
      knownNodePositions.put(node.getNodeID(), node.getNodePosition());
    }
  }

  private void loadKnownNodePositionsFromPlantModel() {
    Optional<Path> modelPath = findPlantModelPath();
    if (modelPath.isEmpty()) {
      logger.warn("No openTCS plant model XML found; AGV state will omit agvPosition until an order provides node positions.");
      return;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      Document document = factory.newDocumentBuilder().parse(modelPath.get().toFile());
      NodeList pointElements = document.getElementsByTagName("point");

      int loaded = 0;
      for (int i = 0; i < pointElements.getLength(); i++) {
        Element pointElement = (Element) pointElements.item(i);
        String name = pointElement.getAttribute("name");
        String positionX = pointElement.getAttribute("positionX");
        String positionY = pointElement.getAttribute("positionY");
        if (name.isBlank() || positionX.isBlank() || positionY.isBlank()) {
          continue;
        }

        NodePosition nodePosition = new NodePosition();
        nodePosition.setMapID(DEFAULT_MAP_ID);
        nodePosition.setX(Double.parseDouble(positionX) / 1000.0);
        nodePosition.setY(Double.parseDouble(positionY) / 1000.0);

        String orientation = pointElement.getAttribute("vehicleOrientationAngle");
        if (!orientation.isBlank() && !"NaN".equals(orientation)) {
          nodePosition.setTheta(normalizeAngleRad(Math.toRadians(Double.parseDouble(orientation))));
        }

        knownNodePositions.put(name, nodePosition);
        loaded++;
      }
      logger.info("Loaded {} point positions from openTCS model '{}'.", loaded, modelPath.get());
    } catch (Exception e) {
      logger.warn("Failed to load point positions from plant model '{}': {}", modelPath.get(), e.toString());
    }
  }

  private Optional<Path> findPlantModelPath() {
    // 1. Explicit system property or env var
    String configuredPath = System.getProperty("agv.simulator.modelFile");
    if (configuredPath == null || configuredPath.isBlank()) {
      configuredPath = System.getenv("OPENTCS_MODEL_FILE");
    }
    if (configuredPath != null && !configuredPath.isBlank()) {
      Path path = Paths.get(configuredPath.trim());
      if (Files.isRegularFile(path)) {
        return Optional.of(path.toAbsolutePath().normalize());
      }
      logger.warn("Configured plant model file does not exist: {}", path.toAbsolutePath().normalize());
    }

    // 2. Well-known fallback locations relative to working directory
    List<String> candidates = List.of(
        "../../opentcs-integration-example/opentcs-example-kernel/build/install/opentcs-example-kernel/data/model.xml",
        "../opentcs-integration-example/opentcs-example-kernel/build/install/opentcs-example-kernel/data/model.xml",
        "../../opentcs-integration-example/opentcs-example-modeleditor/src/dist/data/Demo-01.xml",
        "../opentcs-integration-example/opentcs-example-modeleditor/src/dist/data/Demo-01.xml"
    );
    for (String candidate : candidates) {
      Path path = Paths.get(candidate).toAbsolutePath().normalize();
      if (Files.isRegularFile(path)) {
        logger.info("Auto-detected plant model at: {}", path);
        return Optional.of(path);
      }
    }

    return Optional.empty();
  }

  private void updateAgvPositionAlongPath(
      String sourceNodeId,
      String destinationNodeId,
      double progress,
      Edge edge) {
    if (state == null || sourceNodeId == null || destinationNodeId == null) {
      return;
    }
    NodePosition sourcePos = knownNodePositions.get(sourceNodeId);
    NodePosition destinationPos = knownNodePositions.get(destinationNodeId);
    if (sourcePos == null || destinationPos == null) {
      return;
    }

    double clampedProgress = Math.max(0.0, Math.min(progress, 1.0));
    double x = sourcePos.getX() + (destinationPos.getX() - sourcePos.getX()) * clampedProgress;
    double y = sourcePos.getY() + (destinationPos.getY() - sourcePos.getY()) * clampedProgress;
    double theta = determineVehicleThetaOnPath(sourcePos, destinationPos, edge);

    hasKnownTheta = true;
    lastKnownThetaRad = theta;
    state.setAgvPosition(
        buildAgvPosition(
            x,
            y,
            theta,
            destinationPos.getMapID() != null ? destinationPos.getMapID() : sourcePos.getMapID()));
  }

  private void updateAgvPositionAtNode(String nodeId) {
    if (state == null || nodeId == null) {
      return;
    }
    NodePosition nodePos = knownNodePositions.get(nodeId);
    if (nodePos == null) {
      return;
    }
    double theta;
    if (nodePos.getTheta() != null) {
      theta = normalizeAngleRad(nodePos.getTheta());
    } else if (hasKnownTheta) {
      theta = lastKnownThetaRad;
    } else {
      // No theta from order/node yet -> start vehicle body heading at X+.
      theta = DEFAULT_INITIAL_THETA_RAD;
    }
    hasKnownTheta = true;
    lastKnownThetaRad = theta;
    state.setAgvPosition(buildAgvPosition(nodePos.getX(), nodePos.getY(), theta, nodePos.getMapID()));
  }

  private AgvPosition buildAgvPosition(double x, double y, double theta, String mapId) {
    AgvPosition agvPosition = new AgvPosition();
    agvPosition.setX(x);
    agvPosition.setY(y);
    agvPosition.setTheta(theta);
    agvPosition.setPositionInitialized(true);
    agvPosition.setLocalizationScore(1.0);
    if (mapId == null || mapId.trim().isEmpty()) {
      agvPosition.setMapID(DEFAULT_MAP_ID);
    } else {
      agvPosition.setMapID(mapId.trim());
    }
    return agvPosition;
  }

  private double normalizeAngleRad(double angle) {
    double normalized = angle;
    while (normalized > Math.PI) {
      normalized -= (Math.PI * 2.0);
    }
    while (normalized < -Math.PI) {
      normalized += (Math.PI * 2.0);
    }
    return normalized;
  }

  private double determineVehicleThetaOnPath(NodePosition sourcePos, NodePosition destinationPos, Edge edge) {
    if (edge != null && edge.getOrientation() != null) {
      // Respect explicit orientation from order when provided.
      return normalizeAngleRad(edge.getOrientation());
    }

    if (destinationPos != null && destinationPos.getTheta() != null) {
      // Prefer nodePosition.theta from order so simulator follows FMS/layout
      // direction exactly.
      return normalizeAngleRad(destinationPos.getTheta());
    }

    if (sourcePos != null && sourcePos.getTheta() != null) {
      return normalizeAngleRad(sourcePos.getTheta());
    }

    double sourceX = sourcePos.getX();
    double sourceY = sourcePos.getY();
    double destinationX = destinationPos.getX();
    double destinationY = destinationPos.getY();
    double dx = destinationX - sourceX;
    double dy = destinationY - sourceY;
    if (Math.abs(dx) < 1.0e-9 && Math.abs(dy) < 1.0e-9) {
      return hasKnownTheta ? lastKnownThetaRad : DEFAULT_INITIAL_THETA_RAD;
    }

    // VDA5050: theta=0 at +X axis, counter-clockwise positive → atan2(dy, dx).
    if (isReverseEdge(edge)) {
      dx = -dx;
      dy = -dy;
    }
    return normalizeAngleRad(Math.atan2(dy, dx));
  }

  private boolean isReverseEdge(Edge edge) {
    return edge != null && edge.getMaxSpeed() != null && edge.getMaxSpeed() < 0.0;
  }

  @Override
  public void connectComplete(boolean b, String s) {
    logger.info("connect complete {}", s);
  }

  @Override
  public void connectionLost(Throwable throwable) {
    logger.warn("{} - MQTT connection lost: {}", name, throwable != null ? throwable.toString() : "unknown");
  }

  @Override
  public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
    // Non-blocking callback: chỉ log và đẩy vào executor để xử lý async
    logger.info("Message arrived on topic: {}", s);

    // Copy message payload để tránh vấn đề với MqttMessage có thể bị reuse
    byte[] payloadCopy = Arrays.copyOf(mqttMessage.getPayload(), mqttMessage.getPayload().length);
    String topic = s;

    // Xử lý message trong executor riêng để không block callback thread
    messageProcessingExecutor.execute(() -> {
      try {
        MqttMessage messageCopy = new MqttMessage(payloadCopy);
        messageCopy.setQos(mqttMessage.getQos());
        messageCopy.setRetained(mqttMessage.isRetained());
        processMessage(topic, messageCopy);
      } catch (Exception e) {
        logger.error("Error processing MQTT message on topic: {}", topic, e);
      }
    });
  }

  /**
   * Xử lý message MQTT. Method này được gọi trong executor riêng để không block
   * callback thread.
   * Synchronized để đảm bảo thread-safety khi cập nhật state.
   */
  private synchronized void processMessage(String s, MqttMessage mqttMessage) {
    // Kiểm tra state đã được khởi tạo chưa
    if (state == null) {
      logger.warn("State not initialized yet, ignoring message on topic: {}", s);
      return;
    }

    Error[] fatalErrors = Arrays.stream(state.getErrors())
        .filter(error -> ErrorLevel.FATAL.equals(error.getErrorLevel()))
        .toArray(Error[]::new);
    if (s.endsWith("/order")) {
      Order newOrder = null;
      try {
        newOrder = objectMapper.readValue(mqttMessage.getPayload(), Order.class);
        boolean orderUpdate = Objects.equals(newOrder.getOrderID(), state.getOrderID());
        if (Objects.equals(newOrder.getOrderID(), state.getOrderID())) {
          if (newOrder.getOrderUpdateID() < state.getOrderUpdateID()) {
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("Order update deprecated: " + newOrder.getOrderUpdateID())
                .errorType("orderUpdateError")
                .errorReferences(new ErrorReference[] {
                    new ErrorReference("orderId", newOrder.getOrderID()),
                    new ErrorReference("orderUpdateId", String.valueOf(newOrder.getOrderUpdateID())),
                })
                .build());
          }
          Node node = newOrder.getNodes()[0];
          if (!nodes.isEmpty()) {
            String lastBaseNodeId = nodes.reversed().stream().filter(Node::getReleased).findFirst().map(Node::getNodeID)
                .orElse(position);
            if (!Objects.equals(node.getNodeID(), lastBaseNodeId)) {
              throw new StateErrorException(Error.builder()
                  .errorLevel(ErrorLevel.WARNING)
                  .errorDescription(
                      "The first node of the received order not equal to the last node of the current base: "
                          + node.getNodeID() + " <> " + lastBaseNodeId)
                  .errorType("orderUpdateError")
                  .errorReferences(new ErrorReference[] {
                      new ErrorReference("orderId", newOrder.getOrderID()),
                      new ErrorReference("orderUpdateId", String.valueOf(newOrder.getOrderUpdateID())),
                  })
                  .build());
            }
          } else {
            if (!Objects.equals(node.getNodeID(), state.getLastNodeID())
                || node.getSequenceID() != state.getLastNodeSequenceID()) {
              throw new StateErrorException(Error.builder()
                  .errorLevel(ErrorLevel.WARNING)
                  .errorDescription("The first node of the received order not equal to last node of the current base: "
                      + node.getNodeID() + " <> " + state.getLastNodeID())
                  .errorType("orderUpdateError")
                  .errorReferences(new ErrorReference[] {
                      new ErrorReference("orderId", newOrder.getOrderID()),
                      new ErrorReference("orderUpdateId", String.valueOf(newOrder.getOrderUpdateID())),
                  })
                  .build());
            }
          }
          logger.info("Update order {}: {}", state.getOrderID(), mqttMessage);
        } else {
          if (!nodes.isEmpty()
              || nodeActionStatesMap.values().stream().flatMap(List::stream)
                  .anyMatch(actionState -> !ActionStatus.FINISHED.equals(actionState.getActionStatus())
                      && !ActionStatus.FAILED.equals(actionState.getActionStatus()))
              || !edges.isEmpty()
              || edgeActionStatesMap.values().stream().flatMap(List::stream)
                  .anyMatch(actionState -> !ActionStatus.FINISHED.equals(actionState.getActionStatus())
                      && !ActionStatus.FAILED.equals(actionState.getActionStatus()))) {
            logger.info("Update order {}: {}", state.getOrderID(), mqttMessage);
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("Last order " + state.getOrderID() + " haven't finished yet")
                .errorType("orderError")
                .errorReferences(new ErrorReference[] { new ErrorReference("orderId", newOrder.getOrderID()) })
                .build());
          }
          Node firstNode = newOrder.getNodes()[0];
          if (firstNode.getSequenceID() != 0) {
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("The first node of the new order not start with sequence ID 0")
                .errorType("orderError")
                .errorReferences(new ErrorReference[] { new ErrorReference("orderId", newOrder.getOrderID()) })
                .build());
          }
          if (!firstNode.getNodeID().equals(position)) {
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("Vehicle is not in the first node of the new order")
                .errorType("noRouteError")
                .errorReferences(new ErrorReference[] { new ErrorReference("orderId", newOrder.getOrderID()) })
                .build());
          }
          logger.info("New order arrived: {}", mqttMessage);
        }

        long sequenceCounter = newOrder.getNodes()[0].getSequenceID();
        for (int i = 1; i < newOrder.getNodes().length; i++) {
          Edge edge = newOrder.getEdges()[i - 1];
          if (edge.getSequenceID() != ++sequenceCounter) {
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("Edge sequence ID not incremented by 1, expected: %d, actual: %d"
                    .formatted(sequenceCounter, edge.getSequenceID()))
                .errorType("orderUpdateError")
                .errorReferences(new ErrorReference[] {
                    new ErrorReference("orderId", newOrder.getOrderID()),
                    new ErrorReference("orderUpdateId", String.valueOf(newOrder.getOrderUpdateID())),
                })
                .build());
          }
          Node node = newOrder.getNodes()[i];
          if (node.getSequenceID() != ++sequenceCounter) {
            throw new StateErrorException(Error.builder()
                .errorLevel(ErrorLevel.WARNING)
                .errorDescription("Node sequence ID not incremented by 1, expected: %d, actual: %d"
                    .formatted(sequenceCounter, node.getSequenceID()))
                .errorType("orderUpdateError")
                .build());
          }
        }

        // Error newError = Error.builder()
        // .errorLevel(ErrorLevel.FATAL)
        // .errorType("adapterLostNavigation")
        // .errorDescription("Order " + newOrder.getOrderID() + " is completed")
        // .errorReferences(new ErrorReference[]{new ErrorReference("orderId",
        // newOrder.getOrderID())})
        // .build();
        // fatalErrors = addElement(fatalErrors, newError);
        state.setErrors(fatalErrors);

        state.setOrderID(newOrder.getOrderID());
        state.setOrderUpdateID(newOrder.getOrderUpdateID());
        if (!orderUpdate) {
          state.setLastNodeSequenceID(0);
        }
        rememberNodePositions(newOrder.getNodes());
        updateAgvPositionAtNode(state.getLastNodeID());

        Iterator<Node> nodeIterator = Arrays.stream(newOrder.getNodes()).iterator();
        Iterator<Edge> edgeIterator = Arrays.stream(newOrder.getEdges()).iterator();

        Node startNode = nodeIterator.next();
        if (orderUpdate) {
          mergeOrderUpdate(startNode, newOrder.getEdges());
        }
        if (nodes.isEmpty()) {
          nodes.add(startNode);
          if (startNode.getReleased()) {
            syncActionStates(nodeActionStatesMap.getOrDefault(startNode, new ArrayList<>()), startNode.getActions(),
                actionState -> nodeActionStatesMap.computeIfAbsent(startNode, k -> new ArrayList<>()).add(actionState));
          }
        }
        while (edgeIterator.hasNext()) {
          Edge edge = edgeIterator.next();
          Node node = nodeIterator.next();

          edges.add(edge);
          if (edge.getReleased()) {
            syncActionStates(edgeActionStatesMap.getOrDefault(edge, new ArrayList<>()), edge.getActions(),
                actionState -> edgeActionStatesMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(actionState));

          }

          nodes.add(node);
          if (node.getReleased()) {
            syncActionStates(nodeActionStatesMap.getOrDefault(node, new ArrayList<>()), node.getActions(),
                actionState -> nodeActionStatesMap.computeIfAbsent(node, k -> new ArrayList<>()).add(actionState));
          }
        }
        nodeActionStatesMap.entrySet().removeIf(entry -> !nodes.contains(entry.getKey()));
        edgeActionStatesMap.entrySet().removeIf(entry -> !edges.contains(entry.getKey()));

        // Cleanup instantActionStates with max size limit
        final int MAX_INSTANT_ACTIONS = 20;
        if (instantActionStates.size() >= MAX_INSTANT_ACTIONS) {
          instantActionStates.removeIf(actionState -> actionState.getActionStatus().equals(ActionStatus.FINISHED) ||
              actionState.getActionStatus().equals(ActionStatus.FAILED));
        }
      } catch (StateErrorException e) {
        logger.info("Order rejected: {}", e.getMessage());

        // Log comprehensive vehicle state
        logger.info("=== VEHICLE STATE AT ORDER REJECTION ===");
        logger.info("Vehicle: {} ({})", state.getSerialNumber(), state.getManufacturer());
        logger.info("Current Order ID: {}", state.getOrderID());
        logger.info("Current Order Update ID: {}", state.getOrderUpdateID());
        logger.info("Last Node ID: {}", state.getLastNodeID());
        logger.info("Last Node Sequence ID: {}", state.getLastNodeSequenceID());
        logger.info("Current Position: {}", position);
        logger.info("Is Driving: {}", state.getDriving());
        logger.info("Is Paused: {}", state.getPaused());
        logger.info("Operating Mode: {}", state.getOperatingMode());
        logger.info("Battery Charge: {}%",
            state.getBatteryState() != null ? state.getBatteryState().getBatteryCharge() : "N/A");
        logger.info("Distance Since Last Node: {}", state.getDistanceSinceLastNode());

        // Log pending nodes
        logger.info("Pending Nodes ({}): {}", nodes.size(),
            nodes.stream()
                .map(n -> String.format("%s(seq:%d,released:%b)", n.getNodeID(), n.getSequenceID(), n.getReleased()))
                .collect(Collectors.joining(", ")));

        // Log pending edges
        logger.info("Pending Edges ({}): {}", edges.size(),
            edges.stream()
                .map(
                    ee -> String.format("%s(seq:%d,released:%b)", ee.getEdgeID(), ee.getSequenceID(), ee.getReleased()))
                .collect(Collectors.joining(", ")));

        // Log node action states
        logger.info("Node Action States:");
        nodeActionStatesMap.forEach((node, actionStates) -> {
          logger.info("  Node {}: {}", node.getNodeID(),
              actionStates.stream().map(as -> String.format("%s(%s)", as.getActionID(), as.getActionStatus()))
                  .collect(Collectors.joining(", ")));
        });

        // Log edge action states
        logger.info("Edge Action States:");
        edgeActionStatesMap.forEach((edge, actionStates) -> {
          logger.info("  Edge {}: {}", edge.getEdgeID(),
              actionStates.stream().map(as -> String.format("%s(%s)", as.getActionID(), as.getActionStatus()))
                  .collect(Collectors.joining(", ")));
        });

        // Log instant action states
        if (!instantActionStates.isEmpty()) {
          logger.info("Instant Action States ({}): {}", instantActionStates.size(),
              instantActionStates.stream().map(as -> String.format("%s(%s)", as.getActionID(), as.getActionStatus()))
                  .collect(Collectors.joining(", ")));
        }

        // Log current errors
        if (state.getErrors() != null && state.getErrors().length > 0) {
          logger.info("Current Errors ({}):", state.getErrors().length);
          for (Error error : state.getErrors()) {
            logger.info("  - [{}] {}: {}", error.getErrorLevel(), error.getErrorType(), error.getErrorDescription());
          }
        }

        // Log rejected order information
        if (newOrder != null) {
          logger.info("=== REJECTED ORDER INFORMATION ===");
          logger.info("Rejected Order ID: {}", newOrder.getOrderID());
          logger.info("Rejected Order Update ID: {}", newOrder.getOrderUpdateID());
          logger.info("Order Timestamp: {}", newOrder.getTimestamp());
          logger.info("Order Nodes ({}): {}", newOrder.getNodes() != null ? newOrder.getNodes().length : 0,
              newOrder.getNodes() != null ? Arrays.stream(newOrder.getNodes())
                  .map(n -> String.format("%s(seq:%d,released:%b)", n.getNodeID(), n.getSequenceID(), n.getReleased()))
                  .collect(Collectors.joining(", ")) : "N/A");
          logger.info("Order Edges ({}): {}", newOrder.getEdges() != null ? newOrder.getEdges().length : 0,
              newOrder.getEdges() != null ? Arrays.stream(newOrder.getEdges())
                  .map(eee -> String.format("%s(seq:%d,released:%b)", eee.getEdgeID(), eee.getSequenceID(),
                      eee.getReleased()))
                  .collect(Collectors.joining(", ")) : "N/A");

          // Log node actions in rejected order
          if (newOrder.getNodes() != null) {
            logger.info("Rejected Order Node Actions:");
            for (Node node : newOrder.getNodes()) {
              if (node.getActions() != null && node.getActions().length > 0) {
                logger.info("  Node {}: {}", node.getNodeID(),
                    Arrays.stream(node.getActions())
                        .map(a -> String.format("%s(%s)", a.getActionID(), a.getActionType()))
                        .collect(Collectors.joining(", ")));
              }
            }
          }

          // Log edge actions in rejected order
          if (newOrder.getEdges() != null) {
            logger.info("Rejected Order Edge Actions:");
            for (Edge edge : newOrder.getEdges()) {
              if (edge.getActions() != null && edge.getActions().length > 0) {
                logger.info("  Edge {}: {}", edge.getEdgeID(),
                    Arrays.stream(edge.getActions())
                        .map(a -> String.format("%s(%s)", a.getActionID(), a.getActionType()))
                        .collect(Collectors.joining(", ")));
              }
            }
          }
        }
        logger.info("=== END ORDER REJECTION LOG ===");

        // append to state errors by recreate primitive array and append
        state.setErrors(addElement(fatalErrors, e.getError()));
      } catch (com.fasterxml.jackson.databind.DatabindException e) {
        logger.error("Failed to deserialize order message (DatabindException): {}", e.getMessage(), e);
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        logger.error("Failed to parse order message (JsonProcessingException): {}", e.getMessage(), e);
      } catch (java.io.IOException e) {
        logger.error("IO error while processing order message: {}", e.getMessage(), e);
      } catch (Exception e) {
        logger.error("Unexpected error processing order message: {}", e.getMessage(), e);
        e.printStackTrace();
      }
    } else if (s.endsWith("/instantActions")) {
      logger.info("Processing instantActions message. Payload: {}", new String(mqttMessage.getPayload()));
      try {
        InstantActions instantActions = objectMapper.readValue(mqttMessage.getPayload(), InstantActions.class);
        if (instantActions == null || instantActions.getActions() == null) {
          logger.warn("Invalid instantActions message: null or empty actions");
          return;
        }
        Arrays.stream(instantActions.getActions()).forEach(action -> {
          logger.info("Instant action {}", action.getActionType());
          ActionState actionState = new ActionState(action);
          instantActionStates.add(actionState);
          logger.info("Processing action type: {}", action.getActionType());
          if (action.getActionType().equals("cancelOrder")) {
            Node firstNode = nodes.isEmpty() ? null : nodes.getFirst();
            Edge firstEdge = (nodes.isEmpty() || edges.isEmpty()) ? null : edges.getFirst();
            nodes.clear();
            edges.clear();
            logger.info("Cancel order {} {} {}", state.getDriving(), firstNode, firstEdge);
            if (state.getDriving() && firstNode != null && firstEdge != null) {
              nodes.add(firstNode);
              edges.add(firstEdge);
              logger.info("{} canceling order", name);
            }
            Stream.concat(nodeActionStatesMap.values().stream().flatMap(List::stream),
                edgeActionStatesMap.values().stream().flatMap(List::stream))
                .filter(as -> !as.getActionStatus().equals(ActionStatus.FINISHED))
                .forEach(as -> as.setActionStatus(ActionStatus.FAILED));
            actionState.setActionStatus(state.getDriving() ? ActionStatus.RUNNING : ActionStatus.FINISHED);
          } else if (action.getActionType().equals("stopCharging")) {
            actionExecutor.schedule(() -> state.getBatteryState().setCharging(false), 3, TimeUnit.SECONDS);
            actionState.setActionStatus(ActionStatus.FINISHED);
          } else if (action.getActionType().equals("startPause")) {
            state.setPaused(true);
            actionState.setActionStatus(ActionStatus.FINISHED);
          } else if (action.getActionType().equals("stopPause")) {
            state.setPaused(false);
            actionState.setActionStatus(ActionStatus.FINISHED);
          } else if (action.getActionType().equals("setPinLevel")) {
            logger.info("Found setPinLevel action! Processing...");
            // Extract pin level from action parameters
            String pinLevelStr = "50"; // default value
            if (action.getActionParameters() != null && action.getActionParameters().length > 0) {
              for (var param : action.getActionParameters()) {
                if ("pinLevel".equals(param.getKey())) {
                  Value value = param.getValue();
                  if (value != null && value.stringValue != null) {
                    pinLevelStr = value.stringValue;
                  } else if (value != null && value.doubleValue != null) {
                    pinLevelStr = String.valueOf(value.doubleValue.intValue());
                  }
                  break;
                }
              }
            }
            try {
              int newPinLevel = Integer.parseInt(pinLevelStr);
              // Validate range (0-100)
              newPinLevel = Math.max(0, Math.min(100, newPinLevel));
              this.energyLevel = newPinLevel;
              logger.info("{} - Pin level adjusted to: {}%", name, newPinLevel);
              actionState.setActionStatus(ActionStatus.FINISHED);
            } catch (NumberFormatException e) {
              logger.warn("{} - Invalid pin level value: {}", name, pinLevelStr);
              actionState.setActionStatus(ActionStatus.FAILED);
            }
          } else {
            logger.warn("Unknown action type: {}", action.getActionType());
            actionState.setActionStatus(ActionStatus.FAILED);
          }
        });
      } catch (com.fasterxml.jackson.databind.DatabindException e) {
        logger.error("Failed to deserialize instantActions message (DatabindException): {}", e.getMessage(), e);
        logger.error("Payload: {}", new String(mqttMessage.getPayload()));
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        logger.error("Failed to parse instantActions message (JsonProcessingException): {}", e.getMessage(), e);
        logger.error("Payload: {}", new String(mqttMessage.getPayload()));
      } catch (java.io.IOException e) {
        logger.error("IO error while processing instantActions message: {}", e.getMessage(), e);
        logger.error("Payload: {}", new String(mqttMessage.getPayload()));
      } catch (Exception e) {
        logger.error("Unexpected error processing instantActions message: {}", e.getMessage(), e);
        logger.error("Payload: {}", new String(mqttMessage.getPayload()));
      }
    }
  }

  private void syncActionStates(List<ActionState> lastActions, Action[] newActions, Consumer<ActionState> consumer) {
    Arrays.stream(newActions)
        .filter(action -> lastActions.stream()
            .noneMatch(actionState -> actionState.getActionID().equals(action.getActionID())))
        .forEach(action -> {
          ActionState actionState = new ActionState(action);
          consumer.accept(actionState);
        });
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

  }

  public static <T> T[] addElement(T[] original, T element) {
    // Create a new array of the same type
    T[] newArray = Arrays.copyOf(original, original.length + 1);

    // Add the new element at the end
    newArray[original.length] = element;

    return newArray;
  }

  private void setStateLoaded() {
    Load load = new Load();
    load.setLoadType(DEFAULT_MQTT_LOAD_TYPE);
    state.setLoads(new Load[] { load });
    logger.info("{} - Updated MQTT state loads: [{}]", name, DEFAULT_MQTT_LOAD_TYPE);
  }

  private void setStateUnloaded() {
    state.setLoads(new Load[0]);
    logger.info("{} - Updated MQTT state loads: []", name);
  }

  /**
   * Shutdown and cleanup all resources
   */
  public void shutdown() {
    try {
      // Cancel timers
      if (statePublishTimer != null) {
        statePublishTimer.cancel();
      }
      if (chargingTimer != null) {
        chargingTimer.cancel();
      }

      // Shutdown executor services
      if (chargingExecutor != null) {
        chargingExecutor.shutdown();
        try {
          if (!chargingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            chargingExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          chargingExecutor.shutdownNow();
        }
      }

      if (actionExecutor != null) {
        actionExecutor.shutdown();
        try {
          if (!actionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            actionExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          actionExecutor.shutdownNow();
        }
      }

      if (messageProcessingExecutor != null) {
        messageProcessingExecutor.shutdown();
        try {
          if (!messageProcessingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            messageProcessingExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          messageProcessingExecutor.shutdownNow();
        }
      }

      // Disconnect MQTT
      if (mqttClient != null && mqttClient.isConnected()) {
        mqttClient.disconnect();
        mqttClient.close();
      }

      // Clear collections
      nodes.clear();
      edges.clear();
      nodeActionStatesMap.clear();
      edgeActionStatesMap.clear();
      instantActionStates.clear();

      logger.info("{} - Resources cleaned up successfully", name);
    } catch (Exception e) {
      logger.error("{} - Error during shutdown", name, e);
    }
  }
}
