<%@ include file="/init.jsp" %>

<liferay-staging:defineObjects />

<div class="panel-group panel-group-flush">

	<div class="container-lg container-no-gutters-sm-down container-view">

		<div class="sheet sheet-lg">
			
			<clay:alert
				message="you-cannot-publish-a-layout-if-one-of-its-ancestors-has-not-been-published"
				displayType="warning"
				title="unpublished-ancestors"
			/>
			
			<h3><liferay-ui:message key="list-of-unpublished-ancestors" /></h3>
			
			<ul>
				<c:forEach items="${unpublishedLayouts}" var="curLayout">
				    <li><a target="_blank" href="${curLayout.URL}">${curLayout.friendlyURL}</a></li>
				</c:forEach>
			</ul>
		</div>
	
	</div>

</div>