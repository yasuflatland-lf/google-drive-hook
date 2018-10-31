
package jp.liferay.google.drive.sync.connection;

import com.google.api.services.drive.Drive;
import com.liferay.portal.kernel.exception.PortalException;

public interface GoogleDriveConnection {

	/**
	 * Get Drive object Drive is a handler for Google Drive
	 * 
	 * @param context
	 * @return Drive Object
	 * @throws PortalException
	 */
	Drive getDrive()
		throws PortalException;

	/**
	 * Get Google Drive Session
	 * 
	 * @return
	 * @throws PortalException
	 */
	GoogleDriveSession getGoogleDriveSession()
		throws PortalException;

	/**
	 * Get Context for connection
	 * 
	 * @return GoogleDriveContext
	 */
	GoogleDriveContext getContext();

}
