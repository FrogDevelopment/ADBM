package fr.frogdevelopment.adbm;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.frogdevelopment.nihongo.dico.contentprovider.NihonGoDicoContentProvider;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@SuppressLint("SetTextI18n")
public class _ADBMActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, QueryAsyncTaskLoader.ResultHandler {

	static final String NULL_VALUE = "null";
	static final Map<String, Integer> COLUMNS = new HashMap<>();

	private SQLiteOpenHelper mSqLiteOpenHelper;

	private ScrollView mRootView;
	private ArrayAdapter<String> mTableNamesAdapter;
	private TableLayout mTableLayout;
	private Spinner mTableSpinner;
	private EditText mCustomQueryEditText;
	private Button mSubmitQueryButton;
	private ProgressBar mProgressBar;
	private RelativeLayout mNavigationLayout;
	private Button mPreviousButton;
	private TextView mNbPageTextView;
	private Button mNextButton;
	private TextView mMessageTextView;

	private String currentTableName = "";
	private boolean isCustomQuery;

	// PAGINATION
	private int numberOfPages = 0;
	private int nbRows = 10;
	private int currentPageNumber;
	private int countRows;
	private LinearLayout mNbRowsLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("Android Data Base Manager");

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// *****************************************
		// fixme
		mSqLiteOpenHelper = new NihonGoDicoContentProvider.DictionaryOpenHelper(this);

		// *****************************************
		// rootView = scrollview so we can scroll it ...
		// also anchor for SnackBar
		mRootView = new ScrollView(this);
		mRootView.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mRootView.setPadding(nbRows, nbRows, nbRows, nbRows);
		setContentView(mRootView);

		// the main linear layout to which all views will be added.
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setScrollContainer(true);
		mRootView.addView(mainLayout);

		mProgressBar = new ProgressBar(this);

		// ***************************************** fixme find better UI
		LinearLayout actionLayout = new LinearLayout(this);
		actionLayout.setOrientation(LinearLayout.HORIZONTAL);
		actionLayout.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		TextView actionView = new TextView(this);
		actionView.setText("Actions : ");
		actionView.setTextSize(20);
		actionLayout.addView(actionView);


		mTableSpinner = new Spinner(this, Spinner.MODE_DIALOG);
		mTableSpinner.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mTableSpinner.setOnItemSelectedListener(mTableSelectedListener);
		actionLayout.addView(mTableSpinner);

		mainLayout.addView(actionLayout);

