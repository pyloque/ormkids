package ormkids.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class DemoPartitioning {
	private final static String URI = "jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8";

	public static void main(String[] args) {
		var db = new DemoDB("demo", URI);
		try {
			for (int i = 0; i < BookShelf.PARTITIONS; i++) {
				db.create(BookShelf.class, String.valueOf(i)); // 创建所有分表
			}
			var bss = new ArrayList<BookShelf>();
			for (int i = 0; i < 100; i++) {
				var bs = new BookShelf("user" + i, "book" + i, "comment" + i, new Date());
				bss.add(bs);
				db.insert(bs); // 插入，自动插入相应分表
			}
			for (int i = 0; i < BookShelf.PARTITIONS; i++) {
				System.out.printf("partition %d count %d\n", i, db.count(BookShelf.class, String.valueOf(i)));
			}
			Random random = new Random();
			for (var bs : bss) {
				bs.setComment("comment_update_" + random.nextInt(100));
				db.update(bs); // 更新，自动更新相应分表数据
			}
			bss = new ArrayList<BookShelf>();
			for (int i = 0; i < BookShelf.PARTITIONS; i++) {
				bss.addAll(db.find(BookShelf.class, String.valueOf(i))); // 指定分表列出所有行
			}
			for (var bs : bss) {
				System.out.println(bs.getComment());
			}
			for (var bs : bss) {
				db.delete(bs); // 挨个删除，自动删除相应分表数据
			}
		} finally {
			for (int i = 0; i < BookShelf.PARTITIONS; i++) {
				db.drop(BookShelf.class, String.valueOf(i)); // 删除所有分表
			}
		}
	}

}
