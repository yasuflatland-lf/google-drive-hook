
package jp.liferay.google.drive.sync.connection;

/**
 * @author Yasuyuki Takeo
 */
public class GoogleDriveConnectionFactory {

	public static GoogleDriveConnection getInstance(
		GoogleDriveContext context) {

		return new GoogleDriveConnectionImpl(context);
	}
}
