package com.yen.androidappthesisyen.pushnotificationlistener;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;
import com.yen.androidappthesisyen.R;
import com.yen.androidappthesisyen.main.MainActivity;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
 * An example of how to implement an MQTT client in Android, able to receive
 *  push notifications from an MQTT message broker server.
 *
 *  Dale Lane (dale.lane@gmail.com)
 *    28 Jan 2011
 */

public class MQTTService extends Service implements MqttSimpleCallback {

    private static final String LOG_TAG = MQTTService.class.getName();

    private static int enumeratorTotal = -1;


    // TODO
//    momenteel is de hashmap nog opgeslaan in het MQTTSERVICE OBJECT gewoon: bij orientatieaanpassing of HEROPSTART SERVICE zal het verdwijnen!
//    HEROPSTART SERVICE gebeurt ook als een broker een nieuwe IP heeft en dat moet ingegeven worden!
    // OVERBODIG WNT NU IN SHAREDPREFERENCES: private Map<String, Set<String>> mapSystemIDToSupportedGestures; // TODO gaan we dit bijhouden via SharedPreferences ofzo of niet? is wrsl niet nodig? OF TOCH WEL WANT KAN ER DAN AAN UIT ANDERE ACTIVITY/FRAGMENT
    // key = system ID - value = comma separated list of supported gestures for the specific systemID
    // STAAT OOK IN 3Dgesturefragment dus wijzingen BIJ ALLEBEI DOORVOEREN
    private void addSupportedGesture(String systemID, String gestureToBeAdded) {

        Map<String, String> savedMap = getMapSupportedGestures();
        if (savedMap == null) {
            Log.w(LOG_TAG, "SAVEDMAP IS NULL");
        }


        String concatenatedGestures = savedMap.get(systemID);
//        if(concatenatedGestures == null){
//            Log.w(LOG_TAG, "concatenatedGestures IS NULL");
//        }

        String newConcatenatedString = "";

        if (concatenatedGestures != null) {

            String[] arrayGestures = concatenatedGestures.split(";");
            Set<String> setGestures = new HashSet<String>(Arrays.asList(arrayGestures));
            // adding new gesture
            setGestures.add(gestureToBeAdded);
            // recreate concatenated string from new set
            newConcatenatedString = TextUtils.join(";", setGestures);

            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);

        } else {

            // TODO zien of niet met ";" direct moet.
            newConcatenatedString = gestureToBeAdded;

            Log.w(LOG_TAG, "newConcatenatedString " + newConcatenatedString);
        }


