package fr.frogdevelopment.adbm;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
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

public class TableListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String QUERY_TABLES_NAME = "SELECT name _id FROM sqlite_master WHERE type ='table'";

	interface ADBMTablesCallBack extends ADBMCallBack {

		void onTableClick(String tableName);
	}

	private ADBMTablesCallBack mCallBack;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallBack = (ADBMTablesCallBack) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement " + ADBMTablesCallBack.class.getSimpleName());
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

		TextView textView = new TextView(getActivity());
		textView.setId(android.R.id.empty);
		textView.setText("No data");
		textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearLayout.addView(textView);

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
		return new ADBMAsyncTaskLoader(getActivity(), mCallBack, QUERY_TABLES_NAME);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		List<String> tableNames = new ArrayList<>();
		while (cursor.moveToNext()) {
			tableNames.add(cursor.getString(0));
		}
		setListAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, tableNames));


		cursor.close();
		getLoaderManager().destroyLoader(loader.getId());
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		setListAdapter(null);
	}
}
