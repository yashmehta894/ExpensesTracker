package com.igel.expenses.tracker;

import java.util.HashMap;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class ExpensesDbAdapter {

	public static final String DATABASE_NAME = "expensesTracker";
	public static final int DATABASE_VERSION = 3;

	public static final int TRUE = 0;
	public static final int FALSE = 1;

	public static final String UPDATING_DATE = "updatingDate";
	public static final String DELETED = "deleted";

	private static final String EXPENSE_CATEGORY_TABLE = "expenseCategory";
	public static final String EXPENSE_CATEGORY_ID = "_id";
	public static final String EXPENSE_CATEGORY_NAME = "name";
	public static final String EXPENSE_CATEGORY_DESCRIPTION = "description";

	public static final int UNKNOWN_EXPENSE_CATEGORY_ID = 1;

	private static final String EXPENSE_TABLE = "expense";
	public static final String EXPENSE_ID = "_id";
	public static final String EXPENSE_DATE = "expenseDate";
	public static final String EXPENSE_AMOUNT = "amount";
	public static final String EXPENSE_DETAILS = "details";
	public static final String EXPENSE_EXPENSE_CATEGORY_ID = "expenseCategoryId";

	private static final String TAG = "ExpenseDbAdapter";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private Context mCtx;

	private static HashMap<String, String> sExpensesProjectionMap;

	public ExpensesDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public ExpensesDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		sExpensesProjectionMap = new HashMap<String, String>();
		sExpensesProjectionMap.put(EXPENSE_TABLE + "." + EXPENSE_ID,
				EXPENSE_TABLE + "." + EXPENSE_ID + " AS " + EXPENSE_ID);
		sExpensesProjectionMap.put(EXPENSE_DATE, EXPENSE_DATE);
		sExpensesProjectionMap.put(EXPENSE_AMOUNT, EXPENSE_AMOUNT);
		sExpensesProjectionMap.put(EXPENSE_DETAILS, EXPENSE_DETAILS);
		sExpensesProjectionMap.put(EXPENSE_CATEGORY_NAME, EXPENSE_CATEGORY_NAME);
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public void clearDatabase() {
		mDb.delete(EXPENSE_TABLE, null, null);
		mDb.delete(EXPENSE_CATEGORY_TABLE, null, null);
	}

	public void createExpenseCategories(Set<ContentValues> contentValuesSet) {
		for (ContentValues contentValues : contentValuesSet)
			mDb.insert(EXPENSE_CATEGORY_TABLE, null, contentValues);
	}

	public long createExpenseCategory(String name, String description) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(EXPENSE_CATEGORY_NAME, name);
		initialValues.put(EXPENSE_CATEGORY_DESCRIPTION, description);
		initialValues.put(DELETED, FALSE);
		initialValues.put(UPDATING_DATE, System.currentTimeMillis());

		return mDb.insert(EXPENSE_CATEGORY_TABLE, null, initialValues);
	}

	public boolean updateExpenseCategory(long rowId, String name, String description) {
		ContentValues args = new ContentValues();
		args.put(EXPENSE_CATEGORY_NAME, name);
		args.put(EXPENSE_CATEGORY_DESCRIPTION, description);
		args.put(UPDATING_DATE, System.currentTimeMillis());

		return mDb.update(EXPENSE_CATEGORY_TABLE, args, EXPENSE_CATEGORY_ID + " = " + rowId, null) > 0;
	}

	public boolean deleteExpenseCategory(long rowId) {
		ContentValues args = new ContentValues();
		args.put(DELETED, TRUE);
		args.put(UPDATING_DATE, System.currentTimeMillis());
		return mDb.update(EXPENSE_CATEGORY_TABLE, args, EXPENSE_CATEGORY_ID + " = " + rowId, null) > 0;
	}

	public Cursor fetchExpenseCategory(long rowId) throws SQLException {

		Cursor cursor = mDb.query(false, EXPENSE_CATEGORY_TABLE,
				new String[] { EXPENSE_CATEGORY_ID, EXPENSE_CATEGORY_NAME, EXPENSE_CATEGORY_DESCRIPTION },
				EXPENSE_CATEGORY_ID + " = " + rowId, null, null, null, null, null);

		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor fetchAllExpenseCategories() {
		return mDb.query(EXPENSE_CATEGORY_TABLE,
				new String[] { EXPENSE_CATEGORY_ID, EXPENSE_CATEGORY_NAME, EXPENSE_CATEGORY_DESCRIPTION },
				DELETED + " = " + FALSE, null, null, null, null);
	}

	public Cursor fetchAllExpenseCategoriesForExport() {
		return mDb.query(EXPENSE_CATEGORY_TABLE,
				new String[] { EXPENSE_CATEGORY_ID, EXPENSE_CATEGORY_NAME, EXPENSE_CATEGORY_DESCRIPTION, DELETED },
				null, null, null, null, null);
	}

	public Cursor fetchAllUpdatedExpenseCategories(long after, long to) {
		return mDb.query(EXPENSE_CATEGORY_TABLE,
				new String[] { EXPENSE_CATEGORY_ID, EXPENSE_CATEGORY_NAME, EXPENSE_CATEGORY_DESCRIPTION, DELETED,
						UPDATING_DATE },
				UPDATING_DATE + " > " + after + " and " + UPDATING_DATE + " <= " + to, null, null, null, null);
	}

	public void createExpenses(Set<ContentValues> contentValuesSet) {
		for (ContentValues contentValues : contentValuesSet)
			mDb.insert(EXPENSE_TABLE, null, contentValues);
	}

	public long createExpense(long dateInMillis, int amount, long expenseCategoryId, String details) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(EXPENSE_DATE, dateInMillis);
		initialValues.put(EXPENSE_AMOUNT, amount);
		initialValues.put(EXPENSE_EXPENSE_CATEGORY_ID, expenseCategoryId);
		initialValues.put(EXPENSE_DETAILS, details);
		initialValues.put(DELETED, FALSE);
		initialValues.put(UPDATING_DATE, System.currentTimeMillis());

		return mDb.insert(EXPENSE_TABLE, null, initialValues);
	}

	public boolean updateExpense(long rowId, long dateInMillis, int amount, long expenseCategoryId, String details) {
		ContentValues args = new ContentValues();
		args.put(EXPENSE_DATE, dateInMillis);
		args.put(EXPENSE_AMOUNT, amount);
		args.put(EXPENSE_EXPENSE_CATEGORY_ID, expenseCategoryId);
		args.put(EXPENSE_DETAILS, details);
		args.put(UPDATING_DATE, System.currentTimeMillis());

		return mDb.update(EXPENSE_TABLE, args, EXPENSE_ID + " = " + rowId, null) > 0;
	}

	public Cursor fetchExpense(long rowId) throws SQLException {
		Cursor cursor = mDb.query(false, EXPENSE_TABLE,
				new String[] { EXPENSE_ID, EXPENSE_DATE, EXPENSE_AMOUNT, EXPENSE_DETAILS, EXPENSE_EXPENSE_CATEGORY_ID },
				EXPENSE_ID + " = " + rowId, null, null, null, null, null);

		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public boolean deleteExpense(long rowId) {
		ContentValues args = new ContentValues();
		args.put(DELETED, TRUE);
		args.put(UPDATING_DATE, System.currentTimeMillis());
		return mDb.update(EXPENSE_TABLE, args, EXPENSE_ID + " = " + rowId, null) > 0;
	}

	public boolean deleteExpensePriorTo(long priorTo) {
		ContentValues args = new ContentValues();
		args.put(DELETED, TRUE);
		args.put(UPDATING_DATE, System.currentTimeMillis());
		return mDb.delete(EXPENSE_TABLE, EXPENSE_DATE + " < " + priorTo, null) > 0;
	}

	public Cursor fetchAllExpensesInRange(long from, long to) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(EXPENSE_TABLE + " left outer join " + EXPENSE_CATEGORY_TABLE + " on (" + EXPENSE_TABLE + "."
				+ EXPENSE_EXPENSE_CATEGORY_ID + " = " + EXPENSE_CATEGORY_TABLE + "." + EXPENSE_CATEGORY_ID + ")");
		qb.setProjectionMap(sExpensesProjectionMap);
		return qb.query(mDb,
				new String[] { EXPENSE_TABLE + "." + EXPENSE_ID, EXPENSE_DATE, EXPENSE_AMOUNT, EXPENSE_DETAILS,
						EXPENSE_CATEGORY_NAME },
				EXPENSE_DATE + " >= " + from + " and " + EXPENSE_DATE + " < " + to + " and " + EXPENSE_TABLE + "."
						+ DELETED + " = " + FALSE,
				null, null, null, EXPENSE_TABLE + "." + EXPENSE_DATE + " desc");
	}

	public Cursor fetchAllExpensesInRangeForExport(long from, long to) {
		return mDb.query(EXPENSE_TABLE,
				new String[] { EXPENSE_ID, EXPENSE_DATE, EXPENSE_AMOUNT, EXPENSE_DETAILS, EXPENSE_EXPENSE_CATEGORY_ID },
				EXPENSE_DATE + " >= " + from + " and " + EXPENSE_DATE + " < " + to + " and " + DELETED + " = " + FALSE,
				null, null, null, null);
	}

	public Cursor fetchAllUpdatedExpenses(long after, long to) {
		return mDb.query(EXPENSE_TABLE,
				new String[] { EXPENSE_ID, EXPENSE_DATE, EXPENSE_AMOUNT, EXPENSE_DETAILS, EXPENSE_EXPENSE_CATEGORY_ID,
						DELETED, UPDATING_DATE },
				UPDATING_DATE + " > " + after + " and " + UPDATING_DATE + " <= " + to, null, null, null, null);
	}

	public long getExpensesSum(long from, long to) {
		Cursor cursor = mDb.query(false, EXPENSE_TABLE, new String[] { "sum(" + EXPENSE_AMOUNT + ")" },
				EXPENSE_DATE + " >= " + from + " and " + EXPENSE_DATE + " < " + to + " and " + DELETED + " = " + FALSE,
				null, null, null, null, null);

		long result = 0;
		if (cursor != null && cursor.moveToFirst()) {
			String amountString = cursor.getString(cursor.getColumnIndexOrThrow("sum(" + EXPENSE_AMOUNT + ")"));
			if (amountString != null)
				result = Long.valueOf(amountString);
		}
		if (cursor != null)
			cursor.close();
		return result;
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		private Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "Creating expenses-tracker database");
			db.execSQL("create table " + EXPENSE_CATEGORY_TABLE + "(" + EXPENSE_CATEGORY_ID
					+ " integer primary key autoincrement, " + EXPENSE_CATEGORY_NAME + " text not null, "
					+ EXPENSE_CATEGORY_DESCRIPTION + " text not null, " + DELETED + " integer not null, "
					+ UPDATING_DATE + " integer not null);");
			db.execSQL("create table " + EXPENSE_TABLE + "(" + EXPENSE_ID + " integer primary key autoincrement, "
					+ EXPENSE_DATE + " integer not null, " + EXPENSE_AMOUNT + " integer not null, "
					+ EXPENSE_EXPENSE_CATEGORY_ID + " integer not null, " + EXPENSE_DETAILS + " text not null, "
					+ DELETED + " integer not null, " + UPDATING_DATE
					+ " integer not null, constraint exp2expCat foreign key (" + EXPENSE_EXPENSE_CATEGORY_ID
					+ ") references " + EXPENSE_CATEGORY_TABLE + " (" + EXPENSE_CATEGORY_ID + "));");
			db.execSQL("insert into " + EXPENSE_CATEGORY_TABLE + " (" + EXPENSE_CATEGORY_ID + ", "
					+ EXPENSE_CATEGORY_NAME + ", " + EXPENSE_CATEGORY_DESCRIPTION + ", " + DELETED + ", "
					+ UPDATING_DATE + ") values ( " + UNKNOWN_EXPENSE_CATEGORY_ID + ", '"
					+ mContext.getString(R.string.unknown_expense_category_name) + "', '"
					+ mContext.getString(R.string.unknown_expense_category_description) + "', " + FALSE + ", "
					+ System.currentTimeMillis() + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 1) {
				db.execSQL("alter table " + EXPENSE_CATEGORY_TABLE + " add column " + UPDATING_DATE
						+ " integer not null default " + System.currentTimeMillis() + ";");
				db.execSQL("alter table " + EXPENSE_TABLE + " add column " + UPDATING_DATE
						+ " integer not null default " + System.currentTimeMillis() + ";");
				db.execSQL("alter table " + EXPENSE_TABLE + " add column " + DELETED + " integer not null default "
						+ FALSE + ";");
			} else if (oldVersion == 2) {
				db.execSQL("alter table " + EXPENSE_TABLE + " add column " + DELETED + " integer not null default "
						+ FALSE + ";");
			} else
				throw new IllegalStateException("Didn't see this one coming.");
		}
	}
}
