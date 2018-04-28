package ormkids;

import java.nio.charset.Charset;

public class Utils {
	
	public static Charset UTF8 = Charset.forName("utf8");

	public static String tableWithSuffix(String table, String suffix) {
		if (suffix == null) {
			return table;
		}
		return table + "_" + suffix;
	}

}
