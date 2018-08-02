package com.igel.expenses.tracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditExpense extends Activity {

	// keys used to store information in the activity state or pass information
	// by an intent
	public static final String KEY_EXPENSE_ID = "keyExpenseId";
	public static final String KEY_EXPENSE_DATE = "keyExpenseDate";
	public static final String KEY_EXPENSE_AMOUNT = "keyExpenseAmount";
	public static final String KEY_EXPENSE_DETAILS = "keyExpenseDetails";
	public static final String KEY_EXPENSE_CATEGORY = "keyExpenseCategory";

	// constants used to create dialogs
	private static final int DATE_DIALOG = 0;

	// the ID of the expense to be edited
	private Long mExpenseId;

	// other expense information
	private Calendar mExpenseDate;
	private Long mExpenseCategoryId;

	// widgets
	private Button mExpenseDateButton;
	private EditText mExpenseAmountWidget;
	private Spinner mExpenseCategorySpinner;
	private TextView mExpenseCategoryDescriptionWidget;
	private EditText mExpenseDetailsWidget;

	// used to format date for display
	private DateFormat mDateFormat;

	// database adapter
	private ExpensesDbAdapter mDbAdapter;

	// backup manager
	private BackupManager mBackupManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// create new database adapter and open it
		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();

		// initialize backup manager
		mBackupManager = new BackupManager(this);

		// set view
		setContentView(R.layout.edit_expense);
		mDateFormat = new SimpleDateFormat("dd.MM.yyyy");

		initializeWidgets();
		initializeSpinner();
		addExpenseCategoriesToSpinner();
		setButtonListeners();

		// extras may be given when the activity is called from someone else
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			// set expense category ID (if given)
			mExpenseId = (Long) extras.getSerializable(KEY_EXPENSE_ID);
		}
		if (mExpenseId != null) {
			// initialize view
			fetchDataFromDb();
		} else {
			// initialize expense date to current date
			mExpenseDate = Calendar.getInstance();
		}
		setTitle(mExpenseId);
		updateView();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// this method is called by the system before onPause
		String amountString = mExpenseAmountWidget.getText().toString();
		if (amountString.length() != 0) {
			Integer amount = Integer.valueOf(amountString);
			outState.putSerializable(KEY_EXPENSE_AMOUNT, amount);
		}

		String details = mExpenseDetailsWidget.getText().toString();

		outState.putSerializable(KEY_EXPENSE_ID, mExpenseId);
		outState.putSerializable(KEY_EXPENSE_DATE, mExpenseDate.getTimeInMillis());
		outState.putSerializable(KEY_EXPENSE_CATEGORY, mExpenseCategoryId);
		outState.putSerializable(KEY_EXPENSE_DETAILS, details);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// this method is called when the system restores the activity state
		// (after onStart)
		super.onRestoreInstanceState(savedInstanceState);
		mExpenseId = (Long) savedInstanceState.getSerializable(KEY_EXPENSE_ID);
		Long expenseDateInMillis = (Long) savedInstanceState.getSerializable(KEY_EXPENSE_DATE);
		mExpenseDate = Calendar.getInstance();
		mExpenseDate.setTimeInMillis(expenseDateInMillis);
		Integer amount = (Integer) savedInstanceState.getSerializable(KEY_EXPENSE_AMOUNT);

		if (amount != null)
			mExpenseAmountWidget.setText(amount.toString());
		mExpenseCategoryId = (Long) savedInstanceState.getSerializable(KEY_EXPENSE_CATEGORY);
		String details = (String) savedInstanceState.getSerializable(KEY_EXPENSE_DETAILS);
		mExpenseDetailsWidget.setText(details);
	}

	private void initializeWidgets() {
		mExpenseDateButton = (Button) findViewById(R.id.edit_expense_expense_date);
		mExpenseAmountWidget = (EditText) findViewById(R.id.edit_expense_expense_amount);
		mExpenseCategorySpinner = (Spinner) findViewById(R.id.edit_expense_expense_category_spinner);
		mExpenseCategoryDescriptionWidget = (TextView) findViewById(R.id.edit_expense_expense_category_description);
		mExpenseDetailsWidget = (EditText) findViewById(R.id.edit_expense_expense_details);
	}

	private void updateView() {
		updateDateView();
	}

	private void updateDateView() {
		if (mExpenseDate != null)
			mExpenseDateButton.setText(mDateFormat.format(mExpenseDate.getTime()));
	}

	private void setTitle(Long expenseId) {
		if (expenseId != null)
			setTitle(R.string.edit_expense_title);
		else
			setTitle(R.string.add_expense_title);
	}

	private void initializeSpinner() {
		mExpenseCategorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Object item = parent.getItemAtPosition(position);
				if (item instanceof Cursor) {
					Cursor cursor = (Cursor) item;
					mExpenseCategoryId = cursor
							.getLong(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_ID));
					String categoryDescription = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION));
					if (mExpenseCategoryDescriptionWidget != null)
						mExpenseCategoryDescriptionWidget.setText(categoryDescription);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void addExpenseCategoriesToSpinner() {
		Cursor expenseCategories = mDbAdapter.fetchAllExpenseCategories();
		startManagingCursor(expenseCategories);

		// Create an array to specify the fields we want to display (only name)
		String[] from = new String[] { ExpensesDbAdapter.EXPENSE_CATEGORY_NAME };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { android.R.id.text1 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter categories = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				expenseCategories, from, to);
		categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mExpenseCategorySpinner.setAdapter(categories);
	}

	private void setButtonListeners() {
		mExpenseDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DATE_DIALOG);
			}
		});

		Button cancelButton = (Button) findViewById(R.id.edit_expense_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		Button saveButton = (Button) findViewById(R.id.edit_expense_save);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!validateData()) {
					return;
				} else {
					setResult(RESULT_OK);
					saveExpense();
					finish();
				}
			}
		});
	}

	private boolean validateData() {
		String amountString = mExpenseAmountWidget.getText().toString();
		if (amountString.length() == 0) {
			Toast toast = Toast.makeText(this, R.string.edit_expense_amount_warning, Toast.LENGTH_LONG);
			toast.show();
			return false;
		}
		return true;
	}

	private void saveExpense() {
		// get entered information
		long expenseDateInMillis = mExpenseDate.getTimeInMillis();
		String amountString = mExpenseAmountWidget.getText().toString().trim();
		int amount = Integer.valueOf(amountString);
		String details = mExpenseDetailsWidget.getText().toString().trim();

		if (mExpenseId == null) {
			// create new expense
			long id = mDbAdapter.createExpense(expenseDateInMillis, amount, mExpenseCategoryId, details);
			if (id > 0)
				mExpenseId = id;
		} else {
			mDbAdapter.updateExpense(mExpenseId, expenseDateInMillis, amount, mExpenseCategoryId, details);
		}

		// notify backup manager about changed information
		mBackupManager.dataChanged();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	private void fetchDataFromDb() {
		Cursor expense = mDbAdapter.fetchExpense(mExpenseId);
		startManagingCursor(expense);
		String millisString = expense.getString(expense.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DATE));
		long millis = new Long(millisString).longValue();
		mExpenseDate = Calendar.getInstance();
		mExpenseDate.setTimeInMillis(millis);

		String amountString = expense.getString(expense.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_AMOUNT));
		mExpenseAmountWidget.setText(amountString);
		mExpenseCategoryId = expense
				.getLong(expense.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_EXPENSE_CATEGORY_ID));
		mExpenseCategorySpinner.setSelection(mExpenseCategoryId.intValue() - 1);
		String expenseDetails = expense.getString(expense.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DETAILS));
		mExpenseDetailsWidget.setText(expenseDetails);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG:
			return new DatePickerDialog(this, mDateSetListener, mExpenseDate.get(Calendar.YEAR),
					mExpenseDate.get(Calendar.MONTH), mExpenseDate.get(Calendar.DAY_OF_MONTH));
		}
		return null;
	}

	// the callback received when the user "sets" the date in the expense date
	// dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mExpenseDate.set(Calendar.YEAR, year);
			mExpenseDate.set(Calendar.MONTH, monthOfYear);
			mExpenseDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			updateDateView();
		}
	};
}
