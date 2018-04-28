package ormkids.demo;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Random;

import ormkids.Q;

public class DemoComplexQuery {
	private final static String URI = "jdbc:mysql://localhost:3306/mydrc?user=mydrc&password=mydrc&useUnicode=true&characterEncoding=UTF8";

	public static void main(String[] args) {
		var db = new DemoDB("demo", URI);
		try {
			db.create(Exam.class); // 建表
			var random = new Random();
			for (var i = 0; i < 100; i++) {
				var userId = Math.abs(random.nextLong());
				var exam = new Exam(userId, random.nextInt(100), random.nextInt(100), random.nextInt(100),
						random.nextInt(100), random.nextInt(100), random.nextInt(100));
				db.insert(exam); // 插入
			}
			System.out.println(db.count(Exam.class)); // 查询总行数
			// math >= 50
			var exams = db.find(Exam.class, Q.ge_("math"), 50); // 条件查询
			System.out.println(exams.size());
			var count = db.count(Exam.class, Q.ge_("math"), 50); // 条件总行数
			System.out.println(count);
			// math > 50 & english >= 50
			exams = db.find(Exam.class, Q.and(Q.gt_("math"), Q.ge_("english")), 50, 50); // 条件查询
			System.out.println(exams.size());
			count = db.count(Exam.class, Q.and(Q.gt_("math"), Q.ge_("english")), 50, 50); // 条件总行数
			System.out.println(count);
			// math > 50 || english >= 50
			exams = db.find(Exam.class, Q.or(Q.gt_("math"), Q.ge_("english")), 50, 50); // 条件查询
			System.out.println(exams.size());
			count = db.count(Exam.class, Q.or(Q.gt_("math"), Q.ge_("english")), 50, 50); // 条件总行数
			System.out.println(count);
			// math > 50 && (english >= 50 || chinese > 60)
			exams = db.find(Exam.class, Q.and(Q.gt_("math"), Q.or(Q.ge_("english"), Q.gt_("chinese"))), 50, 50, 60); // 条件查询
			System.out.println(exams.size());
			count = db.count(Exam.class, Q.and(Q.gt_("math"), Q.or(Q.ge_("english"), Q.gt_("chinese"))), 50, 50, 60); // 条件总行数
			System.out.println(count);
			// math > 50 || physics between 60 and 80 || chemistry < 60
			exams = db.find(Exam.class, Q.or(Q.gt_("math"), Q.between_("physics"), Q.lt_("chemistry")), 50, 60, 80, 60); // 条件查询
			System.out.println(exams.size());
			count = db.count(Exam.class, Q.or(Q.gt_("math"), Q.between_("physics"), Q.lt_("chemistry")), 50, 60, 80,
					60); // 条件总行数
			System.out.println(count);
			// group by math / 10
			var q = Q.select().field("(math div 10) * 10 as mathx", "count(1)").table("exam").groupBy("mathx")
					.having(Q.gt_("count(1)")).orderBy("count(1)", "desc"); // 复杂sql构造
			var rank = new LinkedHashMap<Integer, Integer>();
			db.any(Exam.class, q, stmt -> { // 原生sql查询
				stmt.setInt(1, 0);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					rank.put(rs.getInt(1), rs.getInt(2));
				}
				return rs;
			});
			rank.forEach((mathx, c) -> {
				System.out.printf("[%d-%d) = %d\n", mathx, mathx + 10, c);
			});
		} finally {
			db.drop(Exam.class);
		}
	}

}
