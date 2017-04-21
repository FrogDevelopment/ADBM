package fr.frogdevelopment.adbm_tester.data;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class DataContract implements BaseColumns {

	public static final String TABLE_NAME = "test";

	public static final String COL_1 = "column_1";
	public static final String COL_2 = "column_2";
	public static final String COL_3 = "column_3";

	// Queries
	private static final String SQL_CREATE = "CREATE TABLE test (_id INTEGER PRIMARY KEY AUTOINCREMENT, column_1 INTEGER NOT NULL, COL_2 TEXT, COL_3 TEXT NOT NULL);";

	static final String SQL_INSERT = "INSERT INTO entry (seq,kanji,reading) VALUES (?,?,?)";

	private static final String SQL_DROP = "DROP TABLE IF EXISTS entry;";

	static void create(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE);
	}

	static void drop(SQLiteDatabase db) {
		db.execSQL(SQL_DROP);
	}
}
