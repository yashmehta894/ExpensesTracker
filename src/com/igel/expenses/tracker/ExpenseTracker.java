package com.igel.expenses.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.igel.expenses.tracker.ExportExpensesUtils.ClearDirectoryResult;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ExpenseTracker extends ListActivity {

	// constants used to create dialogs
	private static final int REMOVE_EXPORTED_FILES_DIALOG = 0;
	private static final int CLEAR_DATA_DIALOG = 1;
	private static final int CONFIRM_CLEAR_DATA_DIALOG = 2;
	private static final int INFO_DIALOG = 3;

	// activity codes for creating intents
	private static final int ACTIVITY_SHOW_PREFERENCES = 0;

	// constants used to create menu list
	private static final String MENU_ITEM = "menuItem";
	private static final String MENU_ITEM_DESCRIPTION = "menuItemDescription";
	private static final int ADD_EXPENSE = 0;
	private static final int VIEW_EXPENSES = 1;
	private static final int ADD_EXPENSE_CATEGORY = 2;
	private static final int VIEW_EXPENSE_CATEGORIES = 3;
	private static final int EXPORT_EXPENSES = 4;
	private static final int REMOVE_EXPORT_FILES = 5;

	private File mExportDirectory;

	// used when deleting data from DB
	private ExpensesDbAdapter mDbAdapter;

	// backup manager
	private BackupManager mBackupManager;

	// used for clearing data
	private Calendar mClearDataDate = Calendar.getInstance();

	// holds menu items displayed in list
	private List<? extends Map<String, ?>> mListMenuItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set view
		setContentView(R.layout.expense_tracker);
		setTitle(R.string.expenses_tracker_title);

		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();

		// initialize backup manager
		mBackupManager = new BackupManager(this);

		// initialize list view with menu item
		mListMenuItems = initializeListMenuItems();
		String[] from = new String[] { MENU_ITEM, MENU_ITEM_DESCRIPTION };
		int[] to = new int[] { R.id.expense_tracker_menu_item, R.id.expense_tracker_menu_item_description };
		SimpleAdapter adapter = new SimpleAdapter(this, mListMenuItems, R.layout.expense_tracker_row, from, to);
		setListAdapter(adapter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// called when options menu is created
		super.onCreateOptionsMenu(menu);

		// inflate menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.expenses_tracker_menu, menu);
		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// called when list item is clicked
		super.onListItemClick(l, v, position, id);

		Intent intent = null;

		// initialize intent according to clicked list item
		switch (position) {
		case ADD_EXPENSE:
			intent = new Intent(this, EditExpense.class);
			break;
		case VIEW_EXPENSES:
			intent = new Intent(this, ViewExpenses.class);
			break;
		case ADD_EXPENSE_CATEGORY:
			intent = new Intent(this, EditExpenseCategory.class);
			break;
		case VIEW_EXPENSE_CATEGORIES:
			intent = new Intent(this, ViewExpenseCategories.class);
			break;
		case EXPORT_EXPENSES:
			intent = new Intent(this, ExportExpenses.class);
			break;
		case REMOVE_EXPORT_FILES:
			removeExportedFiles();
			break;
		}

		// start activity if intent is initialized
		if (intent != null)
			startActivity(intent);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// called when menu item is selected
		switch (item.getItemId()) {
		case R.id.expenses_tracker_menu_settings:
			Intent intent = new Intent(this, ExpensesTrackerPreferences.class);
			startActivityForResult(intent, ACTIVITY_SHOW_PREFERENCES);
			return true;
		case R.id.expenses_tracker_menu_clear_data:
			showDialog(CLEAR_DATA_DIALOG);
			return true;
		case R.id.expenses_tracker_menu_info:
			showDialog(INFO_DIALOG);
			return true;
		case R.id.expenses_tracker_menu_feedback:
			return sendFeedback();
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private boolean sendFeedback() {
		// send an email
		Intent intent = new Intent(Intent.ACTION_SEND);

		// put stuff in extras
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.expenses_tracker_feedback_email) });
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expenses_tracker_feedback_subject));

		// start intent
		startActivity(Intent.createChooser(intent, getString(R.string.expenses_tracker_feedback_title)));
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case REMOVE_EXPORTED_FILES_DIALOG:
			// create a basic confirmation dialog with yes/no
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = String.format(getString(R.string.expenses_tracker_remove_files_message),
					mExportDirectory.getAbsolutePath());
			builder.setMessage(message).setCancelable(false)
					.setPositiveButton(R.string.expenses_tracker_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// clear directory
							ClearDirectoryResult result = ExportExpensesUtils.clearDirectory(mExportDirectory);

							// prepare message according to result
							int messageId;
							if (result == ClearDirectoryResult.REMOVED_ALL_FILES)
								messageId = R.string.expenses_tracker_remove_files_files_removed;
							else if (result == ClearDirectoryResult.REMOVED_NO_FILES)
								messageId = R.string.expenses_tracker_remove_files_no_files_removed;
							else
								messageId = R.string.expenses_tracker_remove_files_not_all_files_removed;

							// show message
							Toast toast = Toast.makeText(getApplicationContext(), getString(messageId),
									Toast.LENGTH_LONG);
							toast.show();
						}
					}).setNegativeButton(R.string.expenses_tracker_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();

		case CLEAR_DATA_DIALOG:
			Calendar currentMonth = CalendarUtils.getFirstDayOfMonth(Calendar.getInstance());
			DatePickerDialog datePickerDialog = new DatePickerDialog(this, mDateSetListener,
					currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH),
					currentMonth.get(Calendar.DAY_OF_MONTH));
			datePickerDialog.setMessage(getString(R.string.expenses_tracker_clear_data_title));
			return datePickerDialog;

		case CONFIRM_CLEAR_DATA_DIALOG:
			// create a basic confirmation dialog with yes/no
			builder = new AlertDialog.Builder(this);
			message = String.format(getString(R.string.expenses_tracker_clear_data_message),
					CalendarUtils.getDateString(mClearDataDate));
			builder.setMessage(message).setCancelable(false)
					.setPositiveButton(R.string.expenses_tracker_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// remove data
							boolean result = mDbAdapter.deleteExpensePriorTo(mClearDataDate.getTimeInMillis());

							// notify backup manager about changed information
							mBackupManager.dataChanged();

							// show message
							int messageId = result ? R.string.expenses_tracker_clear_data_success
									: R.string.expenses_tracker_clear_data_unsuccess;
							Toast toast = Toast.makeText(getApplicationContext(), getString(messageId),
									Toast.LENGTH_LONG);
							toast.show();
						}
					}).setNegativeButton(R.string.expenses_tracker_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();

		case INFO_DIALOG:
			final Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.expenses_tracker_info);

			TextView textView = (TextView) dialog.findViewById(R.id.expenses_tracker_info_text);
			message = getString(R.string.expenses_tracker_info_text);
			CharSequence html = Html.fromHtml(message);
			textView.setText(html);

			Button okButton = (Button) dialog.findViewById(R.id.expenses_tracker_info_ok);
			okButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});

			// create info dialog
			return dialog;
		}
		return null;
	}

	private void removeExportedFiles() {
		initializeExportDirectory();
		// if export directory is available, show confirmation dialog
		if (mExportDirectory != null)
			showDialog(REMOVE_EXPORTED_FILES_DIALOG);
	}

	private void initializeExportDirectory() {
		// check if export directory needs to be set
		if (mExportDirectory == null) {
			// try to get the directory
			Result<File> result = ExportExpensesUtils.getExportDirectory(this);
			mExportDirectory = result.getResult();

			// check if could be determined
			if (mExportDirectory == null) {
				// no: create and show error message
				String message = getString(result.getMessageId());
				if (result.getMessageArgs().length > 0)
					message = String.format(message, result.getMessageArgs());
				Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
				toast.show();
				return;
			}
		}
	}

	private List<? extends Map<String, ?>> initializeListMenuItems() {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		addMapToList(ADD_EXPENSE, getString(R.string.expenses_tracker_add_expense),
				getString(R.string.expenses_tracker_add_expense_description), result);
		addMapToList(VIEW_EXPENSES, getString(R.string.expenses_tracker_view_expenses),
				getString(R.string.expenses_tracker_view_expenses_description), result);
		addMapToList(ADD_EXPENSE_CATEGORY, getString(R.string.expenses_tracker_add_category),
				getString(R.string.expenses_tracker_add_category_description), result);
		addMapToList(VIEW_EXPENSE_CATEGORIES, getString(R.string.expenses_tracker_view_categories),
				getString(R.string.expenses_tracker_view_categories_description), result);
		addMapToList(EXPORT_EXPENSES, getString(R.string.expenses_tracker_export_expenses),
				getString(R.string.expenses_tracker_export_expenses_description), result);
		addMapToList(REMOVE_EXPORT_FILES, getString(R.string.expenses_tracker_remove_files),
				getString(R.string.expenses_tracker_remove_files_description), result);
		return result;
	}

	private void addMapToList(int position, String menuItem, String menuItemDescription,
			List<Map<String, String>> list) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(MENU_ITEM, menuItem);
		map.put(MENU_ITEM_DESCRIPTION, menuItemDescription);
		list.add(position, map);
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mClearDataDate.set(Calendar.YEAR, year);
			mClearDataDate.set(Calendar.MONTH, monthOfYear);
			mClearDataDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			showDialog(CONFIRM_CLEAR_DATA_DIALOG);
		}
	};
}