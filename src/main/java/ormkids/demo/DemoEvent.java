package ormkids.demo;

public class DemoEvent {

	private final static String URI = "jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8";

	public static void main(String[] args) {
		var db = new DemoDB("demo", URI);
		db.on(ctx -> { // 全局事件回调
			System.out.printf("db=%s sql=%s cost=%dus\n", ctx.db().name(), ctx.q().sql(), ctx.duration());
			return true; // 返回false会导致事件链终止，后续的ORM操作也不会执行
		});
		try {
			db.create(User.class);
			db.scope(ctx -> { // 范围回调，execute方法内部的所有ORM操作都会回调
				System.out.printf("db=%s sql=%s cost=%dus\n", ctx.db().name(), ctx.q().sql(), ctx.duration());
				return true;
			}).execute(() -> {
				db.count(User.class);
				db.find(User.class);
			});
		} finally {
			db.drop(User.class); // 删除表
		}
	}

}
