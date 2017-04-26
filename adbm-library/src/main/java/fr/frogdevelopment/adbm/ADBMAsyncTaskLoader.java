package fr.frogdevelopment.adbm;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

class ADBMAsyncTaskLoader extends AsyncTaskLoader<Cursor> {

	interface ErrorHandler {
		void onError(String message);
	}

	private final ErrorHandler mCallBack;
	private final String mQuery;
	private final SQLiteOpenHelper mSqLiteOpenHelper;

	ADBMAsyncTaskLoader(@NonNull Context context, @NonNull SQLiteOpenHelper sqLiteOpenHelper, @NonNull String query, @NonNull ErrorHandler callBack) {
		super(context);
		this.mSqLiteOpenHelper = sqLiteOpenHelper;
		this.mCallBack = callBack;
		this.mQuery = query;

		// run only once
		onContentChanged();
	}

	@Override
	protected void onStartLoading() {
		// That's how we start every AsyncTaskLoader...
		// -  code snippet from  android.content.CursorLoader  (method  onStartLoading)
		if (takeContentChanged()) {
			forceLoad();
		}
	}

	@Override
	public Cursor loadInBackground() {
		try {
			Log.d("ADBM", "mQuery=" + mQuery);
			return mSqLiteOpenHelper.getReadableDatabase().rawQuery(mQuery, null);
		} catch (SQLException e) {
			Log.e("ADBM", "Error while querying", e);
			mCallBack.onError(e.getMessage());

			return null;
		}
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}
}
