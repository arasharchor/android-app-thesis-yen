package com.yen.androidappthesisyen.pushnotificationlistener;

import com.ibm.mqtt.IMqttClient;

/**
 * Created by Yen on 15/08/2015.
 */
public class Broker {


    // Status of MQTT client connection
    private MQTTService.MQTTConnectionStatus connectionStatus = null;

    // Host name of the server we're receiving push notifications from
    private String brokerAddress = null;
    // We do NOT save a variable brokerName (= systemID) because the MQTT service doesn't need to know the current systemID associated with the IP address.
    // Since there is NO hard link between IP adresses and systemIDs we don't show the systemID in notifications triggered by the MQTT service.
    // We show in notifications the brokerAddress (=IP address) the notification is referring to,
    // to make sure the user can find out which Action Device isn't working.

    private Boolean cleanStart = null;

    // connection to the message broker
    private IMqttClient MQTTClient = null;

    // receiver that notifies the Service when the device gets data connection
    private MQTTService.NetworkConnectionIntentReceiver netConnReceiver = null;

    // receiver that notifies the Service when the user changes data use preferences
    private MQTTService.BackgroundDataChangeIntentReceiver dataEnabledReceiver = null;

    // receiver that wakes the Service up when it's time to ping the server
    private MQTTService.PingSender pingSender = null;


    public Broker() {
        brokerAddress = "";
        // reset status variable to initial state
        connectionStatus = MQTTService.MQTTConnectionStatus.INITIAL;
        cleanStart = false;
    }



    public MQTTService.MQTTConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(MQTTService.MQTTConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public Boolean getCleanStart() {
        return cleanStart;
    }

    public void setCleanStart(Boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public IMqttClient getMQTTClient() {
        return MQTTClient;
    }

    public void setMQTTClient(IMqttClient MQTTClient) {
        this.MQTTClient = MQTTClient;
    }

    public MQTTService.NetworkConnectionIntentReceiver getNetConnReceiver() {
        return netConnReceiver;
    }

    public void setNetConnReceiver(MQTTService.NetworkConnectionIntentReceiver netConnReceiver) {
        this.netConnReceiver = netConnReceiver;
    }

    public MQTTService.BackgroundDataChangeIntentReceiver getDataEnabledReceiver() {
        return dataEnabledReceiver;
    }

    public void setDataEnabledReceiver(MQTTService.BackgroundDataChangeIntentReceiver dataEnabledReceiver) {
        this.dataEnabledReceiver = dataEnabledReceiver;
    }

    public MQTTService.PingSender getPingSender() {
        return pingSender;
    }

    public void setPingSender(MQTTService.PingSender pingSender) {
        this.pingSender = pingSender;
    }
}
