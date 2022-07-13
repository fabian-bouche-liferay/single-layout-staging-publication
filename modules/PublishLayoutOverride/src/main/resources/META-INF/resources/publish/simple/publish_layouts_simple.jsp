<%@ include file="/init.jsp" %>

<liferay-staging:defineObjects />

<%
String cmd = ParamUtil.getString(request, Constants.CMD, Constants.PUBLISH_TO_LIVE);

long exportImportConfigurationId = GetterUtil.getLong(request.getAttribute("exportImportConfigurationId"), ParamUtil.getLong(request, "exportImportConfigurationId"));
ExportImportConfiguration exportImportConfiguration = ExportImportConfigurationLocalServiceUtil.getExportImportConfiguration(exportImportConfigurationId);

GroupDisplayContextHelper groupDisplayContextHelper = new GroupDisplayContextHelper(request);

boolean localPublishing = true;

String publishMessageKey = "publish-to-live";

%>

<div class="panel-group panel-group-flush">

	<portlet:actionURL name="publishSingleLayout" var="publishSingleLayoutActionURL">
		<portlet:param name="exportImportConfigurationId" value="<%= String.valueOf(exportImportConfiguration.getExportImportConfigurationId()) %>" />
	</portlet:actionURL>
	
	<aui:form action="${publishSingleLayoutActionURL}" cssClass="container-lg container-no-gutters-sm-down container-view form lfr-export-dialog" method="post" name="fm2">

		<div class="sheet sheet-lg">

			<p><liferay-ui:message key="are-you-sure-you-want-to-propose-this-layout-for-publication" /></p>
			
			<p><strong><liferay-ui:message key="model.resource.com.liferay.portal.kernel.model.Layout" />: </strong>${layout.getFriendlyURL(locale)}</p>

			<c:if test="hasAncestors">
				<p><liferay-ui:message key="those-ancestor-layouts-will-also-be-published" /></p>
				<c:forEach items="ancestorFriendlyURLs" var="ancestorFriendlyURL">
					<p><strong><liferay-ui:message key="model.resource.com.liferay.portal.kernel.model.Layout" />: </strong>${ancestorFriendlyURL}</p>
				</c:forEach>
			</c:if>

			<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= cmd %>" />
			<aui:input name="redirect" type="hidden" value="<%= themeDisplay.getURLCurrent() %>" />
			<aui:input name="groupId" type="hidden" value="<%= groupDisplayContextHelper.getGroupId() %>" />
			<aui:input name="exportImportConfigurationId" type="hidden" value="<%= exportImportConfigurationId %>" />
		
			<aui:button-row>
				<aui:button type="submit" value="<%= LanguageUtil.get(request, publishMessageKey) %>" />
			</aui:button-row>
		
		</div>
	
	</aui:form>

</div>