package com.igel.expenses.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;

public class ExpensesTrackerBackupAgent extends BackupAgent {

	private static final String EXPENSE_CATEGORY_KEY_PREFIX = "EC";
	private static final String EXPENSE_KEY_PREFIX = "EX";

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {

		// initialize timestamp of last backup
		long lastUpdateTimestamp = 0;

		// check if old state is given
		if (oldState != null) {

			// get an input stream to read from old state
			FileInputStream instream = new FileInputStream(
					oldState.getFileDescriptor());
			DataInputStream in = new DataInputStream(instream);

			// read timestamp from last backup
			try {
				lastUpdateTimestamp = in.readLong();
			} catch (IOException e) {
				// if something went wrong reading the state file, be safe and
				// force a backup of all the data again.
				lastUpdateTimestamp = 0;
			}
		}

		// set timestamp of current backup
		long currentTimestamp = System.currentTimeMillis();

		// get access to the data base
		ExpensesDbAdapter dbAdapter = new ExpensesDbAdapter(
				getApplicationContext());
		dbAdapter.open();

		// initialize streams used to serialize data
		ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bufStream);

		try {
			// find all expense categories updated after lastUpdateTimeStamp
			Cursor expenseCategoryCursor = dbAdapter
					.fetchAllUpdatedExpenseCategories(lastUpdateTimestamp,
							currentTimestamp);
			boolean hasData = expenseCategoryCursor.moveToFirst();
			if (hasData) {
				do {
					// gather expense category information
					String categoryId = expenseCategoryCursor
							.getString(expenseCategoryCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_ID));
					String categoryName = expenseCategoryCursor
							.getString(expenseCategoryCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_NAME));
					String categoryDescription = expenseCategoryCursor
							.getString(expenseCategoryCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION));
					int deleted = expenseCategoryCursor
							.getInt(expenseCategoryCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.DELETED));
					String updatingDate = expenseCategoryCursor
							.getString(expenseCategoryCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.UPDATING_DATE));

					// write data to streams
					bufStream.reset();
					out.writeUTF(categoryName);
					out.writeUTF(categoryDescription);
					out.writeInt(deleted);
					out.writeUTF(updatingDate);

					// write stream data to backup data
					byte[] buf = bufStream.toByteArray();
					data.writeEntityHeader(EXPENSE_CATEGORY_KEY_PREFIX
							+ categoryId, buf.length);
					data.writeEntityData(buf, buf.length);

				} while (expenseCategoryCursor.moveToNext());
			}

			// find all expenses updated after lastUpdateTimeStamp
			Cursor expenseCursor = dbAdapter.fetchAllUpdatedExpenses(
					lastUpdateTimestamp, currentTimestamp);
			hasData = expenseCursor.moveToFirst();
			if (hasData) {
				do {
					// gather expense information
					String expenseId = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_ID));
					String expenseDate = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DATE));
					String expenseAmount = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_AMOUNT));
					String expenseDetails = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DETAILS));
					String expenseCategoryId = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_EXPENSE_CATEGORY_ID));
					int deleted = expenseCursor.getInt(expenseCursor
							.getColumnIndexOrThrow(ExpensesDbAdapter.DELETED));
					String updatingDate = expenseCursor
							.getString(expenseCursor
									.getColumnIndexOrThrow(ExpensesDbAdapter.UPDATING_DATE));

					if (deleted == ExpensesDbAdapter.TRUE) {
						data.writeEntityHeader(EXPENSE_KEY_PREFIX + expenseId,
								-1);
					} else {
						// write data to streams
						bufStream.reset();
						out.writeUTF(expenseDate);
						out.writeUTF(expenseAmount);
						out.writeUTF(expenseDetails);
						out.writeUTF(expenseCategoryId);
						out.writeInt(deleted);
						out.writeUTF(updatingDate);

						// write stream data to backup data
						byte[] buf = bufStream.toByteArray();
						data.writeEntityHeader(EXPENSE_KEY_PREFIX + expenseId,
								buf.length);
						data.writeEntityData(buf, buf.length);
					}
				} while (expenseCategoryCursor.moveToNext());
			}
		} catch (Exception e) {
			currentTimestamp = 0;
		}

		// write current backup timestamp to new backup state
		FileOutputStream outstream = new FileOutputStream(
				newState.getFileDescriptor());
		out = new DataOutputStream(outstream);
		out.writeLong(currentTimestamp);

		dbAdapter.close();
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {

		// set timestamp of current restore
		long currentTimestamp = System.currentTimeMillis();

		// get access to the data base
		ExpensesDbAdapter dbAdapter = new ExpensesDbAdapter(
				getApplicationContext());
		dbAdapter.open();

		Set<ContentValues> expenseCategoryValues = new HashSet<ContentValues>();
		Set<ContentValues> expenseValues = new HashSet<ContentValues>();

		// restore each received entity
		try {
			while (data.readNextHeader()) {
				String key = data.getKey();

				int dataSize = data.getDataSize();
				byte[] dataBuf = new byte[dataSize];
				data.readEntityData(dataBuf, 0, dataSize);
				ByteArrayInputStream instream = new ByteArrayInputStream(
						dataBuf);
				DataInputStream in = new DataInputStream(instream);

				if (key.startsWith(EXPENSE_CATEGORY_KEY_PREFIX))
					expenseCategoryValues.add(restoreExpenseCategory(in, key));
				else if (key.startsWith(EXPENSE_KEY_PREFIX))
					expenseValues.add(restoreExpense(in, key));
			}
			
			// clear database if data is received
			if (!expenseCategoryValues.isEmpty() || !expenseValues.isEmpty())
				dbAdapter.clearDatabase();
			
			if (!expenseCategoryValues.isEmpty())
				dbAdapter.createExpenseCategories(expenseCategoryValues);

			if (!expenseValues.isEmpty())
				dbAdapter.createExpenses(expenseValues);
			
		} catch (Exception e) {
			currentTimestamp = 0;
		}

		// write current restore timestamp to new backup state
		FileOutputStream outstream = new FileOutputStream(
				newState.getFileDescriptor());
		DataOutputStream out = new DataOutputStream(outstream);
		out.writeLong(currentTimestamp);

		dbAdapter.close();
	}

	private ContentValues restoreExpenseCategory(DataInputStream in, String key)
			throws IOException, ClassNotFoundException {
		ContentValues values = new ContentValues();
		values.put(ExpensesDbAdapter.EXPENSE_CATEGORY_ID, key.substring(2));
		values.put(ExpensesDbAdapter.EXPENSE_CATEGORY_NAME, in.readUTF());
		values.put(ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION, in.readUTF());
		values.put(ExpensesDbAdapter.DELETED, in.readInt());
		values.put(ExpensesDbAdapter.UPDATING_DATE, in.readUTF());
		return values;
	}

	private ContentValues restoreExpense(DataInputStream in, String key)
			throws IOException, ClassNotFoundException {
		ContentValues values = new ContentValues();
		values.put(ExpensesDbAdapter.EXPENSE_ID, key.substring(2));
		values.put(ExpensesDbAdapter.EXPENSE_DATE, in.readUTF());
		values.put(ExpensesDbAdapter.EXPENSE_AMOUNT, in.readUTF());
		values.put(ExpensesDbAdapter.EXPENSE_DETAILS, in.readUTF());
		values.put(ExpensesDbAdapter.EXPENSE_EXPENSE_CATEGORY_ID, in.readUTF());
		values.put(ExpensesDbAdapter.DELETED, in.readInt());
		values.put(ExpensesDbAdapter.UPDATING_DATE, in.readUTF());
		return values;
	}

}
