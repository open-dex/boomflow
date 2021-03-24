package boomflow.eip712.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Template {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private Map<String, List<Entry>> types = new HashMap<String, List<Entry>>();
	private String primaryType;
	private Domain domain;
	private Object message;
	
	public Template(TypedData data) {
		this.types.put(Domain.PRIMARY_TYPE, Domain.SCHEMA);
		this.types.putAll(data.schemas());
		this.primaryType = data.primaryType();
		this.domain = data.domain();
		this.message = data;
	}
	
	public Map<String, List<Entry>> getTypes() {
		return types;
	}
	
	public String getPrimaryType() {
		return primaryType;
	}
	
	public Domain getDomain() {
		return domain;
	}
	
	public Object getMessage() {
		return message;
	}
	
	public String toJson() {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize EIP712 template", e);
		}
	}
	
	@Override
	public String toString() {
		return this.toJson();
	}

}
