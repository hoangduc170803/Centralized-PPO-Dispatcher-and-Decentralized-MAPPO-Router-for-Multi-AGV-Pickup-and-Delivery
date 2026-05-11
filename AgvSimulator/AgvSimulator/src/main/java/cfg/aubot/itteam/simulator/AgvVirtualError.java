package cfg.aubot.itteam.simulator;

import javax.swing.*;
import java.util.*;

public class AgvVirtualError extends JFrame {

    private final Map<String, String> errorsList = new HashMap<>();

    private Map<String, String> errors = new HashMap<>();

    private MovingListener listener;

    private Map<AgvError, Boolean> errorsEnum;

    public AgvVirtualError(MovingListener listener) {
        this();
        this.listener = listener;
    }

    public AgvVirtualError() {
        errorsList.put("OUTLINE", "Out line");
        errorsList.put("LOSS_GUIDELINE", "Loss communication with Guideline");
        errorsList.put("LOSS_CAN", "Telecommunication line errror");
        errorsList.put("OVERLOAD", "Overload");
        errorsList.put("E_STOP", "E-stop");
        errorsList.put("LOW_BATTERY", "Low battery");
        errorsList.put("WRONG_POINT", "Wrong point");

        errorsEnum = new TreeMap<>();
        for (AgvError value : AgvError.values()) {
            errorsEnum.put(value, false);
        }

        initComponents();
    }

    public void setListener(MovingListener listener) {
        this.listener = listener;
    }

    private void initComponents() {
        this.getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
//        for (Map.Entry<String, String> error : errorsList.entrySet()) {
//            JCheckBox checkBox = new JCheckBox(error.getKey(), false);
//            checkBox.addActionListener(l -> {
//                if (checkBox.isSelected()) {
//                    errors.put(error.getKey(), error.getKey());
//                } else {
//                    errors.remove(error.getKey());
//                }
//                listener.onError(encode());
//                listener.onError(errors);
//            });
//            this.add(checkBox);
//        }
        for (AgvError agvError : errorsEnum.keySet()) {
            JCheckBox checkBox = new JCheckBox(agvError.name(), false);
            checkBox.addActionListener(l -> {
                if (listener == null) {
                    return;
                }
                if (checkBox.isSelected()) {
                    errors.put(agvError.name(), agvError.name());
                } else {
                    errors.remove(agvError.name());
                }
                errorsEnum.put(agvError, checkBox.isSelected());
                listener.onError(encode());
                listener.onError(errors);
            });
            this.add(checkBox);
        }
        this.pack();

        this.setVisible(true);
    }

    public int encode() {
        int errors = 0;
        Iterator<SortedMap.Entry<AgvError, Boolean>> i = errorsEnum.entrySet().iterator();
        int count = 0;
        while (i.hasNext()) {
            boolean check = i.next().getValue();
            errors |= (check ? 1 : 0) << count++;
        }

        return errors;
    }

    public static enum AgvError {
        OUTLINE,
        LOSS_GUIDELINE,
        LOSS_CAN,
        OVERLOAD,
        E_STOP,
        LOW_BATTERY,
        WRONG_POINT,
    }
}
