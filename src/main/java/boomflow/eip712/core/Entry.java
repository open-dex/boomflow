package boomflow.eip712.core;

import boomflow.common.Utils;

public class Entry {
	
	private String name;
	private String type;
	
	public Entry(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return Utils.toJson(this);
	}
	
}