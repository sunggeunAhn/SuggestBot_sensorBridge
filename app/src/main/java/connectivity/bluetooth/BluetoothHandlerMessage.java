package connectivity.bluetooth;

/**
 * Created by PCPC on 2018-03-29.
 */

public class BluetoothHandlerMessage {
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECT = 6;
    public static final int MESSAGE_VIBRATE = 7;
    public static final int MESSAGE_END = 8;
    public static final int MESSAGE_TOAST2 = 9;
    public static final int MESSAGE_PHASE = 10;
    public static final int MESSAGE_PHASE_SET_2 = 20;
    public static final int MESSAGE_TASK = 11;
    public static final int MESSAGE_EYE_CONDITION = 12;
    public static final int MESSAGE_INPUT = 20;

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "Bluetooth";
}
