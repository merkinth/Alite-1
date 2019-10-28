package de.phbouillon.android.games.alite.io;

/* Alite - Discover the Universe on your Favorite Android Device
 * Copyright (C) 2015 Philipp Bouillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful and
 * fun, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * http://http://www.gnu.org/licenses/gpl-3.0.txt.
 */

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import de.phbouillon.android.games.alite.AliteLog;
import de.phbouillon.android.games.alite.BuildConfig;

/**
 * For more info about APK Expansion files see http://developer.android.com/google/play/expansion-files.html
 *
 * @author Sergey Khokhlov
 *
 */
public class ObbExpansionsManager {
	public static String TAG = "ObbExpansions";

	private File mainFile;
	private StorageManager sm;
	private ObbListener listener;
	private MountChecker mainChecker = new MountChecker();

	private static ObbExpansionsManager instance;

	private ObbExpansionsManager(Context context, final ObbListener listener) {
		this.listener = listener;

		AliteLog.d(TAG, "Creating new instance...");
		String packageName = context.getPackageName();
		AliteLog.d(TAG, "Package name = " + packageName);
		AliteLog.d(TAG, "Package version = " + BuildConfig.VERSION_CODE);
		mainFile = new File(Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName + "/"
			+ "main." + BuildConfig.VERSION_CODE + "." + packageName + ".obb");

		sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
		AliteLog.d(TAG, "Check if main file already mounted: " + sm.isObbMounted(mainFile.getAbsolutePath()));
		if (sm.isObbMounted(mainFile.getAbsolutePath())) {
			AliteLog.d(TAG, "Main file already mounted.");
			listener.onMountSuccess();
		} else {
			mountMain();
		}

		if (!mainFile.exists()) {
			AliteLog.d(TAG, "No expansion files found!");
			listener.onFilesNotFound();
		}
	}

	private void mountMain() {
		if (mainFile.exists()) {
			AliteLog.d(TAG, "Mounting main file...");
			AliteLog.d(TAG, "Scheduling mount checker...");
			// I have left the mount checker in place but extended it's initial activation to
			// 8 seconds.  If it is clear that this modification is working the mountchecker
			// can be removed altogether
			new Timer().schedule(mainChecker, 8000);
			sm.mountObb(mainFile.getAbsolutePath(), null, mainObbStateChangeListener );
		} else {
			AliteLog.d(TAG, "Patch file not found");
		}
	}

	private void unmountMain(OnObbStateChangeListener listener) {
		sm.unmountObb(mainFile.getAbsolutePath(), true, listener);
	}

	// Making the listener into an instance variable ensures that the reference is not lost
	// and zeroed in the storage manager before it can be used..
	private OnObbStateChangeListener mainObbStateChangeListener = new OnObbStateChangeListener() {
		@Override
		public void onObbStateChange(String path, int state) {
			super.onObbStateChange(path, state);
			if (state == MOUNTED) {
				AliteLog.d(TAG, "Mounting main file done.");
//				main = sm.getMountedObbPath(mainFile.getAbsolutePath());
				if ( listener != null ) {
					listener.onMountSuccess();
					mainChecker.cancel();
				}
			} else {
				AliteLog.d(TAG, "Mounting main file failed with state = " + state);
				if ( listener != null )
					listener.onObbStateChange(path, state);
			}
		}
	};

	public String getMainRoot() {
		String result = sm.getMountedObbPath(mainFile.getAbsolutePath());
		if (result == null) {
			return null;
		}
		if (!result.endsWith("/")) {
			return result + "/";
		}
		return result;
	}

	/**
	 * Use this method to create (or reinit) manager
	 *
	 * @param context
	 * @param listener
	 * @return
	 */
	static ObbExpansionsManager createNewInstance(Context context, ObbListener listener) {
		instance = new ObbExpansionsManager(context, listener);
		return instance;
	}

	static void destroyInstance(OnObbStateChangeListener listener) {
		instance.unmountMain(listener);
		instance = null;
	}

	/**
	 * Use this method to get existing instance of manager
	 *
	 * @return instance of manager. If null - call createNewInstance to create new one
	 */
	public static ObbExpansionsManager getInstance() {
		return instance;
	}

	public static abstract class ObbListener extends OnObbStateChangeListener {
		/**
		 * if state == 1 (MOUNTED) - mounting obb files finished successfully
		 * else - mounting obb files finished with errors.
		 */
		@Override
		public void onObbStateChange(String path, int state) {
			super.onObbStateChange(path, state);
		}

		/**
		 * Extension files not found - download required
		 */
		public abstract void onFilesNotFound();

		/**
		 * Mounting obb files finished successfully
		 */
		public abstract void onMountSuccess();
	}

	/**
	 * Russel's teapot, forgive me this dirty hack!
	 * Sometimes obb is mounting without calling OnObbStateChangeListener.onObbStateChange.
	 * So that's how I fixed that
	 */
	private class MountChecker extends TimerTask {
		@Override
		public void run() {
			if (sm != null && mainFile != null) {
				AliteLog.d(TAG, "MountChecker: Check if main file mounted without calling callback: " +
					sm.isObbMounted(mainFile.getAbsolutePath()));
			}
			if (sm != null && mainFile != null && sm.isObbMounted(mainFile.getAbsolutePath())) {
				listener.onMountSuccess();
			} else {
				mainChecker = new MountChecker();
				new Timer().schedule(mainChecker, 1000);
			}
		}
	}
}