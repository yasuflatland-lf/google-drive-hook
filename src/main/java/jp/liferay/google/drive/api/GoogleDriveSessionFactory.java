
package jp.liferay.google.drive.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;

import java.io.IOException;

/**
 * GoogleDriveSession Factory
 * 
 * @author Yasuyuki Takeo
 *
 */
public class GoogleDriveSessionFactory {

	/**
	 * Create Google Drive Session
	 * 
	 * @param _googleClientId
	 * @param _googleClientSecret
	 * @param _googleAccessToken
	 * @param _googleRefreshToken
	 * @return GoogleDriveSession object
	 * @throws IOException
	 * @throws PortalException
	 */
	static public GoogleDriveSession create(
		String _googleClientId, String _googleClientSecret,
		String _googleAccessToken, String _googleRefreshToken)

		throws IOException, PortalException {

		long userId = PrincipalThreadLocal.getUserId();

		User user = UserLocalServiceUtil.getUser(userId);

		if (user.isDefaultUser()) {
			throw new PrincipalException("User is not authenticated");
		}

		GoogleCredential.Builder builder = new GoogleCredential.Builder();

		builder.setClientSecrets(_googleClientId, _googleClientSecret);

		JacksonFactory jsonFactory = new JacksonFactory();

		builder.setJsonFactory(jsonFactory);

		HttpTransport httpTransport = new NetHttpTransport();

		builder.setTransport(httpTransport);

		GoogleCredential googleCredential = builder.build();

		googleCredential.setAccessToken(_googleAccessToken);

		googleCredential.setRefreshToken(_googleRefreshToken);

		Drive.Builder driveBuilder =
			new Drive.Builder(httpTransport, jsonFactory, googleCredential);

		Drive drive = driveBuilder.build();

		Drive.About driveAbout = drive.about();

		Drive.About.Get driveAboutGet = driveAbout.get();

		About about = driveAboutGet.execute();

		return new GoogleDriveSession(drive, about.getRootFolderId());
	}
}
