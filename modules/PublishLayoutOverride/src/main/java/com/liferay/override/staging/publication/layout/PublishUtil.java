package com.liferay.override.staging.publication.layout;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

public class PublishUtil {

	public static Group getGroup(PortletRequest portletRequest)
			throws Exception {

		return getGroup(PortalUtil.getHttpServletRequest(portletRequest));
	}
	
	public static Group getGroup(HttpServletRequest httpServletRequest)
			throws Exception {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(httpServletRequest, Constants.CMD);

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId");

		Group group = null;

		if (groupId > 0) {
			group = GroupLocalServiceUtil.getGroup(groupId);
		}
		else if (!cmd.equals(Constants.ADD)) {
			group = themeDisplay.getSiteGroup();
		}

		httpServletRequest.setAttribute(WebKeys.GROUP, group);

		return group;
	}
	
}
