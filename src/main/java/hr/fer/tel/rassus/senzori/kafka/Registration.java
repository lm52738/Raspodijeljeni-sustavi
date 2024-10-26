package hr.fer.tel.rassus.senzori.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Registration {

	private int id;
	private String address;
	private int port;
	
	
	
	@JsonCreator
    public Registration(@JsonProperty("id") int id, @JsonProperty("address") String address, @JsonProperty("port") int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return "Message [id=" + id + ", address=" + address + ", port=" + port + "]";
	}
	
	
	
}
