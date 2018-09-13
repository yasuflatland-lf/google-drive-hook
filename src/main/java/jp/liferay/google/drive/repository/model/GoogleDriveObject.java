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

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.liferay.document.library.repository.external.ExtRepositoryObject;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author Sergio González
 */
public class GoogleDriveObject
	extends GoogleDriveModel implements ExtRepositoryObject {

	public GoogleDriveObject(File file) {
		super(file);

		_description = GetterUtil.getString(file.getDescription());
		_extension = GetterUtil.getString(file.getFileExtension());

		DateTime createDateTime = file.getCreatedDate();

		_modifiedDate = new Date(createDateTime.getValue());

		_permission = file.getUserPermission();
	}

	@Override
	public boolean containsPermission(
		ExtRepositoryPermission extRepositoryPermission) {

		if (_unsupportedExtRepositoryPermissions.contains(
				extRepositoryPermission)) {

			return false;
		}
		
		String role = _permission.getRole();

		if (extRepositoryPermission.equals(ExtRepositoryPermission.ACCESS) ||
			extRepositoryPermission.equals(ExtRepositoryPermission.VIEW)) {

			return true;
		}
		else if (extRepositoryPermission.equals(
					ExtRepositoryPermission.ADD_DOCUMENT) ||
				 extRepositoryPermission.equals(
					 ExtRepositoryPermission.ADD_FOLDER) ||
				 extRepositoryPermission.equals(
					 ExtRepositoryPermission.ADD_SUBFOLDER) ||
				 extRepositoryPermission.equals(
					 ExtRepositoryPermission.UPDATE)) {

			return isOwnerOrWriter(role);
		}
		else if (extRepositoryPermission.equals(
					ExtRepositoryPermission.DELETE)) {

			return isOwner(role);
		}

		return false;
	}

	@Override
	public String getDescription() {
		return _description;
	}

	@Override
	public String getExtension() {
		return _extension;
	}

	@Override
	public Date getModifiedDate() {
		return _modifiedDate;
	}

	protected boolean isOwner(String role) {
		if (role.equals("owner")) {
			return true;
		}

		return false;
	}

	protected boolean isOwnerOrWriter(String role) {
		if (role.equals("owner") || role.equals("writer")) {
			return true;
		}

		return false;
	}

	private static final Set<ExtRepositoryPermission>
	_unsupportedExtRepositoryPermissions = EnumSet.of(
		ExtRepositoryPermission.ADD_DISCUSSION,
		ExtRepositoryPermission.ADD_SHORTCUT,
		ExtRepositoryPermission.DELETE_DISCUSSION,
		ExtRepositoryPermission.PERMISSIONS,
		ExtRepositoryPermission.UPDATE_DISCUSSION);
	
	private String _description;
	private String _extension;
	private Date _modifiedDate;
	private Permission _permission;

}