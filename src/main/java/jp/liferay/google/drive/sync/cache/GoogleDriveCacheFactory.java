
package jp.liferay.google.drive.sync.cache;

import com.liferay.portal.kernel.util.AutoResetThreadLocal;

/**
 * Google Drive Cache Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
public class GoogleDriveCacheFactory {

	public static GoogleDriveCache create() {

		return _googleDriveThreadLocal.get();
	}

	private static ThreadLocal<GoogleDriveCache> _googleDriveThreadLocal =
		new AutoResetThreadLocal<>(
			GoogleDriveCache.class.getName(), new GoogleDriveCache());
}
