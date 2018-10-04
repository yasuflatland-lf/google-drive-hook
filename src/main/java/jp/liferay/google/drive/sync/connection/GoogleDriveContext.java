
package jp.liferay.google.drive.sync.connection;

import java.io.Serializable;

/**
 * Google Drive Context
 * 
 * @author Yasuyuki Takeo
 *
 */
public class GoogleDriveContext implements Serializable{

	public GoogleDriveContext(
		String googleClientId, String googleClientSecret,
		String googleAccessToken, String googleRefreshToken) {

		_googleClientId = googleClientId;
		_googleClientSecret = googleClientSecret;
		_googleAccessToken = googleAccessToken;
		_googleRefreshToken = googleRefreshToken;
	}

	public String getGoogleClientId() {

		return _googleClientId;
	}

	public void setGoogleClientId(String _googleClientId) {

		this._googleClientId = _googleClientId;
	}

	public String getGoogleClientSecret() {

		return _googleClientSecret;
	}

	public void setGoogleClientSecret(String _googleClientSecret) {

		this._googleClientSecret = _googleClientSecret;
	}

	public String getGoogleAccessToken() {

		return _googleAccessToken;
	}

	public void setGoogleAccessToken(String _googleAccessToken) {

		this._googleAccessToken = _googleAccessToken;
	}

	public String getGoogleRefreshToken() {

		return _googleRefreshToken;
	}

	public void setGoogleRefreshToken(String _googleRefreshToken) {

		this._googleRefreshToken = _googleRefreshToken;
	}

	private String _googleClientId;

	private String _googleClientSecret;

	private String _googleAccessToken;

	private String _googleRefreshToken;
	
	private static final long serialVersionUID = 5405672106153268841L;
}
