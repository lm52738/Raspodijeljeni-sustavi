package hr.fer.tel.rassus.senzori.stupidudp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simplified simulated DatagramSocket that mimics network conditions 
 * by introducing configurable packet loss and delay. This class can be used 
 * to simulate real-world network conditions for testing purposes.
 */
public class SimpleSimulatedDatagramSocket extends DatagramSocket {

    private final double lossRate;
    private final int averageDelay;
    private final Random random;

    /**
     * Constructor for creating a simulated server-side DatagramSocket.
     * This constructor binds the socket to a specified port.
     * 
     * @param port
     * @param lossRate
     * @param averageDelay
     * @throws SocketException
     * @throws IllegalArgumentException
     */
    public SimpleSimulatedDatagramSocket(int port, double lossRate, int averageDelay) throws SocketException, IllegalArgumentException {
        super(port);
        random = new Random();

        this.lossRate = lossRate;
        this.averageDelay = averageDelay;

        // Set time to wait for answer
        super.setSoTimeout(0);
    }

    /**
     * Constructor for creating a simulated client-side DatagramSocket.
     * This constructor does not bind the socket to a specific port.
     *
     * @param lossRate
     * @param averageDelay
     * @throws SocketException
     * @throws IllegalArgumentException
     */
    public SimpleSimulatedDatagramSocket(double lossRate, int averageDelay) throws SocketException, IllegalArgumentException {
        random = new Random();

        this.lossRate = lossRate;
        this.averageDelay = averageDelay;

        // Set time to wait for answer
        super.setSoTimeout(4 * averageDelay);
    }

    /**
     * Sends a datagram packet over the simulated network, introducing potential
     * packet loss and delay. The method checks if the packet passes the loss rate
     * threshold before sending.
     *
     * @param packet
     * @throws IOException 
     */
    @Override
    public void send(DatagramPacket packet) throws IOException {
        // Check if packet should be sent based on the loss rate.
        if (random.nextDouble() >= lossRate) {
            // Delay is uniformly distributed between 0 and 2 times the average delay.
            new Thread(new OutgoingDatagramPacket(packet, (long) (2 * averageDelay * random.nextDouble()))).start();
        }
    }

    /**
     * Inner class for internal use.
     */
    private class OutgoingDatagramPacket implements Runnable {

        private final DatagramPacket packet;
        private final long time;

        private OutgoingDatagramPacket(DatagramPacket packet, long time) {
            this.packet = packet;
            this.time = time;
        }

        @Override
        public void run() {
            try {
                //simulate network delay
                Thread.sleep(time);
                SimpleSimulatedDatagramSocket.super.send(packet);
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (IOException ex) {
                Logger.getLogger(SimulatedDatagramSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

