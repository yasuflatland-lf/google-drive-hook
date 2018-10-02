
package jp.liferay.google.drive.sync.background;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;

import java.io.IOException;
import java.util.List;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;

public class GoogleDriveCrawler {

	public static void retriveFilesExt(Drive drive, String folderKey) {

		try {

			Drive.Files driveFiles = drive.files();

			Drive.Files.List driveFilesList = driveFiles.list();

			StringBundler sb = new StringBundler();

			sb.append("'");
			sb.append(folderKey);
			sb.append("' in parents and ");
			sb.append("trashed = false");

			driveFilesList.setQ(sb.toString());

			FileList fileList = driveFilesList.execute();

			List<File> files = fileList.getItems();

			for (File file : files) {

				_log.info("---------------------------------------");
				_log.info("name : " + file.getTitle());
				_log.info(
					"type : " + (GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
						file.getMimeType()) ? "Folder" : "File"));

				if ((GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
					file.getMimeType()))) {
					retriveFilesExt(drive, file.getId());
				}
			}

		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}

	}

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveCrawler.class);

}
