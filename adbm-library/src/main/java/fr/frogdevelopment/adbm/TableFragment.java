package fr.frogdevelopment.adbm;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class TableFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String QUERY_TABLE_PAGINATED = "SELECT * from %s LIMIT %s OFFSET %s";
	private static final String QUERY_COUNT_ROW = "SELECT count(*) from %s";

	private static final int LOADER_ID_TABLE_QUERY = 1;
	private static final int LOADER_ID_COUNT_ROW = 2;

	static final String NULL_VALUE = "null";
	static final Map<String, Integer> COLUMNS = new HashMap<>();

	private ADBMCallBack mCallBack;

	private TableLayout mTableLayout;

	// PAGINATION
	private int numberOfPages = 0;
	private int nbRows = 10;
	private int currentPageNumber;
	private int countRows;

	private ProgressBar mProgressBar;
	private Button mPreviousButton;
	private TextView mNbPageTextView;
	private Button mNextButton;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mCallBack = (ADBMCallBack) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement " + ADBMCallBack.class.getSimpleName());
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallBack = null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// rootView = scrollview so we can scroll it ...
		// also anchor for SnackBar
		ScrollView mRootView = new ScrollView(getActivity());
		mRootView.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mRootView.setPadding(nbRows, nbRows, nbRows, nbRows);

		// the main linear layout to which all views will be added.
		LinearLayout mainLayout = new LinearLayout(getActivity());
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setScrollContainer(true);
		mRootView.addView(mainLayout);

		mProgressBar = new ProgressBar(getActivity());

		// *****************************************
		LinearLayout mNbRowsLayout = new LinearLayout(getActivity());
		mNbRowsLayout.setOrientation(LinearLayout.HORIZONTAL);
		mNbRowsLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		TextView nbRowsTextView = new TextView(getActivity());
		nbRowsTextView.setTextSize(20);
		nbRowsTextView.setText("Number of rows :");
		mNbRowsLayout.addView(nbRowsTextView);

		Spinner mNbRowsSpinner = new Spinner(getActivity(), Spinner.MODE_DIALOG);
		mNbRowsSpinner.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		ArrayAdapter nbRowsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item, Arrays.asList("10", "20", "50", "100"));
		nbRowsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mNbRowsSpinner.setAdapter(nbRowsAdapter);
		mNbRowsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				nbRows = Integer.parseInt(parent.getItemAtPosition(position).toString());
				numberOfPages = countRows / nbRows + 1;
				displayTablePage(currentPageNumber);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		mNbRowsLayout.addView(mNbRowsSpinner);

		mainLayout.addView(mNbRowsLayout);

		// *****************************************
		// layout with buttons for the pagination
		RelativeLayout mNavigationLayout = new RelativeLayout(getActivity());
		RelativeLayout.LayoutParams navigationLayoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		mNavigationLayout.setLayoutParams(navigationLayoutParams);

		mPreviousButton = new Button(getActivity());
		mPreviousButton.setText("Previous");
		RelativeLayout.LayoutParams previousLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		previousLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
		mPreviousButton.setLayoutParams(previousLayoutParams);
		mPreviousButton.setOnClickListener(v -> displayTablePage(--currentPageNumber));
		mNavigationLayout.addView(mPreviousButton);

		mNbPageTextView = new TextView(getActivity());
		mNbPageTextView.setTextSize(20);
		RelativeLayout.LayoutParams nbPageLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		nbPageLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		mNbPageTextView.setLayoutParams(nbPageLayoutParams);
		mNbPageTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		mNavigationLayout.addView(mNbPageTextView);

		mNextButton = new Button(getActivity());
		mNextButton.setText("Next");
		RelativeLayout.LayoutParams nextLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		nextLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
		mNextButton.setLayoutParams(nextLayoutParams);
		mNextButton.setOnClickListener(v -> displayTablePage(++currentPageNumber));
		mNavigationLayout.addView(mNextButton);

		mainLayout.addView(mNavigationLayout);

		// *****************************************
		mProgressBar.setMinimumHeight(50);
		mProgressBar.setMinimumWidth(50);
		mainLayout.addView(mProgressBar);

		// *****************************************
		// The table
		mTableLayout = new TableLayout(getActivity());
		mTableLayout.setHorizontalScrollBarEnabled(true);

		// the horizontal scroll view for table if the table content does not fit into screen
		HorizontalScrollView mHorizontalScrollView = new HorizontalScrollView(getActivity());
		mHorizontalScrollView.addView(mTableLayout);
		mHorizontalScrollView.setScrollbarFadingEnabled(false);
		mHorizontalScrollView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
		mainLayout.addView(mHorizontalScrollView);

		return mRootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(LOADER_ID_COUNT_ROW, getArguments(), this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		mProgressBar.setVisibility(View.VISIBLE);

		String query;
		switch (id) {
			case LOADER_ID_COUNT_ROW:
				query = String.format(QUERY_COUNT_ROW, args.getString("tableName"));
				break;

			case LOADER_ID_TABLE_QUERY:
				query = String.format(QUERY_TABLE_PAGINATED, args.getString("tableName"), args.getInt("nbRows"), args.getInt("offset"));
				break;

			default:
				return null;
		}
		return new ADBMAsyncTaskLoader(getActivity(), mCallBack, query);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mProgressBar.setVisibility(View.GONE);

		int id = loader.getId();

		switch (id) {
			case LOADER_ID_COUNT_ROW:
				if (cursor.moveToNext()) {
					countRows = cursor.getInt(0);
					numberOfPages = countRows / nbRows + 1;

					cursor.close();
					getLoaderManager().destroyLoader(loader.getId());

					displayTablePage(1);
				} else {
					countRows = 0;
					numberOfPages = countRows / nbRows + 1;

					cursor.close();
					getLoaderManager().destroyLoader(loader.getId());

					// fixme
				}
				break;

			case LOADER_ID_TABLE_QUERY:
				while (cursor.moveToNext()) {

					if (COLUMNS.isEmpty()) { // just once
						// get columns name
						TableRow header = new TableRow(getActivity());
						header.setPadding(1, 1, 1, 1);

						for (int columnIndex = 0, nbCol = cursor.getColumnCount(); columnIndex < nbCol; columnIndex++) {
							String columnName = cursor.getColumnName(columnIndex);
							int columnType = cursor.getType(columnIndex);
							COLUMNS.put(columnName, columnType);

							header.addView(createCellHeader(columnName));
						}

						mTableLayout.addView(header);
					}

					final TableRow tableRow = createRow(cursor);
					mTableLayout.addView(tableRow);

//					// add a listener on the row for edition
//					// fixme utiliser des EditText ??
//					tableRow.setOnClickListener(v -> {
//						Map<String, String> currentRowValues = new HashMap<>();
//						for (int i = 0; i < tableRow.getChildCount(); i++) {
//							TextView cell = (TextView) tableRow.getChildAt(i);
//
//							String columnName = (String) cell.getTag();
//							String value = cell.getText().toString();
//							currentRowValues.put(columnName, value);
//						}
//
//						updateOrDeleteRow(currentRowValues);
//					});

					cursor.close();
					getLoaderManager().destroyLoader(loader.getId());
				}
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	private void displayTablePage(int pageNumber) {
		this.currentPageNumber = pageNumber;

		getActivity().runOnUiThread(() -> mNbPageTextView.setText(currentPageNumber + "/" + numberOfPages));

		// clear data
		COLUMNS.clear();
		mTableLayout.removeAllViews();

		mPreviousButton.setEnabled(currentPageNumber > 1);
		mNextButton.setEnabled(currentPageNumber < numberOfPages);

		int offset;
		if (countRows <= nbRows) {
			offset = 0;
		} else {
			offset = pageNumber * nbRows - nbRows;
		}

		getArguments().putInt("nbRows", nbRows);
		getArguments().putInt("offset", offset);
		getLoaderManager().restartLoader(LOADER_ID_TABLE_QUERY, getArguments(), this);
	}

	@NonNull
	private TextView createCellHeader(String columnName) {
		final TextView cellHeader = new TextView(getActivity());
		cellHeader.setBackgroundColor(Color.DKGRAY);
		cellHeader.setTextColor(Color.WHITE);
		cellHeader.setPadding(4, 4, 4, 4);
		cellHeader.setGravity(Gravity.CENTER);

		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		layoutParams.setMargins(1, 1, 1, 1);
		cellHeader.setLayoutParams(layoutParams);
		cellHeader.setText(columnName);

		return cellHeader;
	}

	@NonNull
	private TableRow createRow(Cursor cursor) {
		final TableRow row = new TableRow(getActivity());
		row.setBackgroundColor(Color.BLACK);
		row.setPadding(1, 1, 1, 1);

		for (int columnIndex = 0, nbCol = cursor.getColumnCount(); columnIndex < nbCol; columnIndex++) {
			String columnName = cursor.getColumnName(columnIndex);
			int columnType = cursor.getType(columnIndex);

			String value;
			switch (columnType) {
				case Cursor.FIELD_TYPE_STRING:
					value = cursor.getString(columnIndex);
					break;

				case Cursor.FIELD_TYPE_INTEGER:
					value = String.valueOf(cursor.getInt(columnIndex));
					break;

				case Cursor.FIELD_TYPE_FLOAT:
					value = String.valueOf(cursor.getFloat(columnIndex));
					break;

				default:
					// Column data is not handle, do not display it
					value = null;
			}

			final TextView cell = new TextView(getActivity());
			cell.setBackgroundColor(Color.WHITE);
			cell.setTextColor(Color.BLACK);
			cell.setPadding(4, 4, 4, 4);

			TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
			layoutParams.setMargins(1, 1, 1, 1);
			cell.setLayoutParams(layoutParams);
			if (value == null) {
				cell.setText(NULL_VALUE);
				cell.setTypeface(null, Typeface.BOLD_ITALIC);
			} else {
				cell.setText(value);
				cell.setTypeface(Typeface.DEFAULT);
			}

			cell.setTag(columnName);

			row.addView(cell);
		}
		return row;
	}

}
