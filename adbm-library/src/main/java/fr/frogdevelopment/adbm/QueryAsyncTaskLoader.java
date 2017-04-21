package fr.frogdevelopment.adbm;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

class QueryAsyncTaskLoader extends AsyncTaskLoader<Cursor> {

	interface ResultHandler {
		void handleStarted();

		void handleException(SQLException e);

		void handleResult(int id, @NonNull Cursor cursor);
	}

	private final SQLiteOpenHelper mSqLiteOpenHelper;
	private final String mQuery;
	private final ResultHandler mHandler;
	private final int id;

	QueryAsyncTaskLoader(@NonNull Context context, ResultHandler handler, int id, @NonNull SQLiteOpenHelper sqLiteOpenHelper, @NonNull String query) {
		super(context);
		this.mHandler = handler;
		this.id = id;
		this.mSqLiteOpenHelper = sqLiteOpenHelper;
		this.mQuery = query;
	}

	protected void onStartLoading() {
		forceLoad();
	}

	@Override
	public boolean isStarted() {
		mHandler.handleStarted();
		return super.isStarted();
	}

	@Override
	public Cursor loadInBackground() {
		try {
			Log.d("ADBM", "mQuery=" + mQuery);
			return mSqLiteOpenHelper.getReadableDatabase().rawQuery(mQuery, null);
		} catch (SQLException e) {
			Log.e("ADBM", "Error while querying", e);
			mHandler.handleException(e);

			return null;
		}
	}

	@Override
	public void deliverResult(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		mHandler.handleResult(id, cursor);

		cursor.close();
	}
}
