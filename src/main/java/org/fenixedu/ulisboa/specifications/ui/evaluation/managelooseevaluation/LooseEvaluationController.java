/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: anil.mamede@qub-it.com
 * Contributors: luis.egidio@qub-it.com
 *
 * 
 * This file is part of FenixEdu Specifications.
 *
 * FenixEdu Specifications is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Specifications is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Specifications.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.ulisboa.specifications.ui.evaluation.managelooseevaluation;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.bennu.TupleDataSourceBean;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.ulisboa.specifications.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.ulisboa.specifications.domain.services.enrollment.EnrolmentServices;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsBaseController;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsController;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.Sets;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixframework.Atomic;

@Component("org.fenixedu.ulisboa.specifications.ui.evaluation.managelooseevaluation")
@SpringFunctionality(app = FenixeduUlisboaSpecificationsController.class, title = "label.LooseEvaluationBean",
        accessGroup = "academic(MANAGE_MARKSHEETS)")
@RequestMapping(LooseEvaluationController.CONTROLLER_URL)
public class LooseEvaluationController extends FenixeduUlisboaSpecificationsBaseController {

    public static final String CONTROLLER_URL = "/fenixedu-ulisboa-specifications/evaluation/managelooseevaluation";

    private static final String JSP_PATH = CONTROLLER_URL.substring(1);

    private String jspPage(final String page) {
        return JSP_PATH + "/" + page;
    }

    @Autowired
    private HttpSession session;

    @Autowired
    private HttpServletRequest request;

    private static final String _CREATE_URI = "/create/";
    public static final String CREATE_URL = CONTROLLER_URL + _CREATE_URI;

    @RequestMapping(value = _CREATE_URI + "{scpId}/{executionSemesterId}", method = RequestMethod.GET)
    public String create(@PathVariable("scpId") final StudentCurricularPlan studentCurricularPlan,
            @PathVariable("executionSemesterId") final ExecutionSemester executionSemester, final Model model) {

        model.addAttribute("studentCurricularPlan", studentCurricularPlan);
        model.addAttribute("LooseEvaluationBean_enrolment_options",
                studentCurricularPlan.getEnrolmentsSet().stream().filter(e -> e.getExecutionPeriod() == executionSemester)
                        .sorted((x, y) -> x.getName().getContent().compareTo(y.getName().getContent()))
                        .collect(Collectors.toList()));
        model.addAttribute("typeValues", EvaluationSeasonServices.findByActive(true)
                .sorted(EvaluationSeasonServices.SEASON_ORDER_COMPARATOR).collect(Collectors.toList()));
        model.addAttribute("gradeScaleValues",
                Arrays.<GradeScale> asList(GradeScale.values()).stream()
                        .map(l -> new TupleDataSourceBean(((GradeScale) l).name(), ((GradeScale) l).getDescription()))
                        .collect(Collectors.<TupleDataSourceBean> toList()));

        model.addAttribute("improvementSemesterValues", ExecutionSemester.readNotClosedPublicExecutionPeriods().stream()
                .sorted(ExecutionSemester.COMPARATOR_BY_BEGIN_DATE.reversed()).collect(Collectors.toList()));

        model.addAttribute("executionSemester", executionSemester);

        final String url = String.format("/academicAdministration/studentEnrolments.do?scpID=%s&method=prepare",
                studentCurricularPlan.getExternalId());

        String backUrl = GenericChecksumRewriter.injectChecksumInUrl(request.getContextPath(), url, session);
        model.addAttribute("backUrl", backUrl);

        final List<EnrolmentEvaluation> evaluations =
                studentCurricularPlan.getEnrolmentsSet().stream().filter(e -> e.getExecutionPeriod() == executionSemester)
                        .map(l -> l.getEvaluationsSet()).reduce((a, c) -> Sets.union(a, c)).orElse(Sets.newHashSet()).stream()
                        .filter(l -> l.getMarkSheet() == null).collect(Collectors.toList());

        model.addAttribute("evaluationsSet", evaluations);

        return jspPage("create");
    }

