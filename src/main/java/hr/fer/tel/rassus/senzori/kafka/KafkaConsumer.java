package hr.fer.tel.rassus.senzori.kafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;

import hr.fer.tel.rassus.senzori.MessageListener;

public class KafkaConsumer {

	private static String COMMAND = "Command";
	private static String REGISTER = "Register";
	
	private Consumer<String, String> consumer;
	private MessageListener messageListener;

    public KafkaConsumer(MessageListener listener) {
        this.messageListener = listener;
    }
	
	public void initializeConsumer(Integer groupId) {
		Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId.toString());
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProperties);       
        consumer.subscribe(Arrays.asList(COMMAND, REGISTER));
        
        System.out.println("Sensor waiting for messages to arrive on topics " + COMMAND + " and " + REGISTER);

	}
	
	public void pollMessages() {
		
		while (true) {
			
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));

            consumerRecords.forEach(record -> {
                messageListener.onMessageReceived(record.topic(), record.value());
               
            });

            consumer.commitAsync();
        }
	}
	
	public void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }
}