<%@page import="pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@ taglib prefix="datatables" uri="http://github.com/dandelion/datatables"%>

<spring:url var="datatablesUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js" />
<spring:url var="datatablesBootstrapJsUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl" value="/CSS/dataTables/dataTables.bootstrap.min.css" />

<link rel="stylesheet" href="${datatablesCssUrl}" />
<spring:url var="datatablesI18NUrl" value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json" />
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css" />

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
${portal.angularToolkit()}
<%--${portal.toolkit()}--%>

<link href="${pageContext.request.contextPath}/static/fenixedu-ulisboa-specifications/css/dataTables.responsive.css" rel="stylesheet" />
<script src="${pageContext.request.contextPath}/static/fenixedu-ulisboa-specifications/js/dataTables.responsive.js"></script>
<link href="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/css/dataTables.tableTools.css" rel="stylesheet" />
<script src="${pageContext.request.contextPath}/webjars/datatables-tools/2.2.4/js/dataTables.tableTools.js"></script>
<link href="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/css/select2.min.css" rel="stylesheet" />
<script src="${pageContext.request.contextPath}/webjars/select2/4.0.0-rc.2/dist/js/select2.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/webjars/bootbox/4.4.0/bootbox.js"></script>
<script src="${pageContext.request.contextPath}/static/fenixedu-ulisboa-specifications/js/omnis.js"></script>

<script src="${pageContext.request.contextPath}/webjars/angular-sanitize/1.3.11/angular-sanitize.js"></script>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/webjars/angular-ui-select/0.11.2/select.min.css" />
<script src="${pageContext.request.contextPath}/webjars/angular-ui-select/0.11.2/select.min.js"></script>



<%-- TITLE --%>
<div class="page-header">
	<h1><spring:message code="label.firstTimeCandidacy.DownloadDocuments" />
		<small></small>
	</h1>
</div>

<c:if test="${not empty infoMessages}">
	<div class="alert alert-info" role="alert">
		
		<c:forEach items="${infoMessages}" var="message"> 
			<p> <span class="glyphicon glyphicon glyphicon-ok-sign" aria-hidden="true">&nbsp;</span>
						${message}
					</p>
		</c:forEach>
		
	</div>	
</c:if>
<c:if test="${not empty warningMessages}">
	<div class="alert alert-warning" role="alert">
		
		<c:forEach items="${warningMessages}" var="message"> 
			<p> <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true">&nbsp;</span>
						${message}
					</p>
		</c:forEach>
		
	</div>	
</c:if>
<c:if test="${not empty errorMessages}">
	<div class="alert alert-danger" role="alert">
		
		<c:forEach items="${errorMessages}" var="message"> 
			<p> <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true">&nbsp;</span>
						${message}
					</p>
		</c:forEach>
		
	</div>	
</c:if>

<script>
angular.module('angularApp', ['ngSanitize', 'ui.select', 'bennuToolkit']).controller('angularController', ['$scope', function($scope) {

    $scope.object=
	{ 'registrationValues' : [
	<c:forEach items="${registrationsSet}" var="registration">
	    {'id': "<c:out value='${registration.externalId}'/>",
		 'text': "<c:out value='${registration.degreeDescription}'/>",
		},
	</c:forEach>
    ]};
	
	${contactsFormJson};
    $scope.postBack = createAngularPostbackFunction($scope);
    
    $scope.booleanvalues = [
            { name : '<spring:message code="label.no"/>', value : false },
            { name : '<spring:message code="label.yes"/>', value : true } 
    ];
    $scope.isPrinted = false;
    $scope.printDocuments = function () {
	    var beginUrl = '${pageContext.request.contextPath}${controllerURL}/';
	    var endUrl = '/printalldocuments';	
	    $('#downloadATag').attr('href',beginUrl + $scope.object.registration + endUrl);
    };
    $scope.submitForm = function() {
        $('form').submit();
    };
}]);
</script>

<form name='form' method="post" class="form-horizontal" ng-app="angularApp" ng-controller="angularController"
     action="#">

    <input type="hidden" name="postback"
        value='${pageContext.request.contextPath}${controllerURL}/fillPostback' />
        
    <input name="bean" type="hidden" value="{{ object }}" />


    <div class="well">
    	<spring:message code="label.firstTimeCandidacy.finished.details.more" />
    	<ul>
    		<li><spring:message code="label.firstTimeCandidacy.finished.details.registrationDeclaration"/></li>
    		<li><spring:message code="label.firstTimeCandidacy.finished.details.model43"/></li>
    		<li><spring:message code="label.firstTimeCandidacy.finished.details.tuitionPayments"/></li>
    	</ul>
    </div>
    
    <div class="form-group row">
        <div class="col-sm-2 control-label required-field">
            <spring:message
                code="label.student.registration" />
        </div>

        <div class="col-sm-10">
            <ui-select  id="student_registration" name="registration" ng-model="$parent.object.registration" theme="bootstrap">
                <ui-select-match >{{$select.selected.text}}</ui-select-match> 
                <ui-select-choices  repeat="registration.id as registration in object.registrationValues | filter: $select.search">
                    <span ng-bind-html="registration.text"></span>
                </ui-select-choices> 
            </ui-select>                    
        </div>
    </div>
    
    <%-- NAVIGATION --%>
    <div class="well well-sm" style="display:inline-block" ng-hide="isPrinted">
    	<span class="glyphicon glyphicon-ok" aria-hidden="true"></span>&nbsp;<a id="downloadATag"class="" ng-click="printDocuments()" href="#" target="_blank"><spring:message code="label.event.firstTimeCandidacy.download" /></a>
    </div>
</form>

<style>
	.slogan {
		font-weight: bold;
		font-size: 17px;
	}
</style>

<script>
$(document).ready(function() {

	
	
	});
</script>
