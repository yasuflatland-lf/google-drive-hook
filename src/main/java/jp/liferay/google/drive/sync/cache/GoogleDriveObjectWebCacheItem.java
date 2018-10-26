
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.webcache.WebCacheException;
import com.liferay.portal.kernel.webcache.WebCacheItem;

import java.io.IOException;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;
import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * Google Drive Object Cache
 * 
 * @author Yasuyuki Takeo
 */
public class GoogleDriveObjectWebCacheItem implements WebCacheItem {

	private static final long serialVersionUID = 5711880074097818978L;

	public GoogleDriveObjectWebCacheItem(Drive drive, long refreshTime) {

		_refreshTime = GoogleDriveConstants._REFRESH_TIME;

		if (refreshTime < 0) {
			_refreshTime = refreshTime;
		}

		_drive = drive;

	}

	public GoogleDriveObjectWebCacheItem(
		File file, Drive drive, long refreshTime) {

		_file = file;

		_refreshTime = GoogleDriveConstants._REFRESH_TIME;

		if (refreshTime < 0) {
			_refreshTime = refreshTime;
		}

		_drive = drive;

	}

	@SuppressWarnings("rawtypes")
	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		File file, Revision revision) {

		GoogleDriveCachedObject googleDriveCachedObject =
			new GoogleDriveCachedObjectImpl(file, revision);
		return googleDriveCachedObject;

	}

	@Override
	public Object convert(String extRepositoryObjectKey)
		throws WebCacheException {

		File file = null;
		Revision revision = null;

		// File
		if (null != _file) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Put <" + extRepositoryObjectKey + "> :" +
						_file.toString());
			}

			return getGoogleDriveCachedObject(_file, null);
		}

		try {

			_file = getFile(extRepositoryObjectKey);

			if (Validator.isNotNull(_file.getCanReadRevisions())) {
				revision = getLatestRevision(file);
			}

			if (_log.isDebugEnabled()) {
				_log.debug(
					"Fetch and Put <" + extRepositoryObjectKey + "> :" +
						_file.toString());
			}

		}
		catch (IOException e) {
			_log.error(e, e);
		}

		return getGoogleDriveCachedObject(file, revision);
	}

	/**
	 * Get File
	 * 
	 * @param extRepositoryObjectKey
	 * @return File object
	 * @throws IOException
	 */
	protected File getFile(String extRepositoryObjectKey)
		throws IOException {

		Drive.Files driveFiles = _drive.files();

		Drive.Files.Get driveFilesGet = driveFiles.get(extRepositoryObjectKey);

		return driveFilesGet.execute();
	}

	/**
	 * Get Latest Revision
	 * 
	 * @param file
	 * @return Latest Revision object
	 * @throws IOException
	 */
	protected Revision getLatestRevision(File file)
		throws IOException {

		Drive.Revisions driveRevisions = _drive.revisions();

		return driveRevisions.get(
			file.getId(), file.getHeadRevisionId()).execute();
	}

	@Override
	public long getRefreshTime() {

		return _refreshTime;
	}

	private long _refreshTime;
	private Drive _drive;
	private File _file = null;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveObjectWebCacheItem.class);

}
