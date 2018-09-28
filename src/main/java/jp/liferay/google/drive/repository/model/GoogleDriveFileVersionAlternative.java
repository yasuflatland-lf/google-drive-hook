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

package jp.liferay.google.drive.repository.model;

import com.google.api.services.drive.model.File;
import com.liferay.document.library.repository.external.ExtRepositoryFileVersion;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;

/**
 * Google Drive File Version Alternative
 * 
 * There are file type where doesn't support versions.
 * For those files, this class provide psudo version to display.
 * 
 * @author Yasuyuki Takeo
 */
public class GoogleDriveFileVersionAlternative
	extends GoogleDriveModel implements ExtRepositoryFileVersion {

	public GoogleDriveFileVersionAlternative(
		File file, String extRepositoryFileEntryKey, int version) {

		super(
			file.getModifiedDate(), file.getId(),
			GetterUtil.getLong(file.getFileSize()),
			GetterUtil.getString(file.getLastModifyingUserName()));

		_file = file;
		_extRepositoryFileEntryKey = extRepositoryFileEntryKey;
		_version = version + ".0";
	}

	@Override
	public String getChangeLog() {
		return StringPool.BLANK;
	}

	public String getDownloadURL() {
		return GetterUtil.getString(_file.getDownloadUrl());
	}

	@Override
	public String getExtRepositoryModelKey() {
		StringBundler sb = new StringBundler(5);

		sb.append(_extRepositoryFileEntryKey);
		sb.append(StringPool.COLON);
		sb.append(DigesterUtil.digestHex(Digester.MD5, _file.getId()));
		sb.append(StringPool.COLON);
		sb.append(_version);

		return sb.toString();
	}

	@Override
	public String getMimeType() {
		return GetterUtil.getString(_file.getMimeType());
	}

	@Override
	public String getVersion() {
		return _version;
	}

	private String _extRepositoryFileEntryKey;
	private File _file;
	private String _version;

}