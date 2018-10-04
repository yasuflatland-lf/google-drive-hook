
package jp.liferay.google.drive.sync.background;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;

@SuppressWarnings("serial")
public class GoogleDriveCrawler extends RecursiveAction {

	public GoogleDriveCrawler(Drive drive, String folderKey) {
		_drive = drive;
		_folderKey = folderKey;
	}

	@Override
	protected void compute() {

		try {

			Drive.Files driveFiles = _drive.files();

			Drive.Files.List driveFilesList = driveFiles.list();

			StringBundler sb = new StringBundler();

			sb.append("'");
			sb.append(_folderKey);
			sb.append("' in parents and ");
			sb.append("trashed = false");

			driveFilesList.setQ(sb.toString());

			FileList fileList = driveFilesList.execute();

			List<File> files = fileList.getItems();

			List<GoogleDriveCrawler> tasks = new ArrayList<>();
			
			for (File file : files) {

				_log.info("---------------------------------------");
				_log.info("name : " + file.getTitle());
				_log.info(
					"type : " + (GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
						file.getMimeType()) ? "Folder" : "File"));

				if ((GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
					file.getMimeType()))) {
					GoogleDriveCrawler newTask = new GoogleDriveCrawler(_drive, file.getId());
					newTask.fork();					
				}
			}
			if (tasks.size() > 0) {
				for (GoogleDriveCrawler task : tasks) {
					task.join();
				}
			}			
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}
	
	private Drive _drive;
	private String _folderKey;	
	
	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveCrawler.class);

}
