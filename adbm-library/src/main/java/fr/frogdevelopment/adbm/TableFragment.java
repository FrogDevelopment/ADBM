package fr.frogdevelopment.adbm;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class TableFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ADBMAsyncTaskLoader.ErrorHandler {


	static class ColumnInfo {
		String name;
		int type;
		boolean nullable;

		static ColumnInfo create(String name, String type, boolean nullable) {
			return create(name, getFieldType(type), nullable);
		}

		static ColumnInfo create(String name, int type, boolean nullable) {
			ColumnInfo columnInfo = new ColumnInfo();
			columnInfo.name = name;
			columnInfo.type = type;
			columnInfo.nullable = nullable;

			return columnInfo;
		}

		private static int getFieldType(String type) {
			switch (type) {
				case "INTEGER":
					return Cursor.FIELD_TYPE_INTEGER;
				case "REAL":
					return Cursor.FIELD_TYPE_FLOAT;
				case "TEXT":
					return Cursor.FIELD_TYPE_STRING;
				case "BLOB":
					return Cursor.FIELD_TYPE_BLOB;
				default:
					return Cursor.FIELD_TYPE_NULL;
			}
		}
	}

	private static final String QUERY_COUNT_ROW = "SELECT count(*) from %s";
	private static final String QUERY_TABLE_INFO = "PRAGMA table_info(%s)";
	private static final String QUERY_TABLE_PAGINATED = "SELECT * from %s LIMIT %s OFFSET %s";

	private static final int LOADER_ID_COUNT_ROW = 0;
	private static final int LOADER_ID_COLUMNS_NAME = 1;
	private static final int LOADER_ID_TABLE_QUERY = 2;

	interface CallBack {
		SQLiteOpenHelper getSqLiteOpenHelper();
	}

	static final String NULL_VALUE = "null";
	static final Map<String, ColumnInfo> COLUMNS = new HashMap<>();

	private CallBack mCallBack;

	private TableLayout mTableHeader;
	private TableRow mRowHeader;
	private TableLayout mTableRows;

	private String mTableName;

	// PAGINATION
	private int mNumberOfPages = 0;
	private int mNbRows = 10;
	private int mCurrentPageNumber;
	private int mCountRows;

	private ProgressBar mProgressBar;
	private Button mPreviousButton;
	private TextView mNbPageTextView;
	private Button mNextButton;

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
		CoordinatorLayout rootView = (CoordinatorLayout) inflater.inflate(R.layout.content_main, container, false);

		// *****************************************
		Spinner mNbRowsSpinner = (Spinner) rootView.findViewById(R.id.row_spinner);
		ArrayAdapter nbRowsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item, Arrays.asList("10", "20", "50", "100"));
		nbRowsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mNbRowsSpinner.setAdapter(nbRowsAdapter);
		mNbRowsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mNbRows = Integer.parseInt(parent.getItemAtPosition(position).toString());
				mNumberOfPages = mCountRows / mNbRows + 1;
				displayTablePage(mCurrentPageNumber);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		// *****************************************
		// layout with buttons for the pagination

		mPreviousButton = (Button) rootView.findViewById(R.id.previous_button);
		mPreviousButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TableFragment.this.displayTablePage(--mCurrentPageNumber);
			}
		});

		mNbPageTextView = (TextView) rootView.findViewById(R.id.page_view);

		mNextButton = (Button) rootView.findViewById(R.id.next_button);
		mNextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TableFragment.this.displayTablePage(++mCurrentPageNumber);
			}
		});

		// *****************************************
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);

		// *****************************************
		mTableHeader = (TableLayout) rootView.findViewById(R.id.table_header);
		mTableHeader.setHorizontalScrollBarEnabled(true);

		// The table
		mTableRows = (TableLayout) rootView.findViewById(R.id.table_rows);
		mTableRows.setHorizontalScrollBarEnabled(true);


		// *****************************************
		// Fab
		FloatingActionButton fabAdd = (FloatingActionButton) rootView.findViewById(R.id.fab_add);
		fabAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNewRow(getActivity());
			}
		});
		FloatingActionButton fabDelete = (FloatingActionButton) rootView.findViewById(R.id.fab_delete);
		fabDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		FloatingActionButton fabDrop = (FloatingActionButton) rootView.findViewById(R.id.fab_drop);
		fabDrop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		return rootView;
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
		mTableName = args.getString("tableName");
		switch (id) {

			case LOADER_ID_COUNT_ROW:
				query = String.format(QUERY_COUNT_ROW, mTableName);
				break;

			case LOADER_ID_COLUMNS_NAME:
				query = String.format(QUERY_TABLE_INFO, mTableName);
				break;

			case LOADER_ID_TABLE_QUERY:
				query = String.format(QUERY_TABLE_PAGINATED, mTableName, args.getInt("mNbRows"), args.getInt("offset"));
				break;

			default:
				return null;
		}
		return new ADBMAsyncTaskLoader(getActivity(), mCallBack.getSqLiteOpenHelper(), query, this);
	}

	@Override
	public void onError(String message) {

	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
//		mProgressBar.setVisibility(View.INVISIBLE);

		int id = loader.getId();

		switch (id) {
			case LOADER_ID_COUNT_ROW:
				cursor.moveToNext();
				mCountRows = cursor.getInt(0);
				mNumberOfPages = mCountRows > 0 ? mCountRows / mNbRows + 1 : 0;

				getLoaderManager().restartLoader(LOADER_ID_COLUMNS_NAME, getArguments(), this);
				break;

			case LOADER_ID_COLUMNS_NAME:

				COLUMNS.clear();

				mRowHeader = new TableRow(getActivity());
				mRowHeader.setPadding(1, 1, 1, 1);

				while (cursor.moveToNext()) {
					String columnName = cursor.getString(1);
					String columnType = cursor.getString(2);
					boolean columnNullable = cursor.getInt(3) == 1;

					COLUMNS.put(columnName, ColumnInfo.create(columnName, columnType, columnNullable));

					mRowHeader.addView(createCellHeader(getActivity(), columnName));
				}

				mTableHeader.addView(mRowHeader);

				if (mNumberOfPages == 0) {
					mCurrentPageNumber = 0;
					mNbPageTextView.setText("0/0");
					mPreviousButton.setEnabled(false);
					mNextButton.setEnabled(false);
				} else {
					displayTablePage(1);
				}

				break;

			case LOADER_ID_TABLE_QUERY:
				Context context = getActivity();
				while (cursor.moveToNext()) {
					final TableRow tableRow = createRow(context, cursor);
					mTableRows.addView(tableRow);

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

				}

				break;
		}

		cursor.close();
		getLoaderManager().destroyLoader(loader.getId());
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	private void displayTablePage(int pageNumber) {
		if (mTableHeader == null) {
			return;
		}

		this.mCurrentPageNumber = pageNumber;

		mNbPageTextView.setText(mCurrentPageNumber + "/" + mNumberOfPages);

		// clear data
		mTableRows.removeAllViews();

		mPreviousButton.setEnabled(mCurrentPageNumber > 1);
		mNextButton.setEnabled(mCurrentPageNumber < mNumberOfPages);

		int offset;
		if (mCountRows <= mNbRows) {
			offset = 0;
		} else {
			offset = pageNumber * mNbRows - mNbRows;
		}

		getArguments().putInt("mNbRows", mNbRows);
		getArguments().putInt("offset", offset);
		getLoaderManager().restartLoader(LOADER_ID_TABLE_QUERY, getArguments(), this);
	}

	@NonNull
	private static TextView createCellHeader(Context context, String columnName) {
		final TextView cellHeader = new TextView(context);
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
	private static TableRow createRow(Context context, Cursor cursor) {
		final TableRow row = new TableRow(context);
		row.setBackgroundColor(Color.BLACK);
		row.setPadding(1, 1, 1, 1);

		TextView cell;
		TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		layoutParams.setMargins(1, 1, 1, 1);
		for (int columnIndex = 0, nbCol = cursor.getColumnCount(); columnIndex < nbCol; columnIndex++) {
			String columnName = cursor.getColumnName(columnIndex);

			String value;
			switch (COLUMNS.get(columnName).type) {
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
					// fixme Column data is not handle, do not display it
					value = null;
			}

			cell = new TextView(context);
			cell.setLayoutParams(layoutParams);
			cell.setBackgroundColor(Color.WHITE);
			cell.setTextColor(Color.BLACK);
			cell.setPadding(4, 4, 4, 4);
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

	private static void addNewRow(Context context) {
		final ScrollView mainView = new ScrollView(context);
		mainView.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, MATCH_PARENT));
		mainView.setPadding(5, 5, 5, 5);

		LinearLayout linearLayout = new LinearLayout(context);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setScrollContainer(true);
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		mainView.addView(linearLayout);

		final Map<String, EditText> mapET = new HashMap<>();
		int nbFields = COLUMNS.size();
		int indexField = 0;
		for (Map.Entry<String, ColumnInfo> entry : COLUMNS.entrySet()) {
			String field = entry.getKey();
			if (BaseColumns._ID.equals(field)) {
				continue;
			}

			ColumnInfo columnInfo = entry.getValue();

			TextInputEditText valueEditText = new TextInputEditText(context);
			valueEditText.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
			valueEditText.setHint(field);

			TextInputLayout row = new TextInputLayout(context);
			row.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
			row.addView(valueEditText);

			int inputType;
			switch (columnInfo.type) {
				case Cursor.FIELD_TYPE_STRING:
					inputType = InputType.TYPE_CLASS_TEXT;
					break;

				case Cursor.FIELD_TYPE_INTEGER:
				case Cursor.FIELD_TYPE_FLOAT:
					inputType = InputType.TYPE_CLASS_NUMBER;
					break;

				default:
					// fixme
					inputType = InputType.TYPE_NULL;
					valueEditText.setEnabled(false);
			}

			valueEditText.setInputType(inputType);

			if (++indexField == nbFields) {
				valueEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
			}

			mapET.put(field, valueEditText);

			linearLayout.addView(row);
		}

		new AlertDialog.Builder(context)
				.setTitle("Add a new row")
				.setView(mainView)
				.setCancelable(false)
				.setPositiveButton("Add", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						ContentValues data = new ContentValues();
						for (Map.Entry<String, ColumnInfo> entry : COLUMNS.entrySet()) {
							String field = entry.getKey();
							ColumnInfo columnInfo = entry.getValue();

							if (BaseColumns._ID.equals(field)) {
								continue;
							}

							String value = mapET.get(field).getText().toString();

							if (value.equals(NULL_VALUE)) {
								data.putNull(field);
							} else {
								// handle type
								switch (columnInfo.type) {
									case Cursor.FIELD_TYPE_STRING:
										data.put(field, value);
										break;

									case Cursor.FIELD_TYPE_INTEGER:
										data.put(field, Integer.valueOf(value));
										break;

									case Cursor.FIELD_TYPE_FLOAT:
										data.put(field, Float.valueOf(value));
										break;

									default:
										// todo
										data.putNull(field);
								}
							}
						}

//						Bundle args = new Bundle();
//						args.putString("currentTableName", mTableName);
//						args.putParcelable("contentValues", data);
//						getLoaderManager().restartLoader(LOADER_ID_INSERT, args, getActivity());
					}
				})
				.setNegativeButton("close", null)
				.show();
	}

}
