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

package jp.liferay.google.drive.repository;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.repository.external.CredentialsProvider;
import com.liferay.document.library.repository.external.ExtRepository;
import com.liferay.document.library.repository.external.ExtRepositoryAdapter;
import com.liferay.document.library.repository.external.ExtRepositoryAdapterCache;
import com.liferay.document.library.repository.external.ExtRepositoryFileEntry;
import com.liferay.document.library.repository.external.ExtRepositoryFileVersion;
import com.liferay.document.library.repository.external.ExtRepositoryFileVersionDescriptor;
import com.liferay.document.library.repository.external.ExtRepositoryFolder;
import com.liferay.document.library.repository.external.ExtRepositoryObject;
import com.liferay.document.library.repository.external.ExtRepositoryObjectType;
import com.liferay.document.library.repository.external.ExtRepositorySearchResult;
import com.liferay.document.library.repository.external.model.ExtRepositoryFileEntryAdapter;
import com.liferay.document.library.repository.external.model.ExtRepositoryFolderAdapter;
import com.liferay.document.library.repository.external.model.ExtRepositoryObjectAdapter;
import com.liferay.document.library.repository.external.model.ExtRepositoryObjectAdapterType;
import com.liferay.document.library.repository.external.search.ExtRepositoryQueryMapper;
import com.liferay.portal.background.task.constants.BackgroundTaskContextMapConstants;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManagerUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.RepositoryEntry;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.HitsImpl;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jp.liferay.google.drive.repository.constants.GoogleDriveConstants;
import jp.liferay.google.drive.repository.model.GoogleDriveFileEntry;
import jp.liferay.google.drive.repository.model.GoogleDriveFileVersion;
import jp.liferay.google.drive.repository.model.GoogleDriveFileVersionAlternative;
import jp.liferay.google.drive.repository.model.GoogleDriveFolder;
import jp.liferay.google.drive.repository.model.GoogleDriveModel;
import jp.liferay.google.drive.repository.model.GoogleDriveObject;
import jp.liferay.google.drive.sync.api.GoogleDriveCachedObject;
import jp.liferay.google.drive.sync.background.GoogleDriveBaseBackgroundTaskExecutor;
import jp.liferay.google.drive.sync.cache.GoogleDriveCache;
import jp.liferay.google.drive.sync.cache.GoogleDriveCacheFactory;
import jp.liferay.google.drive.sync.connection.GoogleDriveConnectionManager;
import jp.liferay.google.drive.sync.connection.GoogleDriveContext;
import jp.liferay.google.drive.sync.connection.GoogleDriveSession;

/**
 * Googold Drive Repository
 * 
 * @author Sergio González
 * @author Yasuyuki Takeo
 */
