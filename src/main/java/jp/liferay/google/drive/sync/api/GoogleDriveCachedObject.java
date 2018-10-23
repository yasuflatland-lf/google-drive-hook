
package jp.liferay.google.drive.sync.api;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.liferay.document.library.repository.external.ExtRepositoryObject;

import java.util.List;

public interface GoogleDriveCachedObject<T extends ExtRepositoryObject> {

	File getFile();

	void setFile(File file);

	Revision getRevision();

	void setRevision(Revision revision);

	String toString();

	List<T> getExtRepositoryObjects();

	void setExtRepositoryObjects(List<T> extRepositoryObjects);

	List<File> getFiles();

	void setFiles(List<File> files);

}
