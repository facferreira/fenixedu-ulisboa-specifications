<%--

    Copyright © 2002 Instituto Superior Técnico

    This file is part of FenixEdu Academic.

    FenixEdu Academic is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Academic is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@page import="java.math.BigDecimal"%>
<%@page import="org.fenixedu.academic.domain.ExecutionYear"%>
<%@page import="org.fenixedu.academic.domain.student.Registration"%>
<%@page import="org.fenixedu.academic.domain.student.curriculum.ICurriculum"%>
<%@page import="org.fenixedu.academic.dto.student.RegistrationCurriculumBean"%>
<html:xhtml />

<h2><bean:message key="registration.curriculum" bundle="ACADEMIC_OFFICE_RESOURCES"/></h2>

<bean:define id="registrationCurriculumBean" name="registrationCurriculumBean" type="org.fenixedu.academic.dto.student.RegistrationCurriculumBean"/>
<%
	final Registration registration = ((RegistrationCurriculumBean) registrationCurriculumBean).getRegistration();
	request.setAttribute("registration", registration);

	final ICurriculum curriculum = registrationCurriculumBean.getCurriculum();
	request.setAttribute("curriculum", curriculum);
	request.setAttribute("sumEctsCredits", curriculum.getSumEctsCredits());
	request.setAttribute("rawGrade", curriculum.getRawGrade());

    // qubExtension, only curricular year is searched at the beginning of the year
	final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
	request.setAttribute("currentExecutionYear", currentExecutionYear);
	final Integer curricularYear = registrationCurriculumBean.getCurriculum(currentExecutionYear).getCurricularYear();
	request.setAttribute("curricularYear", curricularYear);
%>

<%-- Person and Student short info --%>
<p class="mvert2">
	<span class="showpersonid">
	<bean:message key="label.student" bundle="ACADEMIC_OFFICE_RESOURCES"/>: 
		<fr:view name="registration" property="student" schema="student.show.personAndStudentInformation.short">
			<fr:layout name="flow">
				<fr:property name="labelExcluded" value="true"/>
			</fr:layout>
		</fr:view>
	</span>
</p>

<logic:equal name="curriculum" property="empty" value="true">
	<p class="mvert15">
		<em>
			<bean:message key="no.approvements" bundle="ACADEMIC_OFFICE_RESOURCES"/>
		</em>
	</p>	
</logic:equal>

<logic:equal name="curriculum" property="empty" value="false">
		
	<logic:equal name="registrationCurriculumBean" property="conclusionProcessed" value="false">

		<logic:equal name="registrationCurriculumBean" property="concluded" value="true">
			<div class="infoop2 mvert2">
				<p class="mvert05"><span class="error0"><strong><bean:message key="missing.final.average.info" bundle="ACADEMIC_OFFICE_RESOURCES"/></strong></span></p>
			</div>
		</logic:equal>

	</logic:equal>

    <table class="tstyle4 thlight tdcenter mtop15">
        <tr>
            <th><bean:message key="label.numberAprovedCurricularCourses" bundle="ACADEMIC_OFFICE_RESOURCES"/></th>
            <th><bean:message key="label.total.ects.credits" bundle="ACADEMIC_OFFICE_RESOURCES"/></th>
            <th><bean:message key="average" bundle="STUDENT_RESOURCES"/></th>
            <logic:equal name="registrationCurriculumBean" property="conclusionProcessed" value="false">
	            <th><bean:message key="label.curricular.year" bundle="STUDENT_RESOURCES"/></th>
            </logic:equal>
            <logic:equal name="registrationCurriculumBean" property="conclusionProcessed" value="true">
	            <th><bean:message key="label.conclusionDate" bundle="ACADEMIC_OFFICE_RESOURCES"/></th>
	            <th><bean:message key="label.conclusionYear" bundle="ACADEMIC_OFFICE_RESOURCES"/></th>
            </logic:equal>
        </tr>
        <tr>
            <bean:size id="curricularEntriesCount" name="curriculum" property="curriculumEntries"/>
            <td><bean:write name="curricularEntriesCount"/></td>
            <td><bean:write name="sumEctsCredits"/></td>
            <logic:equal name="registrationCurriculumBean" property="conclusionProcessed" value="false">
                <td><bean:write name="rawGrade" property="value"/></td>
                <td><bean:message bundle="FENIXEDU_ULISBOA_SPECIFICATIONS_RESOURCES" key="label.curricularYear.begin.executionYear" arg0="<%=currentExecutionYear.getQualifiedName()%>" arg1="<%=String.valueOf(curricularYear)%>"/></td>
            </logic:equal>
            <logic:equal name="registrationCurriculumBean" property="conclusionProcessed" value="true">
                <td><bean:write name="registrationCurriculumBean" property="finalGrade.value"/></td>
                <td><bean:write name="registrationCurriculumBean" property="conclusionDate"/></td>
                <td><bean:write name="registrationCurriculumBean" property="conclusionYear.qualifiedName"/></td>
            </logic:equal>          
        </tr>
    </table>

		<p>
			<fr:view name="curriculum"/>
		</p>

        <%-- Extension 
        <p>
            <jsp:include page="/academicAdminOffice/student/registration/curriculumGradeCalculator.jsp" /> 
        </p>                
        --%>
	
</logic:equal>
