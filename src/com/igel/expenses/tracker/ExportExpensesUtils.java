package com.igel.expenses.tracker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

public class ExportExpensesUtils {

	public enum ExportResult {
		EXPORTED_DATA, EXPORTED_NO_DATA, ERROR
	};

	public enum ClearDirectoryResult {
		REMOVED_ALL_FILES, REMOVED_NO_FILES, REMOVED_SOME_FILES
	};

	private static final String LOG_TAG = "ExportExpenses";

	private static final String LINE_SEPARTOR = System.getProperty("line.separator");

	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	public static ClearDirectoryResult clearDirectory(File exportDirectory) {
		File[] files = exportDirectory.listFiles();
		if (files == null)
			return ClearDirectoryResult.REMOVED_NO_FILES;

		boolean deletedAllFiles = true;
		boolean deletedSomething = false;
		for (File file : files) {
			deletedAllFiles &= file.delete();
			deletedSomething = true;
		}
		if (deletedAllFiles) {
			if (deletedSomething)
				return ClearDirectoryResult.REMOVED_ALL_FILES;
			else
				return ClearDirectoryResult.REMOVED_NO_FILES;
		} else {
			if (deletedSomething)
				return ClearDirectoryResult.REMOVED_SOME_FILES;
			else
				return ClearDirectoryResult.REMOVED_NO_FILES;

		}
	}

	public static Result<File> getExportDirectory(Activity activity) {
		// get export directory URI from preferences
		SharedPreferences prefs = activity.getSharedPreferences(
				activity.getApplicationContext().getPackageName() + "_preferences", Activity.MODE_PRIVATE);
		String directoryUriString = prefs.getString(activity.getString(R.string.expenses_tracker_preferences_folder),
				null);

		// initialize stuff
		File exportDirectory = null;

		if (directoryUriString != null) {
			// if something is found in preferences
			URI uri;
			try {
				uri = new URI(directoryUriString);
				exportDirectory = new File(uri);
			} catch (URISyntaxException e) {
				Log.e(LOG_TAG, e.getMessage());
				return new Result<File>(null, R.string.export_expenses_error_cannot_find_directoy, directoryUriString);
			}
		} else {
			// default directory
			exportDirectory = getDefaultDirectory(activity.getApplicationContext().getPackageName());
		}
		return new Result<File>(exportDirectory);
	}

	public static Result<File> initExportDirectory(Activity activity) {
		// get export directory
		Result<File> exportDirectoryResult = getExportDirectory(activity);
		File exportDirectory = exportDirectoryResult.getResult();

		// check if export directory not ok
		if (exportDirectory == null)
			return exportDirectoryResult;

		// get flag indicating if external storage is writable
		boolean externalStorageWritable = ExportExpensesUtils.isExternalStorageWritable();

		// show error if export is not possible
		if (!externalStorageWritable || !exportDirectory.canWrite()) {
			return new Result<File>(null, R.string.export_expenses_error_cannot_write_to_export_directoy,
					exportDirectory.getAbsolutePath());
		}

		// if export directory does not exist, create it
		if (!exportDirectory.exists())
			exportDirectory.mkdirs();

		// add .nomedia file if it does not exist
		File nomedia = new File(exportDirectory, ".nomedia");
		if (!nomedia.exists())
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				// doesn't matter if something goes wrong here;
			}

