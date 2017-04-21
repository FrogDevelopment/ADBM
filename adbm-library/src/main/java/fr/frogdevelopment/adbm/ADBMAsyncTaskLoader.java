package fr.frogdevelopment.adbm;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

class ADBMAsyncTaskLoader extends AsyncTaskLoader<Cursor> {

	private final ADBMCallBack adbmCallBack;
	private final String query;

	ADBMAsyncTaskLoader(Context context, ADBMCallBack adbmCallBack, String query) {
		super(context);
		this.adbmCallBack = adbmCallBack;
		this.query = query;

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
			Log.d("ADBM", "mQuery=" + query);
			return adbmCallBack.getSqLiteOpenHelper().getReadableDatabase().rawQuery(query, null);
		} catch (SQLException e) {
			Log.e("ADBM", "Error while querying", e);
			adbmCallBack.showError(e);

			return null;
		}
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}
}