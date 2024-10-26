package hr.fer.tel.rassus.senzori.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * The KafkaProducer class is a simple wrapper around Apache Kafka's Producer API
 * to enable sending messages to a Kafka topic. This class sets up a producer 
 * with basic configuration, sends messages, and manages the producer lifecycle.
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
    
    public void sendMessage(String command, String topic) {
	    System.out.println("Sensor sends message to other sensors on topic " + topic);
	
	    ProducerRecord<String, String> record1 = 
    			new ProducerRecord<String, String>(topic, null, command);
	    
    	producer.send(record1);
	    producer.flush();
	    
    }
    
    public void closeProducer() {
    	producer.close();
    }
}
