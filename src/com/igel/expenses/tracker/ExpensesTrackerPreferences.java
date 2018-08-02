package com.igel.expenses.tracker;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

public class ExpensesTrackerPreferences extends android.preference.PreferenceActivity {

	private PreferenceScreen mFolderPreference;

	private static final int INSTALL_OI_FILE_MANAGER_DIALOG = 0;

	private static final int ACTION_CHOOSE_FOLDER = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		mFolderPreference = (PreferenceScreen) getPreferenceScreen()
				.findPreference(getString(R.string.expenses_tracker_preferences_folder));
		mFolderPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				folderPreferenceClicked(preference);
				return true;
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case INSTALL_OI_FILE_MANAGER_DIALOG:
			// create a basic confirmation dialog with yes/no
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getString(R.string.expenses_tracker_preferences_install_io_file_manager);
			builder.setMessage(message).setCancelable(false)
					.setPositiveButton(R.string.expenses_tracker_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							Intent intent = new Intent(Intent.ACTION_VIEW);

							String oiFileManagerPackage = getString(R.string.open_intents_file_manager_package);
							intent.setData(Uri.parse("market://details?id=" + oiFileManagerPackage));

							startActivity(intent);
						}
					}).setNegativeButton(R.string.expenses_tracker_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		}
		return super.onCreateDialog(id);
	}

	private void folderPreferenceClicked(Preference preference) {
		Intent oiFileManagerIntent = new Intent(getString(R.string.open_intents_file_picker_intent));
		List<ResolveInfo> queryIntentActivities = getPackageManager().queryIntentActivities(oiFileManagerIntent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (queryIntentActivities.isEmpty()) {
			// oi file manager not found, install it
			showDialog(INSTALL_OI_FILE_MANAGER_DIALOG);
			return;
		} else {
			startActivityForResult(oiFileManagerIntent, ACTION_CHOOSE_FOLDER);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// called when started activity is finished
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ACTION_CHOOSE_FOLDER) {
			if (resultCode == RESULT_CANCELED)
				return;
			if (resultCode == RESULT_OK) {
				Uri data = intent.getData();
				if (data != null) {
					Editor editor = mFolderPreference.getEditor();
					editor.putString(getString(R.string.expenses_tracker_preferences_folder), data.toString());
					editor.commit();
				}
			}
		}
	}
}
