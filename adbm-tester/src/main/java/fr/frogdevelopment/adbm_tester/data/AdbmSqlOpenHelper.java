package fr.frogdevelopment.adbm_tester.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AdbmSqlOpenHelper extends SQLiteOpenHelper {

	// When changing the database schema, increment the database version.
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "adbm_tester.db";

	public AdbmSqlOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		DataContract.create(db);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
