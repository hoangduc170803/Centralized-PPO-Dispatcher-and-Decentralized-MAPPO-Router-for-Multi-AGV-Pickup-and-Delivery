# AGV Simulator

Phần mềm giả lập đội xe AGV (Automated Guided Vehicle) để phát triển và kiểm thử hệ thống quản lý đội xe (FMS) mà không cần xe thật.

---

## Mục đích

Trong nhà kho, nhà máy hiện đại, xe AGV tự động vận chuyển hàng hóa và được điều phối bởi một phần mềm FMS. Khi phát triển FMS, việc dùng xe thật để test rất tốn kém và bất tiện.

**AgvSimulator** đóng vai là đội xe ảo: kết nối với FMS qua MQTT y hệt xe thật, FMS không phân biệt được.

```
FMS  ←─ MQTT ─→  MQTT Broker (localhost:1883)  ←─ MQTT ─→  AgvSimulator
```

---

## Yêu cầu

- Java 21
- Maven
- MQTT Broker chạy tại `localhost:1883`

---

## Build & Chạy

```bash
# Compile
mvn compile

# Đóng gói
mvn package

# Chạy (từ IDE hoặc Maven)
mvn exec:java -Dexec.mainClass="cfg.aubot.itteam.simulator.Main"
```

---

## Cấu hình mặc định

| Thông số | Giá trị |
|---------|---------|
| MQTT Broker | `tcp://localhost:1883` |
| Username | `fms` |
| Password | `Aubot@2025` |
| Topic prefix | `aubotagv/2.0.0/AUBOT/<serialNumber>` |
| Giao thức | VDA5050 v2.0.0 |
| Tần suất publish State | 200ms (5Hz) |

---

## Kiến trúc tổng thể

```
┌──────────────────────────────────────────────────┐
│              FMS (Fleet Management System)        │
└───────────────────────┬──────────────────────────┘
                        │ JSON / MQTT
┌───────────────────────▼──────────────────────────┐
│           MQTT Broker (localhost:1883)            │
└───────────────────────┬──────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────┐
│                  AgvSimulator                     │
│  VirtualMqttAgv V01  VirtualMqttAgv V02  ...     │
│  (V01 tại 3027)      (V02 tại 0047)              │
└──────────────────────────────────────────────────┘
```

### Cây thừa kế class

```
Thread
  └── VirtualAgv  (abstract)
        ├── VirtualMqttAgv   ← đang dùng chính (VDA5050 qua MQTT)
        └── VirtualTCPAgv    ← giao thức nhị phân cũ qua TCP
```

---

## Các thành phần chính

### `Main.java`

Khởi động 12 xe ảo (V01–V12), mỗi xe có tên và điểm xuất phát riêng:

```java
new VirtualMqttAgv("V01", "3027", onVehiclePositionChange).open();
new VirtualMqttAgv("V02", "0047", onVehiclePositionChange).open();
// ...
```

`agvPointMap` là bản đồ vị trí dùng chung giữa tất cả xe, dùng để phát hiện va chạm.

---

### `VirtualAgv.java` — Lớp cha

Chứa trạng thái cốt lõi của mọi xe:

| Biến | Ý nghĩa |
|------|---------|
| `position` | nodeId hiện tại, vd `"0047"` |
| `operationState` | `I`=idle, `M`=moving, `A`=action, `E`=error |
| `loadState` | `E`=empty (không hàng), `F`=full (đang chở) |
| `energyLevel` | % pin (0–100) |
| `distance` | tiến trình di chuyển trên đoạn đường hiện tại (0–1) |

---

### `VirtualMqttAgv.java` — Trái tim của project

#### MQTT Topics

| Topic | Chiều | Nội dung |
|-------|-------|---------|
| `.../order` | FMS → Xe | Lệnh di chuyển (Order) |
| `.../instantActions` | FMS → Xe | Lệnh tức thì (dừng, sạc, hủy...) |
| `.../state` | Xe → FMS | Trạng thái xe (publish 200ms/lần) |
| `.../connection` | Xe → Broker | Online / Offline (retained) |

