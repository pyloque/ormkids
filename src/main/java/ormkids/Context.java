package ormkids;

import java.sql.Connection;

public class Context {

	@FunctionalInterface
	public static interface IEventListener {
		boolean on(Context ctx);
	}

	private DB db;
	private Connection conn;
	private Class<? extends IEntity> clazz;
	private Q q;
	private Object[] values;
	private boolean before;
	private Exception error;
	private long duration; // microsecond

	private final static Object[] EmptyValues = new Object[] {};

	public static Context before(DB db, Connection conn, Class<? extends IEntity> clazz, Q q, Object[] values) {
		var ctx = new Context();
		ctx.db = db;
		ctx.conn = conn;
		ctx.clazz = clazz;
		ctx.q = q;
		ctx.values = values;
		ctx.before = true;
		return ctx;
	}

	public static Context after(DB db, Connection conn, Class<? extends IEntity> clazz, Q q, Object[] values,
			Exception error, long duration) {
		var ctx = new Context();
		ctx.db = db;
		ctx.conn = conn;
		ctx.clazz = clazz;
		ctx.q = q;
		ctx.values = values;
		ctx.before = false;
		ctx.error = error;
		ctx.duration = duration;
		return ctx;
	}

	public Class<? extends IEntity> clazz() {
		return clazz;
	}

	public Q q() {
		return q;
	}

	public DB db() {
		return db;
	}

	public Object[] values() {
		return values != null ? values : EmptyValues;
	}

	public boolean before() {
		return before;
	}

	public boolean after() {
		return !before;
	}

	public Connection conn() {
		return conn;
	}

	public Exception error() {
		return error;
	}

	public Context error(Exception error) {
		this.error = error;
		return this;
	}

	public long duration() {
		return duration;
	}

}
