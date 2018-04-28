package ormkids;

import java.lang.reflect.Field;

public class Column {
	private String name;
	private String type;
	private boolean primary;
	private boolean autoIncrement;
	private boolean nullable;
	private String defaultValue;

	private Field field;

	public Column(String name, String type) {
		this(name, type, false, true, null);
	}

	public Column(String name, String type, boolean autoIncrement, boolean nullable, String defaultValue) {
		this.name = name;
		this.type = type;
		this.autoIncrement = autoIncrement;
		this.nullable = nullable;
		this.defaultValue = defaultValue;
	}

	public Column field(Field field) {
		this.field = field;
		return this;
	}

	public void primary(boolean primary) {
		this.primary = primary;
	}

	public boolean primary() {
		return this.primary;
	}

	public Field field() {
		return this.field;
	}

	public String s() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("`%s`", name));
		builder.append(" ");
		builder.append(type);
		if (!nullable) {
			builder.append(" not null");
		}
		if (autoIncrement) {
			builder.append(" auto_increment");
		}

		if (defaultValue != null && !defaultValue.isEmpty()) {
			builder.append(" default ");
			builder.append(defaultValue);
		}
		return builder.toString();
	}

	public String name() {
		return name;
	}

	public String type() {
		return type;
	}

	public boolean autoIncrement() {
		return autoIncrement;
	}

	public boolean nullable() {
		return nullable;
	}

	public String defaultValue() {
		return defaultValue;
	}
}
