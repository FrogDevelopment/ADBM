package fr.frogdevelopment.adbm;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;

public class ADBMCustomQueryFragment extends _ADBMDisplayTableFragment implements LoaderManager.LoaderCallbacks<Cursor>, QueryAsyncTaskLoader.ResultHandler {

	private static final int LOADER_ID_CUSTOM_QUERY = 3;

	private ADBMCallBack mCallBack;

	private EditText mCustomQueryEditText;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		mCallBack = (ADBMCallBack) context;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallBack = null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = super.onCreateView(inflater, container, savedInstanceState);

		// *****************************************
		// CUSTOM QUERY
		mCustomQueryEditText = new EditText(getActivity());
		mCustomQueryEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD); // http://stackoverflow.com/questions/33148168/inputtype-type-text-flag-no-suggestions-in-samsung
		mCustomQueryEditText.setSingleLine(false);
		mMainLayout.addView(mCustomQueryEditText, 0);

		Button mSubmitQueryButton = new Button(getActivity());
		mSubmitQueryButton.setText("Submit Query");
		mSubmitQueryButton.setOnClickListener(v -> {
			String customQuery = mCustomQueryEditText.getText().toString();

			if (!TextUtils.isEmpty(customQuery)) {
				Bundle args = new Bundle();
				args.putString("customQuery", customQuery);
				getLoaderManager().initLoader(LOADER_ID_CUSTOM_QUERY, args, this);
//			} else {
				// fixme
			}
		});
		mMainLayout.addView(mSubmitQueryButton, 1);

		return rootView;
	}

	// https://bitbucket.org/ssutee/418496_mobileapp/src/fc5ee705a2fd1253a3ce9c1455b9597de6273ed8/demo/DotDotListDB/src/th/ac/ku/android/sutee/dotdotlist/DotDotListDBActivity.java?at=master&fileviewer=file-view-default
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		mProgressBar.setVisibility(View.VISIBLE);

		String customQuery = mCustomQueryEditText.getText().toString().trim();
		if (customQuery.startsWith("SELECT")) {
			return new QueryAsyncTaskLoader(getActivity(), this, id, mCallBack.getSqLiteOpenHelper(), customQuery);
		} else {
			/// fixme
			return new CustomExecuteLoader(getActivity(), customQuery);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mProgressBar.setVisibility(View.GONE);

		getLoaderManager().destroyLoader(LOADER_ID_CUSTOM_QUERY);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	@Override
	public void handleStarted() {
		mTableLayout.removeAllViews();
	}

	@Override
	public void handleException(SQLException e) {
		mCallBack.showError(e);
	}

	@Override
	public void handleResult(int id, @NonNull Cursor cursor) {
		// Table headers
		TableRow header = new TableRow(getActivity());
		header.setPadding(1, 1, 1, 1);
		for (String columnName : cursor.getColumnNames()) {
			header.addView(createCellHeader(columnName));
		}
		mTableLayout.addView(header);

		// Data
		while (cursor.moveToNext()) {
			mTableLayout.addView(createRow(cursor));
		}

		mProgressBar.setVisibility(View.GONE);

		getLoaderManager().destroyLoader(LOADER_ID_CUSTOM_QUERY);

		Snackbar.make(mRootView, "Number of rows returned : " + cursor.getCount(), Snackbar.LENGTH_LONG).show();
	}

	private static class CustomExecuteLoader extends AsyncTaskLoader<Cursor> {

		private final String query;

		private CustomExecuteLoader(@NonNull Context context, @NonNull String query) {
			super(context);
			this.query = query;
		}

		@Override
		public boolean isStarted() {
//			mTableLayout.removeAllViews();
			return super.isStarted();
		}

		@Override
		public Cursor loadInBackground() {
//			SQLiteDatabase database = null;
//			try {
//				Log.d("ADBM", "execute query=" + query);
//
//				database =  mCallBack.getSqLiteOpenHelper().getWritableDatabase();
//				database.beginTransaction();
//
//				database.execSQL(query);
//
//				database.setTransactionSuccessful();
//			} finally {
//				if (database != null) database.endTransaction();
//			}

			return null;
		}
	}


}
