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

package com.liferay.users.admin.demo.data.creator.internal;

import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.users.admin.demo.data.creator.UserDemoDataCreator;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Reference;

/**
 * @author Sergio González
 */
public abstract class BaseUserDemoDataCreator implements UserDemoDataCreator {

	public User createUser(long companyId, String emailAddress)
		throws PortalException {

		String email = emailAddress;
		Date birthDate = new Date(0);
		boolean male = true;

		try (InputStream is = (new URL(_RANDOM_USER_API)).openStream()) {
			String json = StringUtil.read(is);

			JSONObject rootJsonObject = JSONFactoryUtil.createJSONObject(json);

			JSONObject userJsonObject = rootJsonObject.getJSONArray(
				"results").getJSONObject(0);

			if (email == null) {
				email = userJsonObject.getString("email");
			}

			male = StringUtil.equalsIgnoreCase(
				userJsonObject.getString("gender"), "male");

			String dateString = userJsonObject.getString("dob");

			try {
				DateFormat dateFormat =
					DateFormatFactoryUtil.getSimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");

				birthDate = dateFormat.parse(dateString);
			}
			catch (ParseException pe) {
				if (_log.isWarnEnabled()) {
					_log.warn(pe, pe);
				}
			}
		}
		catch (IOException ioe) {
			if (_log.isWarnEnabled()) {
				_log.warn(ioe, ioe);
			}

			if (email == null) {
				email = StringUtil.randomString().concat("@liferay.com");
			}
		}

		User user = userLocalService.fetchUserByEmailAddress(companyId, email);

		if (user != null) {
			return user;
		}

		user = _createBasicUser(companyId, email, male, birthDate);

		_userIds.add(user.getUserId());

		return user;
	}

	@Override
	public void delete() throws PortalException {
		try {
			for (long userId : _userIds) {
				_userIds.remove(userId);

				userLocalService.deleteUser(userId);
			}
		}
		catch (NoSuchUserException nsue) {
			if (_log.isWarnEnabled()) {
				_log.warn(nsue, nsue);
			}
		}
	}

	protected String[] getFullNameArray(String emailAddress) {
		String emailAccountName = emailAddress.substring(
			0, emailAddress.indexOf(StringPool.AT));

		String[] fullNameArray = StringUtil.split(
			emailAccountName, StringPool.PERIOD);

		String firstName = StringUtil.randomString();
		String lastName = StringUtil.randomString();

		if (fullNameArray.length > 0) {
			firstName = StringUtil.upperCaseFirstLetter(fullNameArray[0]);
		}

		if (fullNameArray.length > 1) {
			lastName = StringUtil.upperCaseFirstLetter(fullNameArray[1]);
		}

		return new String[] {firstName, lastName};
	}

	@Reference(unbind = "-")
	protected void setUserLocalService(UserLocalService userLocalService) {
		this.userLocalService = userLocalService;
	}

	protected UserLocalService userLocalService;

	private User _createBasicUser(
			long companyId, String email, boolean male, Date birthDate)
		throws PortalException {

		String[] fullNameArray = getFullNameArray(email);

		String firstName = fullNameArray[0];
		String lastName = fullNameArray[1];

		boolean autoPassword = false;
		String password1 = "test";
		String password2 = "test";
		long facebookId = 0;
		String openId = StringPool.BLANK;
		Locale locale = LocaleUtil.SPAIN;

		String middleName = StringPool.BLANK;
		long prefixId = 0;
		long suffixId = 0;

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(birthDate);

		int birthdayMonth = calendar.get(Calendar.MONTH);
		int birthdayDay = calendar.get(Calendar.DATE);
		int birthdayYear = calendar.get(Calendar.YEAR);

		String jobTitle = StringUtil.randomString();
		long[] groupIds = null;
		long[] organizationIds = null;
		long[] roleIds = null;
		long[] userGroupIds = null;
		boolean sendMail = false;

		return userLocalService.addUser(
			UserConstants.USER_ID_DEFAULT, companyId, autoPassword, password1,
			password2, true, StringPool.BLANK, email, facebookId, openId,
			locale, firstName, middleName, lastName, prefixId, suffixId, male,
			birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds,
			organizationIds, roleIds, userGroupIds, sendMail,
			new ServiceContext());
	}

	private static final String _RANDOM_USER_API =
		"https://randomuser.me/api?inc=email,gender,dob&noinfo";

	private static final Log _log = LogFactoryUtil.getLog(
		BaseUserDemoDataCreator.class);

	private final List<Long> _userIds = new CopyOnWriteArrayList<>();

}