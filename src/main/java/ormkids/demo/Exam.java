package ormkids.demo;

import ormkids.E.Column;
import ormkids.E.Table;
import ormkids.E.TableIndices;
import ormkids.E.TableOptions;
import ormkids.IEntity;

@Table
public class Exam implements IEntity {

	@Column(name = "user_id", type = "bigint", nullable = false)
	private Long userId;
	@Column(name = "math", type = "int")
	private Integer math;
	@Column(name = "english", type = "int")
	private Integer english;
	@Column(name = "chinese", type = "int")
	private Integer chinese;
	@Column(name = "physics", type = "int")
	private Integer physics;
	@Column(name = "chemistry", type = "int")
	private Integer chemistry;
	@Column(name = "biology", type = "int")
	private Integer biology;

	public Exam() {
	}

	public Exam(long userId, int math, int english, int chinese, int physics, int chemistry, int biology) {
		this.userId = userId;
		this.math = math;
		this.english = english;
		this.chinese = chinese;
		this.physics = physics;
		this.chemistry = chemistry;
		this.biology = biology;
	}

	public long getUserId() {
		return userId;
	}

	public int getMath() {
		return math;
	}

	public int getEnglish() {
		return english;
	}

	public int getChinese() {
		return chinese;
	}

	public int getPhysics() {
		return physics;
	}

	public int getChemistry() {
		return chemistry;
	}

	public int getBiology() {
		return biology;
	}

	@Override
	public TableOptions options() {
		return new TableOptions().option("engine", "innodb");
	}

	@Override
	public TableIndices indices() {
		return new TableIndices().primary("user_id");
	}

	@Override
	public String table() {
		return "exam";
	}

}
