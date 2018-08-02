package com.igel.expenses.tracker;

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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ViewExpenseCategories extends ListActivity {

	// menu item id
	private static final int ADD_EXPENSE_CATEGORY_ID = Menu.FIRST;
	private static final int DELETE_EXPENSE_CATEGORY_ID = Menu.FIRST + 1;

	// activity codes for creating intents
	private static final int ACTIVITY_ADD_EXPENSE_CATEGORY = 0;
	private static final int ACTIVITY_EDIT_EXPENSE_CATEGORY = 1;

	// constants used to create dialogs
	private static final int DELETE_EXPENSE_CATEGORY_DIALOG = 0;

	// used to pass the ID of the expense category to the delete dialog (bad but
	// bundle not available)
	private long mExpenseCategoryId;

	// database adapter
	private ExpensesDbAdapter mDbAdapter;

	// backup manager
	private BackupManager mBackupManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set view
		setContentView(R.layout.view_expense_categories);

		// create new database adapter and open it
		mDbAdapter = new ExpensesDbAdapter(this);
		mDbAdapter.open();

		// initialize backup manager
		mBackupManager = new BackupManager(this);

		fetchDataFromDb();

		setTitle(R.string.view_expense_categories_title);

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_EXPENSE_CATEGORY_ID, 0, R.string.view_expense_categories_delete_expense_category);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_EXPENSE_CATEGORY_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.view_expense_categories_delete_expense_category_message).setCancelable(false)
					.setPositiveButton(R.string.expenses_tracker_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							deleteSelectedExpenseCategory();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// called when options menu is created
		super.onCreateOptionsMenu(menu);
		// add menu item to add expense category
		menu.add(0, ADD_EXPENSE_CATEGORY_ID, 0, R.string.view_expense_categories_add_expense_category);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_EXPENSE_CATEGORY_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			mExpenseCategoryId = info.id;
			if (mExpenseCategoryId == ExpensesDbAdapter.UNKNOWN_EXPENSE_CATEGORY_ID) {
				Toast toast = Toast.makeText(this,
						R.string.view_expense_categories_delete_unknown_expense_category_message, Toast.LENGTH_LONG);
				toast.show();
			} else {
				showDialog(DELETE_EXPENSE_CATEGORY_DIALOG);
			}
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// called when menu item is selected
		switch (item.getItemId()) {
		case ADD_EXPENSE_CATEGORY_ID:
			addExpenseCategory();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void addExpenseCategory() {
		Intent intent = new Intent(this, EditExpenseCategory.class);
		startActivityForResult(intent, ACTIVITY_ADD_EXPENSE_CATEGORY);
	}

	private void deleteSelectedExpenseCategory() {
		mDbAdapter.deleteExpenseCategory(mExpenseCategoryId);

		// notify backup manager about changed information
		mBackupManager.dataChanged();

		fetchDataFromDb();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// called when list item is clicked
		super.onListItemClick(l, v, position, id);

		// create new intent
		Intent intent = new Intent(this, EditExpenseCategory.class);

		// pass ID of expense category
		intent.putExtra(EditExpenseCategory.KEY_EXPENSE_CATEGORY_ID, id);

		// start activity
		startActivityForResult(intent, ACTIVITY_EDIT_EXPENSE_CATEGORY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// called when started activity is finished
		super.onActivityResult(requestCode, resultCode, intent);
	}

	private void fetchDataFromDb() {
		// Get all of expense categories from the database and create the item
		// list
		Cursor expenseCategoryCursor = mDbAdapter.fetchAllExpenseCategories();
		startManagingCursor(expenseCategoryCursor);

		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { ExpensesDbAdapter.EXPENSE_CATEGORY_NAME,
				ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION };

		// and an array of the fields we want to bind those fields to
		int[] to = new int[] { R.id.view_expense_categories_row_name, R.id.view_expense_categories_row_description };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter categories = new SimpleCursorAdapter(this, R.layout.view_expense_categories_row,
				expenseCategoryCursor, from, to);
		setListAdapter(categories);
	}
}