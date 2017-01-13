package edu.cmu.melle.base.mellebase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

public class ROSBridge {

    String portNumber;
    String ipAddress;

    private JSONObject advertisement;
    private JSONObject unAdvertisement;
    private JSONObject publication;
    private JSONObject subscription;
    private JSONObject unSubscription;
    private JSONObject serviceCall;
    private JSONObject serviceResponse;

    private JSONObject outerPayload;
    private JSONObject innerPayload;

    private String command;

    private static final String TAG = "ros_thread";

    private final WebSocketConnection mConnection = new WebSocketConnection();

    boolean canMessage;
    public boolean isActivated;


    public ROSBridge(String ip, String port){

        advertisement = new JSONObject();
        unAdvertisement = new JSONObject();
        publication = new JSONObject();
        subscription = new JSONObject();
        unSubscription = new JSONObject();
        serviceCall = new JSONObject();
        serviceResponse = new JSONObject();

        portNumber = port;
        ipAddress = ip;

        start();
    }

    // opens the websocket connection
    public void start() {

        final String wsuri = "ws://" + ipAddress + ":" + portNumber;
        Log.d(TAG, "Connecting to: " + wsuri);

        try {
            mConnection.connect(wsuri, new RosbridgeHandler(this)
                    {
                        // WARNING
                        // currently set up for "std_msgs/String"
                        // expand if other types are to be taken in
                        @Override
                        public void onTextMessage(String payload) {
                            try {
                                outerPayload = new JSONObject(payload);
                                innerPayload = new JSONObject(outerPayload.getString("msg"));
                                command = innerPayload.getString("data");
                                Log.d(TAG, "Command: " + command);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (NullPointerException n) {
                                Log.w(TAG, "eater_contol format error");
                                // instead of a million JSONObject "has" and "isNull" checks (for now)
                            }

                            if(command.equals("start"))
                                isActivated = true;
                            if(command.equals("stop"))
                                isActivated = false;
                            if(command.equals("yolo"))
                                Log.d("SWAG", "ROBOEATERS SO IMPOSSIBLY FRESH");
                        }
                    }
            );
        } catch (WebSocketException e) {
            Log.d(TAG, e.toString());
        }

    }

    // sends JSON as text over websocket
    private boolean sendJSON(String topic, JSONObject transmission, String verb) {
        try {
            mConnection.sendTextMessage(transmission.toString());
        } catch (NullPointerException n) {
            Log.d(TAG, "Unable to " + verb + " topic: " + topic + ".");
            return false;
        }
        //Log.d(TAG, "Successfully able to " + verb + " topic: " + topic + ".");
        return true;
    }


    // informs the ROS master that you will publish messages to a topic (required)
    public boolean advertiseToTopic(String topic, String type)
    {
        Log.d("AUTOBAHN", "Attempting to advertise to topic: " + topic + ".");
        try {
            advertisement.put("op", "advertise");
            //advertisement.put("id", String);				// optional
            advertisement.put("topic", topic);				// topic: "eater_input"
            advertisement.put("type", type);	// for stringified/toString(ed) JSONs
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJSON(topic, advertisement, "advertise to");
    }

    // stop publishing messages to topic
    public boolean unadvertiseFromTopic(String topic)
    {
        try {
            unAdvertisement.put("op", "unadvertise");
            //unAdvertisement.put("id", String);		// optional
            unAdvertisement.put("topic", topic);		// "eater_input"
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJSON(topic, unAdvertisement, "unadvertise from");
    }


    // sends a rosbridge JSON containing a JSON-format ROS message.
    public boolean publishToTopic(String topic, JSONObject message)
    {
        try {
            publication.put("op", "publish");
            //publication.put("id", String);	// optional
            publication.put("topic", topic);
            publication.put("msg", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJSON(topic, publication, "publish to");
    }


    // informs the ROS master that you will accept messages that are published to
    // the specified topic.
    public boolean subscribeToTopic(String topic, String type)
    {
        try {
            subscription.put("op", "subscribe");
            subscription.put("topic", topic);
            subscription.put("type", type);				// topics have type specified by default
            //subscription.put("throttle_rate, int)			// min time (ms) between messages default: 0
            //subscription.put("queue_length, int);			// size of message buffer (due to throttle) default: 1
            //subscription.put("fragment_size, int);		// max message size before being fragmented
            //subscription.put("compression, String);		// "none" or "png"
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJSON(topic, subscription, "subscribe to");
    }


    // stop accepting messages from topic
    public boolean unSubscribeFromTopic(String topic)
    {
        try {
            unSubscription.put("op", "unsubscribe");
            //unSubscription.put("id", String);			// optional
            unSubscription.put("topic", topic);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sendJSON(topic, unSubscription, "unsubscribe from");
    }

    // calls ROS service
    public void callService(String service, List<String> args)
    {
        try {
            serviceCall.put("op", "call_service");
            //serviceCall.put("id", String);				// optional
            serviceCall.put("service", service);
            //serviceCall.put("args, list<json>);			// if service has no args, none need be provided
            //serviceCall.put("fragment_size, int);			// max message size before fragmentation
            //serviceCall.put("compression, String);		// "none" or "png"
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // send(serviceCall);
    }

    // responds to a service call send from a ROS node
    public void respondToService (String service, List<String> args)
    {
        try {
            serviceResponse.put("op", "service_response");
            //serviceResponse.put("id", String);				// optional
            serviceResponse.put("service", service);
            //serviceResponse.put("values, list<json>);			// JSON return values
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // do something
    }

    // disconnects, though seems to do so unsuccessfully
    public void end() {
        if (mConnection.isConnected())
            mConnection.disconnect();
    }

    public boolean getIsActivated() {
        return isActivated;
    }

}
