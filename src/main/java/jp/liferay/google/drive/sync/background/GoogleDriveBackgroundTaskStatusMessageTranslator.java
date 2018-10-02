
package jp.liferay.google.drive.sync.background;

import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatus;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskStatusMessageTranslator;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;

public class GoogleDriveBackgroundTaskStatusMessageTranslator
	implements BackgroundTaskStatusMessageTranslator {

	@Override
	public void translate(
		BackgroundTaskStatus backgroundTaskStatus, Message message) {

		_log.info("IN!!!!");
		return;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		GoogleDriveBackgroundTaskStatusMessageTranslator.class);
}
