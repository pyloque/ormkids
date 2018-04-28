package ormkids.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ormkids.IEntity;
import ormkids.Meta;

public class DemoSharding {

	private static DemoDB[] dbs = new DemoDB[3];
	static {
		Map<Class<? extends IEntity>, Meta> metas = new HashMap<>();
		dbs[0] = new DemoDB("demo-0", metas,
				"jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8");
		dbs[1] = new DemoDB("demo-1", metas,
				"jdbc:mysql://localhost:3307/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8");
		dbs[2] = new DemoDB("demo-2", metas,
				"jdbc:mysql://localhost:3308/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8");
	}

	public static void main(String[] args) {
		var grid = new GridDemoDB(dbs); // 构造Grid实例
		try {
			for (int k = 0; k < BookShelf.PARTITIONS; k++) {
				grid.create(BookShelf.class, String.valueOf(k)); // 创建所有分库中的分表
			}
			var bss = new ArrayList<BookShelf>();
			for (int i = 0; i < 100; i++) {
				var bs = new BookShelf("user" + i, "book" + i, "comment" + i, new Date());
				bss.add(bs);
				grid.insert(bs); // 插入，自动分发到相应的分库中的分表
			}
			for (int k = 0; k < grid.size(); k++) {
				for (int i = 0; i < BookShelf.PARTITIONS; i++) {
					System.out.printf("db %d partition %d count %d\n", k, i,
							grid.count(BookShelf.class, k, String.valueOf(i))); // 依次查询出所有分库的分表的行数
				}
			}
			Random random = new Random();
			for (var bs : bss) {
				bs.setComment("comment_update_" + random.nextInt(100));
				grid.update(bs); // 更新，自动分发到相应的分库中的分表
			}
			for (var bs : bss) {
				bs = grid.get(BookShelf.class, bs.getUserId(), bs.getBookId()); // 主键查询，自动分发到相应的分库中的分表
				System.out.println(bs.getComment());
			}
			for (var bs : bss) {
				grid.delete(bs); // 删除，自动分发到相应的分库中的分表
			}
			for (int k = 0; k < grid.size(); k++) {
				for (int i = 0; i < BookShelf.PARTITIONS; i++) {
					System.out.printf("db %d partition %d count %d\n", k, i,
							grid.count(BookShelf.class, k, String.valueOf(i))); // 依次查询出所有分库的分表的行数
				}
			}
		} finally {
			for (int k = 0; k < BookShelf.PARTITIONS; k++) {
				grid.drop(BookShelf.class, String.valueOf(k)); // 删除所有分库中的分表
			}
		}
	}

}
