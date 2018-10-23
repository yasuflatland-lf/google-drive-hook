
package jp.liferay.google.drive.sync.api;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;

public interface GoogleDriveCachedObject {

	File getFile();

	void setFile(File file);

	Revision getRevision();

	void setRevision(Revision revision);

	String toString();

}
