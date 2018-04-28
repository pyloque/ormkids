package ormkids.demo;

import java.util.HashMap;

import ormkids.Q;

public class DemoSimplePk {

	private final static String URI = "jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8";

	public static void main(String[] args) {
		var db = new DemoDB("demo", URI);
		try {
			db.create(User.class); // 创建表
			var user = new User("test1", "nick1", "passwd1");
			db.insert(user); // 插入
			System.out.println(user.getId());
			user = db.get(User.class, user.getId());  // 主键查询
			System.out.printf("%s %s %s %s %s\n", user.getId(), user.getName(), user.getNick(), user.getPasswd(),
					user.getCreatedAt());
			user = new User("test2", "nick2", "passwd2");
			db.insert(user); // 再插入
			var count = db.count(User.class); // 查询总行数
			System.out.println(count);
			var users = db.find(User.class);  // 列出所有行
			System.out.println(users.size());
			for (var u : users) {
				System.out.printf("%s %s %s %s %s\n", u.getId(), u.getName(), u.getNick(), u.getPasswd(),
						u.getCreatedAt());
			}
			users = db.find(User.class, Q.eq_("nick"), "nick2"); // 条件查询
			System.out.println(users.size());
			var setters = new HashMap<String, Object>();
			setters.put("passwd", "whatever");
			db.update(User.class, setters, 2); // 修改
			users = db.find(User.class); // 再列出所有行
			System.out.println(users.size());
			for (var u : users) {
				System.out.printf("%s %s %s %s %s\n", u.getId(), u.getName(), u.getNick(), u.getPasswd(),
						u.getCreatedAt());
			}
			db.delete(User.class, 1); // 删除
			db.delete(User.class, 2); // 再删除
			count = db.count(User.class); // 统计所有行
			System.out.println(count);
		} finally {
			db.drop(User.class); // 删除表
		}
	}

}
