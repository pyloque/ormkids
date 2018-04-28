package ormkids.demo;

import ormkids.GridDB;

public class GridDemoDB extends GridDB<DemoDB> {

	/**
	 * 传进来多个DB对象
	 */
	public GridDemoDB(DemoDB[] dbs) {
		super(dbs);
		this.registerGridables();
	}

	/* 
	 * 注册实体类的分库策略
	 */
	@Override
	public void registerGridables() {
		this.gridWith(BookShelf.class, new BookShelf.GridStrategy<DemoDB>());
	}

}
