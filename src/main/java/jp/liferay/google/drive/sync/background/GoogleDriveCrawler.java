
package jp.liferay.google.drive.sync.background;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.Time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;

@SuppressWarnings("serial")
public class GoogleDriveCrawler extends RecursiveAction {

	public GoogleDriveCrawler(
		Drive drive, String folderKey, boolean sleepFlag) {

		_drive = drive;
		_folderKey = folderKey;
		_sleepFlag = sleepFlag;
	}

	@Override
	protected void compute() {

		try {

			// If this background task hits the Google API's quota, wait until
			// the quota is released.
			if (_sleepFlag) {
				_log.info("Sleep for " + String.valueOf(getDelay()));
				_sleepFlag = false;
				Thread.sleep(getDelay());
			}

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

				if (_log.isDebugEnabled()) {
					_log.debug("---------------------------------------");
					_log.debug("name : " + file.getTitle());
					_log.debug(
						"type : " +
							(GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
								file.getMimeType()) ? "Folder" : "File"));
				}

				if ((GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
					file.getMimeType()))) {
					GoogleDriveCrawler newTask =
						new GoogleDriveCrawler(_drive, file.getId(), false);
					newTask.fork();
				}
			}
			if (tasks.size() > 0) {
				for (GoogleDriveCrawler task : tasks) {
					task.join();
				}
			}
		}
		catch (GoogleJsonResponseException e) {
			// In the case where parallel processing hits the Google API quota,
			// put some sleep and retry to process the same folder.
			_log.info("may hit the quota of Google API. Restart crawling");

			GoogleDriveCrawler newTask =
				new GoogleDriveCrawler(_drive, _folderKey, true);
			newTask.fork();

		}
		catch (IOException | InterruptedException e) {
			_log.error(e, e);

			throw new SystemException(e);
		}
	}

	protected long getDelay() {
		return _DELAY * Time.SECOND;
	}
	
	private Drive _drive;
	private String _folderKey;
	private boolean _sleepFlag;

	// TODO : This need to be configurable.
	private static int _DELAY = 100;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveCrawler.class);

}
