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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.About;
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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.RepositoryEntry;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.servlet.PortalSessionThreadLocal;
import com.liferay.portal.kernel.util.AutoResetThreadLocal;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TransientValue;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import jp.liferay.google.drive.repository.model.GoogleDriveFileEntry;
import jp.liferay.google.drive.repository.model.GoogleDriveFileVersion;
import jp.liferay.google.drive.repository.model.GoogleDriveFolder;

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
			extRepositoryParentFolderKey, _FOLDER_MIME_TYPE, name, description,
			null);

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
			Drive drive = getDrive();

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
			Drive drive = getDrive();

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
			Drive drive = getDrive();

			Drive.Files driveFiles = drive.files();

			Drive.Files.Delete driveFilesDelete =
				driveFiles.delete(extRepositoryObjectKey);

			driveFilesDelete.execute();

			GoogleDriveCache googleDriveCache = GoogleDriveCache.getInstance();

			googleDriveCache.remove(extRepositoryObjectKey);
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

		GoogleDriveFileVersion googleDriveFileVersion =
			(GoogleDriveFileVersion) extRepositoryFileVersion;

		return getContentStream(googleDriveFileVersion.getDownloadURL());
	}

	protected InputStream getContentStream(String downloadURL)
		throws PortalException {

		if (Validator.isNull(downloadURL)) {
			return null;
		}

		Drive drive = getDrive();

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
			Drive drive = getDrive();

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

		try {
			Drive drive = getDrive();

			Drive.Revisions driveRevisions = drive.revisions();

			Drive.Revisions.List driveRevisionsList = driveRevisions.list(
				extRepositoryFileEntry.getExtRepositoryModelKey());

			RevisionList revisionList = driveRevisionsList.execute();

			List<Revision> revisions = revisionList.getItems();

			List<ExtRepositoryFileVersion> extRepositoryFileVersions =
				new ArrayList<>(revisions.size());

			for (int i = 0; i < revisions.size(); i++) {
				Revision revision = revisions.get(i);

				extRepositoryFileVersions.add(
					new GoogleDriveFileVersion(
						revision,
						extRepositoryFileEntry.getExtRepositoryModelKey(),
						i + 1));
			}

			Collections.reverse(extRepositoryFileVersions);

			return extRepositoryFileVersions;
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ExtRepositoryObject> T getExtRepositoryObject(
		ExtRepositoryObjectType<T> extRepositoryObjectType,
		String extRepositoryObjectKey)
		throws PortalException {

		try {
			Drive drive = getDrive();

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

			sb.append(_FOLDER_MIME_TYPE);

			Drive drive = getDrive();

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
			Drive drive = getDrive();

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

				sb.append(_FOLDER_MIME_TYPE);
				sb.append("' and ");
			}

			sb.append("trashed = false");

			driveFilesList.setQ(sb.toString());

			FileList fileList = driveFilesList.execute();

			List<File> files = fileList.getItems();

			List<T> extRepositoryObjects = new ArrayList<>();

			GoogleDriveCache googleDriveCache = GoogleDriveCache.getInstance();

			for (File file : files) {
				if (_FOLDER_MIME_TYPE.equals(file.getMimeType())) {
					extRepositoryObjects.add(
						(T) new GoogleDriveFolder(file, getRootFolderKey()));
				}
				else {
					extRepositoryObjects.add(
						(T) new GoogleDriveFileEntry(file));
				}

				googleDriveCache.put(file);
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
			Drive drive = getDrive();

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
			GoogleDriveSession googleDriveSession = getGoogleDriveSession();

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

		_googleClientId = typeSettingsProperties.getProperty(_GOOGLE_CLIENT_ID);

		_googleClientSecret =
			typeSettingsProperties.getProperty(_GOOGLE_CLIENT_SECRET);

		// At initialization, the values are always null
		if (Validator.isNull(_googleClientId) ||
			Validator.isNull(_googleClientSecret)) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Google Client ID or Google Client Secret are empty");
			}
			return;
		}

		_googleAccessToken =
			typeSettingsProperties.getProperty(_GOOGLE_ACCESS_TOKEN);

		_googleRefreshToken =
			typeSettingsProperties.getProperty(_GOOGLE_REFRESH_TOKEN);

		// At initialization, the values are always null
		if (Validator.isNull(_googleAccessToken) ||
			Validator.isNull(_googleRefreshToken)) {
			if (_log.isDebugEnabled()) {
				_log.debug("Access Token or Refresh Token are empty");
			}
			return;
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Client ID     : " + _googleClientId);
			_log.debug("Client Secret : " + _googleClientSecret);
			_log.debug("Access Token  : " + _googleAccessToken);
			_log.debug("Refresh Token : " + _googleRefreshToken);
		}

		getDrive();
	}

	public Drive getDrive()
		throws PortalException {

		GoogleDriveSession googleDriveSession = getGoogleDriveSession();

		return googleDriveSession.getDrive();
	}

	/**
	 * Get Google Drive Session
	 * 
	 * @return GoogleDriveSession
	 * @throws PortalException
	 */
	@SuppressWarnings("unchecked")
	protected GoogleDriveSession getGoogleDriveSession()
		throws PortalException {

		GoogleDriveSession googleDriveSession = null;

		HttpSession httpSession = PortalSessionThreadLocal.getHttpSession();

		if (httpSession != null) {
			TransientValue<GoogleDriveSession> transientValue =
				(TransientValue<GoogleDriveSession>) httpSession.getAttribute(
					GoogleDriveSession.class.getName());

			if (transientValue != null) {
				googleDriveSession = transientValue.getValue();
			}
		}
		else {
			googleDriveSession = _googleDriveSessionThreadLocal.get();
		}

		if (googleDriveSession != null) {
			return googleDriveSession;
		}

		try {
			googleDriveSession = buildGoogleDriveSession();
		}
		catch (Exception e) {
			throw new PrincipalException(e);
		}

		if (httpSession != null) {
			httpSession.setAttribute(
				GoogleDriveSession.class.getName(),
				new TransientValue<GoogleDriveSession>(googleDriveSession));
		}
		else {
			_googleDriveSessionThreadLocal.set(googleDriveSession);
		}

		return googleDriveSession;
	}

	protected GoogleDriveSession buildGoogleDriveSession()
		throws IOException, PortalException {

		long userId = PrincipalThreadLocal.getUserId();

		User user = userLocalService.getUser(userId);

		if (user.isDefaultUser()) {
			throw new PrincipalException("User is not authenticated");
		}

		GoogleCredential.Builder builder = new GoogleCredential.Builder();

		builder.setClientSecrets(_googleClientId, _googleClientSecret);

		JacksonFactory jsonFactory = new JacksonFactory();

		builder.setJsonFactory(jsonFactory);

		HttpTransport httpTransport = new NetHttpTransport();

		builder.setTransport(httpTransport);

		GoogleCredential googleCredential = builder.build();

		googleCredential.setAccessToken(_googleAccessToken);

		googleCredential.setRefreshToken(_googleRefreshToken);

		Drive.Builder driveBuilder =
			new Drive.Builder(httpTransport, jsonFactory, googleCredential);

		Drive drive = driveBuilder.build();

		Drive.About driveAbout = drive.about();

		Drive.About.Get driveAboutGet = driveAbout.get();

		About about = driveAboutGet.execute();

		return new GoogleDriveSession(drive, about.getRootFolderId());
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
			Drive drive = getDrive();

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
			Drive drive = getDrive();

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
	 * Building search query for Google Drive
	 * 
	 * @param keywords
	 * @param folderIds
	 * @param extRepositoryQueryMapper
	 * @return
	 * @throws SearchException
	 */
	protected String getSearchQuery(
		String keywords, long[] folderIds,
		ExtRepositoryQueryMapper extRepositoryQueryMapper)
		throws SearchException {

		StringBundler sb = new StringBundler();

		sb.append("fullText contains '");
		sb.append(keywords);
		sb.append("' and ");

		for (int i = 0; i < folderIds.length; i++) {
			if (i != 0) {
				sb.append(" and ");
			}

			long folderId = folderIds[i];

			String extRepositoryFolderKey =
				extRepositoryQueryMapper.formatParameterValue(
					"folderId", String.valueOf(folderId));

			sb.append(StringPool.APOSTROPHE);
			sb.append(extRepositoryFolderKey);
			sb.append(StringPool.APOSTROPHE);

			sb.append(" in parents");
		}

		return sb.toString();
	}

	@Override
	public List<ExtRepositorySearchResult<?>> search(
		SearchContext searchContext, Query query,
		ExtRepositoryQueryMapper extRepositoryQueryMapper)
		throws PortalException {

		try {
			Drive drive = getDrive();

			Drive.Files driveFiles = drive.files();

			Drive.Files.List driveFilesList = driveFiles.list();

			String searchQuery = getSearchQuery(
				searchContext.getKeywords(), searchContext.getFolderIds(),
				extRepositoryQueryMapper);

			driveFilesList.setQ(searchQuery);

			FileList fileList = driveFilesList.execute();

			List<File> files = fileList.getItems();

			List<ExtRepositorySearchResult<?>> extRepositorySearchResults =
				new ArrayList<>(files.size());

			for (File file : files) {
				if (_FOLDER_MIME_TYPE.equals(file.getMimeType())) {
					GoogleDriveFolder googleDriveFolder =
						new GoogleDriveFolder(file, getRootFolderKey());

					ExtRepositorySearchResult<GoogleDriveFolder> extRepositorySearchResult =
						new ExtRepositorySearchResult<>(
							googleDriveFolder, 1.0f, StringPool.BLANK);

					extRepositorySearchResults.add(extRepositorySearchResult);
				}
				else {
					GoogleDriveFileEntry googleDriveFileEntry =
						new GoogleDriveFileEntry(file);

					ExtRepositorySearchResult<GoogleDriveFileEntry> extRepositorySearchResult =
						new ExtRepositorySearchResult<>(
							googleDriveFileEntry, 1.0f, StringPool.BLANK);

					extRepositorySearchResults.add(extRepositorySearchResult);
				}
			}

			return extRepositorySearchResults;
		}
		catch (IOException ioe) {
			_log.error(ioe, ioe);

			throw new SystemException(ioe);
		}
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
			Drive drive = getDrive();

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
			file.setOriginalFilename(file.getTitle() + StringPool.COMMA + fileExtension);
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

				repositoryEntryLocalService.updateRepositoryEntry(
					fileEntryId,
					extRepositoryFileEntry.getExtRepositoryModelKey());
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

			Drive drive = getDrive();

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

		GoogleDriveCache googleDriveCache = GoogleDriveCache.getInstance();

		File file = googleDriveCache.get(extRepositoryObjectKey);

		if (file == null) {
			Drive.Files driveFiles = drive.files();

			Drive.Files.Get driveFilesGet =
				driveFiles.get(extRepositoryObjectKey);

			file = driveFilesGet.execute();

			googleDriveCache.put(file);
		}

		return file;
	}

	private static final String _FOLDER_MIME_TYPE =
		"application/vnd.google-apps.folder";

	private ThreadLocal<GoogleDriveSession> _googleDriveSessionThreadLocal =
		new AutoResetThreadLocal<>(Drive.class.getName());

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

	private String _googleClientId;

	private String _googleClientSecret;

	private String _googleAccessToken;

	private String _googleRefreshToken;

	private static final Log _log =
		LogFactoryUtil.getLog(GoogleDriveRepository.class);

}
