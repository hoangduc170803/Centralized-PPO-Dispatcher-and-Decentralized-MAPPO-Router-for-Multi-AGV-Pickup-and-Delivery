package cfg.aubot.itteam.simulator;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class VirtualTCPAgv extends VirtualAgv implements MovingListener {

    private int port;
    private ServerSocket server;
    Socket client;

    protected LinkedList<byte[]> orderQueue = new LinkedList<>();

    private int lastReceivedOrderId = 0;
    private int currentOrderId = 0;
    private int lastFinishedOrderId = 0;

    private Timer loopProcess = new Timer();


    private LinkedList<byte[]> loopOrderQueue = new LinkedList<>();

    private int errorCode = 0;

    private Map<String, String> errors = new HashMap<>();

    private AgvVirtualError errorManager;

    public VirtualTCPAgv(int port, String initialPosition) {
        this(port, initialPosition, null);
    }

    public VirtualTCPAgv(int port, String initialPosition, PositionSubscriber positionSubscriber) {
        super(positionSubscriber);
        this.port = port;
        this.position = initialPosition;
    }

    public void addErrorManager(AgvVirtualError errorManager) {
        this.errorManager = errorManager;
    }

    public void open() {
        try {
            server = new ServerSocket(port);
            System.out.println("Server is open on port " + port);
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(Runnable onSuccess) throws Exception {
        onSuccess.run();
    }

    @Override
    protected void setupAgv() {
        loopProcess = new Timer();
        loopProcess.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isLoop) {
                    try {
                        byte[] request = loopOrderQueue.peek();
                        processStepOrder(request);
                        loopOrderQueue.remove();
                        loopOrderQueue.add(request);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1);

        if (errorManager != null) {
            errorManager.setVisible(true);
        }

        while (true) {
            try {
                client = server.accept();
                System.out.println("Client " + client.getRemoteSocketAddress() + " connected");
                DataInputStream dis = new DataInputStream(client.getInputStream());
                while (true) {
                    byte[] request;
                    byte[] response = new byte[1];
                    dis.readByte();
                    int length = dis.readByte();
                    dis.readByte();
                    dis.readByte();
                    dis.readByte();
                    int type = dis.readByte();
                    request = new byte[length + 1];
                    dis.read(request);
                    switch (type) {
                        case 1:
                            response = createStateResponse(request);
                            break;
                        case 2:
                            response = processOrderRequest(request);
                            break;
                        case 3:
                            response = createErrorResponse(request);
                            break;
                        case 4:
                            response = createMovingResponse(request);
                            break;
                        case 5:
                            response = processRouteRequest(request);
                            break;
                        case 6:
                            response = processCurrentRouteRequest(request);
                            break;
                        case 7:
                            response = processSetRouteRequest(request);
                    }

                    BufferedOutputStream bos = new BufferedOutputStream(client.getOutputStream());
                    bos.write(response);
                    bos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Socket closed.");
            } finally {
                closeSocket(client);
            }
        }
    }

    @Override
    protected void processRequest() {
        while (!orderQueue.isEmpty()) {
            try {
                Thread.sleep(100);
                byte[] request = orderQueue.peek();
                processStepOrder(request);
                if (request[8] == 1) {
                    isLoop = true;
                }
                orderQueue.remove();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        operationState = 'I';
    }

    private byte[] createStateResponse(byte[] request) {
        byte[] response = new byte[47];
        byte[] positionByte = position.getBytes();
        byte[] nextPositionByte = nextPosition.getBytes();
        byte[] lastReceivedOrderIdByte = convertIntToByte2(lastFinishedOrderId);
        byte[] currentOrderIdByte = convertIntToByte2(currentOrderId);
        byte[] lastFinishedOrderIdByte = convertIntToByte2(lastFinishedOrderId);
        byte[] getCurrent = convertFloatTo4Bytes(current);
        byte[] getVoltage = convertFloatTo4Bytes(voltage);
        byte[] distanceByte = convertFloatTo4Bytes(distance);
        byte[] errorCodeByte = convertIntToByte2(errorCode);

        int i = 0;
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) (response.length - 7);
        response[i++] = (byte) ((response.length - 7) >> 8);
        response[i++] = (byte) ((response.length - 7) >> 16);
        response[i++] = (byte) ((response.length - 7) >> 24);
        response[i++] = (byte) 1;
        response[i++] = request[0];
        response[i++] = request[1];
        response[i++] = positionByte[0];
        response[i++] = positionByte[1];
        response[i++] = positionByte[2];
        response[i++] = positionByte[3];
        response[i++] = (byte) operationState;
        response[i++] = (byte) loadState;
        response[i++] = lastReceivedOrderIdByte[0];
        response[i++] = lastReceivedOrderIdByte[1];
        response[i++] = currentOrderIdByte[0];
        response[i++] = currentOrderIdByte[1];
        response[i++] = lastFinishedOrderIdByte[0];
        response[i++] = lastFinishedOrderIdByte[1];
        response[i++] = getVoltage[0];
        response[i++] = getVoltage[1];
        response[i++] = getVoltage[2];
        response[i++] = getVoltage[3];
        response[i++] = getCurrent[0];
        response[i++] = getCurrent[1];
        response[i++] = getCurrent[2];
        response[i++] = getCurrent[3];
        response[i++] = (byte) energyLevel;
        response[i++] = distanceByte[0];
        response[i++] = distanceByte[1];
        response[i++] = distanceByte[2];
        response[i++] = distanceByte[3];
        response[i++] = 0;
        response[i++] = nextPositionByte[0];
        response[i++] = nextPositionByte[1];
        response[i++] = nextPositionByte[2];
        response[i++] = nextPositionByte[3];
        response[i++] = errorCodeByte[0];
        response[i++] = errorCodeByte[1];
        response[i++] = 0;
        response[i++] = 0;
        response[i++] = 0;
        response[i++] = 0; // load
        response[i++] = 0; // charge
        response[i++] = getCheckSum(response);
        response[i++] = (byte) 0xFE;

//        System.out.print("State Response: ");
//        for (int i = 0; i < response.length - 2; i++) {
//            System.out.print(i + 2 + "|" + (int) response[i] + "  ");
//        }
//        System.out.println();
//        System.out.println(distance);

        return response;
    }

    private byte[] processOrderRequest(byte[] request) {
        lastReceivedOrderId = convertByte2ToInt(request[2], request[3]);
        orderQueue.add(request);
        if (request[8] != 0) {
            loopOrderQueue.add(request);
        } else if (isLoop) {
            loopOrderQueue.clear();
            isLoop = false;
        }
        return createOrderResponse(request);
    }

    private void processStepOrder(byte[] request) throws InterruptedException {
        currentOrderId = convertByte2ToInt(request[2], request[3]);
        operationState = 'M';
        String destination = new String(new byte[] {request[4], request[5], request[6], request[7]});
        nextPosition = destination;

        float process = 0;
        do {
            distance = process;
            process += 0.05;
            Thread.sleep(20);
        } while (process < 5);
        distance = 0;

        position = destination;
        nextPosition = "0000";
        char action = (char) request[6];
        if (action == 'L' || action == 'U') {
            operationState = 'A';
            Thread.sleep(1000);
            if (action == 'L') {
                loadState = 'F';
            } else {
                loadState = 'E';
            }
        }
        lastFinishedOrderId = currentOrderId;
        currentOrderId = 0;
        if (operationState == 'A') {
            operationState = 'I';
        }
    }

    private byte[] createOrderResponse(byte[] request) {
        byte[] response = new byte[12];
        byte[] lastReceivedOrderIdByte = convertIntToByte2(lastReceivedOrderId);
        int i = 0;
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) (response.length - 7);
        response[i++] = (byte) ((response.length - 7) >> 8);
        response[i++] = (byte) ((response.length - 7) >> 16);
        response[i++] = (byte) ((response.length - 7) >> 24);
        response[i++] = (byte) 2;
        response[i++] = request[0];
        response[i++] = request[1];
        response[i++] = lastReceivedOrderIdByte[0];
        response[i++] = lastReceivedOrderIdByte[1];
        response[i++] = getCheckSum(response);
        response[i++] = (byte) 0xFE;

//        System.out.print("Order Request: ");
//        for (int i = 0; i < request.length - 2; i++) {
//            System.out.print(i + 2 + "|" + (int) request[i] + "  ");
//        }
//        System.out.println();
        return response;
    }

    private byte[] processRouteRequest(byte[] request) {
        tempRoutes.setMapId(convertByte2ToInt(request[2], request[3]));
        Map<String, String> pointDirections = new HashMap<>();
        int i = 6;
        while (i + 5 <= request.length) {
            pointDirections.put(new String(new byte[]{
                    request[i++],
                    request[i++],
                    request[i++],
                    request[i++]
            }), String.valueOf(request[i++]));
        }
        tempRoutes.getRoutes().add(new WorkingRoutes.Route(request[5], pointDirections));
        boolean isDone = tempRoutes.getRoutes().size() == request[4];
        if (isDone) {
            routes = tempRoutes;
            tempRoutes = new WorkingRoutes();
        }

        return createRouteResponse(request, isDone);
    }

    private byte[] createRouteResponse(byte[] request, boolean isDone) {
        byte[] response = new byte[16];
        int i = 0;
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) (response.length - 7);
        response[i++] = (byte) ((response.length - 7) >> 8);
        response[i++] = (byte) ((response.length - 7) >> 16);
        response[i++] = (byte) ((response.length - 7) >> 24);
        response[i++] = 5;
        response[i++] = request[0];
        response[i++] = request[1];
        response[i++] = request[2];
        response[i++] = request[3];
        response[i++] = request[4];
        response[i++] = request[5];
        response[i++] = 1;
        response[i++] = (byte) (isDone ? 'S' : 'N');
        response[i++] = getCheckSum(response);
        response[i++] = (byte) 0xFE;

        return response;
    }

    private byte[] processCurrentRouteRequest(byte[] request) {
        return createCurrentRouteResponse(request);
    }

    private byte[] processSetRouteRequest(byte[] request) {
        Map<String, String> pointActions = new HashMap<>();
        int mapId = convertByte2ToInt(request[2], request[3]);
        int routeId = request[4];
        WorkingRoutes.Route foundRoute = routes.getRoutes().stream()
                .filter(route -> route.getId() == routeId)
                .findFirst()
                .orElse(null);
        if (mapId == routes.getMapId() && foundRoute != null) {
            int i = 6;
            while (i + 5 <= request.length) {
                pointActions.put(new String(new byte[]{
                        request[i++],
                        request[i++],
                        request[i++],
                        request[i++]
                }), new String(new byte[] {request[i++]}));
            }
            routes.setCurrentRoute(foundRoute);
            routes.setPointActions(pointActions);
        }

        return createCurrentRouteResponse(request);
    }

    private byte[] createCurrentRouteResponse(byte[] request) {
        int length = 7 + routes.getPointActions().size() * 7; // 4 for point, 1 for action, 2 for time stop
        byte[] response = new byte[length + 7];
        int i = 0;
        byte[] mapIdByte = convertIntToByte2(routes.getMapId());
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) length;
        response[i++] = (byte) (length >> 8);
        response[i++] = (byte) (length >> 16);
        response[i++] = (byte) (length >> 24);
        response[i++] = 6;
        response[i++] = request[0];
        response[i++] = request[1];
        response[i++] = mapIdByte[0];
        response[i++] = mapIdByte[1];
        response[i++] = (byte) (routes.getCurrentRoute() != null ? routes.getCurrentRoute().getId() : -1);
        response[i++] = (byte) routes.getPointActions().size();
        for (Map.Entry<String, String> pa : routes.getPointActions().entrySet()) {
            byte[] pointByte = pa.getKey().getBytes();
            response[i++] = pointByte[0];
            response[i++] = pointByte[1];
            response[i++] = pointByte[2];
            response[i++] = pointByte[3];
            response[i++] = (byte) pa.getValue().charAt(0);
            response[i++] = 0;
            response[i++] = 0;
        }
        response[i++] = getCheckSum(response);
        response[i++] = (byte) 0xFE;

        return response;
    }

    private byte[] convertIntToByte2(int x) {
        byte[] result = new byte[2];
        result[1] = (byte) (x >> 8);
        result[0] = (byte) x;
        return result;
    }
    public byte[] convertFloatTo4Bytes(double value) {
        int intBits = Float.floatToIntBits((float) value);
        byte[] result = new byte[4];
        result[3] = (byte) (intBits >> 24);
        result[2] = (byte) (intBits >> 16);
        result[1] = (byte) (intBits >> 8);
        result[0] = (byte) (intBits >> 0);

        return result;
    }

    private byte[] createErrorResponse(byte[] request) {
        byte[] response = new byte[210];
        int i = 0;
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) (response.length - 7);
        response[i++] = (byte) ((response.length - 7) >> 8);
        response[i++] = (byte) ((response.length - 7) >> 16);
        response[i++] = (byte) ((response.length - 7) >> 24);
        response[i++] = (byte) 3;
        response[i++] = request[0];
        response[i++] = request[1];
        byte[] errorsByte = getErrorsInBytes();
        for (byte b : errorsByte) {
            response[i++] = b;
        }
        response[request.length - 1] = 3;
        response[response.length - 2] = getCheckSum(response);

        return response;
    }

    private byte[] createMovingResponse(byte[] request) {
        byte[] response = new byte[10];
        int i = 0;
        response[i++] = (byte) 0xFD;
        response[i++] = (byte) (response.length - 7);
        response[i++] = (byte) ((response.length - 7) >> 8);
        response[i++] = (byte) ((response.length - 7) >> 16);
        response[i++] = (byte) ((response.length - 7) >> 24);
        response[i++] = (byte) 4;
        response[i++] = request[0];
        response[i++] = request[1];
        response[i++] = getCheckSum(response);
        response[i++] = (byte) 0xFE;

        return response;
    }

    private int convertByte2ToInt(byte b1, byte b2) {
        return (b2 & 255) << 8 | b1 & 255;
    }

    public static int convertByteArrayToInt4(byte[] bytes) {
        return ((bytes[0] & 0xFF)) |
                ((bytes[1] & 0xFF) << 8) |
                ((bytes[2] & 0xFF) << 16) |
                ((bytes[3] & 0xFF) << 24);
    }

    public static byte getCheckSum(byte[] rawContent) {
        requireNonNull(rawContent, "rawContent");
        int cs = 0;
        int length = convertByteArrayToInt4(new byte[]{rawContent[1], rawContent[2], rawContent[3], rawContent[4]});
        for (int i = 0; i < length; i++) {
            cs ^= rawContent[5 + i];
        }
        return (byte) cs;
    }

    public void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDistanceChange(float distance) {
        this.distance = distance;
    }

    @Override
    public void onError(Map<String, String> errors) {
//        if (!errors.isEmpty()) {
//            if (operationState != 'E') previousStateBeforeError = operationState;
////            if (orderProcess.isAlive()) orderProcess.interrupt();
////            if (loopProcess.isAlive()) loopProcess.interrupt();
//            operationState = 'E';
//        } else {
//            operationState = previousStateBeforeError;
////            if (orderProcess.isInterrupted()) orderProcess.start();
////            if (loopProcess.isInterrupted() && isLoop) loopProcess.start();
//        }
        this.errors = errors;
    }

    @Override
    public void onError(int errors) {
        if (errorCode != 0) {
            if (operationState != 'E') previousStateBeforeError = operationState;
//            if (orderProcess.isAlive()) orderProcess.interrupt();
//            if (loopProcess.isAlive()) loopProcess.interrupt();
            operationState = 'E';
        } else {
            operationState = previousStateBeforeError;
//            if (orderProcess.isInterrupted()) orderProcess.start();
//            if (loopProcess.isInterrupted() && isLoop) loopProcess.start();
        }
        this.errorCode = errors;
    }

    private byte[] getErrorsInBytes() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> error : errors.entrySet()) {
            builder.append(error.getKey()).append("|").append(error.getValue()).append(";");
        }
        return builder.toString().getBytes();
    }
}