    @RequestMapping(value = _CREATE_URI + "{scpId}/{executionSemesterId}", method = RequestMethod.POST)
    public String create(@PathVariable("scpId") final StudentCurricularPlan studentCurricularPlan,
            @PathVariable("executionSemesterId") final ExecutionSemester executionSemester,
            @RequestParam(value = "enrolment", required = false) Enrolment enrolment,
            @RequestParam(value = "availabledate",
                    required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate availableDate,
            @RequestParam(value = "examdate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate examDate,
            @RequestParam(value = "gradescale", required = false) GradeScale gradeScale,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "type", required = false) EvaluationSeason type,
            @RequestParam(value = "improvementsemester", required = false) ExecutionSemester improvementSemester, Model model,
            final RedirectAttributes redirectAttributes) {

        try {

            if (!checkIfAllGradesAreSameScale(enrolment, gradeScale)) {
                addErrorMessage(ULisboaSpecificationsUtil.bundle("error.LooseEvaluationBean.grade.not.same.scale"), model);
                return create(studentCurricularPlan, executionSemester, model);
            }

            createLooseEvaluation(enrolment, examDate, Grade.createGrade(grade, gradeScale), type, improvementSemester);
            return redirect(CREATE_URL + studentCurricularPlan.getExternalId() + "/" + executionSemester.getExternalId(), model,
                    redirectAttributes);
        } catch (final DomainException e) {
            addErrorMessage(e.getLocalizedMessage(), model);
            return create(studentCurricularPlan, executionSemester, model);
        }
    }

    private boolean checkIfAllGradesAreSameScale(Enrolment enrolment, GradeScale gradeScale) {
        boolean result = true;
        for (final EnrolmentEvaluation enrolmentEvaluation : enrolment.getEvaluationsSet()) {
            result &= enrolmentEvaluation.getGradeScale() == gradeScale;
        }

        return result;
    }

    @Atomic
    public void createLooseEvaluation(Enrolment enrolment, LocalDate examDate, Grade grade, EvaluationSeason type,
            ExecutionSemester improvementSemester) {

        final EnrolmentEvaluation evaluation = new EnrolmentEvaluation(enrolment, type);
        if (type.isImprovement()) {
            evaluation.setExecutionPeriod(improvementSemester);
        }

        evaluation.edit(Authenticate.getUser().getPerson(), grade, new Date(), examDate.toDateTimeAtStartOfDay().toDate());
        evaluation.confirmSubmission(Authenticate.getUser().getPerson(), "");
        EnrolmentServices.updateState(enrolment);
    }

    private static final String _DELETE_URI = "/delete/";
    public static final String DELETE_URL = CONTROLLER_URL + _DELETE_URI;

    @RequestMapping(value = _DELETE_URI + "{scpId}/{evaluationId}/{executionSemesterId}", method = RequestMethod.POST)
    public String delete(@PathVariable("scpId") final StudentCurricularPlan studentCurricularPlan,
            @PathVariable("evaluationId") EnrolmentEvaluation enrolmentEvaluation,
            @PathVariable("executionSemesterId") final ExecutionSemester executionSemester, Model model,
            final RedirectAttributes redirectAttributes) {

        try {
            deleteEnrolment(enrolmentEvaluation);
        } catch (final DomainException e) {
            addErrorMessage(e.getLocalizedMessage(), model);
        }

        return redirect(CREATE_URL + studentCurricularPlan.getExternalId() + "/" + executionSemester.getExternalId(), model,
                redirectAttributes);
    }

    @Atomic
    private void deleteEnrolment(EnrolmentEvaluation enrolmentEvaluation) {
        enrolmentEvaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.TEMPORARY_OBJ);
        enrolmentEvaluation.delete();
    }

}