        savedMap.put(systemID, newConcatenatedString);


        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            JSONObject jsonObject = new JSONObject(savedMap);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("my_map").commit();
            editor.putString("my_map", jsonString);
            editor.commit();
        }
    }

    // STAAT OOK IN 3Dgesturefragment dus wijzingen BIJ ALLEBEI DOORVOEREN
    private Map<String, String> getMapSupportedGestures() {

        Map<String, String> outputMap = new HashMap<String, String>();

        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences("com.yen.androidappthesisyen.system_id_to_supported_gestures", Context.MODE_PRIVATE);

        try {
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getString("my_map", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    String value = (String) jsonObject.get(key); // a value = comma separated list of supported gestures for the specific systemID
                    outputMap.put(key, value);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputMap;
    }


    // DEFAULT:
//    public MQTTService() {
//    }
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }


    /************************************************************************/
    /*    CONSTANTS                                                         */
    /**
     * ********************************************************************
     */

    // something unique to identify your app - used for stuff like accessing
    //   application preferences
    public static final String APP_ID = "com.dalelane.pushnotificationlistener";

    // constants used to notify the Activity UI of received messages
    public static final String MQTT_MSG_RECEIVED_INTENT = "com.dalelane.pushnotificationlistener.MSGRECVD";
    public static final String MQTT_MSG_RECEIVED_TOPIC = "com.dalelane.pushnotificationlistener.MSGRECVD_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG = "com.dalelane.pushnotificationlistener.MSGRECVD_MSGBODY";

    // constants used to tell the Activity UI the connection status
    public static final String MQTT_STATUS_INTENT = "com.dalelane.pushnotificationlistener.STATUS";
    public static final String MQTT_STATUS_MSG = "com.dalelane.pushnotificationlistener.STATUS_MSG";

    // constant used internally to schedule the next ping event
    public static final String MQTT_PING_ACTION = "com.dalelane.pushnotificationlistener.PING";

    // constants used by status bar notifications
    public static final int MQTT_NOTIFICATION_ONGOING = 1;
    public static final int MQTT_NOTIFICATION_UPDATE = 2;

    // constants used to define MQTT connection status
    public enum MQTTConnectionStatus {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone
        //     does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested
        //     disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user
        //     has disabled data access
        NOTCONNECTED_UNKNOWNREASON          // failed to connect for some reason
    }

    // MQTT constants
    public static final int MAX_MQTT_CLIENTID_LENGTH = 22; // TODO mag dit hoger? NEE WANT: MQTT spec doesn't allow client ids longer than 23 chars

    /************************************************************************/
    /*    VARIABLES used to maintain state                                  */
    /**
     * ********************************************************************
     */

    // status of MQTT client connection
//    private MQTTConnectionStatus connectionStatus_1 = MQTTConnectionStatus.INITIAL;
//    private MQTTConnectionStatus connectionStatus_2 = MQTTConnectionStatus.INITIAL;
    private List<MQTTConnectionStatus> listConnectionStatus = null;


    /************************************************************************/
    /*    VARIABLES used to configure MQTT connection                       */
    /**
     * ********************************************************************
     */

    // taken from preferences
    //    host name of the server we're receiving push notifications from
//    private String brokerHostName_1 = "";
    // TOEGEVOEGD:
//    private String brokerHostName_2 = "";
    private List<String> listBrokerHostName = null;

    // taken from preferences
    //    topic we want to receive messages about
    //    can include wildcards - e.g.  '#' matches anything
    private String topicNameAccelStream = "";
    private String topicNameGesturePusher = "";


    // defaults - this sample uses very basic defaults for it's interactions
    //   with message brokers
    private int brokerPortNumber = 1883;
    // TODO port 1883 zal bij elke broker gebruikt worden dus moet geen extra?
    private MqttPersistence usePersistence = null;
    //    private boolean cleanStart_1 = false;
//    private boolean cleanStart_2 = false;
    private List<Boolean> listCleanStart = null;
    private int[] qualitiesOfService = {2}; // was {0};

    //  how often should the app ping the server to keep the connection alive?
    //
    //   too frequently - and you waste battery life
    //   too infrequently - and you wont notice if you lose your connection
    //                       until the next unsuccessfull attempt to ping
    //
    //   it's a trade-off between how time-sensitive the data is that your
    //      app is handling, vs the acceptable impact on battery life
    //
    //   it is perhaps also worth bearing in mind the network's support for
    //     long running, idle connections. Ideally, to keep a connection open
    //     you want to use a keep alive value that is less than the period of
    //     time after which a network operator will kill an idle connection
    private short keepAliveSeconds = 20 * 60;


    // This is how the Android client app will identify itself to the
    //  message broker.
    // It has to be unique to the broker - two clients are not permitted to
    //  connect to the same broker using the same client ID.
    private String mqttClientId = null;
    // TODO ============== hiervoor ook 2de regel nodig?
    // Momenteel dus nog niet gedaan: nu dus 1 clientID maar op zich is niet erg want die clientid telkens bij andere broker gebruikt en dus uniek PER broker?


    /************************************************************************/
    /*    VARIABLES  - other local variables                                */
    /**
     * ********************************************************************
     */
    // connection to the message broker
//    private IMqttClient mqttClient_1 = null;
//    private IMqttClient mqttClient_2 = null;
    private List<IMqttClient> listMQTTClient = null;

    // receiver that notifies the Service when the phone gets data connection
//    private NetworkConnectionIntentReceiver netConnReceiver_1;
//    private NetworkConnectionIntentReceiver netConnReceiver_2;
    private List<NetworkConnectionIntentReceiver> listNetConnReceiver = null;

    // receiver that notifies the Service when the user changes data use preferences
//    private BackgroundDataChangeIntentReceiver dataEnabledReceiver_1;
//    private BackgroundDataChangeIntentReceiver dataEnabledReceiver_2;
    private List<BackgroundDataChangeIntentReceiver> listDataEnabledReceiver = null;

    // receiver that wakes the Service up when it's time to ping the server
//    private PingSender pingSender_1;
//    private PingSender pingSender_2;
    private List<PingSender> listPingSender = null;

    /************************************************************************/
    /*    METHODS - core Service lifecycle methods                          */

    /**
     * ********************************************************************
     */

    // see http://developer.android.com/guide/topics/fundamentals.html#lcycles
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences enumSetting = getSharedPreferences("com.yen.androidappthesisyen.commands_receiver", Context.MODE_PRIVATE);
        int theEnum = enumSetting.getInt("enumerator", -1);
        this.enumeratorTotal = theEnum;


        listCleanStart = new ArrayList<>();
        listMQTTClient = new ArrayList<>();
        listNetConnReceiver = new ArrayList<>();
        listDataEnabledReceiver = new ArrayList<>();
        listPingSender = new ArrayList<>();
        listConnectionStatus = new ArrayList<>();
        listBrokerHostName = new ArrayList<>();
        listDataEnabledReceiver = new ArrayList<>();
        for (int i = 0; i < enumeratorTotal; i++) {
            listCleanStart.add(i, false);
            listMQTTClient.add(i, null);
            listNetConnReceiver.add(i, null);
            listDataEnabledReceiver.add(i, null);
            listPingSender.add(i, null);
            listConnectionStatus.add(i, MQTTConnectionStatus.INITIAL);
            listBrokerHostName.add(i, "");
            listDataEnabledReceiver.add(i, null);
        }


// OVERBODIG WNT NU VIA SHAREDPREFERENCES:
//        if(mapSystemIDToSupportedGestures == null){
//            mapSystemIDToSupportedGestures = new HashMap<>();
//        }


        // reset status variable to initial state
//        connectionStatus_1 = MQTTConnectionStatus.INITIAL;
//        connectionStatus_2 = MQTTConnectionStatus.INITIAL;

        for (int i = 0; i < enumeratorTotal; i++) {
            listConnectionStatus.set(i, MQTTConnectionStatus.INITIAL);
        }

        // create a binder that will let the Activity UI send
        //   commands to the Service
        mBinder = new LocalBinder<MQTTService>(this);

        // get the broker settings out of app preferences
        //   this is not the only way to do this - for example, you could use
        //   the Intent that starts the Service to pass on configuration values
        SharedPreferences settings = getSharedPreferences("com.yen.androidappthesisyen.user_detector", Context.MODE_PRIVATE);
//        brokerHostName_1 = settings.getString("ip_address_broker_1", "192.168.1.1"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.
//        brokerHostName_2 = settings.getString("ip_address_broker_2", "192.168.1.2"); // OF HIER dus checken of er al waarde is: INDIEN NIET: TOON DIALOOG VENSTER.

        for (int i = 0; i < enumeratorTotal; i++) {
            String search = "ip_address_broker_" + i;
            String defaultIP = "192.168.1." + (i + 2); // We do +2 so the first IP is 192.168.1.2 and above. 192.168.1.1 is in a lot of cases the network gateway.
            listBrokerHostName.set(i, settings.getString(search, defaultIP));
        }

        topicNameAccelStream = settings.getString("topic_accelstream", "accelstream/state"); // TODO hardcoden? of behouden want is zo meer generiek?
        topicNameGesturePusher = settings.getString("topics_gesturepusher", "gesturepusher/#"); // TODO hardcoden? of behouden want is zo meer generiek?

        // register to be notified whenever the user changes their preferences
        //  relating to background data use - so that we can respect the current
        //  preference
//        dataEnabledReceiver_1 = new BackgroundDataChangeIntentReceiver(1);
//        registerReceiver(dataEnabledReceiver_1, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
//        dataEnabledReceiver_2 = new BackgroundDataChangeIntentReceiver(2);
//        registerReceiver(dataEnabledReceiver_2, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));

        for (int i = 0; i < enumeratorTotal; i++) {
            BackgroundDataChangeIntentReceiver back = new BackgroundDataChangeIntentReceiver(i);
            listDataEnabledReceiver.set(i, back);
            registerReceiver(back, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
        }

        // define the connection to the broker
//        defineConnectionToBroker(1, brokerHostName_1);
//        defineConnectionToBroker(2, brokerHostName_2);
        Log.w(LOG_TAG, "enumeratorTotal op einde onCreate" + enumeratorTotal);
        for (int i = 0; i < enumeratorTotal; i++) {
            defineConnectionToBroker(i, listBrokerHostName.get(i));
        }

    }


    @Override
    public void onStart(final Intent intent, final int startId) {
        // This is the old onStart method that will be called on the pre-2.0
        // platform.  On 2.0 or later we override onStartCommand() so this
        // method will not be called.

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        }, "MQTTservice").start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {

        /*int extra = intent.getIntExtra("enumerator", -1);
        this.enumeratorTotal = extra;
        Log.w(LOG_TAG, "enumeratorTotal in ONSTARTCOMMAND " + extra);*/

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        }, "MQTTservice").start();

        Log.w(LOG_TAG, "------------------------- SERVICE WERD GESTART");


        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        return START_STICKY;
    }

    synchronized void handleStart(Intent intent, int startId) {


        /*int extra = intent.getIntExtra("enumerator", -1);
        this.enumeratorTotal = extra;
        Log.w(LOG_TAG, "enum aan begin HANDLESTART " + extra);*/

        // before we start - check for a couple of reasons why we should stop


        // TODO of toch door laten gaan zodra 1 broker connected is?

        /*
        // ======================== voor BROKER 1
        if (mqttClient_1 == null) {
            // we were unable to define the MQTT client connection, so we stop
            //  immediately - there is nothing that we can do
            stopSelf();
            return;
        }
        // ======================== voor BROKER 2
        if (mqttClient_2 == null) {
            // we were unable to define the MQTT client connection, so we stop
            //  immediately - there is nothing that we can do
            stopSelf();
            return;
        }
        */
        for (int i = 0; i < enumeratorTotal; i++) {
            if (listMQTTClient.get(i) == null) {
                // we were unable to define the MQTT client connection, so we stop
                //  immediately - there is nothing that we can do
                stopSelf();
                return;
            }
        }


        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getBackgroundDataSetting() == false) // respect the user's request not to use data!
        {

            // user has disabled background data
//            connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
//            connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
            for (int i = 0; i < enumeratorTotal; i++) {
                listConnectionStatus.set(i, MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);
            }


            // update the app to show that the connection has been disabled
            broadcastServiceStatus("Not connected - background data disabled");

            // we have a listener running that will notify us when this
            //   preference changes, and will call handleStart again when it
            //   is - letting us pick up where we leave off now
            return;

        }

        // the Activity UI has started the MQTT service - this may be starting
        //  the Service new for the first time, or after the Service has been
        //  running for some time (multiple calls to startService don't start
        //  multiple Services, but it does call this method multiple times)
        // if we have been running already, we re-send any stored data
