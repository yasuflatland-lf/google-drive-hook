
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.webcache.WebCacheException;
import com.liferay.portal.kernel.webcache.WebCacheItem;

import java.io.IOException;

/**
 * Google Drive Object Cache
 * 
 * @author Yasuyuki Takeo
 *
 */
@SuppressWarnings("serial")
public class GoogleDriveObjectWebCacheItem implements WebCacheItem {

	public GoogleDriveObjectWebCacheItem(Drive drive, long refreshTime) {

		_refreshTime = _REFRESH_TIME;

		if (0 < refreshTime) {
			_refreshTime = refreshTime;
		}

		_drive = drive;
	}

	@Override
	public Object convert(String extRepositoryObjectKey)
		throws WebCacheException {

		File file = null;
		Revision revision = null;

		try {
			file = getFile(extRepositoryObjectKey);

			if (Validator.isNotNull(file.getCanReadRevisions())) {
				revision = getLatestRevision(file);
			}

			return new GoogleDriveCachedObjectImpl(file, revision);
		}
		catch (IOException e) {
			_log.error(e, e);
			return new GoogleDriveCachedObjectImpl(file, revision);
		}
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
	private static final long _REFRESH_TIME = Time.MINUTE * 30;
	private Drive _drive;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveObjectWebCacheItem.class);

}
