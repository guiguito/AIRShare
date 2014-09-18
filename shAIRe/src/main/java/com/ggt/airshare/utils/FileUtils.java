package com.ggt.airshare.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * File utils
 * 
 * @author gduche
 */
public class FileUtils {

	public static String getRealPathFromUri(Context context, Uri contentUri) {
		// TODO improve handling of content from picasa
		String path = null;
		Cursor cursor = null;
		try {
			// same column name for Audio and images.
			String[] proj = { MediaStore.Audio.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null,
					null, null);
			if (cursor != null) {
				int column_index = cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
				cursor.moveToFirst();
				path = cursor.getString(column_index);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		if (path == null) {
			return contentUri.getPath();
		} else {
			return path;
		}
	}
}
