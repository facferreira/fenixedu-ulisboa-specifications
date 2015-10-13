<%@page import="org.fenixedu.ulisboa.specifications.ui.servicerequesttype.ServiceRequestTypeController"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<spring:url var="datatablesUrl"
    value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js" />
<spring:url var="datatablesBootstrapJsUrl"
    value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl"
    value="/CSS/dataTables/dataTables.bootstrap.min.css" />

<link rel="stylesheet" href="${datatablesCssUrl}" />
<spring:url var="datatablesI18NUrl"
    value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json" />

<link rel="stylesheet" type="text/css"
    href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css" />

<link
    href="${pageContext.request.contextPath}/static/ulisboaspecifications/css/dataTables.responsive.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/dataTables.responsive.js"></script>

<link
    href="${pageContext.request.contextPath}/static/ulisboaspecifications/css/dataTables.tableTools.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/dataTables.tableTools.min.js"></script>

<link
    href="${pageContext.request.contextPath}/static/ulisboaspecifications/css/select2.min.css"
    rel="stylesheet" />
<script
    src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/select2.min.js"></script>

<script
    src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/bootbox.min.js"></script>
<script
    src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/omnis.js"></script>

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
    <h1>
        <spring:message
            code="label.manageServiceRequestTypes.readServiceRequestType" />
        <small></small>
    </h1>
</div>
<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
    <span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>
    &nbsp;
    <a class="" href="${pageContext.request.contextPath}<%= ServiceRequestTypeController.SEARCH_URL %>">
        <spring:message code="label.event.back" />
    </a> 
    &nbsp;|&nbsp; 
    <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
    &nbsp;
    <a class="" href="${pageContext.request.contextPath}<%= ServiceRequestTypeController.UPDATE_URL %>${serviceRequestType.externalId}">
        <spring:message code="label.event.update" />
    </a> 
    &nbsp;
</div>
<c:if test="${not empty infoMessages}">
    <div class="alert alert-info" role="alert">

        <c:forEach items="${infoMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon glyphicon-ok-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>

    </div>
</c:if>
<c:if test="${not empty warningMessages}">
    <div class="alert alert-warning" role="alert">

        <c:forEach items="${warningMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon-exclamation-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>

    </div>
</c:if>
<c:if test="${not empty errorMessages}">
    <div class="alert alert-danger" role="alert">

        <c:forEach items="${errorMessages}" var="message">
            <p>
                <span class="glyphicon glyphicon-exclamation-sign"
                    aria-hidden="true">&nbsp;</span> ${message}
            </p>
        </c:forEach>

    </div>
</c:if>

<div class="panel panel-primary">
    <div class="panel-heading">
        <h3 class="panel-title">
            <spring:message code="label.details" />
        </h3>
    </div>
    <div class="panel-body">
        <form method="post" class="form-horizontal">
            <table class="table">
                <tbody>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.code" /></th>
                        <td><c:out
                                value='${serviceRequestType.code}' /></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.name" /></th>
                        <td><c:out
                                value='${serviceRequestType.name.content}' /></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.active" /></th>
                        <td><c:if
                                test="${serviceRequestType.active}">
                                <spring:message code="label.true" />
                            </c:if> <c:if
                                test="${not serviceRequestType.active}">
                                <spring:message code="label.false" />
                            </c:if></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.payable" /></th>
                        <td><c:if
                                test="${serviceRequestType.payable}">
                                <spring:message code="label.true" />
                            </c:if> <c:if
                                test="${not serviceRequestType.payable}">
                                <spring:message code="label.false" />
                            </c:if></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.notifyUponConclusion" /></th>
                        <td><c:if
                                test="${serviceRequestType.notifyUponConclusion}">
                                <spring:message code="label.true" />
                            </c:if> <c:if
                                test="${not serviceRequestType.notifyUponConclusion}">
                                <spring:message code="label.false" />
                            </c:if></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.printable" /></th>
                        <td><c:if
                                test="${serviceRequestType.printable}">
                                <spring:message code="label.true" />
                            </c:if> <c:if
                                test="${not serviceRequestType.printable}">
                                <spring:message code="label.false" />
                            </c:if></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.serviceRequestCategory" /></th>
                        <td><c:out
                                value='${serviceRequestType.serviceRequestCategory.name}' /></td>
                    </tr>
                    <tr>
                        <th scope="row" class="col-xs-3"><spring:message
                                code="label.ServiceRequestType.serviceRequestSlot" /></th>
                        <td>
                            <c:forEach var="slot" items="${ serviceRequestType.serviceRequestSlotsSet }" >
                                <p><c:out value="${ slot.code }"/></p>
                            </c:forEach>
                        </td>
                    </tr>
                </tbody>
            </table>
        </form>
    </div>
</div>

<script>
	$(document).ready(function() {
	});
</script>