		return new Result<File>(exportDirectory);
	}

	public static String getExportFileNamePostfix() {
		Date currentDateTime = Calendar.getInstance().getTime();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'_'yyyyMMddHHmm'.csv'");
		String postfix = simpleDateFormat.format(currentDateTime);
		return postfix;
	}

	public static Result<ExportResult> exportExpenseCategories(File exportDirectory, String postfix,
			ExpensesDbAdapter dbAdapter) {
		// create export file
		String fileName = "expenseCategories" + postfix;
		File categoriesFile = new File(exportDirectory, fileName);

		// forward declarations
		BufferedWriter writer = null;
		Cursor cursor = null;

		try {
			cursor = dbAdapter.fetchAllExpenseCategoriesForExport();
			boolean hasData = cursor.moveToFirst();
			if (hasData) {
				// create the file and a writer
				categoriesFile.createNewFile();
				writer = new BufferedWriter(new FileWriter(categoriesFile));

				// write the header
				String formatString = "%s,\"%s\",\"%s\",%s";
				String header = String.format(formatString, ExpensesDbAdapter.EXPENSE_CATEGORY_ID,
						ExpensesDbAdapter.EXPENSE_CATEGORY_NAME, ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION,
						ExpensesDbAdapter.DELETED);
				writer.write(header);
				writer.newLine();

				do {
					String categoryId = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_ID));
					String categoryName = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_NAME))
							.replace(LINE_SEPARTOR, " ");
					String categoryDescription = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_CATEGORY_DESCRIPTION))
							.replace(LINE_SEPARTOR, " ");
					int deleted = cursor.getInt(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.DELETED));
					String deletedString = deleted == ExpensesDbAdapter.TRUE ? "TRUE" : "FALSE";
					String line = String.format(formatString, categoryId, categoryName, categoryDescription,
							deletedString);
					writer.write(line);
					writer.newLine();
				} while (cursor.moveToNext());
				return new Result<ExportResult>(ExportResult.EXPORTED_DATA);
			}
			return new Result<ExportResult>(ExportResult.EXPORTED_NO_DATA);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(LOG_TAG, e.getMessage());
			return new Result<ExportResult>(ExportResult.ERROR,
					R.string.export_expenses_error_cannot_export_expense_categories, categoriesFile.getAbsolutePath());
		} finally {
			// clean up
			closeWriter(writer);
			if (cursor != null)
				cursor.close();
		}
	}

	public static Result<ExportResult> exportExpenses(File exportDirectory, String postfix, ExpensesDbAdapter dbAdapter,
			Calendar from, Calendar to) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy'-'MM");

		// initialize result
		ExportResult exportedData = ExportResult.EXPORTED_NO_DATA;

		// continue while something needs to be exported
		while (from.before(to)) {
			// initialize stuff data
			String prefix = simpleDateFormat.format(from.getTime());
			long currentFromInMillis = from.getTimeInMillis();
			long currentToInMillis = CalendarUtils.getFirstDayOfNextMonth(from).getTimeInMillis();

			// export >= from and < to
			Result<ExportResult> result = exportExpensesInRange(exportDirectory, prefix + postfix, dbAdapter,
					currentFromInMillis, currentToInMillis);

			// check result
			if (result.getResult() == ExportResult.ERROR)
				return result;

			// update result
			if (exportedData == ExportResult.EXPORTED_NO_DATA)
				exportedData = result.getResult();

			// shift to next month
			from.add(Calendar.MONTH, 1);
			from = CalendarUtils.getFirstDayOfMonth(from);
		}
		return new Result<ExportResult>(exportedData);
	}

	private static Result<ExportResult> exportExpensesInRange(File exportDirectory, String fileName,
			ExpensesDbAdapter dbAdapter, long fromInMillis, long toInMillis) {
		// create export file
		File exportFile = new File(exportDirectory, fileName);

		// forward declarations
		BufferedWriter writer = null;
		Cursor cursor = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd");
		Calendar date = Calendar.getInstance();

		try {
			cursor = dbAdapter.fetchAllExpensesInRangeForExport(fromInMillis, toInMillis);
			boolean hasData = cursor.moveToFirst();
			if (hasData) {
				// create the file and a writer
				exportFile.createNewFile();
				writer = new BufferedWriter(new FileWriter(exportFile));

				// write the header
				String formatString = "%s,%s,%s,\"%s\",%s";
				String header = String.format(formatString, ExpensesDbAdapter.EXPENSE_ID,
						ExpensesDbAdapter.EXPENSE_DATE, ExpensesDbAdapter.EXPENSE_AMOUNT,
						ExpensesDbAdapter.EXPENSE_DETAILS, ExpensesDbAdapter.EXPENSE_EXPENSE_CATEGORY_ID);
				writer.write(header);
				writer.newLine();

				do {
					String expenseId = cursor.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_ID));
					long expenseDateMillis = cursor
							.getLong(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DATE));
					date.setTimeInMillis(expenseDateMillis);
					String expenseDateString = simpleDateFormat.format(date.getTime());
					String expenseAmount = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_AMOUNT));
					String expenseDetails = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_DETAILS))
							.replace(LINE_SEPARTOR, " ");
					String expenseCategoryId = cursor
							.getString(cursor.getColumnIndexOrThrow(ExpensesDbAdapter.EXPENSE_EXPENSE_CATEGORY_ID));
					String line = String.format(formatString, expenseId, expenseDateString, expenseAmount,
							expenseDetails, expenseCategoryId);
					writer.write(line);
					writer.newLine();
				} while (cursor.moveToNext());
				return new Result<ExportResult>(ExportResult.EXPORTED_DATA);
			}
			return new Result<ExportResult>(ExportResult.EXPORTED_NO_DATA);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(LOG_TAG, e.getMessage());
			return new Result<ExportResult>(ExportResult.ERROR, R.string.export_expenses_error_cannot_export_expenses,
					exportFile.getAbsolutePath());
		} finally {
			// clean up
			closeWriter(writer);
			if (cursor != null)
				cursor.close();
		}
	}

	private static void closeWriter(BufferedWriter writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		}
	}

	private static File getDefaultDirectory(String packageName) {
		File externalStorageDirectory = Environment.getExternalStorageDirectory();
		String externalStoragePath = externalStorageDirectory.getAbsolutePath();
		externalStoragePath += "/Android/data/";
		externalStoragePath += packageName;
		externalStoragePath += "/files";
		File exportDirectory = new File(externalStoragePath);
		if (!exportDirectory.exists())
			exportDirectory.mkdirs();
		return exportDirectory;
	}
}
