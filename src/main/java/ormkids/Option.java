package ormkids;

public class Option {
	private String key;
	private String value;

	public Option(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String s() {
		return String.format("%s=%s", key, value);
	}
	
	public String key() {
		return key;
	}
	
	public String value() {
		return value;
	}
}
