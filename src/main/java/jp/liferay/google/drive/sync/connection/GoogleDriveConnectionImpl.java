
package jp.liferay.google.drive.sync.connection;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.servlet.PortalSessionThreadLocal;
import com.liferay.portal.kernel.util.TransientValue;

import java.io.IOException;

import javax.servlet.http.HttpSession;

/**
 * Google Drive Connection
 * 
 * @author Yasuyuki Takeo
 */
public class GoogleDriveConnectionImpl implements GoogleDriveConnection {

	public GoogleDriveConnectionImpl(GoogleDriveContext context) {

		_context = context;
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.connection.GoogleDriveConnection#getDrive()
	 */
	@Override
	public Drive getDrive()
		throws PortalException {

		GoogleDriveSession googleDriveSession = getGoogleDriveSession(_context);

		return googleDriveSession.getDrive();
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.connection.GoogleDriveConnection#getGoogleDriveSession()
	 */
	@Override
	public GoogleDriveSession getGoogleDriveSession()
		throws PortalException {

		return getGoogleDriveSession(_context);
	}

	/**
	 * Get Google Drive Session
	 * 
	 * @return GoogleDriveSession
	 * @throws PortalException
	 */
	@SuppressWarnings("unchecked")
	protected GoogleDriveSession getGoogleDriveSession(
		GoogleDriveContext context)
		throws PortalException {

		GoogleDriveSession googleDriveSession = null;

		HttpSession httpSession = PortalSessionThreadLocal.getHttpSession();

		if (httpSession == null) {
			_log.info(
				"Http Session is null. Generate a new Google Drive Session");
			try {
				googleDriveSession = buildGoogleDriveSession(context);
				return googleDriveSession;
			}
			catch (Exception e) {
				throw new PrincipalException(e);
			}
		}

		Object obj =
			httpSession.getAttribute(GoogleDriveSession.class.getName());

		if (obj != null) {
			try {
				TransientValue<GoogleDriveSession> transientValue =
					(TransientValue<GoogleDriveSession>) obj;

				if (transientValue != null) {
					googleDriveSession = transientValue.getValue();

					if (_log.isDebugEnabled()) {
						_log.debug("Return googleDriveSession");
					}
					return googleDriveSession;
				}

			}
			catch (ClassCastException e) {
				_log.info(
					"Google Drive Session has not been not stored. Restore session.");
			}
		}

		_log.info("Restore Googld Drive Session");

		try {
			googleDriveSession = buildGoogleDriveSession(context);

			httpSession.setAttribute(
				GoogleDriveSession.class.getName(),
				new TransientValue<GoogleDriveSession>(googleDriveSession));
		}
		catch (Exception e) {
			throw new PrincipalException(e);
		}

		return googleDriveSession;
	}

	/**
	 * Build Google Drive Session
	 * 
	 * @param context
	 * @return GoogleDriveSession Object
	 * @throws IOException
	 * @throws PortalException
	 */
	protected GoogleDriveSession buildGoogleDriveSession(
		GoogleDriveContext context)
		throws IOException, PortalException {

		long userId = PrincipalThreadLocal.getUserId();

		User user = UserLocalServiceUtil.getUser(userId);

		if (user.isDefaultUser()) {
			throw new PrincipalException("User is not authenticated");
		}

		GoogleCredential.Builder builder = new GoogleCredential.Builder();

		builder.setClientSecrets(
			context.getGoogleClientId(), context.getGoogleClientSecret());

		JacksonFactory jsonFactory = new JacksonFactory();

		builder.setJsonFactory(jsonFactory);

		HttpTransport httpTransport = new NetHttpTransport();

		builder.setTransport(httpTransport);

		GoogleCredential googleCredential = builder.build();

		googleCredential.setAccessToken(context.getGoogleAccessToken());

		googleCredential.setRefreshToken(context.getGoogleRefreshToken());

		Drive.Builder driveBuilder =
			new Drive.Builder(httpTransport, jsonFactory, googleCredential);

		Drive drive = driveBuilder.build();

		Drive.About driveAbout = drive.about();

		Drive.About.Get driveAboutGet = driveAbout.get();

		About about = driveAboutGet.execute();

		return new GoogleDriveSession(drive, about.getRootFolderId());
	}

	/* (non-Javadoc)
	 * @see jp.liferay.google.drive.sync.connection.GoogleDriveConnection#getContext()
	 */
	@Override
	public GoogleDriveContext getContext() {

		return _context;

	}

	private GoogleDriveContext _context;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveConnectionImpl.class);
}
