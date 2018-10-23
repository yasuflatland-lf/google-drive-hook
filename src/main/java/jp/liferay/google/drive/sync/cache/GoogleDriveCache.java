
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.liferay.document.library.repository.external.ExtRepositoryObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.webcache.WebCacheItem;
import com.liferay.portal.kernel.webcache.WebCachePoolUtil;

import java.util.List;

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

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param drive
	 * @param refreshTime
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, Drive drive, long refreshTime) {

		WebCacheItem wci =
			new GoogleDriveObjectWebCacheItem(drive, refreshTime);

		GoogleDriveCachedObject googleDriveCachedObject = null;

		try {
			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}
		catch (ClassCastException cce) {
			_log.info("Retrive object. Key <" + extRepositoryObjectKey + ">");

			WebCachePoolUtil.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}

	}

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param file
	 * @param drive
	 * @param refreshTime
	 * @return
	 */
	@SuppressWarnings("rawtypes")
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

			return googleDriveCachedObject;
		}
		catch (ClassCastException cce) {
			_log.info("Retrive object. Key <" + extRepositoryObjectKey + ">");

			WebCachePoolUtil.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}
	}

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param files
	 * @param extRepositoryObjects
	 * @param refreshTime
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public <T extends ExtRepositoryObject> GoogleDriveCachedObject getGoogleDriveCachedDirectory(
		String extRepositoryObjectKey, List<File> files,
		List<T> extRepositoryObjects, long refreshTime, boolean initialize) {

		@SuppressWarnings("unchecked")
		WebCacheItem wci = new GoogleDriveDirWebCacheItem(
			files, extRepositoryObjects, refreshTime, initialize);

		GoogleDriveCachedObject googleDriveCachedObject = null;

		try {
			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}
		catch (ClassCastException cce) {
			_log.info("Retrive object. Key <" + extRepositoryObjectKey + ">");

			WebCachePoolUtil.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				(GoogleDriveCachedObject) WebCachePoolUtil.get(
					extRepositoryObjectKey, wci);

			return googleDriveCachedObject;
		}
	}

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param drive
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, Drive drive) {

		return getGoogleDriveCachedObject(extRepositoryObjectKey, drive, -1);

	}

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param file
	 * @param drive
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, File file, Drive drive) {

		return getGoogleDriveCachedObject(
			extRepositoryObjectKey, file, drive, -1);

	}

	/**
	 * Google Drive Cache
	 * 
	 * @param extRepositoryObjectKey
	 * @param files
	 * @param extRepositoryObjects
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public <T extends ExtRepositoryObject> GoogleDriveCachedObject getGoogleDriveCachedDirectory(
		String extRepositoryObjectKey, List<File> files,
		List<T> extRepositoryObjects, boolean initialize) {

		return getGoogleDriveCachedDirectory(
			extRepositoryObjectKey, files, extRepositoryObjects, -1, initialize);
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
