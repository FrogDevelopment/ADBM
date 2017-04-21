package fr.frogdevelopment.adbm;

import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;

public abstract class ADBMActivity extends AppCompatActivity implements ADBMCallBack, TableListFragment.ADBMTablesCallBack {

	static final String NULL_VALUE = "null";
	static final Map<String, Integer> COLUMNS = new HashMap<>();

	private FrameLayout mFrameLayout;

	public abstract SQLiteOpenHelper getSqLiteOpenHelper();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("Android Data Base Manager");

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// *****************************************
		RelativeLayout relativeLayout = new RelativeLayout(this);

		mFrameLayout = new FrameLayout(this);
		mFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mFrameLayout.setId(View.generateViewId());
		relativeLayout.addView(mFrameLayout);

		setContentView(relativeLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		TableListFragment fragment = new TableListFragment();

		getFragmentManager()
				.beginTransaction()
				.replace(mFrameLayout.getId(), fragment)
				.commit();
	}

	@Override
	public void showError(SQLException message) {
		// fixme
//		mMessageTextView.setBackgroundColor(Color.RED);
//		mMessageTextView.setText("Error: " + e.getMessage());
	}

	@Override
	public void onTableClick(String tableName) {
		TableFragment fragment = new TableFragment();
		Bundle args = new Bundle();
		args.putString("tableName", tableName);
		fragment.setArguments(args);

		getFragmentManager()
				.beginTransaction()
				.replace(mFrameLayout.getId(), fragment)
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