//        rebroadcastStatus(1);
//        rebroadcastStatus(2);
        for (int i = 0; i < enumeratorTotal; i++) {
            rebroadcastStatus(i);
        }
        rebroadcastReceivedMessages();


        // ======================== voor BROKER 1

        // if the Service was already running and we're already connected - we
        //   don't need to do anything
        for (int i = 0; i < enumeratorTotal; i++) {

            if (isAlreadyConnected(i) == false) {
                // set the status to show we're trying to connect
//                connectionStatus_1 = MQTTConnectionStatus.CONNECTING;
                listConnectionStatus.set(i, MQTTConnectionStatus.CONNECTING);

                // we are creating a background service that will run forever until
                //  the user explicity stops it. so - in case they start needing
                //  to save battery life - we should ensure that they don't forget
                //  we're running, by leaving an ongoing notification in the status
                //  bar while we are running

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notification = new Notification(R.drawable.logo_yen,
                        "Commands Receiver",
                        System.currentTimeMillis());
                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                notification.flags |= Notification.FLAG_NO_CLEAR;
                Intent notificationIntent = new Intent(this, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setLatestEventInfo(this, "Commands Receiver", "Commands Receiver is running", contentIntent);
                nm.notify(MQTT_NOTIFICATION_ONGOING, notification);


                // before we attempt to connect - we check if the phone has a
                //  working data connection
                if (isOnline()) {
                    // we think we have an Internet connection, so try to connect
                    //  to the message broker
                    if (connectToBroker(i)) {
                        // we subscribe to a topic - registering to receive push
                        //  notifications with a particular key
                        // in a 'real' app, you might want to subscribe to multiple
                        //  topics - I'm just subscribing to one as an example
                        // note that this topicName could include a wildcard, so
                        //  even just with one subscription, we could receive
                        //  messages for multiple topics
                        subscribeToTopic(i, topicNameAccelStream);
                        // TODO dus hier nog zo'n subscribeToTopic toevoegen!
                        // OFWEL topicnaam aanpassen zodat onder 1 hoofdcategorie alle nodige topics zitten en dan via wildcard werken.
                        subscribeToTopic(i, topicNameGesturePusher);

                    }
                } else {
                    // we can't do anything now because we don't have a working
                    //  data connection
//                    connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
                    listConnectionStatus.set(i, MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);

                    // inform the app that we are not connected
                    broadcastServiceStatus("Waiting for network connection");
                }


            }

        }




        /*
        // ======================== voor BROKER 2

        // if the Service was already running and we're already connected - we
        //   don't need to do anything
        if (isAlreadyConnected(2) == false) {
            // set the status to show we're trying to connect
            connectionStatus_2 = MQTTConnectionStatus.CONNECTING;

            // we are creating a background service that will run forever until
            //  the user explicity stops it. so - in case they start needing
            //  to save battery life - we should ensure that they don't forget
            //  we're running, by leaving an ongoing notification in the status
            //  bar while we are running

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.logo_yen,
                    "Commands Receiver",
                    System.currentTimeMillis());
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notification.setLatestEventInfo(this, "Commands Receiver", "Commands Receiver is running", contentIntent);
            nm.notify(MQTT_NOTIFICATION_ONGOING, notification);


            // before we attempt to connect - we check if the phone has a
            //  working data connection
            if (isOnline()) {
                // we think we have an Internet connection, so try to connect
                //  to the message broker
                if (connectToBroker(2)) {
                    // we subscribe to a topic - registering to receive push
                    //  notifications with a particular key
                    // in a 'real' app, you might want to subscribe to multiple
                    //  topics - I'm just subscribing to one as an example
                    // note that this topicName could include a wildcard, so
                    //  even just with one subscription, we could receive
                    //  messages for multiple topics
                    subscribeToTopic(2, topicNameAccelStream);
                    // TODO dus hier nog zo'n subscribeToTopic toevoegen!
                    // OFWEL topicnaam aanpassen zodat onder 1 hoofdcategorie alle nodige topics zitten en dan via wildcard werken.
                    subscribeToTopic(2, topicNameGesturePusher);

                }
            } else {
                // we can't do anything now because we don't have a working
                //  data connection
                connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

                // inform the app that we are not connected
                broadcastServiceStatus("Waiting for network connection");
            }


        }
        */


        // ======================== voor BROKER 1

        // changes to the phone's network - such as bouncing between WiFi
        //  and mobile data networks - can break the MQTT connection
        // the MQTT connectionLost can be a bit slow to notice, so we use
        //  Android's inbuilt notification system to be informed of
        //  network changes - so we can reconnect immediately, without
        //  haing to wait for the MQTT timeout
        for (int i = 0; i < enumeratorTotal; i++) {

            if (listNetConnReceiver.get(i) == null) {
                listNetConnReceiver.set(i, new NetworkConnectionIntentReceiver(i));
                registerReceiver(listNetConnReceiver.get(i),
                        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

                Log.w(LOG_TAG, "NETCONRECEIVER 1 REGISTERED");

            }

        }


        /*
        // ======================== voor BROKER 2

        if (netConnReceiver_2 == null) {
            netConnReceiver_2 = new NetworkConnectionIntentReceiver(2);
            registerReceiver(netConnReceiver_2,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

            Log.w(LOG_TAG, "NETCONRECEIVER 2 REGISTERED");

        }
        */


        // ======================== voor BROKER 1

        // creates the intents that are used to wake up the phone when it is
        //  time to ping the server
        for (int i = 0; i < enumeratorTotal; i++) {
            if (listPingSender.get(i) == null) {
                listPingSender.set(i, new PingSender(i));
                registerReceiver(listPingSender.get(i), new IntentFilter(MQTT_PING_ACTION));
            }
        }


        /*
        // ======================== voor BROKER 2

        if (pingSender_2 == null) {
            pingSender_2 = new PingSender(2);
            registerReceiver(pingSender_2, new IntentFilter(MQTT_PING_ACTION));
        }
        */

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // disconnect immediately
//        disconnectFromBroker(1);
//        disconnectFromBroker(2);
        for (int i = 0; i < enumeratorTotal; i++) {
            disconnectFromBroker(i);
        }


        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");

        // try not to leak the listener
        for (int i = 0; i < enumeratorTotal; i++) {
            if (listDataEnabledReceiver.get(i) != null) {
                unregisterReceiver(listDataEnabledReceiver.get(i));
                listDataEnabledReceiver.set(i, null);
            }
        }

        /*
        if (dataEnabledReceiver_2 != null) {
            unregisterReceiver(dataEnabledReceiver_2);
            dataEnabledReceiver_2 = null;
        }
        */


        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }

        Log.w(LOG_TAG, "------------------------- SERVICE DESTROYED");
    }


    /************************************************************************/
    /*    METHODS - broadcasts and notifications                            */

    /**
     * ********************************************************************
     */

    // methods used to notify the Activity UI of something that has happened
    //  so that it can be updated to reflect status and the data received
    //  from the server
    private void broadcastServiceStatus(String statusDescription) {
        // inform the app (for times when the Activity UI is running /
        //   active) of the current MQTT connection status so that it
        //   can update the UI accordingly
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, String message) {
        // pass a message received from the MQTT server on to the Activity UI
        //   (for times when it is running / active) so that it can be displayed
        //   in the app GUI

        Log.w(LOG_TAG, "------------------------- TOPIC WAS " + topic);
        Log.w(LOG_TAG, "------------------------- MESSAGE WAS " + message);

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
        sendBroadcast(broadcastIntent);
    }

    // methods used to notify the user of what has happened for times when
    //  the app Activity UI isn't running
    private void notifyUser(String alert, String title, String body) {


        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.logo_yen, alert,
                System.currentTimeMillis());
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.MAGENTA;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(this, title, body, contentIntent);
        nm.notify(MQTT_NOTIFICATION_UPDATE, notification);


    }


    /************************************************************************/
    /*    METHODS - binding that allows access from the Activity            */
    /**
     * ********************************************************************
     */

    // trying to do local binding while minimizing leaks - code thanks to
    //   Geoff Bruckner - which I found at
    //   http://groups.google.com/group/cw-android/browse_thread/thread/d026cfa71e48039b/c3b41c728fedd0e7?show_docid=c3b41c728fedd0e7

    private LocalBinder<MQTTService> mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder<S> extends Binder {
        private WeakReference<S> mService;

        public LocalBinder(S service) {
            mService = new WeakReference<S>(service);
        }

        public S getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }

    //
    // public methods that can be used by Activities that bind to the Service
    //

    public MQTTConnectionStatus getConnectionStatus(int enumerator) {

        if (enumerator == -1) {
            Log.w(LOG_TAG, "WRONG ENUM");
            return MQTTConnectionStatus.INITIAL;
        } else {
            return listConnectionStatus.get(enumerator);
        }

        /*
        if (enumerator == 1) {
            return connectionStatus_1;
        } else if (enumerator == 2) {
            return connectionStatus_2;
        } else {

        }
        */

    }


    public void rebroadcastStatus(int enumerator) {
        String status = "";

        if (enumerator != -1) {

            switch (listConnectionStatus.get(enumerator)) {
                case INITIAL:
                    status = "Please wait";
                    break;
                case CONNECTING:
                    status = "Connecting...";
                    break;
                case CONNECTED:
                    status = "Connected";
                    break;
                case NOTCONNECTED_UNKNOWNREASON:
                    status = "Not connected - waiting for network connection";
                    break;
                case NOTCONNECTED_USERDISCONNECT:
                    status = "Disconnected";
                    break;
                case NOTCONNECTED_DATADISABLED:
                    status = "Not connected - background data disabled";
                    break;
                case NOTCONNECTED_WAITINGFORINTERNET:
                    status = "Unable to connect";
                    break;
            }

        }
        /*
        else if (enumerator == 2) {

            switch (connectionStatus_2) {
                case INITIAL:
                    status = "Please wait";
                    break;
                case CONNECTING:
                    status = "Connecting...";
                    break;
                case CONNECTED:
                    status = "Connected";
                    break;
                case NOTCONNECTED_UNKNOWNREASON:
                    status = "Not connected - waiting for network connection";
                    break;
                case NOTCONNECTED_USERDISCONNECT:
                    status = "Disconnected";
                    break;
                case NOTCONNECTED_DATADISABLED:
                    status = "Not connected - background data disabled";
                    break;
                case NOTCONNECTED_WAITINGFORINTERNET:
                    status = "Unable to connect";
                    break;
            }

        }
        */
        else {

            Log.w(LOG_TAG, "wrong enum");

        }


        // inform the app that the Service has successfully connected
        broadcastServiceStatus(status);

    }

    public void disconnect() {
//        disconnectFromBroker(1);
//        disconnectFromBroker(2);
        for (int i = 0; i < enumeratorTotal; i++) {
            disconnectFromBroker(i);
        }

        // set status
//        connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;
//        connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;
        for (int i = 0; i < enumeratorTotal; i++) {
            listConnectionStatus.set(i, MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT);
        }

        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
    }


    /************************************************************************/
    /*    METHODS - MQTT methods inherited from MQTT classes                */

    /**
     * ********************************************************************
     */

    /*
     * callback - method called when we no longer have a connection to the
     *  message broker server
     */
    public void connectionLost() throws Exception {
        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();


        //
        // have we lost our data connection?
        //

        if (!isOnline()) {

//            connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
//            connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
            for (int i = 0; i < enumeratorTotal; i++) {
                listConnectionStatus.set(i, MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
            }


            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - no network connection");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we are no longer able to receive messages
            notifyUser("Connection lost - no network connection",
                    "Commands Receiver", "Connection lost - no network connection");

            //
            // wait until the phone has a network connection again, when we
            //  the network connection receiver will fire, and attempt another
            //  connection to the broker
        } else {
            //
            // we are still online
            //   the most likely reason for this connectionLost is that we've
            //   switched from wifi to cell, or vice versa
            //   so we try to reconnect immediately
            //


//            connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
//            connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
            for (int i = 0; i < enumeratorTotal; i++) {
                listConnectionStatus.set(i, MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
            }

            // inform the app that we are not connected any more, and are
            //   attempting to reconnect
            broadcastServiceStatus("Connection lost - reconnecting...");

            // try to reconnect
            for (int i = 0; i < enumeratorTotal; i++) {
                if (connectToBroker(i)) {
                    subscribeToTopic(i, topicNameAccelStream);
                    subscribeToTopic(i, topicNameGesturePusher);
                }
            }


            /*
            if (connectToBroker(2)) {
                subscribeToTopic(2, topicNameAccelStream);
                subscribeToTopic(2, topicNameGesturePusher);
            }
            */


        }

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }


    /*
     *   callback - called when we receive a message from the server
     */
    public void publishArrived(String topic, byte[] payloadbytes, int qos, boolean retained) {


        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        //
        //  I'm assuming that all messages I receive are being sent as strings
        //   this is not an MQTT thing - just me making as assumption about what
        //   data I will be receiving - your app doesn't have to send/receive
        //   strings - anything that can be sent as bytes is valid
        String messageBody = new String(payloadbytes);

        Log.w(LOG_TAG, "------------------------- publishArrived !!! messageBody: " + messageBody);


        //
        //  for times when the app's Activity UI is not running, the Service
        //   will need to safely store the data that it receives
        addReceivedMessageToStore(topic, messageBody);

        // UITGEZET: if (addReceivedMessageToStore(topic, messageBody)) {
        // this is a new message - a value we haven't seen before

        // !! WE KOMEN DUS ENKEL HIER ALS HET GEKREGEN BERICHT ANDERS IS DAN HET VOORGAAND.
        // DIT IS GOED: WANT ALS DE STREAM AL BV. ENABLED WAS EN KREGEN TERUG ENABLED, MOETEN WE NIET DIRECT STARTEN HE WANT DE STREAM LIEP AL!
        String[] splitArray = messageBody.split(";");
        String systemID = splitArray[0]; // TODO ======= dus ergens toepassen?
        Log.w(LOG_TAG, "============ SYSTEMID " + systemID);

        if (topic.equalsIgnoreCase("accelstream/state") && messageBody.endsWith("enable")) {
            enableAccelStream(systemID);
        } else if (topic.equalsIgnoreCase("accelstream/state") && messageBody.endsWith("disable")) {
            disableAccelStream(systemID);
        } else if (topic.equalsIgnoreCase("gesturepusher/state") && messageBody.endsWith("enable")) {
            // TODO dit ook in map opslaan? of is gans deze enable en disable bij gesturepusher OVERBODIG?
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/state en MESSAGE enable ========================");
        } else if (topic.equalsIgnoreCase("gesturepusher/state") && messageBody.endsWith("disable")) {
            // TODO dit ook in map opslaan? of is gans deze enable en disable bij gesturepusher OVERBODIG?
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/state en MESSAGE disable ========================");
        } else if (topic.equalsIgnoreCase("gesturepusher/supportedgestures") && messageBody.endsWith("up")) {
            addSupportedGesture(systemID, "up");
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/supportedgestures en MESSAGE up ========================");
        } else if (topic.equalsIgnoreCase("gesturepusher/supportedgestures") && messageBody.endsWith("down")) {
            addSupportedGesture(systemID, "down");
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/supportedgestures en MESSAGE down ========================");
        } else if (topic.equalsIgnoreCase("gesturepusher/supportedgestures") && messageBody.endsWith("left")) {
            addSupportedGesture(systemID, "left");
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/supportedgestures en MESSAGE left ========================");
        } else if (topic.equalsIgnoreCase("gesturepusher/supportedgestures") && messageBody.endsWith("right")) {
            addSupportedGesture(systemID, "right");
            Log.w(LOG_TAG, "======================== kreeg TOPIC gesturepusher/supportedgestures en MESSAGE right ========================");
        }


        // inform the app (for times when the Activity UI is running) of the
        //   received message so the app UI can be updated with the new data
        broadcastReceivedMessage(topic, messageBody);

        // inform the user (for times when the Activity UI isn't running)
        //   that there is new data available
        notifyUser("New data received", topic, messageBody);

        // UITGEZET: }

        // receiving this message will have kept the connection alive for us, so
        //  we take advantage of this to postpone the next scheduled ping
        scheduleNextPing();

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }

    // ----------- KOPIE OOK TE VINDEN IN THREEDOLLARGESTUREFRAGMENT.JAVA DUS VOER DAAR OOK WIJZIGINGEN DOOR.
    private void enableAccelStream(String systemID) {

        // Only 1 system at a time can detect the user's face, so only 1 system can have the accel stream enabled. Hence we only allow space for 1 saved systemID.
        SharedPreferences accelStreamSettings = getSharedPreferences("com.yen.androidappthesisyen.system_id_accel_stream", Context.MODE_PRIVATE);
        SharedPreferences.Editor accelStreamEditor = accelStreamSettings.edit();
        accelStreamEditor.putString("enabledSystem", systemID);
        accelStreamEditor.commit();

        Log.w(LOG_TAG, "------------------------- arrived in enableAccelStream");

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(1, 0); // key = 1 = TRUE = start stream, value = 0
        PebbleKit.sendDataToPebble(getApplicationContext(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
    }

    private void disableAccelStream(String systemID) {

//      OVERBODIG  mapSystemIDToIsAccelStreamEnabled.put(systemID, false);
        SharedPreferences accelStreamSettings = getSharedPreferences("com.yen.androidappthesisyen.system_id_accel_stream", Context.MODE_PRIVATE);
        SharedPreferences.Editor accelStreamEditor = accelStreamSettings.edit();
        accelStreamEditor.putString("enabledSystem", "none");
        accelStreamEditor.commit();

        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(0, 0); // key = 0 = FALSE = stop stream, value = 0
        PebbleKit.sendDataToPebble(getApplicationContext(), UUID.fromString("297c156a-ff89-4620-9d31-b00468e976d4"), dict);
    }

    /************************************************************************/
    /*    METHODS - wrappers for some of the MQTT methods that we use       */

    /**
     * ********************************************************************
     */

    /*
     * Create a client connection object that defines our connection to a
     *   message broker server
     */
    private void defineConnectionToBroker(int enumerator, String brokerHostName) {
        String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;


        Log.w(LOG_TAG, "------------------------- brokerHostName " + enumerator + " " + brokerHostName);
        Log.w(LOG_TAG, "------------------------- brokerPortNumber " + brokerPortNumber);


        if (enumerator != -1) {

            try {
                // define the connection to the broker
//                mqttClient_1 = MqttClient.createMqttClient(mqttConnSpec, usePersistence);
                IMqttClient client = MqttClient.createMqttClient(mqttConnSpec, usePersistence);
                listMQTTClient.set(enumerator, client);

                // register this client app as being able to receive messages
                client.registerSimpleHandler(this);

            } catch (MqttException e) {

                // something went wrong!
//                mqttClient_1 = null;
                listMQTTClient.set(enumerator, null);
//                connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
                listConnectionStatus.set(enumerator, MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

                //
                // inform the app that we failed to connect so that it can update
                //  the UI accordingly
                broadcastServiceStatus("Invalid connection parameters");

                //
                // inform the user (for times when the Activity UI isn't running)
                //   that we failed to connect
                notifyUser("Unable to connect", "Commands Receiver", "Unable to connect");
            }

        }
        /*
        else if (enumerator == 2) {

            try {
                // define the connection to the broker
                mqttClient_2 = MqttClient.createMqttClient(mqttConnSpec, usePersistence);

                // register this client app has being able to receive messages
                mqttClient_2.registerSimpleHandler(this);
            } catch (MqttException e) {
                // something went wrong!
                mqttClient_2 = null;
                connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

                //
                // inform the app that we failed to connect so that it can update
                //  the UI accordingly
                broadcastServiceStatus("Invalid connection parameters");

                //
                // inform the user (for times when the Activity UI isn't running)
                //   that we failed to connect
                notifyUser("Unable to connect", "Commands Receiver", "Unable to connect");
            }

        }
        */
        else {
            Log.w(LOG_TAG, "Wrong enum");
        }


    }

    /*
     * (Re-)connect to the message broker
     */
    private boolean connectToBroker(int enumerator) {
        try {

            Log.w(LOG_TAG, "------------------------- TRYING TO CONNECT TO BROKER " + enumerator);

            // try to connect

//                mqttClient_1.connect(generateClientId(), cleanStart_1, keepAliveSeconds);

            Log.w(LOG_TAG, "enumerator in connectToBroker " + enumerator);
            if (listMQTTClient.get(enumerator) == null) {
                Log.w(LOG_TAG, "listMQTTClient null");
            } else if (listCleanStart.get(enumerator) == null) {
                Log.w(LOG_TAG, "listCleanStart null");
            }
            listMQTTClient.get(enumerator).connect(generateClientId(), listCleanStart.get(enumerator), keepAliveSeconds);


            // inform the app that the app has successfully connected
            broadcastServiceStatus("Connected");

            // we are connected

//                connectionStatus_1 = MQTTConnectionStatus.CONNECTED;
            listConnectionStatus.set(enumerator, MQTTConnectionStatus.CONNECTED);


            Log.w(LOG_TAG, "------------------------- CONNECTION SUCCESS " + enumerator);

            // we need to wake up the phone's CPU frequently enough so that the
            //  keep alive messages can be sent
            // we schedule the first one of these now
            scheduleNextPing();

            return true;


        } catch (MqttException e) {


            Log.w(LOG_TAG, "------------------------- CONNECTION FAILED " + enumerator);


            // something went wrong!

//                connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
            listConnectionStatus.set(enumerator, MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);


            // inform the app that we failed to connect so that it can update
            //  the UI accordingly
            broadcastServiceStatus("Unable to connect");


            // inform the user (for times when the Activity UI isn't running)
            //   that we failed to connect
            notifyUser("Unable to connect", "Commands Receiver", "Unable to connect - will retry later");

            // if something has failed, we wait for one keep-alive period before
            //   trying again
            // in a real implementation, you would probably want to keep count
            //  of how many times you attempt this, and stop trying after a
            //  certain number, or length of time - rather than keep trying
            //  forever.
            // a failure is often an intermittent network issue, however, so
            //  some limited retry is a good idea
            scheduleNextPing();

            return false;

        }
    }

    /*
     * Send a request to the message broker to be sent messages published with
     *  the specified topic name. Wildcards are allowed.
     */
    private void subscribeToTopic(int enumerator, String topicName) {

        Log.w(LOG_TAG, "------------------------- SUBSCRIBE TO TOPIC: " + topicName);

        boolean subscribed = false;

        if (isAlreadyConnected(enumerator) == false) {
            // quick sanity check - don't try and subscribe if we
            //  don't have a connection

            Log.e(LOG_TAG, "Unable to subscribe as we are not connected");

        } else {

            try {

                String[] topics = {topicName};

                if (enumerator != -1) {
//                    mqttClient_1.subscribe(topics, qualitiesOfService);
                    listMQTTClient.get(enumerator).subscribe(topics, qualitiesOfService);
                }
                /*
                else if (enumerator == 2) {
                    mqttClient_2.subscribe(topics, qualitiesOfService);
                }
                */
                else {
                    Log.w(LOG_TAG, "foute enumerator");
                }

                subscribed = true;

            } catch (MqttNotConnectedException e) {
                Log.e(LOG_TAG, "subscribe failed - MQTT not connected", e);
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "subscribe failed - illegal argument", e);
            } catch (MqttException e) {
                Log.e(LOG_TAG, "subscribe failed - MQTT exception", e);
            }
        }

        if (subscribed == false) {
            //
            // inform the app of the failure to subscribe so that the UI can
            //  display an error
            broadcastServiceStatus("Unable to subscribe");

            //
            // inform the user (for times when the Activity UI isn't running)
            notifyUser("Unable to subscribe", "Commands Receiver", "Unable to subscribe");
        }
    }

    /*
     * Terminates a connection to the message broker.
     */
    private void disconnectFromBroker(int enumerator) {
        // if we've been waiting for an Internet connection, this can be
        //  cancelled - we don't need to be told when we're connected now

        if (enumerator != -1) {

            try {

                if (listNetConnReceiver.get(enumerator) != null) {
                    unregisterReceiver(listNetConnReceiver.get(enumerator));
//                    netConnReceiver_1 = null;
                    listNetConnReceiver.set(enumerator, null);
                }

                if (listPingSender.get(enumerator) != null) {
                    unregisterReceiver(listPingSender.get(enumerator));
//                    pingSender_1 = null;
                    listPingSender.set(enumerator, null);
                }

            } catch (Exception eee) {
                // probably because we hadn't registered it
                Log.e(LOG_TAG, "unregister failed", eee);
            }

        }
        /*
        else if (enumerator == 2) {

            try {

                if (netConnReceiver_2 != null) {
                    unregisterReceiver(netConnReceiver_2);
                    netConnReceiver_2 = null;
                }

                if (pingSender_2 != null) {
                    unregisterReceiver(pingSender_2);
                    pingSender_2 = null;
                }

            } catch (Exception eee) {
                // probably because we hadn't registered it
                Log.e(LOG_TAG, "unregister failed", eee);
            }

        }
        */
        else {
            Log.w(LOG_TAG, "wrong enum");
        }


        if (enumerator != -1) {

            try {
                if (listMQTTClient.get(enumerator) != null) {
//                    mqttClient_1.disconnect();
                    listMQTTClient.get(enumerator).disconnect();
                }
            } catch (MqttPersistenceException e) {
                Log.e(LOG_TAG, "disconnect failed - persistence exception", e);
            } finally {
//                mqttClient_1 = null;
                listMQTTClient.set(enumerator, null);
            }

        }
        /*
        else if (enumerator == 2) {

            try {
                if (mqttClient_2 != null) {
                    mqttClient_2.disconnect();
                }
            } catch (MqttPersistenceException e) {
                Log.e(LOG_TAG, "disconnect failed - persistence exception", e);
            } finally {
                mqttClient_2 = null;
            }

        }
        */
        else {
            Log.w(LOG_TAG, "wrong enum");
        }


        // we can now remove the ongoing notification that warns users that
        //  there was a long-running ongoing service running
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();

    }

    /*
     * Checks if the MQTT client thinks it has an active connection
     */
    private boolean isAlreadyConnected(int enumerator) {

        if (enumerator != -1) {
//            return ((mqttClient_1 != null) && (mqttClient_1.isConnected() == true));
            return ((listMQTTClient.get(enumerator) != null) && (listMQTTClient.get(enumerator).isConnected() == true));
        }
        /*
        else if (enumerator == 2) {
            return ((mqttClient_2 != null) && (mqttClient_2.isConnected() == true));
        }
        */
        else {
            Log.w(LOG_TAG, "wrong enum");
            return false;
        }

    }

    private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {

        private int enumerator = -1;

        public BackgroundDataChangeIntentReceiver(int enumerator) {

            this.enumerator = enumerator;

        }

        @Override
        public void onReceive(Context ctx, Intent intent) {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm.getBackgroundDataSetting()) {
                // user has allowed background data - we start again - picking
                //  up where we left off in handleStart before
                if (enumerator != -1) {
//                    defineConnectionToBroker(1, brokerHostName_1);
                    defineConnectionToBroker(enumerator, listBrokerHostName.get(enumerator));
                }
                /*
                else if (enumerator == 2) {
                    defineConnectionToBroker(2, brokerHostName_2);
                }
                */
                else {
                    Log.w(LOG_TAG, "wrong enum");
                }

                // TODO HIER NOG DE 2DE REGEL OF TOTALE OMRINGENDE CODE COPYPASTEN.
                handleStart(intent, 0);

            } else {

                // user has disabled background data
                if (enumerator != -1) {
//                    connectionStatus_1 = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
                    listConnectionStatus.set(enumerator, MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);
                }
                /*
                else if (enumerator == 2) {
                    connectionStatus_2 = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
                }
                */
                else {
                    Log.w(LOG_TAG, "wrong enum");
                }

                // update the app to show that the connection has been disabled
                broadcastServiceStatus("Not connected - background data disabled");

                // disconnect from the broker
                if (enumerator != -1) {
//                    disconnectFromBroker(1);
                    disconnectFromBroker(enumerator);
                }
                /*
                else if (enumerator == 2) {
                    disconnectFromBroker(2);
                }
                */
                else {
                    Log.w(LOG_TAG, "wrong enum");
                }

            }

            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }


    /*
     * Called in response to a change in network connection - after losing a
     *  connection to the server, this allows us to wait until we have a usable
     *  data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        private int enumerator = -1;

        public NetworkConnectionIntentReceiver(int enumerator) {
            this.enumerator = enumerator;
        }

        @Override
        public void onReceive(Context ctx, Intent intent) {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            if (isOnline()) {
                // we have an internet connection - have another try at connecting

                if (enumerator != -1) {

                    if (connectToBroker(enumerator)) {
                        // we subscribe to a topic - registering to receive push
                        //  notifications with a particular key
                        subscribeToTopic(enumerator, topicNameAccelStream);
                        subscribeToTopic(enumerator, topicNameGesturePusher);
                    }

                }
                /*
                else if (enumerator == 2) {

                    if (connectToBroker(2)) {
                        // we subscribe to a topic - registering to receive push
                        //  notifications with a particular key
                        subscribeToTopic(2, topicNameAccelStream);
                        subscribeToTopic(2, topicNameGesturePusher);
                    }

                }
                */
                else {

                    Log.w(LOG_TAG, "wrong enum");

                }


            }

            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }


    /*
     * Schedule the next time that you want the phone to wake up and ping the
     *  message broker server
     */
    private void scheduleNextPing() {
        // When the phone is off, the CPU may be stopped. This means that our
        //   code may stop running.
        // When connecting to the message broker, we specify a 'keep alive'
        //   period - a period after which, if the client has not contacted
        //   the server, even if just with a ping, the connection is considered
        //   broken.
        // To make sure the CPU is woken at least once during each keep alive
        //   period, we schedule a wake up to manually ping the server
        //   thereby keeping the long-running connection open
        // Normally when using this Java MQTT client library, this ping would be
        //   handled for us.
        // Note that this may be called multiple times before the next scheduled
        //   ping has fired. This is good - the previously scheduled one will be
        //   cancelled in favour of this one.
        // This means if something else happens during the keep alive period,
        //   (e.g. we receive an MQTT message), then we start a new keep alive
        //   period, postponing the next ping.

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }


    /*
     * Used to implement a keep-alive protocol at this Service level - it sends
     *  a PING message to the server, then schedules another ping after an
     *  interval defined by keepAliveSeconds
     */
    public class PingSender extends BroadcastReceiver {


        private int enumerator = -1;

        public PingSender(int enumerator) {
            this.enumerator = enumerator;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Note that we don't need a wake lock for this method (even though
            //  it's important that the phone doesn't switch off while we're
            //  doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            //  long as the alarm receiver's onReceive() method is executing.
            //  This guarantees that the phone will not sleep until you have
            //  finished handling the broadcast."
            // This is good enough for our needs.

            if (enumerator != -1) {

                try {

                    listMQTTClient.get(enumerator).ping();

                } catch (MqttException e) {
                    // if something goes wrong, it should result in connectionLost
                    //  being called, so we will handle it there
                    Log.e(LOG_TAG, "ping failed - MQTT exception", e);

                    // assume the client connection is broken - trash it
                    try {
                        listMQTTClient.get(enumerator).disconnect();
                    } catch (MqttPersistenceException e1) {
                        Log.e(LOG_TAG, "disconnect failed - persistence exception", e1);
                    }

                    // reconnect
                    if (connectToBroker(enumerator)) {
                        subscribeToTopic(enumerator, topicNameAccelStream);
                        subscribeToTopic(enumerator, topicNameGesturePusher);
                    }
                }

            }
            /*
            else if (enumerator == 2) {

                try {

                    mqttClient_2.ping();

                } catch (MqttException e) {
                    // if something goes wrong, it should result in connectionLost
                    //  being called, so we will handle it there
                    // TODO ff uitgezet of aanlaten? Log.e(LOG_TAG, "ping failed - MQTT exception", e);

                    // assume the client connection is broken - trash it

                    try {
                        mqttClient_2.disconnect();
                    } catch (MqttPersistenceException e1) {
                        // TODO ff uitgezet of aanlaten? Log.e(LOG_TAG, "disconnect failed - persistence exception", e1);
                    }

                    // reconnect
                    if (connectToBroker(2)) {
                        subscribeToTopic(2, topicNameAccelStream);
                        subscribeToTopic(2, topicNameGesturePusher);
                    }
                }

            }
            */
            else {

                Log.w(LOG_TAG, "wrong enum");

            }


            // start the next keep alive period
            scheduleNextPing();


        }
    }


    /************************************************************************/
    /*   APP SPECIFIC - stuff that would vary for different uses of MQTT    */
    /**
     * ********************************************************************
     */

    //  apps that handle very small amounts of data - e.g. updates and
    //   notifications that don't need to be persisted if the app / phone
    //   is restarted etc. may find it acceptable to store this data in a
    //   variable in the Service
    //  that's what I'm doing in this sample: storing it in a local hashtable
    //  if you are handling larger amounts of data, and/or need the data to
    //   be persisted even if the app and/or phone is restarted, then
    //   you need to store the data somewhere safely
    //  see http://developer.android.com/guide/topics/data/data-storage.html
    //   for your storage options - the best choice depends on your needs

    // stored internally

    private Hashtable<String, String> dataCache = new Hashtable<String, String>();

    private boolean addReceivedMessageToStore(String key, String value) {
        String previousValue = null;

        if (value.length() == 0) {
            previousValue = dataCache.remove(key);
        } else {
            previousValue = dataCache.put(key, value);
        }

        // is this a new value? or am I receiving something I already knew?
        //  we return true if this is something new
        return ((previousValue == null) ||
                (previousValue.equals(value) == false));
    }

    // provide a public interface, so Activities that bind to the Service can
    //  request access to previously received messages

    public void rebroadcastReceivedMessages() {
        Enumeration<String> e = dataCache.keys();
        while (e.hasMoreElements()) {
            String nextKey = e.nextElement();
            String nextValue = dataCache.get(nextKey);

            broadcastReceivedMessage(nextKey, nextValue);
        }
    }


    /************************************************************************/
    /*    METHODS - internal utility methods                                */

    /**
     * ********************************************************************
     */

    private String generateClientId() {
        // generate a unique client id if we haven't done so before, otherwise
        //   re-use the one we already have

        if (mqttClientId == null) {
            // generate a unique client ID - I'm basing this on a combination of
            //  the phone device id and the current timestamp
            String timestamp = "" + (new Date()).getTime();
            String android_id = Settings.System.getString(getContentResolver(),
                    Secure.ANDROID_ID);
            mqttClientId = timestamp + android_id;

            // truncate - MQTT spec doesn't allow client ids longer than 23 chars
            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
            }
        }

        return mqttClientId;
    }

    private boolean isOnline() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected()) {
            return true;
        }

        return false;
    }
}
