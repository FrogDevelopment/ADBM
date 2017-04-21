package fr.frogdevelopment.adbm;

import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;

interface ADBMCallBack {
	SQLiteOpenHelper getSqLiteOpenHelper();

	void showError(SQLException message);
}
