package ormkids.demo;

import java.util.Date;
import java.util.zip.CRC32;

import ormkids.DB;
import ormkids.IEntity;
import ormkids.IGridable;
import ormkids.Utils;
import ormkids.E.Column;
import ormkids.E.Table;
import ormkids.E.TableIndices;
import ormkids.E.TableOptions;

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
	public static class GridStrategy<D extends DB> implements IGridable<BookShelf> {

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
