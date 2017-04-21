package fr.frogdevelopment.adbm;

import android.app.Fragment;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class _ADBMDisplayTableFragment extends Fragment {

	protected ScrollView mRootView;
	protected TableLayout mTableLayout;
	protected LinearLayout mMainLayout;
	protected ProgressBar mProgressBar;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// *****************************************
		// rootView = scrollview so we can scroll it ...
		// also anchor for SnackBar
		mRootView = new ScrollView(getActivity());

		mRootView.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		mRootView.setPadding(10, 10, 10, 10);

		// the main linear layout to which all views will be added.
		mMainLayout = new LinearLayout(getActivity());
		mMainLayout.setOrientation(LinearLayout.VERTICAL);
		mMainLayout.setScrollContainer(true);
		mRootView.addView(mMainLayout);

		mProgressBar = new ProgressBar(getActivity());
		mProgressBar.setMinimumHeight(50);
		mProgressBar.setMinimumWidth(50);
		mMainLayout.addView(mProgressBar);

		// *****************************************
		// The table
		mTableLayout = new TableLayout(getActivity());
		mTableLayout.setHorizontalScrollBarEnabled(true);

		// the horizontal scroll view for table if the table content does not fit into screen
		HorizontalScrollView mHorizontalScrollView = new HorizontalScrollView(getActivity());
		mHorizontalScrollView.addView(mTableLayout);
		mHorizontalScrollView.setScrollbarFadingEnabled(false);
		mHorizontalScrollView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
		mMainLayout.addView(mHorizontalScrollView);

		// *****************************************
		return mRootView;
	}

	@NonNull
	protected TextView createCellHeader(String columnName) {
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
	protected TableRow createRow(Cursor cursor) {
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
				cell.setText(ADBMActivity.NULL_VALUE);
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