		// *****************************************
		// CUSTOM QUERY
		mCustomQueryEditText = new EditText(this);
		mCustomQueryEditText.setVisibility(View.GONE);
		mCustomQueryEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD); // http://stackoverflow.com/questions/33148168/inputtype-type-text-flag-no-suggestions-in-samsung
		mCustomQueryEditText.setSingleLine(false);
		mainLayout.addView(mCustomQueryEditText);

		mSubmitQueryButton = new Button(this);
		mSubmitQueryButton.setVisibility(View.GONE);
		mSubmitQueryButton.setText("Submit Query");
		mSubmitQueryButton.setOnClickListener(v -> {
			String customQuery = mCustomQueryEditText.getText().toString();

			if (!TextUtils.isEmpty(customQuery)) {
				Bundle args = new Bundle();
				args.putString("customQuery", customQuery);
				getLoaderManager().restartLoader(LOADER_ID_CUSTOM_QUERY, args, this);
//			} else {
				// fixme
			}
		});
		mainLayout.addView(mSubmitQueryButton);

		// *****************************************
		mNbRowsLayout = new LinearLayout(this);
		mNbRowsLayout.setOrientation(LinearLayout.HORIZONTAL);
		mNbRowsLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		TextView nbRowsTextView = new TextView(this);
		nbRowsTextView.setTextSize(20);
		nbRowsTextView.setText("Number of rows :");
		mNbRowsLayout.addView(nbRowsTextView);

		Spinner mNbRowsSpinner = new Spinner(this, Spinner.MODE_DIALOG);
		mNbRowsSpinner.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		ArrayAdapter nbRowsAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, Arrays.asList("10", "20", "50", "100"));
		nbRowsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mNbRowsSpinner.setAdapter(nbRowsAdapter);
		mNbRowsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				nbRows = Integer.parseInt(parent.getItemAtPosition(position).toString());
				numberOfPages = countRows / nbRows + 1;
				if (!TextUtils.isEmpty(currentTableName)) {
					displayTablePage(currentPageNumber);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		mNbRowsLayout.addView(mNbRowsSpinner);

		mainLayout.addView(mNbRowsLayout);

		// *****************************************
		// layout with buttons for the pagination
		mNavigationLayout = new RelativeLayout(this);
		RelativeLayout.LayoutParams navigationLayoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		mNavigationLayout.setLayoutParams(navigationLayoutParams);

		mPreviousButton = new Button(this);
		mPreviousButton.setText("Previous");
		RelativeLayout.LayoutParams previousLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		previousLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
		mPreviousButton.setLayoutParams(previousLayoutParams);
		mPreviousButton.setOnClickListener(v -> displayTablePage(--currentPageNumber));
		mNavigationLayout.addView(mPreviousButton);

		mNbPageTextView = new TextView(this);
		mNbPageTextView.setTextSize(20);
		RelativeLayout.LayoutParams nbPageLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		nbPageLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		mNbPageTextView.setLayoutParams(nbPageLayoutParams);
		mNbPageTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		mNavigationLayout.addView(mNbPageTextView);

		mNextButton = new Button(this);
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
		mTableLayout = new TableLayout(this);
		mTableLayout.setHorizontalScrollBarEnabled(true);

		// the horizontal scroll view for table if the table content does not fit into screen
		HorizontalScrollView mHorizontalScrollView = new HorizontalScrollView(this);
		mHorizontalScrollView.addView(mTableLayout);
		mHorizontalScrollView.setScrollbarFadingEnabled(false);
		mHorizontalScrollView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
		mainLayout.addView(mHorizontalScrollView);

		// *****************************************
		// Message text fixme find better
		mMessageTextView = new TextView(this);
		mMessageTextView.setTextSize(18);

		mainLayout.addView(mMessageTextView);


		getLoaderManager().initLoader(LOADER_ID_TABLES_NAME, null, this);
	}

	private int addMenuId = -1;
	private int deleteMenuId = -1;
	private int dropMenuId = -1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();

		// no menu when on custom query or no table selected
		if (isCustomQuery || TextUtils.isEmpty(currentTableName)) {
			return false;
		}

		addMenuId = View.generateViewId();
		MenuItem addMenu = menu.add(Menu.NONE, addMenuId, Menu.NONE, "Add row to this table");
		addMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		deleteMenuId = View.generateViewId();
		MenuItem deleteMenu = menu.add(Menu.NONE, deleteMenuId, Menu.NONE, "Delete this table");
		deleteMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		dropMenuId = View.generateViewId();
		MenuItem dropMenu = menu.add(Menu.NONE, dropMenuId, Menu.NONE, "Drop this table");
		dropMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int i = item.getItemId();
		if (i == addMenuId) {
			addNewRow();
			return true;
		} else if (i == deleteMenuId) {
			deleteTable();
			return true;
		} else if (i == dropMenuId) {
			dropTable();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	// fixme use fragment
	private void updateOrDeleteRow(Map<String, String> currentRowValues) {
		final ScrollView mainView = new ScrollView(this);
		mainView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mainView.setPadding(nbRows, nbRows, nbRows, nbRows);

		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setScrollContainer(true);
		linearLayout.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		mainView.addView(linearLayout);

		LayoutParams fieldLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		fieldLayoutParams.weight = 1;

		LayoutParams valueLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		valueLayoutParams.weight = 2;

		final Map<String, EditText> mapET = new HashMap<>();
		for (String field : COLUMNS.keySet()) {
			String value = currentRowValues.get(field);

			final LinearLayout row = new LinearLayout(this);

			TextView fieldTextView = new TextView(this);
			fieldTextView.setLayoutParams(fieldLayoutParams);
			fieldTextView.setText(field);
			row.addView(fieldTextView);

			EditText valueEditText = new EditText(this);
			valueEditText.setLayoutParams(valueLayoutParams);
			valueEditText.setText(value);
			valueEditText.setEnabled(!BaseColumns._ID.equals(field));
			row.addView(valueEditText);

			mapET.put(field, valueEditText);

			linearLayout.addView(row);
		}

		runOnUiThread(() -> {
			if (!isFinishing()) {
				new AlertDialog.Builder(this)
						.setTitle("Update or Delete row")
						.setView(mainView)
						.setCancelable(false)
						.setPositiveButton(android.R.string.ok, (dialog, which) -> {

							List<String> updates = new ArrayList<>();
							for (Map.Entry<String, Integer> entry : COLUMNS.entrySet()) {
								String field = entry.getKey();
								if (BaseColumns._ID.equals(field)) {
									continue;
								}

								String value = mapET.get(field).getText().toString();

								if (value.equals(NULL_VALUE)) {
									updates.add(field + " = null");
								} else {
									// handle type
									int type = entry.getValue();
									switch (type) {
										case Cursor.FIELD_TYPE_STRING:
											updates.add(field + " = " + "'" + value + "'");
											break;

										case Cursor.FIELD_TYPE_INTEGER:
										case Cursor.FIELD_TYPE_FLOAT:
											updates.add(field + " = " + value);
											break;

										default:
											// no handle
									}
								}
							}

							String query = "UPDATE " + currentTableName + " SET " + TextUtils.join(", ", updates) + " WHERE _ID = " + mapET.get(BaseColumns._ID);

							// fixme confirmation ?
							try {
//								mSqLiteOpenHelper.execute(query);
								Snackbar.make(mRootView, currentTableName + " table Updated Successfully", Snackbar.LENGTH_LONG).show();
//								refreshTable(null); fixme
							} catch (SQLException e) {
								Log.e("ADBM", "error while updating row", e);
								mMessageTextView.setBackgroundColor(Color.RED);
								mMessageTextView.setText("Error: " + e.getMessage());
							}
						})
						.setNeutralButton("close", null)
						.setNegativeButton("delete", (dialog, which) -> {
							// fixme confirmation ?
							try {
//								mSqLiteOpenHelper.execute("DELETE FROM " + currentTableName + " WHERE _ID = " + mapET.get(BaseColumns._ID));
								Snackbar.make(mRootView, "Row deleted from " + currentTableName + " table", Snackbar.LENGTH_LONG).show();
//								refreshTable(null); fixme
							} catch (SQLException e) {
								Log.e("ADBM", "error while deleting row", e);
								mMessageTextView.setBackgroundColor(Color.RED);
								mMessageTextView.setText("Error: " + e.getMessage());
							}
						})
						.show();
			}
		});
	}

	private void displayTablePage(int pageNumber) {
		this.currentPageNumber = pageNumber;

		runOnUiThread(() -> mNbPageTextView.setText(currentPageNumber + "/" + numberOfPages));

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

		Bundle args = new Bundle();
		args.putString("currentTableName", currentTableName);
		args.putInt("nbRows", nbRows);
		args.putInt("offset", offset);
		getLoaderManager().restartLoader(LOADER_ID_TABLE_QUERY, args, this);
	}

	private void dropTable() {
		runOnUiThread(() -> {
			if (!isFinishing()) {
				new AlertDialog.Builder(this)
						.setTitle("Drop table [" + currentTableName + "]")
						.setMessage("This will remove the table [" + currentTableName + "] from the database")
						.setPositiveButton("continue", (dialog, which) -> {
							try {
//								mSqLiteOpenHelper.execute("DROP TABLE " + currentTableName);
								Snackbar.make(mRootView, currentTableName + " dropped Successfully", Snackbar.LENGTH_LONG).show();
							} catch (SQLException e) {
								Log.e("ADBM", "Error while droping table " + currentTableName, e);
								mMessageTextView.setBackgroundColor(Color.RED);
								mMessageTextView.setText("Error: " + e.getMessage());
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}
		});
	}

	private void deleteTable() {
		runOnUiThread(() -> {
			if (!isFinishing()) {
				new AlertDialog.Builder(this)
						.setTitle("Delete table [" + currentTableName + "]")
						.setMessage("This will remove all data from table [" + currentTableName + "]")
						.setPositiveButton("continue", (dialog, which) -> {
							try {
//								mSqLiteOpenHelper.execute("DELETE FROM " + currentTableName);
								Snackbar.make(mRootView, currentTableName + " cleared Successfully", Snackbar.LENGTH_LONG).show();
//								refreshTable(null); fixme
							} catch (SQLException e) {
								Log.e("ADBM", "Error while deleting table " + currentTableName, e);
								mMessageTextView.setBackgroundColor(Color.RED);
								mMessageTextView.setText("Error: " + e.getMessage());
							}
						})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}
		});
	}

	private void addNewRow() {
		final ScrollView mainView = new ScrollView(this);
		mainView.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mainView.setPadding(5, 5, 5, 5);

		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setScrollContainer(true);
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

		mainView.addView(linearLayout);

		LayoutParams fieldLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		fieldLayoutParams.weight = 1;

		LayoutParams valueLayoutParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		valueLayoutParams.weight = 1;

		final Map<String, EditText> mapET = new HashMap<>();
		for (String field : COLUMNS.keySet()) {
			if (BaseColumns._ID.equals(field)) {
				continue;
			}

			final LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

			TextView fieldTextView = new TextView(this);
			fieldTextView.setLayoutParams(fieldLayoutParams);
			fieldTextView.setText(field);
			row.addView(fieldTextView);

			EditText valueEditText = new EditText(this);
			valueEditText.setLayoutParams(valueLayoutParams);
			row.addView(valueEditText);

			mapET.put(field, valueEditText);

			linearLayout.addView(row);
		}

		runOnUiThread(() -> {
			if (!isFinishing()) {
				new AlertDialog.Builder(_ADBMActivity.this)
						.setTitle("Add a new row")
						.setView(mainView)
						.setCancelable(false)
						.setPositiveButton("Add", (dialog, which) -> {

							ContentValues data = new ContentValues();
							for (Map.Entry<String, Integer> entry : COLUMNS.entrySet()) {
								String field = entry.getKey();

								if (BaseColumns._ID.equals(field)) {
									continue;
								}

								String value = mapET.get(field).getText().toString();

								if (!value.equals(NULL_VALUE)) {
									data.putNull(field);
								} else {
									// handle type
									int type = entry.getValue();
									switch (type) {
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

							Bundle args = new Bundle();
							args.putString("currentTableName", currentTableName);
							args.putParcelable("contentValues", data);
							getLoaderManager().restartLoader(LOADER_ID_INSERT, args, this);
						})
						.setNegativeButton("close", null)
						.show();
			}
		});
	}

	private OnItemSelectedListener mTableSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			mMessageTextView.setText(null);
			mMessageTextView.setBackgroundColor(Color.TRANSPARENT);

			switch (pos) {
				case 0: // custom query
					isCustomQuery = true;
					currentTableName = null;

					mTableSpinner.setSelection(0);
					mCustomQueryEditText.setVisibility(View.VISIBLE);
					mSubmitQueryButton.setVisibility(View.VISIBLE);
					mNbRowsLayout.setVisibility(View.GONE);
					mNavigationLayout.setVisibility(View.GONE);
					mMessageTextView.setVisibility(View.VISIBLE);

					mTableLayout.removeAllViews();

					break;

				case 1: // select a table
					isCustomQuery = false;
					currentTableName = null;
					mCustomQueryEditText.setVisibility(View.GONE);
					mSubmitQueryButton.setVisibility(View.GONE);
					mNbRowsLayout.setVisibility(View.GONE);
					mNavigationLayout.setVisibility(View.GONE);
					mMessageTextView.setVisibility(View.GONE);
					break;

				default: //  table selected
					isCustomQuery = false;
					currentTableName = mTableNamesAdapter.getItem(pos);
					Log.d("ADBM", "selected table name is " + currentTableName);

					mCustomQueryEditText.setVisibility(View.GONE);
					mSubmitQueryButton.setVisibility(View.GONE);
					mNbRowsLayout.setVisibility(View.VISIBLE);
					mNavigationLayout.setVisibility(View.VISIBLE);
					mMessageTextView.setVisibility(View.VISIBLE);

					// count total rows for pagination
					Bundle args = new Bundle();
					args.putString("currentTableName", currentTableName);
					getLoaderManager().restartLoader(LOADER_ID_COUNT_ROW, args, _ADBMActivity.this);

					break;
			}

			//
			invalidateOptionsMenu();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	};

	@NonNull
	private TextView createCellHeader(String columnName) {
		final TextView cellHeader = new TextView(this);
		cellHeader.setBackgroundColor(Color.DKGRAY);
		cellHeader.setTextColor(Color.WHITE);
		cellHeader.setPadding(4, 4, 4, 4);
		cellHeader.setGravity(Gravity.CENTER);

		LayoutParams layoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		layoutParams.setMargins(1, 1, 1, 1);
		cellHeader.setLayoutParams(layoutParams);
		cellHeader.setText(columnName);

		return cellHeader;
	}

	@NonNull
	private TableRow createRow(Cursor cursor) {
		final TableRow row = new TableRow(this);
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

			final TextView cell = new TextView(this);
			cell.setBackgroundColor(Color.WHITE);
			cell.setTextColor(Color.BLACK);
			cell.setPadding(4, 4, 4, 4);

			LayoutParams layoutParams = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
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


	private static final int LOADER_ID_TABLES_NAME = 0;
	private static final int LOADER_ID_TABLE_QUERY = 1;
	private static final int LOADER_ID_COUNT_ROW = 2;
	private static final int LOADER_ID_CUSTOM_QUERY = 3;
	private static final int LOADER_ID_INSERT = 4;

	private static final String QUERY_TABLES_NAME = "SELECT name _id FROM sqlite_master WHERE type ='table'";
	private static final String QUERY_TABLE_PAGINATED = "SELECT * from %s LIMIT %s OFFSET %s";
	private static final String QUERY_COUNT_ROW = "SELECT count(*) from %s";

	// https://bitbucket.org/ssutee/418496_mobileapp/src/fc5ee705a2fd1253a3ce9c1455b9597de6273ed8/demo/DotDotListDB/src/th/ac/ku/android/sutee/dotdotlist/DotDotListDBActivity.java?at=master&fileviewer=file-view-default
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		mProgressBar.setVisibility(View.VISIBLE);

		String query;

		switch (id) {
			case LOADER_ID_TABLES_NAME:
				query = QUERY_TABLES_NAME;
				break;

			case LOADER_ID_COUNT_ROW:
				query = String.format(QUERY_COUNT_ROW, args.getString("currentTableName"));
				break;

			case LOADER_ID_TABLE_QUERY:
				query = String.format(QUERY_TABLE_PAGINATED, args.getString("currentTableName"), args.getInt("nbRows"), args.getInt("offset"));
				break;

			case LOADER_ID_CUSTOM_QUERY:
				String customQuery = mCustomQueryEditText.getText().toString().trim();
				if (customQuery.startsWith("SELECT")) {
					query = customQuery;
					break;
				} else {
					return new CustomExecuteLoader(this, customQuery);
				}

			case LOADER_ID_INSERT:
				String currentTableName = args.getString("currentTableName");
				ContentValues contentValues = args.getParcelable("contentValues");

				return new InsertTaskLoader(this, currentTableName, contentValues);

			default:
				return null;
		}

		return new QueryAsyncTaskLoader(this, this, id, mSqLiteOpenHelper, query);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mProgressBar.setVisibility(View.GONE);

		getLoaderManager().destroyLoader(loader.getId());
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
		mMessageTextView.setBackgroundColor(Color.RED);
		mMessageTextView.setText("Error: " + e.getMessage());
	}

	@Override
	public void handleResult(int id, @NonNull Cursor cursor) {
		switch (id) {
			case LOADER_ID_TABLES_NAME:
				List<String> tableNames = new ArrayList<>();
				tableNames.add("Custom query");
				tableNames.add("Select a table");
				while (cursor.moveToNext()) {
					tableNames.add(cursor.getString(0));
				}

				mTableNamesAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.select_dialog_item, tableNames);
				mTableNamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mTableSpinner.setAdapter(mTableNamesAdapter);
				break;

			case LOADER_ID_COUNT_ROW:
				if (cursor.moveToNext()) {
					countRows = cursor.getInt(0);
					numberOfPages = countRows / nbRows + 1;

					displayTablePage(1);
				} else {
					countRows = 0;
					numberOfPages = countRows / nbRows + 1;

					// fixme
				}
				break;

			case LOADER_ID_TABLE_QUERY:
				while (cursor.moveToNext()) {

					if (COLUMNS.isEmpty()) { // just once
						// get columns name
						TableRow header = new TableRow(getApplicationContext());
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

					// add a listener on the row for edition
					// fixme utiliser des EditText ??
					tableRow.setOnClickListener(v -> {
						Map<String, String> currentRowValues = new HashMap<>();
						for (int i = 0; i < tableRow.getChildCount(); i++) {
							TextView cell = (TextView) tableRow.getChildAt(i);

							String columnName = (String) cell.getTag();
							String value = cell.getText().toString();
							currentRowValues.put(columnName, value);
						}

						updateOrDeleteRow(currentRowValues);
					});
				}
				break;

			case LOADER_ID_CUSTOM_QUERY:
				// Table headers
				TableRow header = new TableRow(getApplicationContext());
				header.setPadding(1, 1, 1, 1);
				for (String columnName : cursor.getColumnNames()) {
					header.addView(createCellHeader(columnName));
				}
				mTableLayout.addView(header);

				// Data
				while (cursor.moveToNext()) {
					mTableLayout.addView(createRow(cursor));
				}

				Snackbar.make(mRootView, "Number of rows returned : " + cursor.getCount(), Snackbar.LENGTH_LONG).show();
				break;
		}

		mProgressBar.setVisibility(View.GONE);

		getLoaderManager().destroyLoader(id);
	}

	private class InsertTaskLoader extends AsyncTaskLoader<Cursor> {

		private final String table;
		private final ContentValues values;

		private InsertTaskLoader(Context context, String table, ContentValues values) {
			super(context);
			this.table = table;
			this.values = values;
		}

		@Override
		public boolean isStarted() {
			mTableLayout.removeAllViews();
			return super.isStarted();
		}

		@Override
		public Cursor loadInBackground() {
			SQLiteDatabase database = null;
			try {
				Log.d("ADBM", "insert int table=[" + table + "] values=" + values);

				database = mSqLiteOpenHelper.getWritableDatabase();
				database.beginTransaction();

				database.insert(table, null, values);

				database.setTransactionSuccessful();

				Snackbar.make(mRootView, "New row added successfully to " + table, Snackbar.LENGTH_LONG).show();
			} catch (SQLException e) {
				Log.e("ADBM", "Error while inserting new row for " + currentTableName, e);
				mMessageTextView.setBackgroundColor(Color.RED);
				mMessageTextView.setText("Error: " + e.getMessage());
			} finally {
				if (database != null) database.endTransaction();
			}

			return null;
		}
	}

	private class CustomExecuteLoader extends AsyncTaskLoader<Cursor> {

		private final String query;

		private CustomExecuteLoader(@NonNull Context context, @NonNull String query) {
			super(context);
			this.query = query;
		}

		@Override
		public boolean isStarted() {
			mTableLayout.removeAllViews();
			return super.isStarted();
		}

		@Override
		public Cursor loadInBackground() {
			SQLiteDatabase database = null;
			try {
				Log.d("ADBM", "execute query=" + query);

				database = mSqLiteOpenHelper.getWritableDatabase();
				database.beginTransaction();

				database.execSQL(query);

				database.setTransactionSuccessful();
			} finally {
				if (database != null) database.endTransaction();
			}

			return null;
		}
	}
}
