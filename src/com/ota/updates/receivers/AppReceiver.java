/*
 * Copyright (C) 2014 Matt Booth (Kryten2k35).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ota.updates.receivers;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

import com.ota.updates.R;
import com.ota.updates.RomUpdate;
import com.ota.updates.activities.AvailableActivity;
import com.ota.updates.activities.MainActivity;
import com.ota.updates.tasks.LoadUpdateManifest;
import com.ota.updates.utils.Constants;
import com.ota.updates.utils.Preferences;
import com.ota.updates.utils.Utils;

public class AppReceiver extends BroadcastReceiver implements Constants{

	public final String TAG = this.getClass().getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		long mDownloadID = Preferences.getDownloadID(context);

		if(DEBUGGING)
			Log.v(TAG, "Receiving " + mDownloadID);

		if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (id != mDownloadID) {
				if(DEBUGGING)
					Log.v(TAG, "Ignoring unrelated download " + id);
				return;
			}

			DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(id);
			Cursor cursor = downloadManager.query(query);

			// it shouldn't be empty, but just in case
			if (!cursor.moveToFirst()) {
				if(DEBUGGING)
					Log.e(TAG, "Empty row");
				return;
			}

			int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
				if(DEBUGGING)
					Log.w(TAG, "Download Failed");
				Preferences.setDownloadFinished(context, false);
				AvailableActivity.invalidateMenu();
				return;
			} else {
				if(DEBUGGING)
					Log.v(TAG, "Download Succeeded");
				Preferences.setDownloadFinished(context, true);
				AvailableActivity.setupProgress(context.getResources());
				AvailableActivity.invalidateMenu();
				return;
			}
		}

		if(action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)){
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (id != mDownloadID) {
				if(DEBUGGING)
					Log.v(TAG, "Ignoring unrelated download " + id);
				return;
			} else {
				Intent i = new Intent(context, MainActivity.class);
				context.startActivity(i);
			}
		}

		if(action.equals(MANIFEST_CHECK_BACKGROUND)){
			if(DEBUGGING)
				Log.d(TAG, "Receiving background check confirmation");

			boolean updateAvailable = RomUpdate.getUpdateAvailability(context);
			String filename = RomUpdate.getFilename(context);

			if(updateAvailable){
				setupNotification(context, filename);
			}
		}

		if(action.equals(START_UPDATE_CHECK)) {
			if (DEBUGGING)
				Log.d(TAG, "Update check started");
			new LoadUpdateManifest(context, false).execute();
		}

		if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
			if(DEBUGGING) {
				Log.d(TAG, "Boot received");
			}
			boolean backgroundCheck = Preferences.getBackgroundService(context);
			if(backgroundCheck){
				if(DEBUGGING)
					Log.d(TAG, "Starting background check alarm");
				Utils.setBackgroundCheck(context, true);
			}
		}
	}

	private void setupNotification(Context context, String filename){
		if(DEBUGGING)
			Log.d(TAG, "Showing notification");
		NotificationManager mNotifyManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setContentTitle(context.getString(R.string.update_available))
		.setContentText(filename)
		.setSmallIcon(R.drawable.ic_notif)
		.setContentIntent(intent)
		.setAutoCancel(true)
		.setPriority(NotificationCompat.PRIORITY_HIGH)
		.setDefaults(NotificationCompat.DEFAULT_ALL)
		.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		mNotifyManager.notify(0, mBuilder.build());
	}
}


