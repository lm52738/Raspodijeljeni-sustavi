package hr.fer.tel.rassus.sensors;

import hr.fer.tel.rassus.sensors.stupidudp.network.Reading;

public interface MessageListener {
	
	/**
     * Handles messages received from Kafka topics and determines actions based on message type.
     * 
     * @param topic  
     * @param message 
     */
	void onMessageReceived(String topic, String message);

	/**
     * Handles received UDP messages containing readings and updates local clocks if necessary.
     * 
     * @param reading The reading received from another sensor
     */
	void onUdpMessageReceived(Reading reading);
}
