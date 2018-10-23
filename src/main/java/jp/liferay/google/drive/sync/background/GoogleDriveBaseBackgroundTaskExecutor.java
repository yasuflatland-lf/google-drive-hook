
package jp.liferay.google.drive.sync.background;

import com.google.api.services.drive.Drive;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskConstants;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskResult;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusMessageTranslator;
import com.liferay.portal.kernel.backgroundtask.BaseBackgroundTaskExecutor;
import com.liferay.portal.kernel.backgroundtask.display.BackgroundTaskDisplay;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LoggingTimer;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.osgi.service.component.annotations.Component;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;
import jp.liferay.google.drive.sync.connection.GoogleDriveConnectionManager;
import jp.liferay.google.drive.sync.connection.GoogleDriveContext;

/**
 * @author Yasuyuki Takeo
 */
@Component(
	immediate = true, 
	property = "background.task.executor.class.name=jp.liferay.google.drive.sync.background.GoogleDriveBaseBackgroundTaskExecutor",
	service = BackgroundTaskExecutor.class
)
public class GoogleDriveBaseBackgroundTaskExecutor
	extends BaseBackgroundTaskExecutor {

	/**
	 * Get Background Task Name
	 * 
	 * @param repositoryId
	 * @return Background Task name
	 */
	public static String getBackgroundTaskName(String repositoryId) {

		return _BACKGROUND_TASK_NAME_PREFIX.concat(repositoryId);
	}

	@Override
	public BackgroundTaskResult execute(BackgroundTask backgroundTask)
		throws Exception {

		Map<String, Serializable> taskContextMap =
			backgroundTask.getTaskContextMap();

		int parallelism =
			(int) taskContextMap.get(GoogleDriveConstants.THREAD_POOL_SIZE);

		String contextJson = (String) taskContextMap.get(
			GoogleDriveConstants.GOOGLE_DRIVE_CONTEXT);

_log.error(contextJson);
		GoogleDriveContext context = (GoogleDriveContext)JSONFactoryUtil.deserialize(contextJson);

		String rootFolderKey =
			(String) taskContextMap.get(GoogleDriveConstants.ROOT_FOLDER_KEY);

		_log.info("parallelism : " + String.valueOf(parallelism));

		GoogleDriveConnectionManager connectionManager =
			new GoogleDriveConnectionManager(context);

		Drive drive = connectionManager.getDrive();

		ForkJoinPool pool = new ForkJoinPool(parallelism);

		try (LoggingTimer loggingTimer = new LoggingTimer(
			String.valueOf(backgroundTask.getBackgroundTaskId()))) {

			GoogleDriveCrawler googleDriveCrawler =
				new GoogleDriveCrawler(drive, rootFolderKey, false);
			pool.invoke(googleDriveCrawler);

		}
		finally {
			pool.shutdown();
		}

		return BackgroundTaskResult.SUCCESS;
	}

	@Override
	public BackgroundTaskDisplay getBackgroundTaskDisplay(
		BackgroundTask backgroundTask) {

		return new GoogleDriveBaseBackgroundTaskDisplay(backgroundTask);
	}

	@Override
	public BackgroundTaskStatusMessageTranslator getBackgroundTaskStatusMessageTranslator() {

		return new GoogleDriveBackgroundTaskStatusMessageTranslator();
	}

	@Override
	public int getIsolationLevel() {

		return BackgroundTaskConstants.ISOLATION_LEVEL_TASK_NAME;
	}

	@Override
	public BackgroundTaskExecutor clone() {

		return this;
	}

	private static final String _BACKGROUND_TASK_NAME_PREFIX =
		GoogleDriveBaseBackgroundTaskExecutor.class.getName() + "-";

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveBaseBackgroundTaskExecutor.class);

}
