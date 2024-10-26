package hr.fer.tel.rassus.koordinator;

import java.util.Scanner;

/**
 * The Coordinator class manages the start and stop commands for sensors in a Kafka-based system.
 * It interacts with the user through the console to initiate or end the process, sending the 
 * corresponding command messages to a Kafka topic.
 */
public class Coordinator {
	
	private static final String START = "Start";
	private static final String STOP = "Stop";
	private static String TOPIC = "Command";
	private static KafkaProducer kafkaProducer;

	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		String input;
        do {
            System.out.println("Unesite 'START' za pocetak rada:");
            input = scanner.nextLine();
        } while (!input.equalsIgnoreCase("START"));
        
        start();
        
        System.out.println("Program started.");
		
        do {
            System.out.println("Unesite 'STOP' za kraj rada:");
            input = scanner.nextLine();
        } while (!input.equalsIgnoreCase("STOP"));

        stop();
        
        scanner.close();
        
        System.out.println("Program stopped.");
        System.exit(0);
	}
	
	public static void start() {
		kafkaProducer = new KafkaProducer();
		kafkaProducer.initializeProducer();
		kafkaProducer.sendCommand(START, TOPIC);
	}
	
	public static void stop() {
		kafkaProducer.sendCommand(STOP, TOPIC);
		kafkaProducer.closeProducer();
	}

}
