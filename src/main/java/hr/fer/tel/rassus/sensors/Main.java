package hr.fer.tel.rassus.sensors;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		// After starting, prompt the user to enter the sensor's identifier and UDP port
        System.out.println("Enter Sensor ID:");
        int id = scanner.nextInt();

        System.out.println("Enter UDP port:");
        int udpPort = scanner.nextInt();
        
        System.out.println("Enter number of sensors:");
        int sensors = scanner.nextInt();
        
        scanner.close();
        
		Sensor sensor = new Sensor(id, udpPort, sensors);
		
		ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(sensor::run);
		
	}
		
}
