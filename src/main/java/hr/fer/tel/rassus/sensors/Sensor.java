package hr.fer.tel.rassus.sensors;

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

import hr.fer.tel.rassus.sensors.kafka.KafkaConsumer;
import hr.fer.tel.rassus.sensors.kafka.KafkaProducer;
import hr.fer.tel.rassus.sensors.kafka.Registration;
import hr.fer.tel.rassus.sensors.stupidudp.client.StupidUDPClient;
import hr.fer.tel.rassus.sensors.stupidudp.network.EmulatedSystemClock;
import hr.fer.tel.rassus.sensors.stupidudp.network.Reading;
import hr.fer.tel.rassus.sensors.stupidudp.server.StupidUDPServer;

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

    private Map<Integer, Registration> sensors; // Stores other registered sensors
    private List<Reading> readings; // Stores received readings
	
    // Clocks for time tracking
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
	
	/**
     * Starts the sensor's main processes: Kafka consumer and UDP server each in separate threads.
     */
	public void run() {    

		startTime = System.currentTimeMillis();
		vectorClock.put(id, 0);
		
		// Start KafkaConsumer in a separate thread
        Thread kafkaThread = new Thread(() -> {
            kafkaConsumer.initializeConsumer(id);
            kafkaConsumer.pollMessages();
        });
        kafkaThread.start();
	    
        // Start UDP server in a separate thread
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
	
	/**
     * Registers the sensor by sending its information to the registration topic.
     */
	private void register() {

		System.out.printf("Sensor got a message:(%s : %s)\n",COMMAND, START);
		
		// Initialize KafkaProducer for sending the registration message
	    KafkaProducer kafkaProducer = new KafkaProducer();
	    kafkaProducer.initializeProducer();
	    
	    // Create and send registration message as JSON
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

	/**
     * Stops the sensor's work by closing communication channels and printing all readings.
     */
	private void stopWorking() {
		
		System.out.printf("Sensor got a message:(%s : %s)\n",COMMAND, STOP);

		// Stop UDP server and Kafka consumer communication
        udpServer.stopServer();
        kafkaConsumer.closeConsumer();
		
        // Print all collected readings
		printReadings();
		
		// Stop UDP communciation
		System.out.println("Sensor shutting down ...");
		System.exit(0);
	}
	
	/**
     * Saves the registration details of other sensors from the received message.
     * 
     * @param message JSON string containing sensor registration information
     */
	private void saveRegistration(String message) {
		
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
		
		
		// Once all sensors are registered, start UDP communication in a new thread
		if (messageCounter == sensorCount-1) {
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

	/**
     * Manages UDP communication by generating and sending readings to all registered sensors.
     */
	private void udpCommunication() throws IOException, ClassNotFoundException, InterruptedException {
		
		System.out.println("UDP communication started ...");
		
		Timer timer = new Timer();

		// Schedule reading output every 5 seconds
        timer.schedule(new ReadingPrinterTask(), 0, 5 * 1000);
		
		while(true) {
			System.out.println("--------------------------------");
			
			// Generate a new reading
			Double no2 = getNO2();
			
			Integer clock = vectorClock.get(id);
			clock++;
			vectorClock.put(id, clock);
			
			Reading reading = new Reading(id, no2, vectorClock, systemClock.currentTimeMillis());
			System.out.println("New generated reading: " + reading.toString());
			
			// Send the reading to all registered sensors
			for (Registration registration : sensors.values()) {
				
				StupidUDPClient udpClient = new StupidUDPClient(registration.getPort());
				udpClient.sendMessage(reading);	
				System.out.println();
			}
			
		}
	}
	
	@Override
	public void onUdpMessageReceived(Reading reading) {
		
		// Check and synchronize scalar clocks if the received clock is ahead
		long scalarClock = systemClock.currentTimeMillis();
		if (reading.getScalarClock() > scalarClock) {
			System.out.println();
			System.out.println("The message reception time " + scalarClock + 
					" is earlier than the message sending time " + reading.getScalarClock() + "!!");
			systemClock.updateTimeMillis(reading.getScalarClock());
			System.out.println("New updated scalar clock: " + systemClock.currentTimeMillis());
			System.out.println();
		}
		
		// Increment local vector clock
		Integer clock = vectorClock.get(id);
		clock++;
		
		// Update the vector clock with the received vector clock and new local clock
		vectorClock = reading.getVectorClock();
		vectorClock.put(id, clock);
		
		// Add the reading to the list of collected readings
		readings.add(reading);		
	}
	
	/**
     * Timer task to print readings and calculate the average reading every 5 seconds.
     */
	class ReadingPrinterTask extends TimerTask {
        @Override
        public void run() {
        	
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
	
	/**
     * Sorts and prints readings based on different clock comparisons.
     */
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
		
		readings.clear(); // Clear readings after printing
	}
	
	/**
     * Reads NO2 value from a CSV file based on elapsed time to simulate sensor data.
     * 
     * @return NO2 reading as Double or null if reading fails
     */
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
            
            if (rowIndex >= 0 && rowIndex < data.size()) {
                String[] row = data.get(rowIndex);
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
