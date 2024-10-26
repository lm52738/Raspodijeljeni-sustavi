package hr.fer.tel.rassus.senzori.stupidudp.network;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

/**
 * The `Reading` class represents a sensor reading with multiple attributes, 
 * including an ID, a NO2 concentration value, a vector clock for tracking 
 * distributed events, and a scalar clock.
 */
public class Reading implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int id;
	private Double no2;
	private Map<Integer, Integer> vectorClock;
	private Long scalarClock;
	
	public Reading() {
		super();
	}
	
	public Reading(int id, Double no2, Map<Integer, Integer> vectorClock, long scalarClock) {
		super();
		this.id = id;
		this.no2 = no2;
		this.vectorClock = vectorClock;
		this.scalarClock = scalarClock;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Double getNo2() {
		return no2;
	}
	
	public void setNo2(Double no2) {
		this.no2 = no2;
	}
	
	public Map<Integer, Integer> getVectorClock() {
		return vectorClock;
	}

	public void setVectorClock(Map<Integer, Integer> vectorClock) {
		this.vectorClock = vectorClock;
	}

	public Long getScalarClock() {
		return scalarClock;
	}

	public void setScalarClock(Long scalarClock) {
		this.scalarClock = scalarClock;
	}
	
	@Override
	public String toString() {
		return "Reading [id=" + id + ", no2=" + no2 + ", vectorClock=" + vectorClock + ", scalarClock=" + scalarClock
				+ "]";
	}

	public static Comparator<Reading> readingScalarComparator = (r1,r2) ->
		r1.getScalarClock().compareTo(r2.getScalarClock());

	public static Comparator<Reading> readingVectorComparator = (r1, r2) ->
	    r1.getVectorClock().get(r1.getId()).compareTo(r2.getVectorClock().get(r2.getId()));

	public static Comparator<Reading> readingComparator =  readingScalarComparator.thenComparing(readingVectorComparator);

	
}
