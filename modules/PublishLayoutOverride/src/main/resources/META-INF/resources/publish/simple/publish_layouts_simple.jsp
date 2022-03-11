<%@ include file="/init.jsp" %>

<liferay-staging:defineObjects />

<%
String cmd = ParamUtil.getString(request, Constants.CMD, Constants.PUBLISH_TO_LIVE);

long exportImportConfigurationId = GetterUtil.getLong(request.getAttribute("exportImportConfigurationId"), ParamUtil.getLong(request, "exportImportConfigurationId"));
ExportImportConfiguration exportImportConfiguration = ExportImportConfigurationLocalServiceUtil.getExportImportConfiguration(exportImportConfigurationId);

GroupDisplayContextHelper groupDisplayContextHelper = new GroupDisplayContextHelper(request);

long selPlid = ParamUtil.getLong(request, "selPlid", LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);
boolean localPublishing = true;

String publishMessageKey = "publish-to-live";

if (exportImportConfiguration.getType() == ExportImportConfigurationConstants.TYPE_PUBLISH_LAYOUT_REMOTE) {
	cmd = Constants.PUBLISH_TO_REMOTE;
	localPublishing = false;
	publishMessageKey = "publish-to-remote-live";
}

ThemeDisplay td  = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
%>

<div class="panel-group panel-group-flush">

	<portlet:actionURL name="publishSingleLayout" var="publishSingleLayoutActionURL">
		<portlet:param name="exportImportConfigurationId" value="<%= String.valueOf(exportImportConfiguration.getExportImportConfigurationId()) %>" />
	</portlet:actionURL>
	
	<aui:form action="${publishSingleLayoutActionURL}" cssClass="container-lg container-no-gutters-sm-down container-view form lfr-export-dialog" method="post" name="fm2">

		<div class="sheet sheet-lg">

			<p><liferay-ui:message key="are-you-sure-you-want-to-propose-this-layout-for-publication" /></p>
			
			<strong><liferay-ui:message key="model.resource.com.liferay.portal.kernel.model.Layout" />: </strong>${layout.getFriendlyURL(locale)}</p>

			<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= cmd %>" />
			<aui:input name="redirect" type="hidden" value="<%= themeDisplay.getURLCurrent() %>" />
			<aui:input name="groupId" type="hidden" value="<%= groupDisplayContextHelper.getGroupId() %>" />
		
			<aui:button-row>
				<aui:button type="submit" value="<%= LanguageUtil.get(request, publishMessageKey) %>" />
			</aui:button-row>
		
		</div>
	
	</aui:form>

</div>