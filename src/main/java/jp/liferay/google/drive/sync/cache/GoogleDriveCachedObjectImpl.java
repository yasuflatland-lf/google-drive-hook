
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;

import java.util.List;

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

	public GoogleDriveCachedObjectImpl(List<File> files, Revision revision) {

		_files = files;
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

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#toString()
	 */
	@Override
	public String toString() {

		return "GoogleDriveCachedObjectImpl [_file=" + _file + ", _files=" +
			_files + ", _revision=" + _revision + "]";
	}

	private File _file;
	private List<File> _files;
	private Revision _revision;

}
