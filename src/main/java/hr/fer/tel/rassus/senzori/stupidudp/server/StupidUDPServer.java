/*
 * This code has been developed at Departement of Telecommunications,
 * Faculty of Electrical Engineering and Computing, University of Zagreb.
 */
package hr.fer.tel.rassus.senzori.stupidudp.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import hr.fer.tel.rassus.senzori.MessageListener;
import hr.fer.tel.rassus.senzori.stupidudp.network.Reading;
import hr.fer.tel.rassus.senzori.stupidudp.network.SimpleSimulatedDatagramSocket;

public class StupidUDPServer {
	
	private byte[] rcvBuf = new byte[1024]; // received bytes
	private byte[] ackBuf = new byte[256];// sent bytes
	private DatagramSocket socket;
	private MessageListener messageListener;
	private boolean isRunning;
	
	public StupidUDPServer(int port, MessageListener messageListener ) {
		try {
			socket = new SimpleSimulatedDatagramSocket(port, 0.3, 1000);
			this.messageListener = messageListener;
			isRunning = true;
		} catch (SocketException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}


 
    public void start() throws IOException, ClassNotFoundException {      

    	System.out.println("Udp server started ...");
    	
        while (isRunning) { 
        	
            DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length);

            socket.receive(packet); 
            
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteStream);

            // receives a Reading object (including reading, vector clock, and scalar clock)
            Reading receivedReading = (Reading) objectInputStream.readObject(); 
            
            System.out.println("Server received: " + receivedReading.toString());
            
            // Saves package into class Sensor ( messageListener.onUdpMessageReceived(object) )
            messageListener.onUdpMessageReceived(receivedReading);
            
            // Sends an acknowledgment packet (Boolean)
            ackBuf = Boolean.TRUE.toString().getBytes();            
            System.out.println("Server sends: " + Boolean.TRUE.toString());

            DatagramPacket confirmationPacket = new DatagramPacket(ackBuf, 
            		ackBuf.length, packet.getAddress(), packet.getPort());
            
            socket.send(confirmationPacket); 
            
            objectInputStream.close();
            byteStream.close();
        }
    }
    
    public void stopServer() {
        isRunning = false;
        // Closing the socket will terminate the blocking 'receive' call and allow the server to stop
        
        socket.close();
        System.out.println("Stopping UDP server...");
    }
}
