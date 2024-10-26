package hr.fer.tel.rassus.senzori;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import hr.fer.tel.rassus.senzori.kafka.KafkaConsumer;
import hr.fer.tel.rassus.senzori.kafka.KafkaProducer;
import hr.fer.tel.rassus.senzori.kafka.Registration;
import hr.fer.tel.rassus.senzori.stupidudp.client.StupidUDPClient;
import hr.fer.tel.rassus.senzori.stupidudp.network.EmulatedSystemClock;
import hr.fer.tel.rassus.senzori.stupidudp.network.Reading;
import hr.fer.tel.rassus.senzori.stupidudp.server.StupidUDPServer;

public class Sensor implements MessageListener{
	
	private static String COMMAND = "Command";
	private static String REGISTER = "Register";
	private static String START = "Start";
	private static String STOP = "Stop";

	private int id;
	private int udpPort;
	private int sensorCount;
	private int messageCounter;
	private long startTime;
	
    private StupidUDPServer udpServer;
    private KafkaConsumer kafkaConsumer;

	private Map<Integer, Registration> sensors; // registracije
	private List<Reading> readings; // primljena ocitanja
	
	// vremenske oznake
	private EmulatedSystemClock systemClock;
	private Map<Integer, Integer> vectorClock;
	
	
	
	public Sensor(int id,int udpPort, int sensorCount) {
		this.id = id;
		this.udpPort = udpPort;
		this.sensorCount = sensorCount;
		this.messageCounter = 0;
		this.sensors = new HashMap<>();
		this.readings = new ArrayList<>();
		this.systemClock = new EmulatedSystemClock();
		this.vectorClock = new HashMap<>();	
		this.kafkaConsumer = new KafkaConsumer(this);
		this.udpServer = new StupidUDPServer(udpPort, this);
	}
	
