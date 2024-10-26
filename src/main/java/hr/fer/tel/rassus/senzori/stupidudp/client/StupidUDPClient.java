/*
 * This code has been developed at Departement of Telecommunications,
 * Faculty of Electrical Eengineering and Computing, University of Zagreb.
 */
package hr.fer.tel.rassus.senzori.stupidudp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hr.fer.tel.rassus.senzori.stupidudp.network.Reading;
import hr.fer.tel.rassus.senzori.stupidudp.network.SimpleSimulatedDatagramSocket;


public class StupidUDPClient {

    private InetAddress address;
    private DatagramSocket socket;
    private static final byte[] rcvBuf = new byte[256];
    private int port;
   
    
    
    public StupidUDPClient(int port) {
    	try {
			this.address = InetAddress.getByName("localhost");
			this.socket = new SimpleSimulatedDatagramSocket(0.3, 1000); //SOCKET
			this.port = port;
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }

    public void sendMessage(Reading reading) throws IOException {
    	
    	ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
    	
    	objectOutputStream.writeObject(reading);
        objectOutputStream.flush();

        byte[] sendData = byteStream.toByteArray();
    	
        // salje objekt Reading (ocitanje, vektorsku vremensku oznaku, skalarnu vremensku oznaku)
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);
        
        System.out.println("Client sent: " + reading.toString());

        Boolean retransmision = Boolean.FALSE;

        while (true) {
            DatagramPacket ackPacket = new DatagramPacket(rcvBuf, rcvBuf.length);

            // ceka da primi paket potvrde
            try {
                socket.receive(ackPacket); 
                String receivedAck = new String(ackPacket.getData(), ackPacket.getOffset(), ackPacket.getLength());

                if (receivedAck.equals(Boolean.TRUE.toString())) {
                    System.out.println("Client received ACK."); 
                } else {
                    System.out.println("Client did not receive ACK.");
                }
                
                break;
            } catch (SocketTimeoutException e) {
            	retransmision = Boolean.TRUE;
                break;
            } catch (IOException ex) {
                Logger.getLogger(StupidUDPClient.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        // ako nije primio paket potvrde tj. dogodio se SocketTimeoutException
        // obavlja retransmisiju paketa
        if (retransmision.equals(Boolean.TRUE)) {
        	System.out.println("Client retransmision: " + reading.toString());
        	sendMessage(reading);
        } else
        	socket.close();
    }
    
    
}
