
package hr.fer.tel.rassus.sensors.stupidudp.network;

import java.util.Random;

/*
 * EmulatedSystemClock simulates a system clock with jitter (random time variation),
 * which emulates slight inaccuracies in time measurement that may occur in distributed
 * or emulated environments. The clock provides time in milliseconds and can be updated
 * with external time values to correct any drift over time.
 */
public class EmulatedSystemClock {

    private long startTime;
    private double jitter; // jitter per second,  percentage of deviation per 1 second

    public EmulatedSystemClock() {
        startTime = System.currentTimeMillis();
        Random r = new Random();
        jitter = (r.nextInt(20 )) / 100d; // divide by 10 to get the interval between [0, 20], and then divide by 100 to get percentage
    }

    public long currentTimeMillis() {
        long current = System.currentTimeMillis();
        long diff =current - startTime;
        double coef = diff / 1000;
        return startTime + Math.round(diff * Math.pow((1+jitter), coef));
    }
    
    public void updateTimeMillis(long otherTimeMillis) {
    	long current = currentTimeMillis();
    	this.startTime += otherTimeMillis - current + 1;
    }


}