public class GoogleDriveRepository extends ExtRepositoryAdapter
	implements ExtRepository {

	public GoogleDriveRepository() {

		super(null);
	}

	@Override
	public String[] getSupportedConfigurations() {

		return _SUPPORTED_CONFIGURATIONS;
	}

	@Override
	public String[][] getSupportedParameters() {

		return _SUPPORTED_PARAMETERS;
	}

	@Override
	public ExtRepositoryFileEntry addExtRepositoryFileEntry(
		String extRepositoryParentFolderKey, String mimeType, String title,
		String description, String changeLog, InputStream inputStream)
		throws PortalException {

		File file = addFile(
			extRepositoryParentFolderKey, mimeType, title, description,
			inputStream);

		return new GoogleDriveFileEntry(file);
	}

	@Override
	public ExtRepositoryFolder addExtRepositoryFolder(
		String extRepositoryParentFolderKey, String name, String description)
		throws PortalException {

		File file = addFile(
			extRepositoryParentFolderKey, GoogleDriveConstants.FOLDER_MIME_TYPE,
			name, description, null);

		return new GoogleDriveFolder(file, getRootFolderKey());
	}

	@Override
	public ExtRepositoryFileVersion cancelCheckOut(
		String extRepositoryFileEntryKey) {

		_log.info("Cancel checkout is not supported for Google Drive");

		return null;
	}

	@Override
	public void checkInExtRepositoryFileEntry(
		String extRepositoryFileEntryKey, boolean createMajorVersion,
		String changeLog) {

		_log.info("Check in is not supported for Google Drive");
	}

	@Override
	public ExtRepositoryFileEntry checkOutExtRepositoryFileEntry(
		String extRepositoryFileEntryKey) {

		try {
			Drive drive = _connectionManager.getDrive();

			File file = getFile(drive, extRepositoryFileEntryKey);

			return new GoogleDriveFileEntry(file);
		}
		catch (IOException | PortalException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> T copyExtRepositoryObject(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryFileEntryKey, String newExtRepositoryFolderKey,
		String newTitle)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			File newFile = new File();

			ParentReference parentReference = new ParentReference();

			parentReference.setId(newExtRepositoryFolderKey);

			newFile.setParents(Arrays.asList(parentReference));

			Drive.Files.Copy driveFilesCopy =
				driveFiles.copy(extRepositoryFileEntryKey, newFile);

			driveFilesCopy.execute();

			T extRepositoryObject = null;

			if (extRepositoryObjectType.equals(
				ExtRepositoryObjectType.FOLDER)) {

				extRepositoryObject =
					(T) new GoogleDriveFolder(newFile, getRootFolderKey());
			}
			else {
				extRepositoryObject = (T) new GoogleDriveFileEntry(newFile);
			}

			return extRepositoryObject;
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public void deleteExtRepositoryObject(
		ExtRepositoryObjectType<? extends ExtRepositoryObject> extRepositoryObjectType,
		String extRepositoryObjectKey)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			Drive.Files.Delete driveFilesDelete =
				driveFiles.delete(extRepositoryObjectKey);

			driveFilesDelete.execute();

			GoogleDriveCache gdc = GoogleDriveCacheFactory.create();
			gdc.remove(extRepositoryObjectKey);
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public String getAuthType() {

		// return CompanyConstants.AUTH_TYPE_SN;
		return null;
	}

	@Override
	public InputStream getContentStream(
		ExtRepositoryFileEntry extRepositoryFileEntry)
		throws PortalException {

		GoogleDriveFileEntry googleDriveFileEntry =
			(GoogleDriveFileEntry) extRepositoryFileEntry;

		return getContentStream(googleDriveFileEntry.getDownloadURL());
	}

	@Override
	public InputStream getContentStream(
		ExtRepositoryFileVersion extRepositoryFileVersion)
		throws PortalException {

		if (extRepositoryFileVersion instanceof GoogleDriveFileVersion) {
			GoogleDriveFileVersion googleDriveFileVersion =
				(GoogleDriveFileVersion) extRepositoryFileVersion;

			return getContentStream(googleDriveFileVersion.getDownloadURL());

		}
		else {
			GoogleDriveFileVersionAlternative googleDriveFileVersionAlternative =
				(GoogleDriveFileVersionAlternative) extRepositoryFileVersion;

			return getContentStream(
				googleDriveFileVersionAlternative.getDownloadURL());
		}
	}

	protected InputStream getContentStream(String downloadURL)
		throws PortalException {

		if (Validator.isNull(downloadURL)) {
			return null;
		}

		Drive drive = _connectionManager.getDrive();

		HttpRequestFactory httpRequestFactory = drive.getRequestFactory();

		GenericUrl genericUrl = new GenericUrl(downloadURL);

		try {
			HttpRequest httpRequest =
				httpRequestFactory.buildGetRequest(genericUrl);

			HttpResponse httpResponse = httpRequest.execute();

			return httpResponse.getContent();
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public ExtRepositoryFileVersion getExtRepositoryFileVersion(
		ExtRepositoryFileEntry extRepositoryFileEntry, String version)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			Drive.Revisions driveRevisions = drive.revisions();

			Drive.Revisions.List driveRevisionsList = driveRevisions.list(
				extRepositoryFileEntry.getExtRepositoryModelKey());

			RevisionList revisionList = driveRevisionsList.execute();

			List<Revision> revisions = revisionList.getItems();

			int[] versionParts =
				StringUtil.split(version, StringPool.PERIOD, 0);

			Revision revision = revisions.get(versionParts[0]);

			return new GoogleDriveFileVersion(
				revision, extRepositoryFileEntry.getExtRepositoryModelKey(),
				versionParts[0]);
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public ExtRepositoryFileVersionDescriptor getExtRepositoryFileVersionDescriptor(
		String extRepositoryFileVersionKey) {

		String[] extRepositoryFileVersionKeyParts =
			StringUtil.split(extRepositoryFileVersionKey, StringPool.COLON);

		String extRepositoryFileEntryKey = extRepositoryFileVersionKeyParts[0];
		String version = extRepositoryFileVersionKeyParts[2];

		return new ExtRepositoryFileVersionDescriptor(
			extRepositoryFileEntryKey, version);
	}

	@Override
	public List<ExtRepositoryFileVersion> getExtRepositoryFileVersions(
		ExtRepositoryFileEntry extRepositoryFileEntry)
		throws PortalException {

		Drive drive = null;
		List<ExtRepositoryFileVersion> extRepositoryFileVersions =
			new ArrayList<>();

		try {
			drive = _connectionManager.getDrive();

			Drive.Revisions driveRevisions = drive.revisions();

			Drive.Revisions.List driveRevisionsList = driveRevisions.list(
				extRepositoryFileEntry.getExtRepositoryModelKey());

			RevisionList revisionList = driveRevisionsList.execute();

			List<Revision> revisions = revisionList.getItems();
			Collections.reverse(revisions);
			Revision latest = revisions.stream().findFirst().get();

			extRepositoryFileVersions.add(
				new GoogleDriveFileVersion(
					latest, extRepositoryFileEntry.getExtRepositoryModelKey(),
					1));

			// Google Drive stores bunch of versions, so registering all these
			// aren't realistic for production use due to taking too long
			// If a user does need to retrive all versions, it should be done by
			// a background process.
			//
			// List<ExtRepositoryFileVersion> extRepositoryFileVersions =
			// new ArrayList<>(revisions.size());
			//
			// for (int i = 0; i < revisions.size(); i++) {
			// Revision revision = revisions.get(i);
			//
			// extRepositoryFileVersions.add(
			// new GoogleDriveFileVersion(
			// revision,
			// extRepositoryFileEntry.getExtRepositoryModelKey(),
			// i + 1));
			// }
			//
			// Collections.reverse(extRepositoryFileVersions);

			return extRepositoryFileVersions;
		}
		catch (GoogleJsonResponseException e) {

			File file;
			try {
				file = getFile(
					drive, extRepositoryFileEntry.getExtRepositoryModelKey());
			}
			catch (IOException e1) {
				_log.error(e1, e1);
				throw new SystemException(e1);
			}

			if (_log.isDebugEnabled()) {
				GoogleJsonError ge = e.getDetails();
				_log.debug(
					"file name : " + file.getTitle() + " path : <" +
						file.getDefaultOpenWithLink() + "> code : " +
						ge.getCode() + " Message : " + ge.getMessage());

			}

			extRepositoryFileVersions.add(
				new GoogleDriveFileVersionAlternative(
					file, extRepositoryFileEntry.getExtRepositoryModelKey(),
					1));

			return extRepositoryFileVersions;
		}
		catch (IOException e) {
			_log.error(e, e);

			throw new SystemException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> T getExtRepositoryObject(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryObjectKey)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			File file = getFile(drive, extRepositoryObjectKey);

			T extRepositoryObject = null;

			if (extRepositoryObjectType.equals(
				ExtRepositoryObjectType.FOLDER)) {

				extRepositoryObject =
					(T) new GoogleDriveFolder(file, getRootFolderKey());
			}
			else {
				extRepositoryObject = (T) new GoogleDriveFileEntry(file);
			}

			return extRepositoryObject;
		}
		catch (IOException ioe) {
			if (extRepositoryObjectType == ExtRepositoryObjectType.FOLDER) {
				throw new NoSuchFolderException(ioe);
			}

			throw new NoSuchFileEntryException(ioe);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> T getExtRepositoryObject(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryFolderKey, String title)
		throws PortalException {

		try {
			StringBundler sb = new StringBundler();

			sb.append("'");
			sb.append(extRepositoryFolderKey);
			sb.append("' in parents and title contains '");
			sb.append(title);
			sb.append(" and mimeType ");

			if (extRepositoryObjectType.equals(
				ExtRepositoryObjectType.FOLDER)) {

				sb.append("= ");
			}
			else {
				sb.append("!= ");
			}

			sb.append(GoogleDriveConstants.FOLDER_MIME_TYPE);

			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			Drive.Files.List driveFilesList = driveFiles.list();

			driveFilesList.setQ(sb.toString());

			FileList fileList = driveFilesList.execute();

			List<File> files = fileList.getItems();

			if (files.isEmpty()) {
				if (extRepositoryObjectType == ExtRepositoryObjectType.FOLDER) {
					throw new NoSuchFolderException(title);
				}

				throw new NoSuchFileEntryException(title);
			}

			if (extRepositoryObjectType.equals(
				ExtRepositoryObjectType.FOLDER)) {

				return (T) new GoogleDriveFolder(
					files.get(0), getRootFolderKey());
			}

			return (T) new GoogleDriveFileEntry(files.get(0));
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> List<T> getExtRepositoryObjects(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryFolderKey)
		throws PortalException {

		try {
			List<File> files = new ArrayList<>();

			List<T> extRepositoryObjects = new ArrayList<>();

			GoogleDriveCache googleDriveCache =
				GoogleDriveCacheFactory.create();

			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			Drive.Files.List driveFilesList = driveFiles.list();

			StringBundler sb = new StringBundler();

			if (extRepositoryFolderKey != null) {
				sb.append("'");
				sb.append(extRepositoryFolderKey);
				sb.append("' in parents and ");
			}

			if (!extRepositoryObjectType.equals(
				ExtRepositoryObjectType.OBJECT)) {

				sb.append("mimeType");

				if (extRepositoryObjectType.equals(
					ExtRepositoryObjectType.FILE)) {

					sb.append(" != '");
				}
				else {
					sb.append(" = '");
				}

				sb.append(GoogleDriveConstants.FOLDER_MIME_TYPE);
				sb.append("' and ");
			}

			sb.append("trashed = false");

			driveFilesList.setQ(sb.toString());

			FileList fileList = driveFilesList.execute();

			files = fileList.getItems();

			for (File file : files) {

				if (GoogleDriveConstants.FOLDER_MIME_TYPE.equals(
					file.getMimeType())) {
					extRepositoryObjects.add(
						(T) new GoogleDriveFolder(file, getRootFolderKey()));
				}
				else {
					extRepositoryObjects.add(
						(T) new GoogleDriveFileEntry(file));
				}

				googleDriveCache.getGoogleDriveCachedObject(
					file.getId(), file, drive);
			}

			return extRepositoryObjects;
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public int getExtRepositoryObjectsCount(
		ExtRepositoryObjectType<? extends ExtRepositoryObject> extRepositoryObjectType,
		String extRepositoryFolderKey)
		throws PortalException {

		List<? extends ExtRepositoryObject> extRepositoryObjects =
			getExtRepositoryObjects(
				extRepositoryObjectType, extRepositoryFolderKey);

		return extRepositoryObjects.size();
	}

	@Override
	public ExtRepositoryFolder getExtRepositoryParentFolder(
		ExtRepositoryObject extRepositoryObject)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			File file =
				getFile(drive, extRepositoryObject.getExtRepositoryModelKey());

			List<ParentReference> parentReferences = file.getParents();

			if (!parentReferences.isEmpty()) {
				ParentReference parentReference = parentReferences.get(0);

				File parentFile = getFile(drive, parentReference.getId());

				return new GoogleDriveFolder(parentFile, getRootFolderKey());
			}
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);
		}

		return null;
	}

	@Override
	public String getLiferayLogin(String extRepositoryLogin) {

		return null;
	}

	@Override
	public String getRootFolderKey() {

		try {
			GoogleDriveSession googleDriveSession =
				_connectionManager.getGoogleDriveSession();

			return googleDriveSession.getRootFolderKey();
		}
		catch (PortalException pe) {
			_log.error(pe, pe);
		}

		return StringPool.BLANK;
	}

	protected List<String> getSubfolderKeys(
		String extRepositoryFolderKey, boolean recurse,
		List<String> subfolderKeys)
		throws PortalException {

		List<ExtRepositoryFolder> extRepositoryFolders =
			getExtRepositoryObjects(
				ExtRepositoryObjectType.FOLDER, extRepositoryFolderKey);

		for (ExtRepositoryFolder extRepositoryFolder : extRepositoryFolders) {
			subfolderKeys.add(extRepositoryFolder.getExtRepositoryModelKey());

			if (recurse) {
				getSubfolderKeys(
					extRepositoryFolder.getExtRepositoryModelKey(), recurse,
					subfolderKeys);
			}
		}

		return subfolderKeys;
	}

	@Override
	public List<String> getSubfolderKeys(
		String extRepositoryFolderKey, boolean recurse)
		throws PortalException {

		List<String> subfolderKeys = new ArrayList<>();

		getSubfolderKeys(extRepositoryFolderKey, recurse, subfolderKeys);

		return subfolderKeys;
	}

	/**
	 * Initialize Repository
	 */
	@Override
	public void initRepository(
		UnicodeProperties typeSettingsProperties,
		CredentialsProvider credentialsProvider)
		throws PortalException {

		String googleClientId =
			typeSettingsProperties.getProperty(_GOOGLE_CLIENT_ID);

		String googleClientSecret =
			typeSettingsProperties.getProperty(_GOOGLE_CLIENT_SECRET);

		// At initialization, the values are always null
		if (Validator.isNull(googleClientId) ||
			Validator.isNull(googleClientSecret)) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Google Client ID or Google Client Secret are empty");
			}
			return;
		}

		String googleAccessToken =
			typeSettingsProperties.getProperty(_GOOGLE_ACCESS_TOKEN);

		String googleRefreshToken =
			typeSettingsProperties.getProperty(_GOOGLE_REFRESH_TOKEN);

		// At initialization, the values are always null
		if (Validator.isNull(googleAccessToken) ||
			Validator.isNull(googleRefreshToken)) {
			if (_log.isDebugEnabled()) {
				_log.debug("Access Token or Refresh Token are empty");
			}
			return;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Client ID     : " + googleClientId);
			_log.debug("Client Secret : " + googleClientSecret);
			_log.debug("Access Token  : " + googleAccessToken);
			_log.debug("Refresh Token : " + googleRefreshToken);
		}

		GoogleDriveContext context = new GoogleDriveContext(
			googleClientId, googleClientSecret, googleAccessToken,
			googleRefreshToken);

		_connectionManager =
			new GoogleDriveConnectionManager(context, getRepositoryId());

		_connectionManager.getDrive();
	}

	/**
	 * Retriving Drive Information Background Task
	 * 
	 * @throws PortalException
	 */
	public void retriveDriveInfoBackgroundTask()
		throws PortalException {

		Map<String, Serializable> taskContextMap = new HashMap<>();

		taskContextMap.put(
			BackgroundTaskContextMapConstants.DELETE_ON_SUCCESS, true);

		// TODO : Change this size configurable by a property
		taskContextMap.put(
			GoogleDriveConstants.THREAD_POOL_SIZE, _THREAD_POOL_SIZE);

		String serizlizedContext =
			JSONFactoryUtil.serialize(_connectionManager.getContext());

		taskContextMap.put(
			GoogleDriveConstants.GOOGLE_DRIVE_CONTEXT, serizlizedContext);

		taskContextMap.put(
			GoogleDriveConstants.GOOGLE_DRIVE_REPOSITORY_ID,
			String.valueOf(getRepositoryId()));

		taskContextMap.put(
			GoogleDriveConstants.ROOT_FOLDER_KEY,
			_connectionManager.getGoogleDriveSession().getRootFolderKey());

		final String jobName =
			GoogleDriveBaseBackgroundTaskExecutor.getBackgroundTaskName(
				PortalUUIDUtil.generate());

		long userId = PrincipalThreadLocal.getUserId();
		User user = UserLocalServiceUtil.getUser(userId);

		// To delete background task, call DeleteBackgroundTaskMVCActionCommand
		BackgroundTask backgroundTask = null;
		try {
			backgroundTask = BackgroundTaskManagerUtil.addBackgroundTask(
				user.getUserId(), user.getGroupId(), jobName,
				GoogleDriveBaseBackgroundTaskExecutor.class.getName(),
				taskContextMap, new ServiceContext());

			_log.info(
				"Google Drive Background task ID : " +
					String.valueOf(backgroundTask.getBackgroundTaskId()));
			_log.info("Job Name : " + jobName);
		}
		catch (Exception e) {
			if (null != backgroundTask) {
				_log.info("Task deleted");
				BackgroundTaskManagerUtil.deleteBackgroundTask(
					backgroundTask.getBackgroundTaskId());
			}
		}
	}

	/**
	 * Rename the object
	 * 
	 * @param extRepositoryObjectKey
	 * @param newTitle
	 * @throws PortalException
	 */
	protected void renameObject(String extRepositoryObjectKey, String newTitle)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			File file = getFile(drive, extRepositoryObjectKey);

			file.setOriginalFilename(newTitle);
			file.setTitle(newTitle);

			// Rename the file.
			Files.Patch patchRequest = drive.files().patch(file.getId(), file);
			patchRequest.setFields("title,originalFilename");

			patchRequest.execute();

		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	/**
	 * Move Object in the google drive This method is also called at updateing
	 * the object as well. This method is responsible for changing name and
	 * moving object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> T moveExtRepositoryObject(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryObjectKey, String newExtRepositoryFolderKey,
		String newTitle)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			File file = getFile(drive, extRepositoryObjectKey);

			// Change Name
			if (!file.getTitle().equals(newTitle)) {
				renameObject(extRepositoryObjectKey, newTitle);
			}

			Drive.Parents driveParents = drive.parents();

			List<ParentReference> parentReferences = file.getParents();

			for (ParentReference parentReference : parentReferences) {
				Drive.Parents.Delete driveParentsDelete =
					driveParents.delete(file.getId(), parentReference.getId());

				driveParentsDelete.execute();
			}

			ParentReference parentReference = new ParentReference();

			parentReference.setId(newExtRepositoryFolderKey);

			Drive.Parents.Insert driveParentsInsert =
				driveParents.insert(file.getId(), parentReference);

			driveParentsInsert.execute();

			if (extRepositoryObjectType.equals(ExtRepositoryObjectType.FILE)) {
				return (T) new GoogleDriveFileEntry(file);
			}

			return (T) new GoogleDriveFolder(file, getRootFolderKey());
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	/**
	 * Search
	 */
	@Override
	public Hits search(SearchContext searchContext, Query query)
		throws SearchException {

		long startTime = System.currentTimeMillis();

		SearchResult<File> searchResults = null;

		try {
			Drive drive = _connectionManager.getDrive();
			searchResults = searchDrive(drive, searchContext);
		}
		catch (PortalException | SystemException e) {
			throw new SearchException("Unable to perform search", e);
		}

		QueryConfig queryConfig = searchContext.getQueryConfig();

		List<Document> documents = new ArrayList<>();
		List<String> snippets = new ArrayList<>();
		List<Float> scores = new ArrayList<>();

		for (File file : searchResults.getLists()) {

			try {
				Document document = new DocumentImpl();

				RepositoryEntry repositoryEntry =
					getRepositoryEntry(file.getId());

				document.addKeyword(
					Field.ENTRY_CLASS_NAME, GoogleDriveModel.class.getName());
				document.addKeyword(
					Field.ENTRY_CLASS_PK,
					repositoryEntry.getRepositoryEntryId());
				document.addKeyword(
					GoogleDriveConstants.REPOSITORY_ID,
					repositoryEntry.getRepositoryId());
				document.addKeyword(Field.TITLE, file.getTitle());
				document.addKeyword(
					GoogleDriveConstants.THUMBNAIL_SRC,
					file.getThumbnailLink());
				document.addKeyword(Field.URL, file.getAlternateLink());
				document.addKeyword(
					GoogleDriveConstants.MIME_TYPE, file.getMimeType());
				document.addUID(
					GoogleDriveObject.class.getName(),
					repositoryEntry.getRepositoryEntryId());

				documents.add(document);
				scores.add(1.0F);
				snippets.add("");

			}
			catch (PortalException | SystemException e) {
				if (_log.isWarnEnabled()) {
					_log.warn("Invalid entry returned from search", e);
				}
			}
		}

		float searchTime =
			(float) (System.currentTimeMillis() - startTime) / Time.SECOND;

		Hits hits = new HitsImpl();

		hits.setDocs(documents.toArray(new Document[documents.size()]));
		hits.setLength(searchResults.getTotal());
		hits.setQueryTerms(new String[0]);
		hits.setScores(ArrayUtil.toFloatArray(scores));
		hits.setSearchTime(searchTime);
		hits.setSnippets(snippets.toArray(new String[snippets.size()]));
		hits.setStart(startTime);

		return hits;
	}

	/**
	 * Filter List
	 * 
	 * @param fileList
	 * @param delta
	 * @param maxSize
	 * @return
	 */
	protected List<File> filterList(
		List<File> fileList, int delta, int maxSize) {

		return fileList.stream().skip(delta).limit(maxSize).collect(
			Collectors.toList());
	}

	/**
	 * Search Google Drive
	 * 
	 * @param drive
	 * @param searchContext
	 * @return
	 * @throws PortalException
	 */
	protected SearchResult<File> searchDrive(
		Drive drive, SearchContext searchContext)
		throws PortalException {

		List<File> googleDriveFolderList = new ArrayList<>();

		String pageToken = null;

		StringBuilder searchQuery = new StringBuilder();
		searchQuery.append("fullText contains ");
		searchQuery.append(StringPool.APOSTROPHE);
		searchQuery.append(searchContext.getKeywords());
		searchQuery.append(StringPool.APOSTROPHE);
		searchQuery.append(" and ");
		searchQuery.append("trashed = false");

		try {
			do {
				FileList result;

				result = drive.files().list().setQ(
					searchQuery.toString()).setPageToken(pageToken).execute();

				googleDriveFolderList.addAll(result.getItems());

				pageToken = result.getNextPageToken();
			}
			while (pageToken != null);
		}
		catch (IOException e) {
			throw new PortalException("Search query error.", e);
		}

		if (_log.isDebugEnabled()) {
			for (File file : googleDriveFolderList) {
				_log.debug("Found file -> " + file.getTitle());
			}
		}

		/**
		 * Filter the search data with Search Context
		 */
		List<File> filteredList = filterList(
			googleDriveFolderList, searchContext.getStart(),
			searchContext.getEnd());

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Search -> Start : " + searchContext.getStart() + " End : " +
					searchContext.getEnd() + " Size " +
					googleDriveFolderList.size());

		}

		return new SearchResult<File>(
			filteredList, googleDriveFolderList.size());
	}

	@Override
	public List<ExtRepositorySearchResult<?>> search(
		SearchContext searchContext, Query query,
		ExtRepositoryQueryMapper extRepositoryQueryMapper)
		throws PortalException {

		throw new UnsupportedOperationException();
	}

	/**
	 * Detect extension from input stream
	 * 
	 * @param mimeType
	 * @return
	 */
	protected String getExtension(String mimeType) {

		Set<String> extensions = MimeTypesUtil.getExtensions(mimeType);
		if (extensions.size() == 0) {
			_log.error(
				"There are no matched mime type. Mime type was : " + mimeType);
			return "";
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Extensions : " + extensions);
		}

		return extensions.stream().findFirst().get();
	}

	@Override
	public ExtRepositoryFileEntry updateExtRepositoryFileEntry(
		String extRepositoryFileEntryKey, String mimeType,
		InputStream inputStream)
		throws PortalException {

		try {
			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			File file = getFile(drive, extRepositoryFileEntryKey);

			String fileExtension = getExtension(mimeType);

			// Check if mime type has been changed.
			if (!file.getMimeType().equals(mimeType)) {
				// Both mime type and extensions must be udpated
				file.setMimeType(mimeType);
				file.setFullFileExtension(fileExtension);
				file.setFileExtension(fileExtension);
			}

			InputStreamContent inputStreamContent =
				new InputStreamContent(mimeType, inputStream);

			Drive.Files.Update driveFilesUpdate = driveFiles.update(
				extRepositoryFileEntryKey, file, inputStreamContent);

			// Update Mime type and extention for Liferay side.
			file = driveFilesUpdate.execute();
			file.setFileExtension(fileExtension);
			file.setFullFileExtension(fileExtension);
			file.setMimeType(mimeType);
			file.setOriginalFilename(
				file.getTitle() + StringPool.COMMA + fileExtension);
			file.setTitle(file.getTitle() + StringPool.COMMA + fileExtension);

			return new GoogleDriveFileEntry(file);
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@Override
	public FileEntry updateFileEntry(
		long userId, long fileEntryId, String sourceFileName, String mimeType,
		String title, String description, String changeLog,
		boolean majorVersion, InputStream inputStream, long size,
		ServiceContext serviceContext)
		throws PortalException {

		boolean needsCheckIn = false;

		String extRepositoryFileEntryKey =
			getExtRepositoryObjectKey(fileEntryId);

		try {
			ExtRepositoryFileEntry extRepositoryFileEntry =
				getExtRepositoryObject(
					ExtRepositoryObjectType.FILE, extRepositoryFileEntryKey);

			if (!isCheckedOut(extRepositoryFileEntry)) {
				checkOutExtRepositoryFileEntry(extRepositoryFileEntryKey);

				needsCheckIn = true;
			}

			if (inputStream != null) {
				extRepositoryFileEntry = updateExtRepositoryFileEntry(
					extRepositoryFileEntryKey, mimeType, inputStream);
			}

			if (!title.equals(extRepositoryFileEntry.getTitle())) {
				ExtRepositoryFolder folder =
					getExtRepositoryParentFolder(extRepositoryFileEntry);

				extRepositoryFileEntry = moveExtRepositoryObject(
					ExtRepositoryObjectType.FILE, extRepositoryFileEntryKey,
					folder.getExtRepositoryModelKey(), title);

				ExtRepositoryAdapterCache extRepositoryAdapterCache =
					ExtRepositoryAdapterCache.getInstance();

				extRepositoryAdapterCache.get(
					extRepositoryFileEntry.getExtRepositoryModelKey());
				extRepositoryAdapterCache.clear();

				_log.info("saved title : " + extRepositoryFileEntry.getTitle());
				// repositoryEntryLocalService.updateRepositoryEntry(
				// fileEntryId,
				// extRepositoryFileEntry.getExtRepositoryModelKey());
			}

			if (needsCheckIn) {
				checkInExtRepositoryFileEntry(
					extRepositoryFileEntry.getExtRepositoryModelKey(),
					majorVersion, changeLog);

				needsCheckIn = false;
			}

			return _toExtRepositoryObjectAdapter(
				ExtRepositoryObjectAdapterType.FILE, extRepositoryFileEntry);
		}
		catch (PortalException | SystemException e) {
			if (needsCheckIn) {
				cancelCheckOut(extRepositoryFileEntryKey);
			}

			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	protected <T extends ExtRepositoryObjectAdapter<?>> T _toExtRepositoryObjectAdapter(
		ExtRepositoryObjectAdapterType<T> extRepositoryObjectAdapterType,
		ExtRepositoryObject extRepositoryObject)
		throws PortalException {

		ExtRepositoryAdapterCache extRepositoryAdapterCache =
			ExtRepositoryAdapterCache.getInstance();

		String extRepositoryModelKey =
			extRepositoryObject.getExtRepositoryModelKey();

		ExtRepositoryObjectAdapter<?> extRepositoryObjectAdapter =
			extRepositoryAdapterCache.get(extRepositoryModelKey);

		if (extRepositoryObjectAdapter == null) {
			RepositoryEntry repositoryEntry =
				getRepositoryEntry(extRepositoryModelKey);

			if (extRepositoryObject instanceof ExtRepositoryFolder) {
				ExtRepositoryFolder extRepositoryFolder =
					(ExtRepositoryFolder) extRepositoryObject;

				extRepositoryObjectAdapter = new ExtRepositoryFolderAdapter(
					this, repositoryEntry.getRepositoryEntryId(),
					repositoryEntry.getUuid(), extRepositoryFolder);
			}
			else {
				ExtRepositoryFileEntry extRepositoryFileEntry =
					(ExtRepositoryFileEntry) extRepositoryObject;

				extRepositoryObjectAdapter = new ExtRepositoryFileEntryAdapter(
					this, repositoryEntry.getRepositoryEntryId(),
					repositoryEntry.getUuid(), extRepositoryFileEntry);

			}

			extRepositoryAdapterCache.put(extRepositoryObjectAdapter);
		}

		if (extRepositoryObjectAdapterType == ExtRepositoryObjectAdapterType.FILE) {

			if (!(extRepositoryObjectAdapter instanceof ExtRepositoryFileEntryAdapter)) {

				throw new NoSuchFileEntryException(
					"External repository object is not a file " +
						extRepositoryObject);
			}
		}
		else if (extRepositoryObjectAdapterType == ExtRepositoryObjectAdapterType.FOLDER) {

			if (!(extRepositoryObjectAdapter instanceof ExtRepositoryFolderAdapter)) {

				throw new NoSuchFolderException(
					"External repository object is not a folder " +
						extRepositoryObject);
			}
		}
		else if (extRepositoryObjectAdapterType != ExtRepositoryObjectAdapterType.OBJECT) {

			throw new IllegalArgumentException(
				"Unsupported repository object type " +
					extRepositoryObjectAdapterType);
		}

		return (T) extRepositoryObjectAdapter;
	}

	/**
	 * Add File
	 * 
	 * @param extRepositoryParentFolderKey
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param inputStream
	 * @return
	 * @throws PortalException
	 */
	protected File addFile(
		String extRepositoryParentFolderKey, String mimeType, String title,
		String description, InputStream inputStream)
		throws PortalException {

		try {
			File file = new File();

			file.setDescription(description);
			file.setMimeType(mimeType);

			Drive drive = _connectionManager.getDrive();

			Drive.Files driveFiles = drive.files();

			File extRepositoryParentFolderFile =
				getFile(drive, extRepositoryParentFolderKey);

			ParentReference parentReference = new ParentReference();

			parentReference.setId(extRepositoryParentFolderFile.getId());

			file.setParents(Arrays.asList(parentReference));

			file.setTitle(title);

			if (inputStream != null) {
				InputStreamContent inputStreamContent =
					new InputStreamContent(mimeType, inputStream);

				Drive.Files.Insert driveFilesInsert =
					driveFiles.insert(file, inputStreamContent);

				return driveFilesInsert.execute();
			}
			else {
				Drive.Files.Insert driveFilesInsert = driveFiles.insert(file);

				return driveFilesInsert.execute();
			}
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	/**
	 * Get File information
	 * 
	 * @param drive
	 * @param extRepositoryObjectKey
	 * @return
	 * @throws IOException
	 */
	protected File getFile(Drive drive, String extRepositoryObjectKey)
		throws IOException {

		GoogleDriveCache googleDriveCache = GoogleDriveCacheFactory.create();

		GoogleDriveCachedObject googleDriveCachedObject =
			googleDriveCache.getGoogleDriveCachedObject(
				extRepositoryObjectKey, drive);

		if (null == googleDriveCachedObject.getFile()) {
			Drive.Files driveFiles = drive.files();

			Drive.Files.Get driveFilesGet =
				driveFiles.get(extRepositoryObjectKey);

			File file = driveFilesGet.execute();

			googleDriveCache.remove(extRepositoryObjectKey);

			googleDriveCachedObject =
				googleDriveCache.getGoogleDriveCachedObject(
					extRepositoryObjectKey, file, drive);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("extRepositoryObjectKey : " + extRepositoryObjectKey);
		}

		return googleDriveCachedObject.getFile();
	}

	/**
	 * Return results;
	 */
	private static class SearchResult<T> {

		public SearchResult(List<T> list, int total) {

			_list = list;
			_total = total;
		}

		public List<T> getLists() {

			return _list;
		}

		public int getTotal() {

			return _total;
		}

		private List<T> _list = new ArrayList<>();
		private int _total = 0;
	}

	private static final String _CONFIGURATION_WS = "GOOGLEDRIVE_CONFIG";

	private static final String _GOOGLE_CLIENT_ID = "GOOGLE_CLIENT_ID";

	private static final String _GOOGLE_CLIENT_SECRET = "GOOGLE_CLIENT_SECRET";

	private static final String _GOOGLE_ACCESS_TOKEN = "GOOGLE_ACCESS_TOKEN";

	private static final String _GOOGLE_REFRESH_TOKEN = "GOOGLE_REFRESH_TOKEN";

	private static final String[] _SUPPORTED_CONFIGURATIONS = {
		_CONFIGURATION_WS
	};

	private static final String[][] _SUPPORTED_PARAMETERS = {
		{
			_GOOGLE_CLIENT_ID, _GOOGLE_CLIENT_SECRET, _GOOGLE_ACCESS_TOKEN,
			_GOOGLE_REFRESH_TOKEN
		}
	};

	// TODO: Change this value configuable by a property
	private int _THREAD_POOL_SIZE = 5;

	private GoogleDriveConnectionManager _connectionManager;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveRepository.class);

}
