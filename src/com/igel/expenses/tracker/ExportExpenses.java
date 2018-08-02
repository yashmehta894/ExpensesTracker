package com.igel.expenses.tracker;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.igel.expenses.tracker.ExportExpensesUtils.ExportResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

public class ExportExpenses extends Activity {

	// keys used to store information in the activity state or pass information
	// by an intent
	public static final String KEY_FROM_DATE = "keyFromDate";
	public static final String KEY_TO_DATE = "keyToDate";

	// constants used to create dialogs
	private static final int FROM_DATE_DIALOG = 0;
	private static final int TO_DATE_DIALOG = 1;

	private static final int ERROR_DIALOG = 99;

	// widgets
	private Button mFromDateButton;
	private Button mToDateButton;

	// other expense information
	private Calendar mFromDate;
	private Calendar mToDate;

	// used to format date for display
	private DateFormat mDateFormat;

	// database adapter
	private ExpensesDbAdapter mDbAdapter;

	// holds error messages
	private int mErrorMessageId;
	private Object[] mErrorMessageArgs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set view
		setContentView(R.layout.export_expenses);
		mDateFormat = new SimpleDateFormat("EEE, dd.MM.yyyy");

		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();

		// extras may be given when the activity is called from someone else
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			// initialize dates to current date
			mFromDate = CalendarUtils.getFirstDayOfMonth(Calendar.getInstance());
			mToDate = CalendarUtils.getLastDayOfMonth(mFromDate);
		}

		initializeWidgets();
		setButtonListeners();
		setTitle(R.string.export_expenses_title);
		updateView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// this method is called by the system before onPause
		outState.putSerializable(KEY_FROM_DATE, mFromDate.getTimeInMillis());
		outState.putSerializable(KEY_TO_DATE, mToDate.getTimeInMillis());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// this method is called when the system restores the activity state
		// (after onStart)
		super.onRestoreInstanceState(savedInstanceState);
		Long fromDateInMillis = (Long) savedInstanceState.getSerializable(KEY_FROM_DATE);
		mFromDate = Calendar.getInstance();
		mFromDate.setTimeInMillis(fromDateInMillis);
		Long toDateInMillis = (Long) savedInstanceState.getSerializable(KEY_TO_DATE);
		mToDate = Calendar.getInstance();
		mToDate.setTimeInMillis(toDateInMillis);
	}

	private void initializeWidgets() {
		mFromDateButton = (Button) findViewById(R.id.export_expenses_from_date);
		mToDateButton = (Button) findViewById(R.id.export_expenses_to_date);
	}

	private void setButtonListeners() {
		mFromDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(FROM_DATE_DIALOG);
			}
		});
		mToDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(TO_DATE_DIALOG);
			}
		});

		Button cancelButton = (Button) findViewById(R.id.export_expenses_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		Button saveButton = (Button) findViewById(R.id.export_expenses_export);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				if (!validateData()) {
					return;
				} else {
					Result<ExportResult> result = exportExpenses();
					ExportResult exportResult = result.getResult();
					if (exportResult == ExportResult.ERROR) {
						// show error message
						mErrorMessageId = result.getMessageId();
						mErrorMessageArgs = result.getMessageArgs();
						showDialog(ERROR_DIALOG);
					} else {
						// show a result toast
						int messageId = 0;
						if (exportResult == ExportResult.EXPORTED_DATA)
							messageId = R.string.export_expenses_info_exported_data;
						else
							messageId = R.string.export_expenses_info_exported_no_data;

						Toast toast = Toast.makeText(getApplicationContext(), getString(messageId), Toast.LENGTH_LONG);
						toast.show();

						// done if something was exported
						if (exportResult == ExportResult.EXPORTED_DATA)
							finish();
					}
				}
			}
		});
	}

	private Result<ExportResult> exportExpenses() {
		// initialize directory
		Result<File> initResult = ExportExpensesUtils.initExportDirectory(this);
		File exportDirectory = initResult.getResult();

		// check if directory available
		if (exportDirectory == null) {
			// pass error message
			return new Result<ExportResult>(ExportResult.ERROR, initResult.getMessageId(), initResult.getMessageArgs());
		} else {
			// get file name postfix
			String fileNamePostfix = ExportExpensesUtils.getExportFileNamePostfix();

			// export up to end of selected day
			Calendar to = CalendarUtils.getEndOfDay(mToDate);

			// export expenses
			Result<ExportResult> exportResult = ExportExpensesUtils.exportExpenses(exportDirectory, fileNamePostfix,
					mDbAdapter, mFromDate, to);

			// check for error
			if (exportResult.getResult() == ExportResult.ERROR) {
				return exportResult;
			}

			// check if something exported
			if (exportResult.getResult() == ExportResult.EXPORTED_NO_DATA) {
				return exportResult;
			}

			// only export categories if data has been exported
			exportResult = ExportExpensesUtils.exportExpenseCategories(exportDirectory, fileNamePostfix, mDbAdapter);
			return exportResult;
		}
	}

	private boolean validateData() {
		if (mFromDate.after(mToDate)) {
			mErrorMessageId = R.string.export_expenses_date_error;
			mErrorMessageArgs = null;
			showDialog(ERROR_DIALOG);
			return false;
		}
		return true;
	}

	private void updateView() {
		if (mFromDate != null)
			mFromDateButton.setText(mDateFormat.format(mFromDate.getTime()));
		if (mToDate != null)
			mToDateButton.setText(mDateFormat.format(mToDate.getTime()));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case FROM_DATE_DIALOG:
			return new DatePickerDialog(this, mFromDateSetListener, mFromDate.get(Calendar.YEAR),
					mFromDate.get(Calendar.MONTH), mFromDate.get(Calendar.DAY_OF_MONTH));
		case TO_DATE_DIALOG:
			return new DatePickerDialog(this, mToDateSetListener, mToDate.get(Calendar.YEAR),
					mToDate.get(Calendar.MONTH), mToDate.get(Calendar.DAY_OF_MONTH));
		case ERROR_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// get message
			String message = getString(mErrorMessageId);

			// check if message has to be enriched with args
			if (mErrorMessageArgs != null && mErrorMessageArgs.length > 0)
				message = String.format(message, mErrorMessageArgs);

			// create alert dialog
			builder.setMessage(message).setCancelable(false).setPositiveButton(R.string.expenses_tracker_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
						}
					});
			return builder.create();
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener mFromDateSetListener = new DatePickerDialog.OnDateSetListener() {

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mFromDate.set(Calendar.YEAR, year);
			mFromDate.set(Calendar.MONTH, monthOfYear);
			mFromDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			updateView();
		}
	};

	private DatePickerDialog.OnDateSetListener mToDateSetListener = new DatePickerDialog.OnDateSetListener() {

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mToDate.set(Calendar.YEAR, year);
			mToDate.set(Calendar.MONTH, monthOfYear);
			mToDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			updateView();
		}
	};

}
