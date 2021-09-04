<%@page language="java"%>

<%@taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="acme" tagdir="/WEB-INF/tags"%>

<acme:list>
	<acme:list-column code="authenticated.work-plan.list.label.title" path="title" width="30%"/>
	<acme:list-column code="authenticated.work-plan.list.label.executionStart" path="executionStart" sortable="true" width="25%"/>
	<acme:list-column code="authenticated.work-plan.list.label.executionEnd" path="executionEnd" sortable="true" width="25%"/>
	<acme:list-column code="authenticated.work-plan.list.label.workloadParsed" path="workloadParsed" sortable="true" width="20%"/>
</acme:list>
