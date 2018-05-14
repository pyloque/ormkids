package ormkids;

public interface IGridable<T extends IEntity> {

	/**
	 * 根据实体对象选择分库索引
	 * @param dbs db num
	 * @param t entity object
	 * @return db index
	 */
	int select(int dbs, T t);

	/**
	 * 根据特定参数选择分库索引
	 * @param dbs db num
	 * @param params sharding parameters
	 * @return db index
	 */
	int select(int dbs, Object... params);

}