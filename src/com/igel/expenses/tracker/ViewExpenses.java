package com.igel.expenses.tracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ViewExpenses extends ListActivity {

	// keys used to store information in the activity state or pass information
	// by an intent
	public static final String KEY_SELECTED_MONTH = "keySelectedMonth";

	// menu item id
	private static final int ADD_EXPENSE_ID = Menu.FIRST;
	private static final int DELETE_EXPENSE_ID = Menu.FIRST + 1;

	// activity codes for creating intents
	private static final int ACTIVITY_ADD_EXPENSE = 0;
	private static final int ACTIVITY_EDIT_EXPENSE = 1;

	// constants used to create dialogs
	private static final int DELETE_EXPENSE_DIALOG = 0;

	// constants used to create bundles for dialogs
	private static final String EXPENSE_ID = "expenseID";

	// widgets
	private TextView mTotalsWidget;

	// used to format date for display
	private DateFormat mDateFormat;
	// the currently selected month
	private Calendar mSelectedMonth;
	private long mFromMillis;
	private long mToMillis;

	private String mCurrencySymbol;

	// database adapter
	private ExpensesDbAdapter mDbAdapter;

	// backup manager
	private BackupManager mBackupManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set view
		setContentView(R.layout.view_expenses);
		mDateFormat = new SimpleDateFormat(" MMMM yyyy");

		initializeWidgets();
		setButtonListeners();

		mSelectedMonth = CalendarUtils.getFirstDayOfMonth(Calendar.getInstance());
		updateMillisRange();

		mCurrencySymbol = Currency.getInstance(Locale.getDefault()).getSymbol();

		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();
		fetchDataFromDb();

		// initialize backup manager
		mBackupManager = new BackupManager(this);

		updateTitle();

		registerForContextMenu(getListView());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// called when options menu is created
		super.onCreateOptionsMenu(menu);

		// add menu item to add expense category
		menu.add(0, ADD_EXPENSE_ID, 0, R.string.view_expenses_add_expense);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_EXPENSE_ID, 0, R.string.view_expenses_delete_expense);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_EXPENSE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			Bundle bundle = new Bundle();
			bundle.putLong(EXPENSE_ID, info.id);
			showDialog(DELETE_EXPENSE_DIALOG, bundle);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// called when menu item is selected
		switch (item.getItemId()) {
		case ADD_EXPENSE_ID:
			addExpense();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// this method is called by the system before onPause
		Long selectedMonthMillis = mSelectedMonth.getTimeInMillis();
		outState.putSerializable(KEY_SELECTED_MONTH, selectedMonthMillis);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// this method is called when the system restores the activity state
		// (after onStart)
		super.onRestoreInstanceState(savedInstanceState);
		Long selectedMonthMillis = (Long) savedInstanceState.getSerializable(KEY_SELECTED_MONTH);
		mSelectedMonth = Calendar.getInstance();
		mSelectedMonth.setTimeInMillis(selectedMonthMillis);
		updateMillisRange();
	}

	private void addExpense() {
		Intent intent = new Intent(this, EditExpense.class);
		startActivityForResult(intent, ACTIVITY_ADD_EXPENSE);
	}

	private void deleteSelectedExpense(long expenseId) {
		mDbAdapter.deleteExpense(expenseId);

		// notify backup manager about changed information
		mBackupManager.dataChanged();

		fetchDataFromDb();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// called when list item is clicked
		super.onListItemClick(l, v, position, id);

		// create new intent
		Intent intent = new Intent(this, EditExpense.class);

		// pass ID of expense category
		intent.putExtra(EditExpense.KEY_EXPENSE_ID, id);

		// start activity
		startActivityForResult(intent, ACTIVITY_EDIT_EXPENSE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// called when started activity is finished
		super.onActivityResult(requestCode, resultCode, intent);
		fetchDataFromDb();
	}

	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {
		case DELETE_EXPENSE_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.view_expenses_delete_expense_message).setCancelable(false)
					.setPositiveButton(R.string.expenses_tracker_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							deleteSelectedExpense(bundle.getLong(EXPENSE_ID));
						}
					}).setNegativeButton(R.string.expenses_tracker_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		}
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	private void updateMillisRange() {
		mFromMillis = mSelectedMonth.getTimeInMillis();
		Calendar toCalendar = (Calendar) mSelectedMonth.clone();
		toCalendar.add(Calendar.MONTH, 1);
		mToMillis = toCalendar.getTimeInMillis();
	}

	private void initializeWidgets() {
		mTotalsWidget = (TextView) findViewById(R.id.view_expenses_totals);
	}

	private void setButtonListeners() {
		ImageButton previousMonthButton = (ImageButton) findViewById(R.id.view_expenses_prev_month);
		ImageButton nextMonthButton = (ImageButton) findViewById(R.id.view_expenses_next_month);
		previousMonthButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSelectedMonth.add(Calendar.MONTH, -1);
				updateMillisRange();
				updateTitle();
				fetchDataFromDb();
			}
		});

		nextMonthButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSelectedMonth.add(Calendar.MONTH, 1);
				updateMillisRange();
				updateTitle();
				fetchDataFromDb();
			}
		});
	}

	private void updateTitle() {
		String dateString = mDateFormat.format(mSelectedMonth.getTime());
		setTitle(getText(R.string.view_expenses_title_prefix) + dateString);
	}

	private void fetchDataFromDb() {
		long sum = mDbAdapter.getExpensesSum(mFromMillis, mToMillis);

		String totalsString = getString(R.string.view_expenses_totals_title) + ": " + sum + " " + mCurrencySymbol;
		mTotalsWidget.setText(totalsString);

		// Get all of expenses from the database and create the item list
		Cursor expensesCursor = mDbAdapter.fetchAllExpensesInRange(mFromMillis, mToMillis);
		startManagingCursor(expensesCursor);

		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { ExpensesDbAdapter.EXPENSE_DATE, ExpensesDbAdapter.EXPENSE_AMOUNT,
				ExpensesDbAdapter.EXPENSE_DETAILS, ExpensesDbAdapter.EXPENSE_CATEGORY_NAME };

		// and an array of the fields we want to bind those fields to
		int[] to = new int[] { R.id.view_expense_row_date, R.id.view_expense_row_amount, R.id.view_expense_row_details,
				R.id.view_expense_row_category };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter expenses = new ExpensesCursorAdapter(this, R.layout.view_expenses_row, expensesCursor, from,
				to);
		setListAdapter(expenses);
	}

	private class ExpensesCursorAdapter extends SimpleCursorAdapter {

		// used to format date for display
		private DateFormat mDateFormat;
		private Calendar mCalendar;

		public ExpensesCursorAdapter(Activity context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			mDateFormat = new SimpleDateFormat("EEE, dd.MM.yyyy");
			mCalendar = Calendar.getInstance();
		}

		@Override
		public void setViewText(TextView v, String text) {
			if (v.getId() == R.id.view_expense_row_date) {
				long millis = new Long(text).longValue();
				mCalendar.setTimeInMillis(millis);
				String dateString = mDateFormat.format(mCalendar.getTime());
				v.setText(dateString);
			} else if (v.getId() == R.id.view_expense_row_details) {
				if (text.length() != 0)
					text = " - " + text;
				v.setText(text);
			} else if (v.getId() == R.id.view_expense_row_amount) {
				if (text.length() != 0)
					text = text + " " + mCurrencySymbol;
				v.setText(text);
			} else
				super.setViewText(v, text);
		}
	}
}
