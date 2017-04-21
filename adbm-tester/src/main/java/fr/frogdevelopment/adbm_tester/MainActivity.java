package fr.frogdevelopment.adbm_tester;

import android.database.sqlite.SQLiteOpenHelper;

import fr.frogdevelopment.adbm.ADBMActivity;
import fr.frogdevelopment.adbm_tester.data.AdbmSqlOpenHelper;

public class MainActivity extends ADBMActivity {

	@Override
	public SQLiteOpenHelper getSqLiteOpenHelper() {

		return new AdbmSqlOpenHelper(this);
	}

}
