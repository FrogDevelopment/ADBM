package fr.frogdevelopment.adbm_tester.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class DataContract implements BaseColumns {

	// Queries
	private static final String SQL_CREATE_1 = "CREATE TABLE emptyTable (_id INTEGER PRIMARY KEY AUTOINCREMENT, column_null NULL, column_integer INTEGER, column_integer_not_null INTEGER NOT NULL, column_real REAL, column_real_not_null REAL NOT NULL, column_text TEXT, column_text_not_null TEXT NOT NULL, column_blob BLOB);";
	private static final String SQL_CREATE_2 = "CREATE TABLE filledTable (_id INTEGER PRIMARY KEY AUTOINCREMENT, column_1 INTEGER NOT NULL, column_2 TEXT, column_3 TEXT NOT NULL);";

	private static final String SQL_DROP_1 = "DROP TABLE IF EXISTS emptyTable;";
	private static final String SQL_DROP_2 = "DROP TABLE IF EXISTS filledTable;";

	static void create(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_1);
		db.execSQL(SQL_CREATE_2);

		ContentValues contentValues = new ContentValues(3);
		for (int i = 0; i < 1000; i++) {
			contentValues.put("column_1",i);
			contentValues.put("column_2","string  "+i);
			contentValues.put("column_3","blablabla");
			db.insert("filledTable",null, contentValues);
		}
	}

	static void drop(SQLiteDatabase db) {
		db.execSQL(SQL_DROP_1);
		db.execSQL(SQL_DROP_2);
	}
}