#### Luồng xử lý Order

```
FMS gửi Order → messageArrived() → messageProcessingExecutor
  └─ processMessage():
       1. Validate: xe phải idle, node đầu = vị trí hiện tại, sequenceId hợp lệ
       2. Thêm nodes và edges vào hàng đợi
       3. Map actions từ order sang ActionState

processRequest() (mỗi 100ms):
  1. Lấy node đầu tiên trong hàng đợi (destination)
  2. Kiểm tra va chạm: nếu xe khác đang ở destination → chờ
  3. Di chuyển qua Edge: distance tăng 0 → 1 (mỗi 100ms += maxSpeed * 0.005)
  4. Đến nơi: cập nhật position, thực hiện actions, remove node khỏi hàng đợi
```

#### Instant Actions được hỗ trợ

| actionType | Hành động |
|-----------|-----------|
| `cancelOrder` | Hủy toàn bộ lệnh, giữ lại edge đang đi dở |
| `startPause` | Dừng xe sau edge hiện tại |
| `stopPause` | Tiếp tục di chuyển |
| `stopCharging` | Tắt sạc sau 3 giây |
| `setPinLevel` | Đặt cứng % pin (0–100) |

#### Tính toán tọa độ

Trong khi di chuyển giữa hai node, vị trí xe được nội suy tuyến tính:

```
x = sourceX + (destX - sourceX) * progress
y = sourceY + (destY - sourceY) * progress
```

Góc hướng xe (`theta`) tính theo thứ tự ưu tiên:
1. `edge.orientation` (nếu có)
2. `nodePosition.theta` của node đích
3. Tính từ vector hướng di chuyển: `atan2(dx, dy)`

> **Lưu ý:** Quy ước góc trong simulator: `theta = 0` hướng lên Y+ (khác thông thường). Hướng X+ tương ứng `+π/2`.  
> Edge có `maxSpeed < 0` nghĩa là xe đi **lùi**.

---

### `telegrams/vda5050/` — Mô hình dữ liệu VDA5050

Toàn bộ schema của chuẩn VDA5050 v2.0.0, mỗi file là một kiểu dữ liệu:

```
Order
  ├── Node[]       điểm dừng trên lộ trình
  │     ├── nodeId          mã điểm (vd: "0047")
  │     ├── released        true = được phép đi vào
  │     ├── sequenceId      thứ tự: 0, 2, 4, 6...
  │     ├── nodePosition    tọa độ thực {x, y, theta, mapId}
  │     └── actions[]       việc làm khi đến đây
  └── Edge[]       đoạn đường nối giữa các node
        ├── startNodeId, endNodeId
        ├── maxSpeed        tốc độ tối đa (âm = đi lùi)
        ├── released        true = được phép đi
        ├── sequenceId      thứ tự: 1, 3, 5, 7...
        └── actions[]       việc làm khi đi trên đoạn này

State             trạng thái xe gửi về FMS (200ms/lần)
  ├── lastNodeId, driving, paused
  ├── nodeStates[], edgeStates[]    còn bao nhiêu việc phải làm
  ├── actionStates[]                trạng thái từng action
  ├── batteryState                  {charge%, voltage, isCharging}
  ├── agvPosition                   {x, y, theta, mapId}
  └── errors[]                      danh sách lỗi hiện tại

InstantActions    lệnh tức thì từ FMS
Connection        trạng thái kết nối (retained MQTT)
```

> **Tại sao sequenceId xen kẽ?**  
> `Node(0) → Edge(1) → Node(2) → Edge(3) → Node(4)...`  
> Giúp FMS biết chính xác xe đang ở bước nào khi update order giữa chừng.

---

### `VirtualTCPAgv.java` — Giao thức nhị phân cũ