	public void run() {    

		startTime = System.currentTimeMillis();
		vectorClock.put(id, 0);
		
		// Pokretanje KafkaConsumera u zasebnoj niti
        Thread kafkaThread = new Thread(() -> {
            kafkaConsumer.initializeConsumer(id);
            kafkaConsumer.pollMessages();
        });
        kafkaThread.start();
	    
		// Pokretanje UDP servera u zasebnoj niti
        Thread udpThread = new Thread(() -> {
            try {
                udpServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        udpThread.start();
	    
	}

	@Override
	public void onMessageReceived(String topic, String message) {
		
		if (topic.equals(COMMAND) && message.equals(START)) register();
		
		else if (topic.equals(COMMAND) && message.equals(STOP)) stopWorking();
		
		else if (topic.equals(REGISTER)) saveRegistration(message);
		
		return;
	}
	
	private void register() {

		System.out.printf("Sensor got a message:(%s : %s)\n",COMMAND, START);
		
		// Inicijalizacija Kafka producera
	    KafkaProducer kafkaProducer = new KafkaProducer();
	    kafkaProducer.initializeProducer();
	    
	    // posalji registracijsku poruku
		try {
		    Registration registration = new Registration(id, "localhost", udpPort);
		    
		    ObjectMapper objectMapper = new ObjectMapper();
		    String jsonString = objectMapper.writeValueAsString(registration);
			
			kafkaProducer.sendMessage(jsonString, REGISTER);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		kafkaProducer.closeProducer();
		
	}

	private void stopWorking() {
		
		System.out.printf("Sensor got a message:(%s : %s)\n",COMMAND, STOP);

		// zaustavlja se komunikacija
        udpServer.stopServer();
        kafkaConsumer.closeConsumer();
		
		// sortirati i ispisati sva ocitanja
		printReadings();
		
		// zaustavlja se UDP komunikacija
		System.out.println("Sensor shutting down ...");
		System.exit(0);
	}
	
	private void saveRegistration(String message) {
		
		// spremi registraciju cvora
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			Registration registration = objectMapper.readValue(message, Registration.class);
			
			if (registration.getId() == id)	return;
			
			messageCounter++;
			
			System.out.printf("Sensor got a message:(%s)\n",REGISTER);
			System.out.println(registration.toString());
			System.out.println();
			
			sensors.put(registration.getId(), registration);
			vectorClock.put(registration.getId(), 0);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		if (messageCounter == sensorCount-1) {
			// Pokretanje UDP komunikacije u zasebnoj niti
			Thread udpCommunicationThread = new Thread(() -> {
				try {
					udpCommunication();
				} catch (IOException | ClassNotFoundException | InterruptedException e) {
					e.printStackTrace();
				} 
	        });
			udpCommunicationThread.start();
		}
		
	}

	private void udpCommunication() throws IOException, ClassNotFoundException, InterruptedException {
		
		// Inicijalizacija UDP servera
		System.out.println("UDP communication started ...");
		
		Timer timer = new Timer();

        // ispis ocitanja svakih 5 sekundi
        timer.schedule(new ReadingPrinterTask(), 0, 5 * 1000);
		
		while(true) {
			System.out.println("--------------------------------");
			
			// generira vlastito ocitanje i stvara UDP paket
			Double no2 = getNO2();
			
			Integer clock = vectorClock.get(id);
			clock++;
			vectorClock.put(id, clock);
			
			Reading reading = new Reading(id, no2, vectorClock, systemClock.currentTimeMillis());
			System.out.println("New generated reading: " + reading.toString());
			
			// šalje ga svim cvorovima u mrezi
			for (Registration registration : sensors.values()) {
				
				StupidUDPClient udpClient = new StupidUDPClient(registration.getPort());
				udpClient.sendMessage(reading);	
				System.out.println();
			}
			
		}
	}
	
	@Override
	public void onUdpMessageReceived(Reading reading) {
		
		// provjerava primljeni skalar sa vlastitim
		long scalarClock = systemClock.currentTimeMillis();
		if (reading.getScalarClock() > scalarClock) {
			System.out.println();
			System.out.println("The message reception time " + scalarClock + 
					" is earlier than the message sending time " + reading.getScalarClock() + "!!");
			systemClock.updateTimeMillis(reading.getScalarClock());
			System.out.println("New updated scalar clock: " + systemClock.currentTimeMillis());
			System.out.println();
		}
		
		// uveca svoj sat
		Integer clock = vectorClock.get(id);
		clock++;
		
		// postavlja primljeni vektor sa svojom novom vrijednoscu
		vectorClock = reading.getVectorClock();
		vectorClock.put(id, clock);
		
		// spremi ocitanja za ispis
		readings.add(reading);		
	}
	
	
	class ReadingPrinterTask extends TimerTask {
        @Override
        public void run() {
        	
        	// nakon svaki 5 sekundi sortirati i ispisati sva ocitanja i njihovu srednju vrijednost
    		Double average = readings.stream()
    				.filter(r -> r.getNo2()!=null)
                    .mapToDouble(Reading::getNo2)
                    .average()
                    .orElse(0.0);
        	
    		System.out.println();
    		System.out.println("--------------------------------");
        	System.out.println("The average value of all readings is: " + average);
   
            printReadings();
            System.out.println("--------------------------------");
            System.out.println();
        }
    }	
	
	private void printReadings() {
		
		Collections.sort(readings, Reading.readingScalarComparator);
		System.out.println("Readings sort by scalar clock:");
		readings.stream().forEach(r -> System.out.println(r));
		
		Collections.sort(readings, Reading.readingVectorComparator);
		System.out.println("Readings sort by vector clock:");
		readings.stream().forEach(r -> System.out.println(r));
		
		Collections.sort(readings, Reading.readingComparator);
		System.out.println("Readings sort by both clocks:");
		readings.stream().forEach(r -> System.out.println(r));
		
		// resetirati vrijeme i listu ocitanja
		readings.clear();
	}
	
	public Double getNO2() {
		
		long endTime = System.currentTimeMillis();
		
		int executionTime = (int) (endTime - startTime);
	
		int rowIndex = (executionTime % 100) + 1;
		
        ClassLoader classLoader = StupidUDPClient.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("readings.csv");

        if (inputStream == null) {
            System.err.println("File not found in src/main/resources");
            return null;
        }

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream)) ) {
    
            List<String[]> data = csvReader.readAll();
            
            // dohvati red iz readings.csv
            if (rowIndex >= 0 && rowIndex < data.size()) {
                String[] row = data.get(rowIndex);

                // kreiraj ocitanje na temelju reda
                Double no2 = (row[4] == null || row[4] == "") ? null : Double.parseDouble(row[4]);
                
                return no2;
            } else {
                System.out.println("Row index is out of bounds.");
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
		
		return null;
	}
   
	
}
