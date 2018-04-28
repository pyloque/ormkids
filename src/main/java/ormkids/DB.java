package ormkids;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ormkids.Context.IEventListener;
import ormkids.Q.Filterable;

public abstract class DB {

	private Map<Class<? extends IEntity>, Meta> metas;
	private String name;

	public DB(String name) {
		this(name, new HashMap<>());
	}

	public DB(String name, Map<Class<? extends IEntity>, Meta> metas) {
		this.name = name;
		this.metas = Collections.synchronizedMap(metas);
	}

	public String name() {
		return name;
	}

	private <T extends IEntity> T empty(Class<T> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new KidsException("entity class should provide default constructor");
		}
	}

	private <T extends IEntity> T empty(Class<T> clazz, Object... ids) {
		var empty = empty(clazz);
		var meta = meta(clazz);
		var i = 0;
		for (var name : meta.indices.primary().columns()) {
			var column = meta.columns.get(name);
			try {
				column.field().set(empty, ids[i]);
			} catch (Exception e) {
				throw new KidsException("access field errror", e);
			}
			i++;
		}
		return empty;
	}

	static class Holder<T> {
		T value;
	}

	private List<IEventListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private ThreadLocal<IEventListener> localListener = new ThreadLocal<>();

	public DB on(IEventListener listener) {
		this.listeners.add(listener);
		return this;
	}

	public DB off(IEventListener listener) {
		this.listeners.remove(listener);
		return this;
	}

	@FunctionalInterface
	public static interface IExecutor {
		void execute(Runnable runnable);
	}

	public IExecutor scope(IEventListener listener) {
		return new IExecutor() {

			public void execute(Runnable runnable) {
				localListener.set(listener);
				try {
					runnable.run();
				} finally {
					localListener.remove();
				}
			}

		};
	}

	public boolean invokeListeners(Context event) {
		if (localListener.get() != null) {
			if (!localListener.get().on(event)) {
				return false;
			}
		}
		for (var listener : listeners) {
			if (!listener.on(event)) {
				return false;
			}
		}
		return true;
	}

	@FunctionalInterface
	public static interface ITransactionOp {
		void execute(Connection conn);
	}

	public void withinTx(ITransactionOp op) {
		withinTx(op, true);
	}

	public void withinTx(ITransactionOp op, boolean tx) {
		var conn = conn();
		try {
			op.execute(conn);
			if (tx) {
				conn.commit();
			}
		} catch (SQLException e) {
			if (tx) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
				}
			}
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	@FunctionalInterface
	public static interface IPrepareOp {
		void prepare(PreparedStatement stmt) throws SQLException;
	}

	public void withinPrepare(Connection conn, String sql, IPrepareOp op) {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(sql);
			op.prepare(stmt);
		} catch (SQLException e) {
			throw new KidsException(e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	@FunctionalInterface
	public static interface IQueryOp {
		ResultSet query(PreparedStatement stmt) throws SQLException;
	}

	public void withinQuery(Connection conn, String sql, IQueryOp op) {
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = conn.prepareStatement(sql);
			result = op.query(stmt);
		} catch (SQLException e) {
			throw new KidsException(e);
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
		}
	}

	protected abstract Connection conn();

	public void create(Class<? extends IEntity> clazz) {
		withinTx(conn -> {
			create(conn, clazz, null);
		});
	}

	public void create(Class<? extends IEntity> clazz, String suffix) {
		withinTx(conn -> {
			create(conn, clazz, suffix);
		});
	}

	public void create(Connection conn, Class<? extends IEntity> clazz, String suffix) {
		withinPrepare(conn, createSQL(clazz, suffix), stmt -> {
			stmt.execute();
		});
	}

	private String createSQL(Class<? extends IEntity> clazz, String suffix) {
		var meta = meta(clazz);
		var q = Q.create().table(meta.name, suffix);
		meta.columns.forEach((name, column) -> {
			q.column(column.name(), column.type(), column.autoIncrement(), column.nullable(), column.defaultValue());
		});
		meta.indices.indices().forEach(index -> {
			q.index(index.primaryKey(), index.unique(), index.columns());
		});
		meta.options.options().forEach(option -> {
			q.option(option.key(), option.value());
		});
		return q.sql();
	}

	public void drop(Class<? extends IEntity> clazz) {
		drop(clazz, null);
	}

	public void drop(Class<? extends IEntity> clazz, String suffix) {
		withinTx(conn -> {
			drop(conn, clazz, suffix);
		});
	}

	public void drop(Connection conn, Class<? extends IEntity> clazz, String suffix) {
		withinPrepare(conn, dropSQL(clazz, suffix), stmt -> {
			stmt.execute();
		});
	}

	private String dropSQL(Class<? extends IEntity> clazz, String suffix) {
		var meta = meta(clazz);
		var q = Q.drop().table(meta.name, suffix);
		return q.sql();
	}

	public void truncate(Class<? extends IEntity> clazz) {
		truncate(clazz, null);
	}

	public void truncate(Class<? extends IEntity> clazz, String suffix) {
		withinTx(conn -> {
			truncate(conn, clazz, suffix);
		});
	}

	public void truncate(Connection conn, Class<? extends IEntity> clazz, String suffix) {
		withinPrepare(conn, truncateSQL(clazz, suffix), stmt -> {
			stmt.execute();
		});
	}

	private String truncateSQL(Class<? extends IEntity> clazz, String suffix) {
		var meta = meta(clazz);
		var q = Q.truncate().table(meta.name, suffix);
		return q.sql();
	}

	private <T extends IEntity> T translate(ResultSet rs, Class<T> clazz) throws SQLException {
		var meta = this.meta(clazz);
		var rowMeta = rs.getMetaData();
		try {
			var res = empty(clazz);
			for (int i = 0; i < rowMeta.getColumnCount(); i++) {
				String name = rowMeta.getColumnName(i + 1);
				var column = meta.columns.get(name.toLowerCase());
				if (column != null) {
					column.field().setAccessible(true);
					column.field().set(res, rs.getObject(i + 1));
				}
			}
			return res;
		} catch (Exception e) {
			throw new KidsException("entity class should provide default constructor");
		}
	}

	public <T extends IEntity> T get(Class<T> clazz, Object... ids) {
		var holder = new Holder<T>();
		withinTx(conn -> {
			holder.value = get(conn, clazz, ids);
		}, false);
		return holder.value;
	}

	public <T extends IEntity> T get(Connection conn, Class<T> clazz, Object... ids) {
		var holder = new Holder<T>();
		withinQuery(conn, selectSQL(clazz, ids), stmt -> {
			for (int i = 0; i < ids.length; i++) {
				stmt.setObject(i + 1, ids[i]);
			}
			ResultSet result = stmt.executeQuery();
			if (!result.next()) {
				return null;
			}
			holder.value = translate(result, clazz);
			return result;
		});
		return holder.value;
	}

	private String selectSQL(Class<? extends IEntity> clazz, Object... ids) {
		var meta = meta(clazz);
		var columns = meta.indices.primary().columns();
		if (columns.length != ids.length) {
			throw new KidsException("ids length must match with primary columns");
		}
		var filters = new Filterable[ids.length];
		for (int i = 0; i < ids.length; i++) {
			filters[i] = Q.eq_(columns[i]);
		}
		var empty = empty(clazz, ids);
		var q = Q.select().field("*").table(empty.table(), empty.suffix()).where(Q.and(filters));
		return q.sql();
	}

	public <T extends IEntity> List<T> find(Class<T> clazz) {
		return this.find(clazz, null, 0, 0, null, new Object[] {});
	}

	public <T extends IEntity> List<T> find(Class<T> clazz, Filterable filter, Object... values) {
		return this.find(clazz, null, 0, 0, filter, values);
	}

	public <T extends IEntity> List<T> find(Class<T> clazz, String suffix, Object... values) {
		return this.find(clazz, suffix, 0, 0, null, values);
	}

	public <T extends IEntity> List<T> find(Class<T> clazz, String suffix, Filterable filter, Object... values) {
		return this.find(clazz, suffix, 0, 0, filter, values);
	}

	public <T extends IEntity> List<T> find(Class<T> clazz, String suffix, int offset, int limit, Filterable filter,
			Object... values) {
		var holder = new Holder<List<T>>();
		withinTx(conn -> {
			holder.value = find(conn, clazz, suffix, filter, offset, limit, values);
		}, false);
		return holder.value;
	}

	public <T extends IEntity> List<T> find(Connection conn, Class<T> clazz, String suffix, Filterable filter,
			Object... values) {
		return this.find(conn, clazz, suffix, filter, 0, 0, values);
	}

	public <T extends IEntity> List<T> find(Connection conn, Class<T> clazz, String suffix, Filterable filter,
			int offset, int limit, Object... values) {
		var holder = new Holder<List<T>>();
		withinQuery(conn, findSQL(clazz, suffix, filter, offset, limit), stmt -> {
			int i = 1;
			for (; i <= values.length; i++) {
				stmt.setObject(i, values[i - 1]);
			}
			if (offset > 0) {
				stmt.setObject(i++, offset);
			}
			if (limit > 0) {
				stmt.setObject(i++, limit);
			}
			ResultSet res = stmt.executeQuery();
			var result = new ArrayList<T>();
			while (res.next()) {
				result.add(translate(res, clazz));
			}
			holder.value = result;
			return res;
		});
		return holder.value;
	}

	private String findSQL(Class<? extends IEntity> clazz, String suffix, Filterable filter, int offset, int limit) {
		var meta = meta(clazz);
		var q = Q.select().field("*").table(meta.name, suffix);
		if (filter != null) {
			q.where(filter);
		}
		if (offset > 0) {
			q.offset_();
		}
		if (limit > 0) {
			q.limit_();
		}
		return q.sql();
	}

	public long count(Class<? extends IEntity> clazz) {
		// eliminate warnings
		String suffix = null;
		Filterable filter = null;
		return count(clazz, suffix, filter);
	}

	public long count(Class<? extends IEntity> clazz, Filterable filter, Object... values) {
		return count(clazz, null, filter, values);
	}

	public long count(Class<? extends IEntity> clazz, String suffix) {
		return count(clazz, suffix, null);
	}

	public long count(Class<? extends IEntity> clazz, String suffix, Filterable filter, Object... values) {
		var holder = new Holder<Long>();
		withinTx(conn -> {
			holder.value = count(conn, clazz, suffix, filter, values);
		}, false);
		return holder.value;
	}

	public long count(Connection conn, Class<? extends IEntity> clazz, String suffix, Filterable filter,
			Object... values) {
		var holder = new Holder<Long>();
		var q = countSQL(clazz, suffix, filter);
		var evt = Context.before(this, conn, clazz, q, values);
		if (!this.invokeListeners(evt)) {
			return -1;
		}
		long start = System.nanoTime();
		Exception error = null;
		try {
			withinQuery(conn, q.sql(), stmt -> {
				var i = 1;
				for (; i <= values.length; i++) {
					stmt.setObject(i, values[i - 1]);
				}
				ResultSet res = stmt.executeQuery();
				res.next();
				holder.value = res.getLong(1);
				return res;
			});
		} catch (RuntimeException e) {
			error = e;
		}
		long duration = (System.nanoTime() - start) / 1000;
		evt = Context.after(this, conn, clazz, q, values, error, duration);
		this.invokeListeners(evt);
		return holder.value;
	}

	private Q countSQL(Class<? extends IEntity> clazz, String suffix, Filterable filter) {
		var meta = meta(clazz);
		var q = Q.select().field("count(1)").table(meta.name, suffix);
		if (filter != null) {
			q.where(filter);
		}
		return q;
	}

	public void any(Class<? extends IEntity> clazz, Q q, IQueryOp op, Object... values) {
		this.withinTx(conn -> {
			any(conn, clazz, q, op, values);
		}, false);
	}

	public void any(Connection conn, Class<? extends IEntity> clazz, Q q, IQueryOp op, Object... values) {
		var evt = Context.before(this, conn, clazz, q, values);
		if (!this.invokeListeners(evt)) {
			return;
		}
		long start = System.nanoTime();
		Exception error = null;
		try {
			this.withinQuery(conn, q.sql(), op);
		} catch (RuntimeException e) {
			error = e;
		}
		long duration = (System.nanoTime() - start) / 1000;
		this.invokeListeners(Context.after(this, conn, clazz, q, values, error, duration));
	}

	private <T extends IEntity> Object[] ids(T t) {
		var meta = meta(t.getClass());
		Object[] ids = new Object[meta.indices.primary().columns().length];
		var i = 0;
		for (var name : meta.indices.primary().columns()) {
			var field = meta.columns.get(name).field();
			try {
				ids[i++] = field.get(t);
			} catch (Exception e) {
				throw new KidsException("access field errror", e);
			}
		}
		return ids;
	}

	private <T extends IEntity> Map<String, Object> values(T t) {
		var meta = meta(t.getClass());
		var values = new HashMap<String, Object>();
		meta.columns.forEach((name, column) -> {
			Field f = column.field();
			try {
				values.put(f.getName(), f.get(t));
			} catch (Exception e) {
				throw new KidsException("access field errror", e);
			}
		});
		return values;
	}

	public <T extends IEntity> int delete(T t) {
		return delete(t.getClass(), ids(t));
	}

	public int delete(Class<? extends IEntity> clazz, Object... ids) {
		var holder = new Holder<Integer>();
		withinTx(conn -> {
			holder.value = delete(conn, clazz, ids);
		});
		return holder.value;
	}

	public int delete(Connection conn, Class<? extends IEntity> clazz, Object... ids) {
		var holder = new Holder<Integer>();
		var q = deleteSQL(clazz, ids);
		var evt = Context.before(this, conn, clazz, q, ids);
		if (!this.invokeListeners(evt)) {
			return -1;
		}
		long start = System.nanoTime();
		Exception error = null;
		try {
			withinPrepare(conn, q.sql(), stmt -> {
				for (int i = 0; i < ids.length; i++) {
					stmt.setObject(i + 1, ids[i]);
				}
				holder.value = stmt.executeUpdate();
			});
		} catch (RuntimeException e) {
			error = e;
		}
		long duration = (System.nanoTime() - start) / 1000;
		this.invokeListeners(Context.after(this, conn, clazz, q, ids, error, duration));
		return holder.value;
	}

	private Q deleteSQL(Class<? extends IEntity> clazz, Object... ids) {
		var meta = meta(clazz);
		var columns = meta.indices.primary().columns();
		if (columns.length != ids.length) {
			throw new KidsException("ids length must match with primary columns");
		}
		var filters = new Filterable[ids.length];
		for (int i = 0; i < ids.length; i++) {
			filters[i] = Q.eq_(columns[i]);
		}
		var empty = empty(clazz, ids);
		var q = Q.delete().table(empty.table(), empty.suffix()).where(Q.and(filters));
		return q;
	}

	public <T extends IEntity> int update(T t) {
		return update(t.getClass(), values(t), ids(t));
	}

	public int update(Class<? extends IEntity> clazz, Map<String, Object> values, Object... ids) {
		var holder = new Holder<Integer>();
		var valuesModify = new HashMap<>(values); // copy it for modify
		var meta = meta(clazz);
		// remove pk values
		for (var name : meta.indices.primary().fields()) {
			valuesModify.remove(name);
		}
		withinTx(conn -> {
			holder.value = update(conn, clazz, valuesModify, ids);
		});
		return holder.value;
	}

	public int update(Connection conn, Class<? extends IEntity> clazz, Map<String, Object> values, Object... ids) {
		var holder = new Holder<Integer>();
		var q = updateSQL(clazz, values, ids);
		var hybridValues = new Object[values.size() + ids.length];
		int i = 0;
		for (var value : values.values()) {
			hybridValues[i++] = value;
		}
		for (var id : ids) {
			hybridValues[i++] = id;
		}
		var evt = Context.before(this, conn, clazz, q, hybridValues);
		if (!this.invokeListeners(evt)) {
			return -1;
		}
		long start = System.nanoTime();
		Exception error = null;
		try {
			withinPrepare(conn, q.sql(), stmt -> {
				for (int k = 0; k < hybridValues.length; k++) {
					stmt.setObject(k + 1, hybridValues[k]);
				}
				holder.value = stmt.executeUpdate();
			});
		} catch (RuntimeException e) {
			error = e;
		}
		long duration = (System.nanoTime() - start) / 1000;
		this.invokeListeners(Context.after(this, conn, clazz, q, hybridValues, error, duration));
		return holder.value;
	}

	private Q updateSQL(Class<? extends IEntity> clazz, Map<String, Object> values, Object... ids) {
		var meta = meta(clazz);
		var columns = meta.indices.primary().columns();
		if (columns.length != ids.length) {
			throw new KidsException("ids length must match with primary columns");
		}
		var filters = new Filterable[ids.length];
		for (int i = 0; i < ids.length; i++) {
			filters[i] = Q.eq_(columns[i]);
		}

		var empty = empty(clazz, ids);
		var q = Q.update().table(empty.table(), empty.suffix()).where(Q.and(filters));
		values.forEach((name, value) -> {
			var column = meta.fields.get(name);
			if (column == null || column.primary()) {
				return;
			}
			q.with_(column.name());
		});
		return q;
	}

	public <T extends IEntity> boolean insert(T t) {
		var values = new LinkedHashMap<String, Object>();
		var meta = meta(t.getClass());
		Holder<Integer> lastInsertId = null;
		Field lastInsertField = null;
		for (var entry : meta.columns.entrySet()) {
			var column = entry.getValue();
			try {
				var field = column.field();
				var o = field.get(t);
				if (column.autoIncrement() && o == null) {
					lastInsertId = new Holder<Integer>();
					lastInsertField = column.field();
					continue;
				}
				values.put(field.getName(), o);
			} catch (Exception e) {
				throw new KidsException("access field errror", e);
			}
		}
		boolean res = insert(t, values, lastInsertId);
		if (res && lastInsertId != null && lastInsertId.value != null) {
			try {
				lastInsertField.set(t, lastInsertId.value);
			} catch (Exception e) {
				throw new KidsException("access field errror", e);
			}
		}
		return res;
	}

	public <T extends IEntity> boolean insert(T t, Map<String, Object> values, Holder<Integer> lastInsertId) {
		var holder = new Holder<Boolean>();
		withinTx(conn -> {
			holder.value = insert(conn, t, values);
			if (lastInsertId != null) {
				var q = Q.select().field("last_insert_id()");
				this.any(conn, t.getClass(), q, stmt -> {
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						lastInsertId.value = rs.getInt(1);
					}
					return rs;
				});
			}
		});
		return holder.value;
	}

	public <T extends IEntity> boolean insert(Connection conn, T t, Map<String, Object> values) {
		var meta = meta(t.getClass());
		var q = insertSQL(t, values);
		var paramsList = new ArrayList<>();
		for (var entry : meta.columns.entrySet()) {
			var name = meta.columns.get(entry.getKey()).field().getName();
			var value = values.get(name);
			if (value != null) {
				paramsList.add(value);
			}
		}
		var params = paramsList.toArray();
		var evt = Context.before(this, conn, t.getClass(), q, params);
		if (!this.invokeListeners(evt)) {
			return false;
		}
		long start = System.nanoTime();
		Exception error = null;
		try {
			withinPrepare(conn, q.sql(), stmt -> {
				for (var k = 0; k < params.length; k++) {
					stmt.setObject(k + 1, params[k]);
				}
				stmt.executeUpdate();
			});
		} catch (RuntimeException e) {
			error = e;
		}
		long duration = (System.nanoTime() - start) / 1000;
		this.invokeListeners(Context.after(this, conn, t.getClass(), q, params, error, duration));
		return true;
	}

	private <T extends IEntity> Q insertSQL(T t, Map<String, Object> values) {
		var meta = meta(t.getClass());
		var q = Q.insert().table(t.table(), t.suffix());
		for (var entry : meta.columns.entrySet()) {
			var column = entry.getValue();
			var value = values.get(column.field().getName());
			if (value == null) {
				if (column.autoIncrement()) {
					continue;
				}
				if (column.nullable()) {
					continue;
				}
				if (!column.nullable() && column.defaultValue() != null && !column.defaultValue().isEmpty()) {
					continue;
				}
			}
			q.with_(entry.getKey());
		}
		return q;
	}

	private Meta meta(Class<? extends IEntity> clazz) {
		var meta = metas.get(clazz);
		if (meta != null) {
			return meta;
		}
		var entity = empty(clazz);
		meta = new Meta();
		meta.name = entity.table();
		meta.options = entity.options();
		meta.indices = entity.indices();
		if (meta.indices.primary() == null) {
			throw new KidsException("entity class should provide primary index");
		}
		fillMeta(clazz, meta);
		for (String name : meta.indices.primary().columns()) {
			var column = meta.columns.get(name);
			column.primary(true);
		}
		for (var index : meta.indices.indices()) {
			var fields = new String[index.columns().length];
			var i = 0;
			for (var column : index.columns()) {
				fields[i++] = meta.columns.get(column).field().getName();
			}
			index.fields(fields);
		}
		metas.put(clazz, meta);
		return meta;
	}

	private void fillMeta(Class<? extends IEntity> clazz, Meta meta) {
		for (Field field : clazz.getDeclaredFields()) {
			for (Annotation anno : field.getDeclaredAnnotations()) {
				if (anno instanceof E.Column) {
					if (field.getType().isPrimitive()) {
						throw new KidsException("column must not be primitive type");
					}
					var columnDef = (E.Column) anno;
					var column = new Column(columnDef.name().toLowerCase(), columnDef.type(), columnDef.autoincrement(),
							columnDef.nullable(), columnDef.defaultValue());
					column.field(field);
					field.setAccessible(true);
					meta.column(column);
					break;
				}
			}
		}
		if (meta.columns.isEmpty()) {
			throw new KidsException("entity class should provide at least one column");
		}
	}

}
