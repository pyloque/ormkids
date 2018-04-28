package ormkids;

import java.util.HashMap;
import java.util.Map;

import ormkids.Context.IEventListener;

public abstract class GridDB<D extends DB> {

	private D[] dbs;
	private Map<Class<? extends IEntity>, IGridable<? extends IEntity>> gridables = new HashMap<>();

	public GridDB(D[] dbs) {
		this.dbs = dbs;
	}

	public int size() {
		return dbs.length;
	}

	public <T extends IEntity> GridDB<D> gridWith(Class<T> clazz, IGridable<T> gridable) {
		this.gridables.put(clazz, gridable);
		return this;
	}

	public D selectIndex(int idx) {
		return dbs[idx];
	}

	@SuppressWarnings("unchecked")
	public <T extends IEntity> D select(T t) {
		var gridable = (IGridable<T>) (gridables.get(t.getClass()));
		int idx = gridable.select(dbs.length, t);
		return dbs[idx];
	}

	@SuppressWarnings("unchecked")
	public <T extends IEntity> D select(Class<T> clazz, Object... params) {
		var gridable = (IGridable<T>) (gridables.get(clazz));
		int idx = gridable.select(dbs.length, params);
		return dbs[idx];
	}

	public GridDB<D> on(IEventListener listener) {
		for (D db : dbs) {
			db.on(listener);
		}
		return this;
	}

	public GridDB<D> off(IEventListener listener) {
		for (D db : dbs) {
			db.off(listener);
		}
		return this;
	}

	public GridDB<D> once(IEventListener listener) {
		for (D db : dbs) {
			db.scope(listener);
		}
		return this;
	}

	public void create(Class<? extends IEntity> clazz, String suffix) {
		for (D db : dbs) {
			db.create(clazz, suffix);
		}
	}

	public void drop(Class<? extends IEntity> clazz, String suffix) {
		for (D db : dbs) {
			db.drop(clazz, suffix);
		}
	}

	public void truncate(Class<? extends IEntity> clazz, String suffix) {
		for (D db : dbs) {
			db.truncate(clazz, suffix);
		}
	}

	public <T extends IEntity> long count(Class<T> clazz, int dbIndex, String suffix) {
		var db = selectIndex(dbIndex);
		return db.count(clazz, suffix);
	}

	public <T extends IEntity> T get(Class<T> clazz, Object... ids) {
		var db = select(clazz, ids);
		return db.get(clazz, ids);
	}

	public <T extends IEntity> boolean insert(T t) {
		var db = select(t);
		return db.insert(t);
	}

	public <T extends IEntity> int update(T t) {
		var db = select(t);
		return db.update(t);
	}

	public <T extends IEntity> int delete(T t) {
		var db = select(t);
		return db.delete(t);
	}

	public abstract void registerGridables();

}
