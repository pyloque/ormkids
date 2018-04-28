package ormkids;

import ormkids.E.TableIndices;
import ormkids.E.TableOptions;

/**
 * 所有的实体类必须实现该接口
 */
public interface IEntity {

	/**
	 * 表名
	 * @return
	 */
	String table();

	/**
	 * 分表必须覆盖此方法
	 * @return
	 */
	default String suffix() {
		return null;
	}

	default String tableWithSuffix() {
		return tableWith(suffix());
	}

	default String tableWith(String suffix) {
		return Utils.tableWithSuffix(table(), suffix);
	}
	
	/**
	 * 定义表的物理结构属性如engine=innodb，用于动态创建表
	 * @return
	 */
	TableOptions options();

	/**
	 * 定义表的主键和索引信息，用于动态创建表
	 * @return
	 */
	TableIndices indices();

}
