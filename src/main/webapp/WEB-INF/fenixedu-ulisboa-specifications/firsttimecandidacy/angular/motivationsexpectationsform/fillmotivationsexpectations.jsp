<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

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

<script src="${pageContext.request.contextPath}/static/ulisboaspecifications/js/angularjs-dropdown-multiselect.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/lodash.js/2.4.1/lodash.min.js"></script>


<%-- TITLE --%>
<div class="page-header">
	<h1><spring:message code="label.firstTimeCandidacy.fillMotivationsExpectations" />
		<small></small>
	</h1>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display:inline-block">
    <span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a class="" href="${pageContext.request.contextPath}${controllerURL}/back"><spring:message code="label.back"/></a> 
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
angular.module('angularApp', ['ngSanitize', 'ui.select', 'bennuToolkit', 'angularjs-dropdown-multiselect']).controller('angularController', ['$scope', function($scope) {

    $scope.object= ${motivationsExpectationsFormJson};
    $scope.postBack = createAngularPostbackFunction($scope);
    
    $scope.booleanvalues = [
                            { name : '<spring:message code="label.no"/>', value : false },
                            { name : '<spring:message code="label.yes"/>', value : true } 
                    ];

    $scope.onDiscoveryAnswerSelect = function(item) {
	    if(item.selected) {
		    if($scope.object.universityDiscoveryMeansAnswers.length < 10) {
		        $scope.object.universityDiscoveryMeansAnswers.push(item.id);
		    } else {
                var indexOfItem = $scope.object.universityDiscoveryMeansAnswerValues.indexOf(item);
                $scope.object.universityDiscoveryMeansAnswerValues[indexOfItem].selected = false;
		    }		
	    } else {
	        var indexOfItem = $scope.object.universityDiscoveryMeansAnswers.indexOf(item.id);
	        $scope.object.universityDiscoveryMeansAnswers.splice(indexOfItem, 1);
	    }
	
        var bool = false;
	    angular.forEach($scope.object.universityDiscoveryMeansAnswers, function(elementId, index) {
 	        if($scope.object.otherUniversityDiscoveryMeansAnswerValues.indexOf(elementId) != -1) {
	            bool = true;
            }
	    }, bool);
	    $scope.object.otherDiscoveryAnswer = bool;
	};
	
	$scope.onChoiceAnswerSelect = function(item) {
	    if(item.selected) {
		    if($scope.object.universityChoiceMotivationAnswers.length < 10) {
			    $scope.object.universityChoiceMotivationAnswers.push(item.id);
		    } else {
			    var indexOfItem = $scope.object.universityChoiceMotivationAnswerValues.indexOf(item);
			    $scope.object.universityChoiceMotivationAnswerValues[indexOfItem].selected = false;
		    }
	    } else {
		    var indexOfItem = $scope.object.universityChoiceMotivationAnswers.indexOf(item.id);
	            $scope.object.universityChoiceMotivationAnswers.splice(indexOfItem, 1);
	    }
	    
	    var bool = false;
	    angular.forEach($scope.object.universityChoiceMotivationAnswers, function(elementId, index) {
		    if($scope.object.otherUniversityChoiceMotivationAnswerValues.indexOf(elementId) != -1) {
			    bool = true;
			}
		}, bool);
		$scope.object.otherChoiceAnswer = bool;
    };
    
    $scope.transformDataToShow = function () {
        angular.forEach($scope.object.universityDiscoveryMeansAnswerValues, function(element, index) {
            if($scope.object.universityDiscoveryMeansAnswers.indexOf(element.id)== -1){
                element.selected = false;
            }else {
           	    element.selected = true;
            }
        });
        angular.forEach($scope.object.universityChoiceMotivationAnswerValues, function(element, index) {
            if($scope.object.universityChoiceMotivationAnswers.indexOf(element.id)== -1){
                element.selected = false;
            }else {
                element.selected = true;
            }
        });
	    
    };
	
	$scope.submitForm = function(event) {
	    $('form').submit();	    
	};
}]);
</script>

