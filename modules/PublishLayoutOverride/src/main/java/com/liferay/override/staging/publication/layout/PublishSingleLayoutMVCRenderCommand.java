package com.liferay.override.staging.publication.layout;

import com.liferay.exportimport.constants.ExportImportPortletKeys;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationFactory;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.NoSuchGroupException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.constants.MVCRenderConstants;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		immediate = true,
		property = {
			"javax.portlet.name=" + ExportImportPortletKeys.EXPORT_IMPORT,
			"mvc.command.name=publishLayoutsSimple",
			"service.ranking:Integer=100"
		},
		service = MVCRenderCommand.class
	)
public class PublishSingleLayoutMVCRenderCommand implements MVCRenderCommand {

	private static Logger LOG = LoggerFactory.getLogger(PublishSingleLayoutMVCRenderCommand.class);

	@Override
	public String render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException {

		long plid = getPlid(renderRequest);

		try {
			long exportImportConfigurationId = ParamUtil.getLong(
				renderRequest, "exportImportConfigurationId");
			
			renderRequest.setAttribute("plid", plid);

			if (exportImportConfigurationId <= 0) {
				createExportImportConfiguration(renderRequest);
			}

			PublishUtil.getGroup(renderRequest);
		}
		catch (Exception exception) {
			if (exception instanceof NoSuchGroupException ||
				exception instanceof PrincipalException) {

				SessionErrors.add(renderRequest, exception.getClass());

				return "/error.jsp";
			}

			throw new PortletException(exception);
		}
		
		Layout layout;
		try {
			layout = _layoutLocalService.getLayout(plid);
			String path;
			if(ancestorNotPublished(layout.getAncestors(), renderRequest)) {
				
				path = "/publish/simple/missing_ancestor.jsp";
			} else {
				path = "/publish/simple/publish_layouts_simple.jsp";
			}
			dispatch(renderRequest, renderResponse, path);
		} catch (PortalException e) {
			throw new PortletException(e);
		}
		
		return MVCRenderConstants.MVC_PATH_VALUE_SKIP_DISPATCH;
	}

	private boolean ancestorNotPublished(List<Layout> ancestorLayouts, PortletRequest portletRequest) throws PortletException, PortalException {

		boolean ancestorNotPublished = false;

		List<Layout> unpublishedLayouts = new ArrayList<Layout>(); 
		
		try {
			long liveGroupId = PublishUtil.getGroup(portletRequest).getLiveGroupId();
			
			Iterator<Layout> layoutsIterator = ancestorLayouts.iterator();
			
			while(layoutsIterator.hasNext()) {
			
				Layout layout = layoutsIterator.next();
				if(!_layoutLocalService.hasLayout(layout.getUuid(), liveGroupId, layout.isPrivateLayout())) {
					ancestorNotPublished = true;
					unpublishedLayouts.add(layout);
				}
				
			}
		} catch (Exception e) {
			throw new PortletException(e);
		}
		
		if(ancestorNotPublished) {
			ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(WebKeys.THEME_DISPLAY);
			portletRequest.setAttribute("unpublishedLayouts", getUnpublishedLayouts(themeDisplay, unpublishedLayouts));
		}
		
		return ancestorNotPublished;
	}

	private List<UnpublishedLayout> getUnpublishedLayouts(ThemeDisplay themeDisplay, List<Layout> layouts) throws PortalException {
		
		List<UnpublishedLayout> unpublishedLayouts = new ArrayList<UnpublishedLayout>();
		
		for(int i = 0; i < layouts.size(); i++) {
			Layout layout = layouts.get(i);
			UnpublishedLayout unpublishedLayout = new UnpublishedLayout();
			unpublishedLayout.setFriendlyURL(layout.getFriendlyURL());
			unpublishedLayout.setURL(_portal.getLayoutFriendlyURL(layout, themeDisplay));
			unpublishedLayouts.add(unpublishedLayout);
		}
		
		return unpublishedLayouts;
	}

	private void dispatch(RenderRequest renderRequest, RenderResponse renderResponse, String path) throws PortletException {
		
		RequestDispatcher requestDispatcher =
	            servletContext.getRequestDispatcher(path);
		
        try {
            HttpServletRequest httpServletRequest = 
                _portal.getHttpServletRequest(renderRequest);
            HttpServletResponse httpServletResponse = 
                _portal.getHttpServletResponse(renderResponse);

            requestDispatcher.include
                (httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            throw new PortletException
                ("Unable to include " + path, e);
        }
	}

	protected void createExportImportConfiguration(RenderRequest renderRequest)
		throws PortalException {

		long plid = getPlid(renderRequest);
		Layout layout = _layoutLocalService.getLayout(plid);
		long[] layoutIds = new long[1];
		layoutIds[0] = layout.getLayoutId();
		
		ExportImportConfiguration exportImportConfiguration = null;
		
		exportImportConfiguration =
			ExportImportConfigurationFactory.
				buildDefaultLocalPublishingExportImportConfiguration(
					renderRequest);
		
		Map<String, Serializable> settingsMap = exportImportConfiguration.getSettingsMap();
		settingsMap.remove("layoutIds");
		settingsMap.put("layoutIds", layoutIds);
		
		_exportImportConfigurationLocalService.updateExportImportConfiguration(
				exportImportConfiguration.getUserId(),
				exportImportConfiguration.getExportImportConfigurationId(),
				"Publish single layout " + layout.getName(Locale.getDefault()),
				StringPool.BLANK,
				settingsMap,
				new ServiceContext());
		
		renderRequest.setAttribute("layout", layout);
		if(layout.getAncestors().size() > 0) {
			renderRequest.setAttribute("hasAncestors", true);
			List<String> ancestorFriendlyURLs = new ArrayList<String>();
			layout.getAncestors().forEach(ancestor -> {
				ancestorFriendlyURLs.add(ancestor.getFriendlyURL(getLocale(renderRequest)));
			});
			renderRequest.setAttribute("ancestorFriendlyURLs", ancestorFriendlyURLs);
		}

		renderRequest.setAttribute(
			"exportImportConfigurationId",
			exportImportConfiguration.getExportImportConfigurationId());
	}

	private long getPlid(RenderRequest renderRequest) {
		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(renderRequest);
		ThemeDisplay themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long plid = themeDisplay.getPlid();
		return plid;
	}

	private Locale getLocale(RenderRequest renderRequest) {
		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(renderRequest);
		ThemeDisplay themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		return themeDisplay.getLocale();
	}

	@Reference
	private Portal _portal;
	
	@Reference
	private LayoutLocalService _layoutLocalService;
	
	@Reference
	private ExportImportConfigurationLocalService _exportImportConfigurationLocalService;
	
    @Reference(target = "(osgi.web.symbolicname=com.liferay.override.staging.publication.layout)")
    protected ServletContext servletContext;
}