package hr.fer.tel.rassus.coordinator;

import java.util.Properties;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * The KafkaProducer class handles message production to Kafka topics for the coordinator.
 * It allows initialization, message sending, and cleanup of the producer.
 */
public class KafkaProducer {

	private Producer<String, String> producer;
	
    public void initializeProducer(){
    	
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(producerProperties);
    }
    
    public void sendCommand(String command, String topic) {
	    
	    	
    	System.out.println("Coordinator sends message to sensor on topic " + topic);
    	
    	ProducerRecord<String, String> record1 = 
    			new ProducerRecord<String, String>(topic, null, command);
	    
    	producer.send(record1);
    	producer.flush();	
    	
	    
    }
    
    public void closeProducer() {
    	producer.close();
    }
}
