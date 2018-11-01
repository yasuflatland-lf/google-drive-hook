
package jp.liferay.google.drive.repository.constants;

import com.liferay.portal.kernel.util.Time;

/**
 * @author Yasuyuki Takeo
 */
public class GoogleDriveConstants {

	public static final String FOLDER_MIME_TYPE =
		"application/vnd.google-apps.folder";
	
	public static final String THREAD_POOL_SIZE = "threadPoolSize";	
	
	/**
	 * Google Drive Context
	 */
	public static final String GOOGLE_DRIVE_CONTEXT = "googleDriveContext";	
	
	/**
	 * Googld Drive Repository ID
	 */
	public static final String GOOGLE_DRIVE_REPOSITORY_ID = "googleDriveRepositoryId";	

	/**
	 * The root folder key of the target Google Drive
	 */
	public static final String ROOT_FOLDER_KEY = "rootFolderKey";	
	
	public static final long BASE_UNIT_FOR_CACHE = Time.MINUTE;

	public static final long CACHE_DURAITON = 30L;
	
	public static final long _REFRESH_TIME = BASE_UNIT_FOR_CACHE * CACHE_DURAITON;
}