Xe này mở `ServerSocket`, FMS kết nối TCP trực tiếp. Định dạng gói tin:

```
[0xFD] [length 4 bytes] [type 1 byte] [seqId 2 bytes] [payload] [checksum] [0xFE]
```

| type | Ý nghĩa |
|------|---------|
| 1 | Hỏi trạng thái xe |
| 2 | Gửi lệnh di chuyển |
| 3 | Hỏi danh sách lỗi |
| 4 | Hỏi trạng thái đang di chuyển |
| 5 | Upload bản đồ route |
| 6 | Hỏi route hiện tại |
| 7 | Đặt route cho xe |

> Không được sử dụng trong `Main.java` hiện tại. Đây là phiên bản trước khi chuyển sang VDA5050.

---

### `AgvVirtualError.java` — Công cụ inject lỗi thủ công

Cửa sổ Swing với các checkbox để developer kích hoạt lỗi giả lập trong khi test:

```
☐ OUTLINE          xe ra khỏi đường kẻ
☐ LOSS_GUIDELINE   mất tín hiệu dẫn đường
☐ LOSS_CAN         mất kết nối CAN bus
☐ OVERLOAD         quá tải
☐ E_STOP           nút dừng khẩn cấp
☐ LOW_BATTERY      pin yếu
☐ WRONG_POINT      sai điểm
```

Cách dùng:
```java
agv.setErrorManager(new AgvVirtualError());
```

---

### `telegrams/predefined/` — Giao thức MQTT cũ (legacy)

Format tin nhắn trước khi áp dụng VDA5050. Không được dùng bởi `VirtualMqttAgv` hiện tại, chỉ giữ lại để tham khảo.

---

## Luồng chạy đầy đủ (ví dụ)

```
1. main() khởi động 12 xe ảo

2. Mỗi xe:
   └─ connect MQTT → publish "ONLINE" (retained)
   └─ bật timer publish State mỗi 200ms
   └─ subscribe topic .../order và .../instantActions
   └─ processRequest() chạy mỗi 100ms (idle vì nodes=[])

3. FMS gửi Order vào aubotagv/2.0.0/AUBOT/V01/order:
   {
     "orderId": "order-001",
     "nodes": [
       { "nodeId": "3027", "sequenceId": 0, "released": true },
       { "nodeId": "0047", "sequenceId": 2, "released": true }
     ],
     "edges": [
       { "edgeId": "e1", "startNodeId": "3027", "endNodeId": "0047",
         "sequenceId": 1, "released": true, "maxSpeed": 1.5 }
     ]
   }

4. processRequest() xử lý:
   ├─ kiểm tra va chạm: "0047" trống → ok
   ├─ di chuyển 3027 → 0047 (distance 0 → 1, mỗi 100ms += 0.0075)
   └─ đến "0047": cập nhật position, hoàn thành Order

5. State timer liên tục gửi về FMS:
   { "lastNodeId": "0047", "driving": true, "batteryState": {"batteryCharge": 99}, ... }
```

---

## Thêm xe mới

Mở `Main.java` và thêm dòng:

```java
new VirtualMqttAgv("V13", "0118", onVehiclePositionChange).open();
```

Tham số:
- **Tên xe**: phải là duy nhất, dùng làm MQTT clientId và serialNumber
- **Điểm xuất phát**: nodeId 4 ký tự, phải tồn tại trong bản đồ FMS
- **positionSubscriber**: luôn truyền `onVehiclePositionChange` để bật tính năng phát hiện va chạm

---

## Logging

Cấu hình trong `src/main/resources/log4j2.properties`. Mặc định in ra console, level INFO.

```properties
rootLogger.level = info
appender.console.layout.pattern = [%d{yyyy-MM-dd HH:mm:ss}] [%p] [%t] %c{1} - %m%n
```

Để bật file log, bỏ comment phần `File Appender` trong file cấu hình.
