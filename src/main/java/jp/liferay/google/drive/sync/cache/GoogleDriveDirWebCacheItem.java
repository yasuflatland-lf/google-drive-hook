
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.model.File;
import com.liferay.document.library.repository.external.ExtRepositoryObject;
import com.liferay.portal.kernel.webcache.WebCacheException;
import com.liferay.portal.kernel.webcache.WebCacheItem;

import java.util.ArrayList;
import java.util.List;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;
import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * Directory object cache
 * 
 * @author Yasuyuki Takeo
 * @param <T>
 */
public class GoogleDriveDirWebCacheItem<T extends ExtRepositoryObject>
	implements WebCacheItem {

	private static final long serialVersionUID = 1644824337736312571L;

	public GoogleDriveDirWebCacheItem(
		List<File> files, List<T> extRepositoryObjects, long refreshTime,
		boolean initialize) {

		_refreshTime = GoogleDriveConstants._REFRESH_TIME;

		if (refreshTime < 0) {
			_refreshTime = refreshTime;
		}

		_extRepositoryObjects = new ArrayList<>();
		_extRepositoryObjects.addAll(extRepositoryObjects);
		_files = new ArrayList<>();
		_files.addAll(files);
		_initialize = initialize;
	}

	@Override
	public Object convert(String key)
		throws WebCacheException {

		if (isInitialize()) {
			@SuppressWarnings({
				"rawtypes", "unchecked"
			})
			GoogleDriveCachedObject googleDriveCachedObject =
				new GoogleDriveCachedObjectImpl(_files, _extRepositoryObjects);

			return googleDriveCachedObject;
		}

		return null;
	}

	@Override
	public long getRefreshTime() {

		return _refreshTime;
	}

	public boolean isInitialize() {

		return _initialize;
	}

	public void setInitialize(boolean initialize) {

		this._initialize = initialize;
	}

	private boolean _initialize = true;
	private long _refreshTime;
	private List<T> _extRepositoryObjects;
	private List<File> _files;
}
