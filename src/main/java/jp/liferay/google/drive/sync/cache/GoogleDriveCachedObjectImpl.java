
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;

import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * Google Drive Cached Object
 * 
 * @author Yasuyuki Takeo
 */
public class GoogleDriveCachedObjectImpl implements GoogleDriveCachedObject {

	public GoogleDriveCachedObjectImpl(File file, Revision revision) {

		_file = file;
		_revision = revision;
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#getFile()
	 */
	@Override
	public File getFile() {

		return _file;
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#setFile(com.google.api.services.drive.model.File)
	 */
	@Override
	public void setFile(File file) {

		_file = file;
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#getRevision()
	 */
	@Override
	public Revision getRevision() {

		return _revision;
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#setRevision(com.google.api.services.drive.model.Revision)
	 */
	@Override
	public void setRevision(Revision revision) {

		_revision = revision;
	}

	private File _file;
	private Revision _revision;
}
