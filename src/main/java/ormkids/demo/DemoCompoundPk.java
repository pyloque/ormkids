package ormkids.demo;

import java.util.Date;
import java.util.HashMap;

import ormkids.Q;

public class DemoCompoundPk {
	private final static String URI = "jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8";

	public static void main(String[] args) {
		var db = new DemoDB("demo", URI);
		try {
			db.create(Member.class);  // 建表
			var member = new Member(1, 2, "boss", null);
			db.insert(member); // 插入
			member = db.get(Member.class, 1, 2); // 主键查询
			System.out.println(member.getTitle());
			member = new Member(2, 2, "manager", new Date());
			db.insert(member); // 再插入
			var count = db.count(Member.class);  // 获取总行数 
			System.out.println(count);
			var members = db.find(Member.class); // 获取全部行
			for (var m : members) {
				System.out.printf("%d %d %s %s\n", m.getUserId(), m.getGroupId(), m.getTitle(), m.getCreatedAt());
			}
			member = new Member(2, 3, "manager", new Date());
			db.insert(member); // 再插入
			members = db.find(Member.class, Q.eq_("group_id"), 2);  // 条件查询
			for (var m : members) {
				System.out.printf("%d %d %s %s\n", m.getUserId(), m.getGroupId(), m.getTitle(), m.getCreatedAt());
			}
			var setters = new HashMap<String, Object>();
			setters.put("title", "employee");
			db.update(Member.class, setters, 2, 3); // 修改
			member = db.get(Member.class, 2, 3); // 主键查询
			System.out.println(member.getTitle());
			db.delete(Member.class, 1, 2); // 删除
			db.delete(Member.class, 2, 2); // 删除
			db.delete(Member.class, 2, 3); // 删除
			count = db.count(Member.class); // 再获取总行数
			System.out.println(count);
		} finally {
			db.drop(Member.class); // 删表
		}
	}

}
