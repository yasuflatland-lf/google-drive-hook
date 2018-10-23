
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.liferay.document.library.repository.external.ExtRepositoryObject;

import java.util.ArrayList;
import java.util.List;

import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * Google Drive Cached Object
 * 
 * @author Yasuyuki Takeo
 */
@SuppressWarnings("rawtypes")
public class GoogleDriveCachedObjectImpl<T extends ExtRepositoryObject>
	implements GoogleDriveCachedObject {

	public GoogleDriveCachedObjectImpl(File file, Revision revision) {

		_file = file;
		_revision = revision;
	}

	@SuppressWarnings("unchecked")
	public GoogleDriveCachedObjectImpl(
		List<File> files, List extRepositoryObjects) {

		_files = files;
		_extRepositoryObjects = extRepositoryObjects;
	}

	/*
	 * (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#getFile()
	 */
	@Override
	public File getFile() {

		return _file;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#setFile(com.
	 * google.api.services.drive.model.File)
	 */
	@Override
	public void setFile(File file) {

		_file = file;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#getRevision()
	 */
	@Override
	public Revision getRevision() {

		return _revision;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#setRevision(
	 * com.google.api.services.drive.model.Revision)
	 */
	@Override
	public void setRevision(Revision revision) {

		_revision = revision;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * jp.liferay.google.drive.sync.cache.GoogleDriveCachedObject#toString()
	 */
	@Override
	public String toString() {

		return "GoogleDriveCachedObjectImpl [_file=" + _file + ", _revision=" +
			_revision + "]";
	}

	public List<T> getExtRepositoryObjects() {

		return _extRepositoryObjects;
	}

	public List<File> getFiles() {

		return _files;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setExtRepositoryObjects(List extRepositoryObjects) {

		_extRepositoryObjects.addAll(extRepositoryObjects);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setFiles(List files) {

		_files.addAll(files);
	}

	private List<T> _extRepositoryObjects = new ArrayList<>();
	private List<File> _files;
	private File _file;
	private Revision _revision;

}
