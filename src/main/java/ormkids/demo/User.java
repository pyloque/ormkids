package ormkids.demo;

import java.util.Date;

import ormkids.IEntity;
import ormkids.E.Column;
import ormkids.E.Table;
import ormkids.E.TableIndices;
import ormkids.E.TableOptions;

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
