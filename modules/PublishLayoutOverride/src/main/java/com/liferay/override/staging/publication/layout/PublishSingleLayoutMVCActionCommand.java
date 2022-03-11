package com.liferay.override.staging.publication.layout;

import com.liferay.exportimport.constants.ExportImportPortletKeys;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationParameterMapFactory;
import com.liferay.exportimport.kernel.lar.ExportImportHelper;
import com.liferay.exportimport.kernel.lar.PortletDataHandlerKeys;
import com.liferay.exportimport.kernel.staging.Staging;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletURL;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		immediate = true,
		property = {
			"javax.portlet.name=" + ExportImportPortletKeys.EXPORT_IMPORT,
			"mvc.command.name=publishSingleLayout"
		},
		service = MVCActionCommand.class
	)
public class PublishSingleLayoutMVCActionCommand extends BaseMVCActionCommand {

	private final static Logger LOG = LoggerFactory.getLogger(PublishSingleLayoutMVCActionCommand.class);
	
	@Override
	protected void doProcessAction(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		if (Validator.isNull(cmd)) {
			SessionMessages.add(
				actionRequest,
				_portal.getPortletId(actionRequest) +
					SessionMessages.KEY_SUFFIX_FORCE_SEND_REDIRECT);

			hideDefaultSuccessMessage(actionRequest);

			return;
		}

		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(actionRequest);
		ThemeDisplay themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long plid = themeDisplay.getPlid();
		long liveGroupId = PublishUtil.getGroup(actionRequest).getLiveGroupId();
		
		long userId = themeDisplay.getUserId();
		
		long backgroundTaskId = publishSingleLayout(plid, userId, liveGroupId); 
		
		LOG.debug("Single page publication for plid {}, Background task Id: {}", plid, backgroundTaskId);

	}
	
	protected long publishSingleLayout(long plid, long userId, long liveGroupId) throws PortalException {
		
		Layout layout = _layoutLocalService.getLayout(plid);

		Map<String, String[]> parameterMap =
				_exportImportConfigurationParameterMapFactory.buildParameterMap();

		parameterMap.put(
			PortletDataHandlerKeys.DELETE_MISSING_LAYOUTS,
			new String[] {Boolean.FALSE.toString()});
		
		parameterMap.put("name", new String[] {"Single layout publication: " + layout.getFriendlyURL()});
		
		List<Layout> layouts = new ArrayList<>();

		layouts.add(layout);

		return _staging.publishLayouts(
			userId, layout.getGroupId(), liveGroupId, layout.isPrivateLayout(),
			_exportImportHelper.getLayoutIds(layouts), parameterMap);
		
	}
	
	protected void setLayoutIdMap(ActionRequest actionRequest) {

		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(actionRequest);
		ThemeDisplay themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long plid = themeDisplay.getPlid();

		JSONArray layoutsJSONArray = JSONFactoryUtil.createJSONArray();
		layoutsJSONArray.put(
				JSONUtil.put(
					"includeChildren", false
				).put(
					"plid", plid
				));
		
		String selectedLayoutsJSON = layoutsJSONArray.toString();
		
		LOG.error("Selected layouts {}", selectedLayoutsJSON);

		actionRequest.setAttribute("layoutIdMap", selectedLayoutsJSON);
	}
	
	protected void setRedirect(
			ActionRequest actionRequest, ActionResponse actionResponse,
			long backgroundTaskId) {

		LiferayPortletResponse liferayPortletResponse =
			_portal.getLiferayPortletResponse(actionResponse);

		PortletURL renderURL = liferayPortletResponse.createRenderURL();

		renderURL.setParameter("mvcPath", "/view_export_import.jsp");
		renderURL.setParameter(
			"backgroundTaskId", String.valueOf(backgroundTaskId));

		actionRequest.setAttribute(WebKeys.REDIRECT, renderURL.toString());
	}

	@Reference
	private BackgroundTaskLocalService _backgroundLocalService;
	
	@Reference
	private BackgroundTaskManager _backgroundTaskManager;
	
	@Reference
	private ExportImportConfigurationParameterMapFactory
		_exportImportConfigurationParameterMapFactory;
	
	@Reference
	private LayoutLocalService _layoutLocalService;
	
	@Reference
	private ExportImportHelper _exportImportHelper;
	
	@Reference
	private Portal _portal;

	@Reference
	private Staging _staging;
	
}