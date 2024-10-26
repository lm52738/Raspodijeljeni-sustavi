package hr.fer.tel.rassus.senzori;

import hr.fer.tel.rassus.senzori.stupidudp.network.Reading;

public interface MessageListener {
	
	void onMessageReceived(String topic, String message);

	void onUdpMessageReceived(Reading reading);
}
