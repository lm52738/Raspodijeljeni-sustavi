package hr.fer.tel.rassus.senzori;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		// nakon pokretanja traži upis svog identifikatora i UDP port
        System.out.println("Unesite ID:");
        int id = scanner.nextInt();

        System.out.println("Unesite UDP port:");
        int udpPort = scanner.nextInt();
        
        System.out.println("Unesite broj senzora:");
        int sensors = scanner.nextInt();
        
        scanner.close();
        
		Sensor sensor = new Sensor(id, udpPort, sensors);
		
		ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(sensor::run);
		
	}
		
}
