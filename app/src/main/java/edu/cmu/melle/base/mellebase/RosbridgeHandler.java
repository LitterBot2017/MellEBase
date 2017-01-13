package edu.cmu.melle.base.mellebase;

// tweak to suit rosbridge

import android.util.Log;
import de.tavendo.autobahn.WebSocketHandler;

public class RosbridgeHandler extends WebSocketHandler {

    ROSBridge ros_thread;
    final static String TAG = "ROSBridgeHandler";

    public RosbridgeHandler(ROSBridge r_t) {
        ros_thread = r_t;
    }

    /**
     * Fired when the WebSockets connection has been established.
     * After this happened, messages may be sent.
     */
    public void onOpen() {
        Log.d(TAG, "Status: Connected to ws://" + ros_thread.ipAddress + ":" + ros_thread.portNumber);
        ros_thread.canMessage = true;
    }

    /**
     * Fired when the WebSockets connection has deceased (or could
     * not established in the first place).
     *
     * @param code       Close code.
     * @param reason     Close reason (human-readable).
     */
    public void onClose(int code, String reason) {
        Log.d(TAG, "Connection lost.");
        ros_thread.canMessage = false;
    }

    /**
     * Fired when a text message has been received (and text
     * messages are not set to be received raw).
     *
     * @param payload    Text message payload or null (empty payload).
     */
    public void onTextMessage(String payload) {
        // receives JSONs in string form from ROS
        Log.d(TAG, "onTextMessage: " + payload);
    }

    /**
     * Fired when a text message has been received (and text
     * messages are set to be received raw).
     *
     * @param payload    Text message payload as raw UTF-8 or null (empty payload).
     */
    public void onRawTextMessage(byte[] payload) {
        // unused
        Log.d(TAG, "onRawTextMessage: " + payload.toString());
    }

    /**
     * Fired when a binary message has been received.
     *
     * @param payload    Binar message payload or null (empty payload).
     */
    public void onBinaryMessage(byte[] payload) {
        // unused
        Log.d(TAG, "onBinaryMessage: " + payload.toString());
    }

}