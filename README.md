OrmKids
==
支持分库分表的MySQL单表ORM框架，暂用于学习，后续会在生产环境进行检验

```
<dependency>
    <groupId>com.github.pyloque</groupId>
    <artifactId>ormkids</artifactId>
    <version>0.0.4</version>
</dependency>
```

功能特性
--
1. 代码简洁，没有任何依赖项
2. 易于使用，无须复杂的配置
3. 提供自动创建表功能
4. 支持分库又分表，可以只分库，也可以只分表
5. 支持groupby/having/order by
6. 支持原生SQL
7. 支持事件回调，可用于服务跟踪调试和动态sql改写

不支持多表关联
--
1. 多表比较复杂，实现成本高，学习成本也高，容易出错
2. 常用的多表的操作一般都可以使用多条单表操作组合实现
3. 在分库分表的场合，很少使用多表操作
4. 不使用外键，专注于sql逻辑

db.withinTx
--
对于复杂的多表查询和批量数据处理，可以使用该方法。
用户可以获得原生的jdbc链接，通过编写jdbc代码来实现。

Q
--
用户可以使用Q对象构建复杂的SQL查询


其它数据库支持
--
暂时没有


实体接口
--

```java
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
```

单表单主键
--

```java
@Table
public class User implements IEntity {

	@Column(name = "id", type = "int", autoincrement = true, nullable = false)
	private Integer id;
	@Column(name = "name", type = "varchar(255)", nullable = false)
	private String name;
	@Column(name = "nick", type = "varchar(255)", nullable = false)
	private String nick;
	@Column(name = "passwd", type = "varchar(255)")
	private String passwd;
	@Column(name = "created_at", type = "datetime", nullable = false, defaultValue = "now()")
	private Date createdAt;

	public User() {
	}

	public User(String name, String nick, String passwd) {
		this.name = name;
		this.nick = nick;
		this.passwd = passwd;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNick() {
		return nick;
	}

	public String getPasswd() {
		return passwd;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public TableOptions options() {
		return new TableOptions().option("engine", "innodb");
	}

	@Override
	public TableIndices indices() {
		return new TableIndices().primary("id").unique("name");
	}

	@Override
	public String table() {
		return "user";
	}

}

```

单表复合主键
--

```java
@Table
public class Member implements IEntity {

	@Column(name = "user_id", type = "int", nullable = false)
	private Integer userId;
	@Column(name = "group_id", type = "int", nullable = false)
	private Integer groupId;
	@Column(name = "title", type = "varchar(255)")
	private String title;
	@Column(name = "created_at", type = "datetime", nullable = false, defaultValue = "now()")
	private Date createdAt;

	public Member() {
	}

	public Member(Integer userId, Integer groupId, String title, Date createdAt) {
		this.userId = userId;
		this.groupId = groupId;
		this.title = title;
		this.createdAt = createdAt;
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public String getTitle() {
		return title;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public TableOptions options() {
		return new TableOptions().option("engine", "innodb");
	}

	@Override
	public TableIndices indices() {
		return new TableIndices().primary("user_id", "group_id");
	}

	@Override
	public String table() {
		return "member";
	}

}
```

分库接口
--

```java
public interface IGridable<T extends IEntity> {

	/**
	 * 根据实体对象选择分库索引
	 */
	int select(int dbs, T t);

	/**
	 * 根据特定参数选择分库索引
	 */
	int select(int dbs, Object... params);

}
```

分库分表
--

```java
@Table
public class BookShelf implements IEntity {

	public final static int PARTITIONS = 4;

	@Column(name = "user_id", type = "varchar(255)", nullable = false)
	private String userId;
	@Column(name = "book_id", type = "varchar(255)", nullable = false)
	private String bookId;
	@Column(name = "comment", type = "varchar(255)")
	private String comment;
	@Column(name = "created_at", type = "datetime", nullable = false, defaultValue = "now()")
	private Date createdAt;

	public BookShelf() {
	}

	public BookShelf(String userId, String bookId, String comment, Date createdAt) {
		this.userId = userId;
		this.bookId = bookId;
		this.comment = comment;
		this.createdAt = createdAt;
	}

	public String getUserId() {
		return userId;
	}

	public String getBookId() {
		return bookId;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public String table() {
		return "book_shelf";
	}

	@Override
	public TableOptions options() {
		return new TableOptions().option("engine", "innodb");
	}

	@Override
	public TableIndices indices() {
		return new TableIndices().primary("user_id", "book_id");
	}

	/* 
	 * 分表策略
	 */
	@Override
	public String suffix() {
		var crc32 = new CRC32();
		crc32.update(userId.getBytes(Utils.UTF8));
		return String.valueOf(Math.abs(crc32.getValue()) % PARTITIONS);
	}

	/**
	 * 分库策略
	 */
	public static class GridStrategy implements IGridable<BookShelf> {

		@Override
		public int select(int dbs, BookShelf t) {
			return Math.abs(t.getUserId().hashCode()) % dbs;
		}

		@Override
		public int select(int dbs, Object... params) {
			String userId = (String) params[0];
			return Math.abs(userId.hashCode()) % dbs;
		}

	}

}
```

定义单个数据库
--

```java
public class DemoDB extends DB {

	private DataSource ds;

	public DemoDB(String name, String uri) {
		this(name, new HashMap<>(), uri);
	}

	public DemoDB(String name, Map<Class<? extends IEntity>, Meta> metas, String uri) {
		super(name, metas);
		var ds = new MysqlConnectionPoolDataSource(); // 连接池
		ds.setUrl(uri);
		this.ds = ds;
	}

	@Override
	protected Connection conn() {  // 获取链接
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			throw new KidsException(e);
		}
	}

}
```

定义网格数据库——分库
--

```java
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

```

单表单主键增删改查
--

```java
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
```

单表复合主键增删改查
--

```java
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
```

复杂查询
--

```java
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
```

分表
--

```java
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
```

分库
--

```java
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
```

事件上下文对象
--

```java
public class Context {

	private DB db; // 数据库实例
	private Connection conn;  // 当前的链接
	private Class<? extends IEntity> clazz; // 当前的实体类
	private Q q; // 查询sql
	private Object[] values; // 查询的绑定参数
	private boolean before; // before or after
	private Exception error; // 异常
	private long duration; // 耗时microsecond

}
```

事件回调
--

```java
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
```

相关链接
--
撸web框架 https://github.com/pyloque/httpkids

撸rpc框架 https://github.com/pyloque/rpckids

撸依赖注入框架 https://github.com/pyloque/iockids

讨论
--
关注公众号「码洞」，跟大佬们一起撸
