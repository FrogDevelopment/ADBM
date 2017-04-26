package fr.frogdevelopment.adbm;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TableListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, ADBMAsyncTaskLoader.ErrorHandler {

	private static final String QUERY_TABLES_NAME = "SELECT name _id FROM sqlite_master WHERE type ='table'";

	interface CallBack {
		SQLiteOpenHelper getSqLiteOpenHelper();

		void onTableClick(String tableName);
	}

	private CallBack mCallBack;
	private TextView mEmptyView;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallBack = (CallBack) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement " + CallBack.class.getSimpleName());
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallBack = null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout linearLayout = new LinearLayout(getActivity());
		linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		ListView listView = new ListView(getActivity());
		listView.setId(android.R.id.list);
		listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearLayout.addView(listView);

		mEmptyView = new TextView(getActivity());
		mEmptyView.setId(android.R.id.empty);
		mEmptyView.setText("No data");
		mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearLayout.addView(mEmptyView);

		return linearLayout;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String tableName = (String) getListAdapter().getItem(position);
		mCallBack.onTableClick(tableName);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new ADBMAsyncTaskLoader(getActivity(), mCallBack.getSqLiteOpenHelper(), QUERY_TABLES_NAME, this);
	}

	@Override
	public void onError(String message) {
		setListAdapter(null);
		mEmptyView.setText(message);
		mEmptyView.setBackgroundColor(Color.RED);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		List<String> tableNames = new ArrayList<>();
		while (cursor.moveToNext()) {
			tableNames.add(cursor.getString(0));
		}
		cursor.close();
		getLoaderManager().destroyLoader(loader.getId());

		setListAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, tableNames));

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		setListAdapter(null);
	}
}
