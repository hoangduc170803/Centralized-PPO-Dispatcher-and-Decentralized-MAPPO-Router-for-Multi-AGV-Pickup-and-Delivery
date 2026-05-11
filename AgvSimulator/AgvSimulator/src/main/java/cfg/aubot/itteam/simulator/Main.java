package cfg.aubot.itteam.simulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
//    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        //VirtualAgv agv1 = new VirtualMqttAgv("Vehicle-01", 2);
        //VirtualAgv agv2 = new VirtualMqttAgv("Vehicle-02", 4);
//        VirtualTCPAgv agv1 = new VirtualTCPAgv(2020, "0001");
//        VirtualTCPAgv agv2 = new VirtualTCPAgv(2021, "0000");
//        VirtualTCPAgv agv3 = new VirtualTCPAgv(2022, 75);
//        VirtualTCPAgv agv4 = new VirtualTCPAgv(2023, 76);
//        VirtualTCPAgv agv5 = new VirtualTCPAgv(2024, 77);
//        VirtualTCPAgv agv6 = new VirtualTCPAgv(2025, 78);
//        AgvVirtualError errorManager = new AgvVirtualError(agv1);
//        agv1.addErrorManager(errorManager);
//        agv1.open();
//        agv2.open();
//        agv3.open();
//        agv4.open();
//        agv5.open();
//        agv6.open();
        Map<VirtualAgv, String> agvPointMap = new ConcurrentHashMap<>();
        var onVehiclePositionChange = new VirtualAgv.PositionSubscriber() {
            @Override
            public void onPositionChange(VirtualAgv agv, String position) {
                agvPointMap.forEach((a, p) -> {
                    if (p.equals(position)) {
//                        logger.warn("AGV {}: Position {} is already locked by {}", agv.getName(), position, a.getName());
                    }
                });
                agvPointMap.put(agv, position);
            }
        };
// =====================================================================
// SMOKE TEST configuration — matches Demo-01.xml plant model on OpenTCS.
// Vehicle name is used as the MQTT serialNumber; the topic prefix becomes
// "aubotagv/v2/AUBOT/<vehicleName>/...". The OpenTCS vehicle must have:
//   vda5050:interfaceName = aubotagv
//   vda5050:manufacturer  = AUBOT
//   vda5050:serialNumber  = <vehicleName>
// for the kernel to subscribe to the matching topic.
// Initial position must be a Point name that exists in the loaded plant.
// =====================================================================
        String initialPoint = System.getProperty(
                "agv.simulator.initialPoint",
                System.getenv().getOrDefault("AGV_SIMULATOR_INITIAL_POINT", "Point-0001"));
        new VirtualMqttAgv("VDA5050-Vehicle-01", initialPoint, onVehiclePositionChange).open();
//      Add more vehicles here once the smoke test passes:
//      new VirtualMqttAgv("Vehicle-02", "Point-0003", onVehiclePositionChange).open();
//      new VirtualMqttAgv("Vehicle-03", "Point-0005", onVehiclePositionChange).open();
//      new VirtualMqttAgv("Vehicle-04", "Point-0007", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V13", "0118", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V14", "0119", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V15", "0120", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V16", "0121", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V17", "0122", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V18", "0420", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V19", "0430", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V20", "0440", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V21", "0450", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V22", "0460", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V23", "0470", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V24", "0480", onVehiclePositionChange).open();
//        new VirtualMqttAgv("V25", "0490", onVehiclePositionChange).open();
    }
}
