package com.liferay.override.staging.publication.layout;

import com.liferay.exportimport.constants.ExportImportPortletKeys;
import com.liferay.exportimport.kernel.background.task.BackgroundTaskExecutorNames;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalService;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManager;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.Serializable;

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
		
		long userId = themeDisplay.getUserId();
		
		long exportImportConfigurationId = ParamUtil.getLong(actionRequest, "exportImportConfigurationId");

		try {
			LOG.debug("Do Single page publication for plid {}", plid);
	
			ExportImportConfiguration exportImportConfiguration = _exportImportConfigurationLocalService.getExportImportConfiguration(exportImportConfigurationId);
			
			String backgroundTaskName = exportImportConfiguration.getName();

			BackgroundTask backgroundTask =
					_backgroundTaskManager.addBackgroundTask(
						userId, exportImportConfiguration.getGroupId(),
						backgroundTaskName,
						BackgroundTaskExecutorNames.
							LAYOUT_STAGING_BACKGROUND_TASK_EXECUTOR,
						HashMapBuilder.<String, Serializable>put(
							"exportImportConfigurationId",
							exportImportConfiguration.getExportImportConfigurationId()
						).put(
							"privateLayout",
							MapUtil.getBoolean(exportImportConfiguration.getSettingsMap(), "privateLayout")
						).build(),
						new ServiceContext());

			long backgroundTaskId = backgroundTask.getBackgroundTaskId();
			
			LOG.debug("Done Single page publication for plid {}, Background task Id: {}", plid, backgroundTaskId);
			actionRequest.setAttribute("liferay-staging:select-pages:exportImportConfigurationId", exportImportConfigurationId);
		} catch (Exception e) {
			LOG.error("EXCEPTION", e);
			throw e;
		}
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
	private BackgroundTaskManager _backgroundTaskManager;
	
	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private ExportImportConfigurationLocalService _exportImportConfigurationLocalService;
	
	@Reference
	private Portal _portal;

}