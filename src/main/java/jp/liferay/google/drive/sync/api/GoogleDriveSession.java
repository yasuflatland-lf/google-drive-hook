/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jp.liferay.google.drive.sync.api;

import com.google.api.services.drive.Drive;

/**
 * GoogleDriveSession
 * 
 * @author Sergio González
 */
public class GoogleDriveSession {

	public GoogleDriveSession(Drive drive, String rootFolderKey) {
		_drive = drive;
		_rootFolderKey = rootFolderKey;
	}

	public Drive getDrive() {
		return _drive;
	}

	public String getRootFolderKey() {
		return _rootFolderKey;
	}

	public void setDrive(Drive drive) {
		_drive = drive;
	}

	public void setRootFolderKey(String rootFolderKey) {
		_rootFolderKey = rootFolderKey;
	}

	private Drive _drive;
	private String _rootFolderKey;

}