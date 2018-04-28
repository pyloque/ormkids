package ormkids;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class E {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	public @interface Table {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface Column {
		String name();

		String type();

		boolean nullable() default true;

		boolean autoincrement() default false;

		String defaultValue() default "";
	}

	public static class TableOptions {
		private List<Option> options = new ArrayList<>();

		public TableOptions option(String key, String value) {
			this.options.add(new Option(key, value));
			return this;
		}

		public List<Option> options() {
			return options;
		}
	}

	public static class TableIndices {
		private List<Index> indices = new ArrayList<>();
		private Index primary;

		public TableIndices unique(String... columns) {
			this.indices.add(new Index(false, true, columns));
			return this;
		}

		public TableIndices primary(String... columns) {
			this.primary = new Index(true, false, columns);
			this.indices.add(primary);
			return this;
		}

		public Index primary() {
			return primary;
		}

		public TableIndices index(String... columns) {
			this.indices.add(new Index(false, false, columns));
			return this;
		}

		public List<Index> indices() {
			return indices;
		}
	}

}
