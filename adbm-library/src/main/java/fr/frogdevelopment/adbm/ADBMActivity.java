package fr.frogdevelopment.adbm;

import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;

public abstract class ADBMActivity extends AppCompatActivity implements TableListFragment.CallBack , TableFragment.CallBack {

	static final String NULL_VALUE = "null";
	static final Map<String, Integer> COLUMNS = new HashMap<>();

	public abstract SQLiteOpenHelper getSqLiteOpenHelper();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setTitle("Android Data Base Manager");

//		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//		setSupportActionBar(toolbar);

//		ActionBar actionBar = getSupportActionBar();
//		if (actionBar != null) {
//			actionBar.setDisplayHomeAsUpEnabled(true);
//		}

		TableListFragment fragment = new TableListFragment();

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.main_frame, fragment)
				.commit();
	}

	@Override
	public void onTableClick(String tableName) {
		TableFragment fragment = new TableFragment();
		Bundle args = new Bundle();
		args.putString("tableName", tableName);
		fragment.setArguments(args);

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.main_frame, fragment)
				.addToBackStack(null)
				.commit();
	}

	@Override
	public void onBackPressed() {
		if (getFragmentManager().getBackStackEntryCount() > 0) {
			getFragmentManager().popBackStack();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
