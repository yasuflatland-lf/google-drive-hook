
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.webcache.WebCacheItem;
import com.liferay.portal.kernel.webcache.WebCachePoolUtil;

import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * Google Drive Cache
 * 
 * @author Yasuyuki Takeo
 */
public class GoogleDriveCache implements Cloneable {

	@Override
	public GoogleDriveCache clone() {

		if (_log.isInfoEnabled()) {
			Thread currentThread = Thread.currentThread();

			_log.info("Create " + currentThread.getName());
		}

		try {
			return (GoogleDriveCache) super.clone();
		}
		catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse);
		}
	}

	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, Drive drive, long refreshTime) {

		WebCacheItem wci =
			new GoogleDriveObjectWebCacheItem(drive, refreshTime);

		GoogleDriveCachedObject googleDriveCachedObject = null;

		try {
			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			// In case where the Root folder fails to fetch file object
			if (null == googleDriveCachedObject.getFile()) {
				WebCachePoolUtil.remove(extRepositoryObjectKey);

				googleDriveCachedObject =
					(GoogleDriveCachedObject) WebCachePoolUtil.get(
						extRepositoryObjectKey, wci);
			}

			return googleDriveCachedObject;
		}
		catch (ClassCastException cce) {
			_log.error(cce, cce);

			WebCachePoolUtil.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}

	}

	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, File file, Drive drive,
		long refreshTime) {

		WebCacheItem wci =
			new GoogleDriveObjectWebCacheItem(file, drive, refreshTime);

		GoogleDriveCachedObject googleDriveCachedObject = null;

		try {
			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			// In case where the Root folder fails to fetch file object
			if (null == googleDriveCachedObject.getFile()) {
				// Reset cache and force to retrive a File
				WebCachePoolUtil.remove(extRepositoryObjectKey);

				googleDriveCachedObject =
					(GoogleDriveCachedObject) WebCachePoolUtil.get(
						extRepositoryObjectKey, wci);
			}

			return googleDriveCachedObject;
		}
		catch (ClassCastException cce) {
			_log.error(cce, cce);

			WebCachePoolUtil.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}
	}

	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, Drive drive) {

		return getGoogleDriveCachedObject(extRepositoryObjectKey, drive, -1);

	}

	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, File file, Drive drive) {

		return getGoogleDriveCachedObject(
			extRepositoryObjectKey, file, drive, -1);

	}

	public void remove(String extRepositoryObjectKey) {

		if (_log.isDebugEnabled()) {
			_log.debug("Cache removed : " + extRepositoryObjectKey);
		}
		WebCachePoolUtil.remove(extRepositoryObjectKey);
	}

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveCache.class);
}
