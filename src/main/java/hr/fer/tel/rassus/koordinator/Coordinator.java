package hr.fer.tel.rassus.koordinator;

import java.util.Scanner;

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