<form name='form' method="post" class="form-horizontal" ng-app="angularApp" ng-controller="angularController"
     action="${pageContext.request.contextPath}${controllerURL}/fill">

    <input type="hidden" name="postback"
        value='${pageContext.request.contextPath}${controllerURL}/fillPostback' />
        
    <input name="bean" type="hidden" value="{{ object }}" />
    
	<div class="panel panel-default" ng-init="transformDataToShow()">
		<div class="panel-body">
            <div class="form-group row">
                <div class="col-sm-2 control-label">
                    <spring:message code="label.MotivationsExpectationsForm.universityDiscoveryMeansAnswers" />
                </div>

                <div class="col-sm-10">
                    <ul>
                        <div ng-repeat="discoveryAnswer in object.universityDiscoveryMeansAnswerValues">
                            <li ng-show="discoveryAnswer.parentId == null">
                                <input style="width:15px;height:15px"
                                  type="checkbox"
                                  name="selectedDiscoveryAnswer[]"
                                  ng-model="discoveryAnswer.selected"
                                  ng-change="onDiscoveryAnswerSelect(discoveryAnswer)"
                                >
                                {{ discoveryAnswer.text }}
                            </li>
                            <li ng-show="discoveryAnswer.parentId != null && object.universityDiscoveryMeansAnswers.indexOf(discoveryAnswer.parentId) != -1">
                                <div style="width:25px;float:left;">&nbsp;</div>
                                <input style="width:15px;height:15px"
                                  type="checkbox"
                                  name="selectedDiscoveryAnswer[]"
                                  ng-model="discoveryAnswer.selected"
                                  ng-change="onDiscoveryAnswerSelect(discoveryAnswer)"
                                >
                                {{ discoveryAnswer.text }}
                            </li>
                        </div>
                    </ul>
                </div>
            </div>
			
			<div class="form-group row" ng-show="object.otherDiscoveryAnswer">
				<div class="col-sm-2 control-label">
					<spring:message code="label.MotivationsExpectationsForm.otherUniversityDiscoveryMeans" />
				</div>

				<div class="col-sm-10">
					<input id="motivationsexpectationsform_otherUniversityDiscoveryMeans" class="form-control" type="text" ng-model="object.otherUniversityDiscoveryMeans" name="otherUniversityDiscoveryMeans"
						value='<c:out value='${not empty param.otherUniversityDiscoveryMeans ? param.otherUniversityDiscoveryMeans : motivationsexpectationsform.otherUniversityDiscoveryMeans }'/>' />
				</div>
			</div>
			
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message code="label.MotivationsExpectationsForm.universityChoiceMotivationAnswers" />
				</div>

				<div class="col-sm-10">
                    <ul>
                        <li ng-repeat="choiceAnswer in object.universityChoiceMotivationAnswerValues">
                            <input style="width:15px;height:15px"
                              type="checkbox"
                              name="selectedChoicesAnswer[]"
                              ng-model="choiceAnswer.selected"
                              ng-change="onChoiceAnswerSelect(choiceAnswer)"
                            >
                            {{ choiceAnswer.text }}
                        </li>
                    </ul>
				</div>
			</div>
			
			<div class="form-group row" ng-show="object.otherChoiceAnswer">
				<div class="col-sm-2 control-label">
					<spring:message code="label.MotivationsExpectationsForm.otherUniversityChoiceMotivation" />
				</div>

				<div class="col-sm-10">
					<input id="motivationsexpectationsform_otherUniversityChoiceMotivation" class="form-control" type="text" ng-model="object.otherUniversityChoiceMotivation" name="otherUniversityChoiceMotivation"
						value='<c:out value='${not empty param.otherUniversityChoiceMotivation ? param.otherUniversityChoiceMotivation : motivationsexpectationsform.otherUniversityChoiceMotivation }'/>' />
				</div>
			</div>
		</div>
		<div class="panel-footer">
            <button ng-click="submitForm($event)" class="btn btn-primary" type="button" role="button">
                <spring:message code="label.submit" />
            </button>
		</div>
	</div>
</form>

<style>
ul {
    list-style-type: none;
}
</style>

<script>
$(document).ready(function() {
});
</script>
