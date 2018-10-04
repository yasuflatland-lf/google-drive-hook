
package jp.liferay.google.drive.sync.cache;

import com.google.api.services.drive.Drive;
import com.liferay.portal.kernel.webcache.WebCacheItem;
import com.liferay.portal.kernel.webcache.WebCachePoolUtil;

import org.osgi.service.component.annotations.Component;

import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;

/**
 * @author Yasuyuki Takeo
 */
@Component(immediate = true, service = GoogleDriveCacheService.class)
public class GoogleDriveCacheService {

	public GoogleDriveCachedObject getGoogleDriveCachedObject(
		String extRepositoryObjectKey, Drive drive, long refreshTime) {

		WebCacheItem wci =
			new GoogleDriveObjectWebCacheItem(drive, refreshTime);

		try {
			return (GoogleDriveCachedObject) WebCachePoolUtil.get(
				extRepositoryObjectKey, wci);
		}
		catch (ClassCastException cce) {
			WebCachePoolUtil.remove(extRepositoryObjectKey);

			return (GoogleDriveCachedObject) WebCachePoolUtil.get(
				extRepositoryObjectKey, wci);
		}
		
	}
}
