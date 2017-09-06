package org.fenixedu.ulisboa.specifications.ui.reports.registrationhistory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.spreadsheet.SheetData;
import org.fenixedu.commons.spreadsheet.SpreadsheetBuilderForXLSX;
import org.fenixedu.ulisboa.specifications.domain.CompetenceCourseServices;
import org.fenixedu.ulisboa.specifications.domain.evaluation.EvaluationComparator;
import org.fenixedu.ulisboa.specifications.domain.evaluation.season.EvaluationSeasonServices;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;
import org.fenixedu.ulisboa.specifications.domain.file.ULisboaSpecificationsTemporaryFile;
import org.fenixedu.ulisboa.specifications.domain.services.CurricularPeriodServices;
import org.fenixedu.ulisboa.specifications.domain.services.RegistrationServices;
import org.fenixedu.ulisboa.specifications.domain.services.enrollment.EnrolmentServices;
import org.fenixedu.ulisboa.specifications.dto.report.registrationhistory.RegistrationHistoryReportParametersBean;
import org.fenixedu.ulisboa.specifications.service.report.registrationhistory.RegistrationHistoryReport;
import org.fenixedu.ulisboa.specifications.service.report.registrationhistory.RegistrationHistoryReportService;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsBaseController;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsController;
import org.fenixedu.ulisboa.specifications.ui.registrationsdgesexport.RegistrationDGESStateBeanController;
import org.fenixedu.ulisboa.specifications.ui.registrationsdgesexport.RegistrationDGESStateBeanController.RegistrationDGESStateBean;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.opensagres.xdocreport.core.io.internal.ByteArrayOutputStream;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@SpringFunctionality(app = FenixeduUlisboaSpecificationsController.class, title = "label.title.registrationHistoryReport")
@RequestMapping(RegistrationHistoryReportController.CONTROLLER_URL)
public class RegistrationHistoryReportController extends FenixeduUlisboaSpecificationsBaseController {

    public static final String CONTROLLER_URL =
            "/fenixedu-ulisboa-specifications/reports/registrationhistory/registrationhistoryreport";

    private static final String JSP_PATH = CONTROLLER_URL.substring(1);

    private void setParametersBean(RegistrationHistoryReportParametersBean bean, Model model) {
        model.addAttribute("beanJson", getBeanJson(bean));
        model.addAttribute("bean", bean);
    }

    @RequestMapping
    public String home(Model model, RedirectAttributes redirectAttributes) {
        return redirect(CONTROLLER_URL + "/search", model, redirectAttributes);
    }

    @RequestMapping(value = "/search")
    public String search(Model model, RedirectAttributes redirectAttributes) {
        setParametersBean(new RegistrationHistoryReportParametersBean(), model);
        return jspPage("registrationhistoryreport");
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String search(@RequestParam("bean") RegistrationHistoryReportParametersBean bean, Model model,
            RedirectAttributes redirectAttributes) {
        setParametersBean(bean, model);

        setResults(generateReport(bean, !bean.getGraduatedExecutionYears().isEmpty()), model);

        return jspPage("registrationhistoryreport");
    }

    private void setResults(Collection<RegistrationHistoryReport> results, Model model) {
        model.addAttribute("results", results);
    }

    @RequestMapping(value = "/exportregistrations", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportRegistrations(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        final String reportId = UUID.randomUUID().toString();
        new Thread(() -> processReport(this::exportRegistrationsToXLS, bean, reportId)).start();

        return new ResponseEntity<String>(reportId, HttpStatus.OK);
    }

    @Atomic(mode = TxMode.READ)
    protected void processReport(final Function<RegistrationHistoryReportParametersBean, byte[]> reportProcessor,
            final RegistrationHistoryReportParametersBean bean, final String reportId) {

        byte[] content = null;
        try {
            content = reportProcessor.apply(bean);
        } catch (Throwable e) {
            content = createXLSWithError(
                    e instanceof ULisboaSpecificationsDomainException ? ((ULisboaSpecificationsDomainException) e)
                            .getLocalizedMessage() : ExceptionUtils.getFullStackTrace(e));
        }

        ULisboaSpecificationsTemporaryFile.create(reportId, content, Authenticate.getUser());
    }

    @RequestMapping(value = "/exportstatus/{reportId}", method = RequestMethod.GET, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportStatus(@PathVariable(value = "reportId") final String reportId,
            final Model model) {
        return new ResponseEntity<String>(
                String.valueOf(
                        ULisboaSpecificationsTemporaryFile.findByUserAndFilename(Authenticate.getUser(), reportId).isPresent()),
                HttpStatus.OK);
    }

    @RequestMapping(value = "/downloadreport/{reportId}", method = RequestMethod.GET)
    public void downloadReport(@PathVariable("reportId") String reportId, final Model model,
            RedirectAttributes redirectAttributes, HttpServletResponse response) throws IOException {
        final Optional<ULisboaSpecificationsTemporaryFile> temporaryFile =
                ULisboaSpecificationsTemporaryFile.findByUserAndFilename(Authenticate.getUser(), reportId);
        writeFile(response, "Report_" + new DateTime().toString("yyyy-MM-dd_HH-mm-ss") + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", temporaryFile.get().getContent());
    }

    static private Collection<RegistrationHistoryReport> generateReport(final RegistrationHistoryReportParametersBean bean,
            final boolean detailed) {

        final RegistrationHistoryReportService service = new RegistrationHistoryReportService();
        service.filterEnrolmentExecutionYears(bean.getExecutionYears());
        service.filterDegrees(bean.getDegrees());
        service.filterDegreeTypes(bean.getDegreeTypes());
        service.filterIngressionTypes(bean.getIngressionTypes());
        service.filterRegimeTypes(bean.getRegimeTypes());
        service.filterRegistrationProtocols(bean.getRegistrationProtocols());
        service.filterRegistrationStateTypes(bean.getRegistrationStateTypes());
        service.filterStatuteTypes(bean.getStatuteTypes());
        service.filterFirstTimeOnly(bean.getFirstTimeOnly());
        service.filterWithEnrolments(bean.getFilterWithEnrolments());
        service.filterDismissalsOnly(bean.getDismissalsOnly());
        service.filterImprovementEnrolmentsOnly(bean.getImprovementEnrolmentsOnly());
        service.filterStudentNumber(bean.getStudentNumber());
        service.setDetailed(detailed);

        service.filterGraduatedExecutionYears(bean.getGraduatedExecutionYears());
        service.filterGraduationPeriodStartDate(bean.getGraduationPeriodStartDate());
        service.filterGraduationPeriodEndDate(bean.getGraduationPeriodEndDate());
        service.filterProgramConclusions(bean.getProgramConclusions());

        final Comparator<RegistrationHistoryReport> byYear =
                (x, y) -> ExecutionYear.COMPARATOR_BY_BEGIN_DATE.compare(x.getExecutionYear(), y.getExecutionYear());
        final Comparator<RegistrationHistoryReport> byDegreeType =
                (x, y) -> x.getRegistration().getDegreeType().compareTo(y.getRegistration().getDegreeType());
        final Comparator<RegistrationHistoryReport> byDegree =
                (x, y) -> Degree.COMPARATOR_BY_NAME.compare(x.getRegistration().getDegree(), y.getRegistration().getDegree());
        final Comparator<RegistrationHistoryReport> byDegreeCurricularPlan =
                (x, y) -> x.getDegreeCurricularPlan().getName().compareTo(y.getDegreeCurricularPlan().getName());

        return service.generateReport().stream()
                .sorted(byYear.thenComparing(byDegreeType).thenComparing(byDegree).thenComparing(byDegreeCurricularPlan))
                .collect(Collectors.toList());
    }

    private byte[] exportRegistrationsToXLS(final RegistrationHistoryReportParametersBean bean) {

        final Collection<RegistrationHistoryReport> toExport = generateReport(bean, true);

        final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.students"),
                new SheetData<RegistrationHistoryReport>(toExport) {

                    @Override
                    protected void makeLine(final RegistrationHistoryReport report) {
                        addPrimaryData(report);
                        addConclusionData(bean, report);
                        addQualificationAndOriginInfo(bean, report);
                        addPersonalData(bean, report);
                        addContactsData(bean, report);
                    }

                    private void addPrimaryData(final RegistrationHistoryReport report) {
                        final Registration registration = report.getRegistration();
                        final Person person = registration.getPerson();
                        final Degree degree = registration.getDegree();

                        addData("RegistrationHistoryReport.executionYear", report.getExecutionYear().getQualifiedName());
                        addData("Student.number", registration.getStudent().getNumber().toString());
                        addData("Registration.number", registration.getNumber().toString());
                        addData("Person.username", person.getUsername());
                        addData("Person.name", person.getName());
                        addData("Degree.code", degree.getCode());
                        addData("Degree.ministryCode", degree.getMinistryCode());
                        addData("Degree.degreeType", degree.getDegreeType().getName());
                        addData("Degree.presentationName", degree.getPresentationNameI18N().getContent());
                        addData("Registration.ingressionType", registration.getIngressionType().getDescription().getContent());
                        addData("Registration.registrationProtocol",
                                registration.getRegistrationProtocol().getDescription().getContent());
                        addData("Registration.startDate", registration.getStartDate());

                        final RegistrationState firstState = registration.getFirstRegistrationState();
                        addData("Registration.firstStateDate", firstState != null ? firstState.getStateDate().toLocalDate() : "");
                        addData("Registration.registrationYear", registration.getRegistrationYear().getQualifiedName());
                        addData("RegistrationHistoryReport.studentCurricularPlan", report.getStudentCurricularPlan().getName());
                        addData("RegistrationHistoryReport.isReingression", booleanString(report.isReingression()));
                        addData("RegistrationHistoryReport.hasPreviousReingression",
                                booleanString(report.hasPreviousReingression()));
                        addData("RegistrationHistoryReport.curricularYear", report.getCurricularYear().toString());

                        final Integer previousYearCurricularYear = report.getPreviousYearCurricularYear();
                        addData("RegistrationHistoryReport.previousYearCurricularYear",
                                previousYearCurricularYear != null ? previousYearCurricularYear.toString() : "");

                        addData("RegistrationHistoryReport.ectsCredits", report.getEctsCredits());
                        addData("RegistrationHistoryReport.average",
                                report.getAverage() != null ? report.getAverage().getValue() : null);
                        addData("RegistrationHistoryReport.currentAverage",
                                report.getCurrentAverage() != null ? report.getCurrentAverage().toPlainString() : null);
                        addData("RegistrationHistoryReport.enrolmentYears", report.getEnrolmentYears().toString());
                        final BigDecimal enrolmentYearsForPrescription = report.getEnrolmentYearsForPrescription();
                        addData("RegistrationHistoryReport.enrolmentYearsForPrescription",
                                enrolmentYearsForPrescription == null ? "-" : enrolmentYearsForPrescription.toString());

                        addData("RegistrationHistoryReport.enrolmentDate", report.getEnrolmentDate());

                        final ExecutionYear lastEnrolmentExecutionYear = registration.getLastEnrolmentExecutionYear();
                        addData("Registration.lastEnrolmentExecutionYear",
                                lastEnrolmentExecutionYear != null ? lastEnrolmentExecutionYear.getQualifiedName() : "");

                        addData("RegistrationHistoryReport.primaryBranch", report.getPrimaryBranchName());
                        addData("RegistrationHistoryReport.secondaryBranch", report.getSecondaryBranchName());
                        addData("RegistrationHistoryReport.statutes", report.getStudentStatutesNames());
                        addData("RegistrationHistoryReport.regimeType", report.getRegimeType().getLocalizedName());
                        addData("RegistrationHistoryReport.enrolmentsWithoutShifts",
                                booleanString(report.hasEnrolmentsWithoutShifts()));
                        addData("RegistrationHistoryReport.inactiveRegistrationStateForYear",
                                booleanString(report.hasAnyInactiveRegistrationStateForYear()));

                        final RegistrationState lastRegistrationState = report.getLastRegistrationState();
                        addData("RegistrationHistoryReport.lastRegistrationState",
                                lastRegistrationState != null ? lastRegistrationState.getStateType().getDescription() : null);
                        addData("RegistrationHistoryReport.lastRegistrationStateDate",
                                lastRegistrationState != null ? lastRegistrationState.getStateDate().toLocalDate() : null);
                        addData("RegistrationHistoryReport.firstTime", booleanString(report.isFirstTime()));
                        addData("RegistrationHistoryReport.dismissals", booleanString(report.hasDismissals()));
                        addData("RegistrationHistoryReport.enroledInImprovement",
                                booleanString(report.hasImprovementEvaluations()));
                        addData("RegistrationHistoryReport.annulledEnrolments", booleanString(report.hasAnnulledEnrolments()));
                        addData("RegistrationHistoryReport.enrolmentsCount", report.getEnrolmentsCount());
                        addData("RegistrationHistoryReport.enrolmentsCredits", report.getEnrolmentsCredits());
                        addData("RegistrationHistoryReport.extraCurricularEnrolmentsCount",
                                report.getExtraCurricularEnrolmentsCount());
                        addData("RegistrationHistoryReport.extraCurricularEnrolmentsCredits",
                                report.getExtraCurricularEnrolmentsCredits());
                        addData("RegistrationHistoryReport.standaloneEnrolmentsCount", report.getStandaloneEnrolmentsCount());
                        addData("RegistrationHistoryReport.standaloneEnrolmentsCredits", report.getStandaloneEnrolmentsCredits());
                        addData("RegistrationHistoryReport.executionYearSimpleAverage", report.getExecutionYearSimpleAverage());
                        addData("RegistrationHistoryReport.executionYearWeightedAverage",
                                report.getExecutionYearWeightedAverage());

                        addData("RegistrationHistoryReport.tuitionCharged", booleanString(report.isTuitionCharged()));
                        addData("RegistrationHistoryReport.tuitionAmount", report.getTuitionAmount().toPlainString());
                        addData("Registration.registrationObservations",
                                registration.getRegistrationObservationsSet().stream()
                                        .map(o -> o.getVersioningUpdatedBy().getUsername() + ":" + o.getValue())
                                        .collect(Collectors.joining(" \n --------------\n ")));
                    }

                    private void addConclusionData(final RegistrationHistoryReportParametersBean parametersBean,
                            final RegistrationHistoryReport report) {

                        if (parametersBean.getExportConclusionData()) {

                            //TODO: program conclusions should already be sorted
                            final List<ProgramConclusion> sortedProgramConclusions = report.getProgramConclusions().stream()
                                    .sorted(Comparator.comparing(ProgramConclusion::getName)
                                            .thenComparing(ProgramConclusion::getDescription)
                                            .thenComparing(ProgramConclusion::getExternalId))
                                    .collect(Collectors.toList());

                            for (final ProgramConclusion programConclusion : sortedProgramConclusions) {

                                final RegistrationConclusionBean bean = report.getConclusionReportFor(programConclusion);

                                final String concluded = bean == null ? null : booleanString(bean.isConcluded());
                                addCell(labelFor(programConclusion, "concluded"), concluded);

                                final String conclusionProcessed =
                                        bean == null ? null : booleanString(bean.isConclusionProcessed());
                                addCell(labelFor(programConclusion, "conclusionProcessed"), conclusionProcessed);

                                final String rawGrade =
                                        bean == null || bean.getRawGrade() == null ? null : bean.getRawGrade().getValue();
                                addCell(labelFor(programConclusion, "rawGrade"), rawGrade);

                                final String finalGrade =
                                        bean == null || bean.getFinalGrade() == null ? null : bean.getFinalGrade().getValue();
                                addCell(labelFor(programConclusion, "finalGrade"), finalGrade);

                                final String descriptiveGrade = bean == null || bean.getDescriptiveGrade() == null ? null : bean
                                        .getDescriptiveGradeExtendedValue() + " (" + bean.getDescriptiveGrade().getValue() + ")";
                                addCell(labelFor(programConclusion, "descriptiveGrade"), descriptiveGrade);

                                final YearMonthDay conclusionDate =
                                        bean == null || bean.getConclusionDate() == null ? null : bean.getConclusionDate();
                                addCell(labelFor(programConclusion, "conclusionDate"), conclusionDate);

                                final String conclusionYear = bean == null || bean.getConclusionYear() == null ? null : bean
                                        .getConclusionYear().getQualifiedName();
                                addCell(labelFor(programConclusion, "conclusionYear"), conclusionYear);

                                final String ectsCredits = bean == null ? null : String.valueOf(bean.getEctsCredits());
                                addCell(labelFor(programConclusion, "ectsCredits"), ectsCredits);

                            }

                            addData("RegistrationHistoryReport.otherConcludedRegistrationYears",
                                    report.getOtherConcludedRegistrationYears());
                        }
                    }

                    private String labelFor(ProgramConclusion programConclusion, String field) {
                        final String programConclusionPrefix = programConclusion.getName().getContent() + " - "
                                + programConclusion.getDescription().getContent() + ": ";

                        return programConclusionPrefix + bundle("label.RegistrationConclusionBean." + field);
                    }

                    private void addQualificationAndOriginInfo(final RegistrationHistoryReportParametersBean bean,
                            final RegistrationHistoryReport report) {

                        if (bean.getExportQualificationAndOriginInfo()) {

                            addData("PrecedentDegreeInformation.institutionUnit", report.getQualificationInstitutionName());
                            addData("PrecedentDegreeInformation.schoolLevel", report.getQualificationSchoolLevel());
                            addData("PrecedentDegreeInformation.degreeDesignation", report.getQualificationDegreeDesignation());
                            addData("PrecedentDegreeInformation.precedentInstitution", report.getOriginInstitutionName());
                            addData("PrecedentDegreeInformation.precedentSchoolLevel", report.getOriginSchoolLevel());
                            addData("PrecedentDegreeInformation.precedentDegreeDesignation", report.getOriginDegreeDesignation());
                        }
                    }

                    private void addPersonalData(final RegistrationHistoryReportParametersBean bean,
                            final RegistrationHistoryReport report) {

                        if (bean.getExportPersonalInfo()) {

                            addData("Person.idDocumentType", report.getIdDocumentType());
                            addData("Person.idDocumentNumber", report.getDocumentIdNumber());
                            addData("Person.gender", report.getGender());
                            addData("Person.dateOfBirth", report.getDateOfBirthYearMonthDay());
                            addData("Person.nameOfFather", report.getNameOfFather());
                            addData("Person.nameOfMother", report.getNameOfMother());
                            addData("Person.nationality", report.getNationality());
                            addData("Person.countryOfBirth", report.getCountryOfBirth());
                            addData("Person.socialSecurityNumber", report.getFiscalNumber());
                            addData("Person.districtOfBirth", report.getDistrictOfBirth());
                            addData("Person.districtSubdivisionOfBirth", report.getDistrictSubdivisionOfBirth());
                            addData("Person.parishOfBirth", report.getParishOfBirth());
                            addData("Student.studentPersonalDataAuthorizationChoice",
                                    report.getStudentPersonalDataAuthorizationChoice());
                        }
                    }

                    private void addContactsData(final RegistrationHistoryReportParametersBean bean,
                            final RegistrationHistoryReport report) {

                        if (bean.getExportContacts()) {

                            addData("Person.defaultEmailAddress", report.getDefaultEmailAddressValue());
                            addData("Person.institutionalEmailAddress", report.getInstitutionalEmailAddressValue());
                            addData("Person.otherEmailAddresses", report.getOtherEmailAddresses());
                            addData("Person.defaultPhone", report.getDefaultPhoneNumber());
                            addData("Person.defaultMobilePhone", report.getDefaultMobilePhoneNumber());

                            if (report.hasDefaultPhysicalAddress()) {
                                addData("PhysicalAddress.address", report.getDefaultPhysicalAddress());
                                addData("PhysicalAddress.districtOfResidence",
                                        report.getDefaultPhysicalAddressDistrictOfResidence());
                                addData("PhysicalAddress.districtSubdivisionOfResidence",
                                        report.getDefaultPhysicalAddressDistrictSubdivisionOfResidence());
                                addData("PhysicalAddress.parishOfResidence", report.getDefaultPhysicalAddressParishOfResidence());
                                addData("PhysicalAddress.area", report.getDefaultPhysicalAddressArea());
                                addData("PhysicalAddress.areaCode", report.getDefaultPhysicalAddressAreaCode());
                                addData("PhysicalAddress.areaOfAreaCode", report.getDefaultPhysicalAddressAreaOfAreaCode());
                                addData("PhysicalAddress.countryOfResidence",
                                        report.getDefaultPhysicalAddressCountryOfResidenceName());
                            }
                        }
                    }

                    private void addData(String bundleKey, Object value) {
                        addCell(bundle("label." + bundleKey), value == null ? "" : value);
                    }

                    private String booleanString(boolean value) {
                        return value ? bundle("label.yes") : bundle("label.no");
                    }

                    private String bundle(String key) {
                        return ULisboaSpecificationsUtil.bundle(key);
                    }

                });

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            builder.build(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();

    }

    private byte[] createXLSWithError(String error) {

        try {

            final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
            builder.addSheet("Registrations", new SheetData<String>(Collections.singleton(error)) {
                @Override
                protected void makeLine(String item) {
                    addCell(ULisboaSpecificationsUtil.bundle("label.unexpected.error.occured"), item);
                }
            });

            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            builder.build(result);

            return result.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String _POSTBACK_URI = "/postback";
    public static final String POSTBACK_URL = CONTROLLER_URL + _POSTBACK_URI;

    @RequestMapping(value = _POSTBACK_URI, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> postback(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        bean.updateData();

        return new ResponseEntity<String>(getBeanJson(bean), HttpStatus.OK);
    }

    private String jspPage(final String page) {
        return JSP_PATH + "/" + page;
    }

    @RequestMapping(value = "/exportapprovals", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportApprovals(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        final String reportId = UUID.randomUUID().toString();
        new Thread(() -> processReport(this::exportApprovalsToXLS, bean, reportId)).start();

        return new ResponseEntity<String>(reportId, HttpStatus.OK);
    }

    private byte[] exportApprovalsToXLS(RegistrationHistoryReportParametersBean bean) {

        final Collection<RegistrationHistoryReport> reports = generateReport(bean, false);

        final Collection<ICurriculum> curriculums =
                reports.stream().map(r -> RegistrationServices.getCurriculum(r.getRegistration(), (ExecutionYear) null))
                        .sorted((x, y) -> x.getStudentCurricularPlan().getRegistration().getNumber()
                                .compareTo(x.getStudentCurricularPlan().getRegistration().getNumber()))
                        .distinct().collect(Collectors.toList());

        final Multimap<ICurriculum, ICurriculumEntry> approvalsByCurriculum = HashMultimap.create();
        curriculums.stream().forEach(c -> approvalsByCurriculum.putAll(c, c.getCurriculumEntries()));

        final ExecutionYear executionYearForCurricularYear =
                bean.getExecutionYears().stream().max(ExecutionYear.COMPARATOR_BY_BEGIN_DATE).get();

        final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.approvals"),
                new SheetData<Map.Entry<ICurriculum, ICurriculumEntry>>(approvalsByCurriculum.entries()) {

                    @Override
                    protected void makeLine(Entry<ICurriculum, ICurriculumEntry> entry) {
                        final Registration registration = entry.getKey().getStudentCurricularPlan().getRegistration();
                        final ICurriculumEntry curriculumEntry = entry.getValue();

                        addData("Student.number", registration.getStudent().getNumber());
                        addData("Registration.number", registration.getNumber().toString());
                        addData("Person.name", registration.getStudent().getPerson().getName());
                        addData("Degree.code", registration.getDegree().getCode());
                        addData("Degree.presentationName", registration.getDegree().getPresentationNameI18N().getContent());
                        addData("RegistrationHistoryReport.curricularYear",
                                RegistrationServices.getCurricularYear(registration, executionYearForCurricularYear).getResult());
                        addData("ICurriculumEntry.code", curriculumEntry.getCode());
                        addData("ICurriculumEntry.name", curriculumEntry.getPresentationName().getContent());
                        addData("ICurriculumEntry.grade", curriculumEntry.getGradeValue());
                        addData("ICurriculumEntry.ectsCreditsForCurriculum", curriculumEntry.getEctsCreditsForCurriculum());
                        addData("ICurriculumEntry.executionPeriod", curriculumEntry.getExecutionPeriod().getQualifiedName());
                        addData("creationDate", curriculumEntry.getCreationDateDateTime().toString("yyyy-MM-dd HH:mm"));
                        addData("ICurriculumEntry.dismissal", ULisboaSpecificationsUtil
                                .bundle(isDismissal(entry.getKey(), entry.getValue()) ? "label.yes" : "label.no"));
                        addData("ICurriculumEntry.curricularYear", getCurricularYear(entry.getKey(), curriculumEntry));
                        addData("ICurriculumEntry.curricularSemester", getCurricularSemester(entry.getKey(), curriculumEntry));
                        addData("ICurriculumEntry.groupPath", getGroupPath(entry.getKey(), curriculumEntry));
                        addData("Curriculum.totalApprovals", entry.getKey().getCurriculumEntries().size());
                        final OptionalDouble average =
                                entry.getKey().getCurriculumEntries().stream().filter(e -> e.getGrade().isNumeric())
                                        .map(e -> e.getGrade().getNumericValue()).mapToDouble(v -> v.doubleValue()).average();
                        addData("Curriculum.simpleAverage", average.isPresent() ? average.getAsDouble() : null);

                    }

                    private boolean isDismissal(ICurriculum curriculum, ICurriculumEntry entry) {
                        return ((Curriculum) curriculum).getDismissalRelatedEntries().contains(entry);
                    }

                    private Integer getCurricularSemester(ICurriculum curriculum, ICurriculumEntry entry) {
                        return belongsToStudentCurricularPlan(curriculum, entry) ? CurricularPeriodServices
                                .getCurricularSemester((CurriculumLine) entry) : null;
                    }

                    private Integer getCurricularYear(ICurriculum curriculum, ICurriculumEntry entry) {
                        return belongsToStudentCurricularPlan(curriculum, entry) ? CurricularPeriodServices
                                .getCurricularYear((CurriculumLine) entry) : null;
                    }

                    private String getGroupPath(ICurriculum curriculum, ICurriculumEntry entry) {
                        return belongsToStudentCurricularPlan(curriculum, entry) ? ((CurriculumLine) entry).getCurriculumGroup()
                                .getFullPath() : null;

                    }

                    protected boolean belongsToStudentCurricularPlan(ICurriculum curriculum, ICurriculumEntry entry) {
                        return entry instanceof Dismissal || entry instanceof Enrolment
                                && ((Enrolment) entry).getStudentCurricularPlan() == curriculum.getStudentCurricularPlan();
                    }

                    private void addData(String key, Object data) {
                        addCell(ULisboaSpecificationsUtil.bundle("label." + key), data);
                    }

                });

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            builder.build(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();
    }

    @RequestMapping(value = "/exportenrolments", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportEnrolments(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        final String reportId = UUID.randomUUID().toString();
        new Thread(() -> processReport(this::exportEnrolmentsToXLS, bean, reportId)).start();

        return new ResponseEntity<String>(reportId, HttpStatus.OK);
    }

    private byte[] exportEnrolmentsToXLS(RegistrationHistoryReportParametersBean bean) {

        final Collection<RegistrationHistoryReport> reports = generateReport(bean, false);

        final Multimap<RegistrationHistoryReport, Enrolment> enrolments = HashMultimap.create();
        reports.stream().forEach(r -> enrolments.putAll(r, r.getEnrolments()));

        final Map<Enrolment, ExecutionSemester> improvementsOnly = Maps.newHashMap();
        reports.stream().forEach(r -> {
            r.getImprovementEvaluations().forEach(ev -> {
                enrolments.put(r, ev.getEnrolment());

                if (ev.getExecutionPeriod() != ev.getEnrolment().getExecutionPeriod()) {
                    improvementsOnly.put(ev.getEnrolment(), ev.getExecutionPeriod());
                }
            });
        });

        final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.enrolments"),
                new SheetData<Map.Entry<RegistrationHistoryReport, Enrolment>>(enrolments.entries()) {

                    @Override
                    protected void makeLine(Entry<RegistrationHistoryReport, Enrolment> entry) {

                        final RegistrationHistoryReport report = entry.getKey();
                        final Registration registration = report.getRegistration();
                        final Enrolment enrolment = entry.getValue();

                        final boolean improvementOnly = improvementsOnly.containsKey(enrolment);
                        final ExecutionSemester enrolmentPeriod =
                                improvementOnly ? improvementsOnly.get(enrolment) : enrolment.getExecutionPeriod();

                        final EnrolmentEvaluation finalEvaluation = enrolment.getFinalEnrolmentEvaluation();

                        addData("Student.number", registration.getStudent().getNumber());
                        addData("Registration.number", registration.getNumber().toString());
                        addData("Person.name", registration.getStudent().getPerson().getName());
                        addData("Degree.code", registration.getDegree().getCode());
                        addData("Degree.presentationName", registration.getDegree().getPresentationNameI18N().getContent());
                        addData("RegistrationHistoryReport.curricularYear", report.getCurricularYear().toString());
                        addData("Enrolment.code", enrolment.getCode());
                        addData("Enrolment.name", enrolment.getPresentationName().getContent());
                        addData("Enrolment.ectsCreditsForCurriculum", enrolment.getEctsCreditsForCurriculum());
                        addData("Enrolment.grade", finalEvaluation != null ? finalEvaluation.getGradeValue() : null);
                        addData("Enrolment.executionPeriod", enrolmentPeriod.getQualifiedName());
                        addData("enrolmentDate", enrolment.getCreationDateDateTime().toString("yyyy-MM-dd HH:mm"));
                        addData("Enrolment.improvementOnly",
                                ULisboaSpecificationsUtil.bundle(improvementOnly ? "label.yes" : "label.no"));
                        addData("Enrolment.shifts", EnrolmentServices.getShiftsDescription(enrolment, enrolmentPeriod));
                        addData("Enrolment.curriculumGroup", enrolment.getCurriculumGroup().getFullPath());
                        addData("Enrolment.numberOfEnrolments", CompetenceCourseServices.countEnrolmentsUntil(
                                report.getStudentCurricularPlan(), enrolment.getCurricularCourse(), report.getExecutionYear()));
                        addData("ICurriculumEntry.curricularYear", CurricularPeriodServices.getCurricularYear(enrolment));
                        addData("ICurriculumEntry.curricularSemester", CurricularPeriodServices.getCurricularSemester(enrolment));
                        addData("Person.defaultEmailAddress", enrolment.getStudent().getPerson().getDefaultEmailAddressValue());
                        addData("Person.institutionalEmailAddress",
                                enrolment.getStudent().getPerson().getInstitutionalEmailAddressValue());
                    }

                    private void addData(String key, Object data) {
                        addCell(ULisboaSpecificationsUtil.bundle("label." + key), data);
                    }

                });

        final List<EnrolmentEvaluation> evaluations =
                enrolments.entries().stream().flatMap(e -> e.getValue().getEvaluationsSet().stream())
                        .filter(e -> EvaluationSeasonServices.isRequiredEnrolmentEvaluation(e.getEvaluationSeason())
                                && bean.getExecutionYears().contains(e.getExecutionPeriod().getExecutionYear()))
                        .sorted(EnrolmentEvaluation.SORT_BY_STUDENT_NUMBER.thenComparing(DomainObjectUtil.COMPARATOR_BY_ID))
                        .collect(Collectors.toList());
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.evaluations"),
                new SheetData<EnrolmentEvaluation>(evaluations) {

                    @Override
                    protected void makeLine(EnrolmentEvaluation item) {

                        final Registration registration = item.getRegistration();
                        final Enrolment enrolment = item.getEnrolment();

                        addData("Student.number", registration.getStudent().getNumber());
                        addData("Registration.number", registration.getNumber().toString());
                        addData("Person.name", registration.getStudent().getPerson().getName());
                        addData("Degree.code", registration.getDegree().getCode());
                        addData("Degree.presentationName", registration.getDegree().getPresentationNameI18N().getContent());
                        addData("Enrolment.code", enrolment.getCode());
                        addData("Enrolment.name", enrolment.getPresentationName().getContent());
                        addData("Enrolment.executionPeriod", item.getExecutionPeriod().getQualifiedName());
                        addData("EnrolmentEvaluation.grade", item.getGradeValue());

                        if (item.getEvaluationSeason().isImprovement() && item.isFinal()) {
                            addData("Enrolment.gradeImproved", gradeWasImproved(item) ? ULisboaSpecificationsUtil
                                    .bundle("label.yes") : ULisboaSpecificationsUtil.bundle("label.no"));
                        } else {
                            addData("Enrolment.gradeImproved", "");
                        }

                        addData("EnrolmentEvaluation.season", item.getEvaluationSeason().getName().getContent());
                    }

                    private void addData(String key, Object data) {
                        addCell(ULisboaSpecificationsUtil.bundle("label." + key), data);
                    }

                });

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            builder.build(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();
    }

    //Report specific. Cannot be at service level (depends on instance logic to calculate final grade)
    private boolean gradeWasImproved(final EnrolmentEvaluation improvement) {
        final EnrolmentEvaluation previousEvaluation = improvement.getEnrolment().getEvaluationsSet().stream()
                .filter(ev -> ev.getEvaluationSeason() != improvement.getEvaluationSeason() && ev.isFinal() && ev.isApproved()
                        && !ev.getEvaluationSeason().isImprovement())
                .sorted(new EvaluationComparator().reversed()).findFirst().orElse(null);

        return previousEvaluation != null && improvement.getGrade().compareTo(previousEvaluation.getGrade()) > 0;
    }

    @RequestMapping(value = "/exportregistrationsbystatute", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportRegistrationsByStatute(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        final String reportId = UUID.randomUUID().toString();
        new Thread(() -> processReport(this::exportRegistrationsByStatuteToXLS, bean, reportId)).start();

        return new ResponseEntity<String>(reportId, HttpStatus.OK);
    }

    private byte[] exportRegistrationsByStatuteToXLS(RegistrationHistoryReportParametersBean bean) {

        if (bean.getExecutionYears().size() != 1) {
            return createXLSWithError(ULisboaSpecificationsUtil.bundle(
                    "error.reports.registrationHistory.to.export.registrations.by.statute.choose.a.single.execution.year"));
        }

        final ExecutionYear executionYear = bean.getExecutionYears().iterator().next();

        final Collection<StudentStatute> studentStatutes = Sets.newHashSet();
        for (final ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
            studentStatutes.addAll(executionSemester.getBeginningStudentStatutesSet().stream()
                    .filter(x -> bean.getStatuteTypes().isEmpty() || bean.getStatuteTypes().contains(x.getType()))
                    .collect(Collectors.toSet()));
            studentStatutes.addAll(executionSemester.getEndingStudentStatutesSet().stream()
                    .filter(x -> bean.getStatuteTypes().isEmpty() || bean.getStatuteTypes().contains(x.getType()))
                    .collect(Collectors.toSet()));
        }

        final Set<RegistrationHistoryReport> registrations = Sets.newHashSet();
        for (final StudentStatute studentStatute : studentStatutes) {

            if (studentStatute.getRegistration() != null) {
                registrations.add(new RegistrationHistoryReport(studentStatute.getRegistration(), executionYear));
                continue;
            }

            for (final Registration registration : studentStatute.getStudent().getRegistrationsSet()) {
                if (registration.getRegistrationStatesTypes(executionYear).stream().anyMatch(x -> x.isActive())) {
                    registrations.add(new RegistrationHistoryReport(registration, executionYear));
                }
            }

        }

        final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.statutes"),
                new SheetData<RegistrationHistoryReport>(registrations) {

                    @Override
                    protected void makeLine(RegistrationHistoryReport entry) {
                        final Registration registration = entry.getRegistration();
                        addData("RegistrationHistoryReport.executionYear", entry.getExecutionYear().getQualifiedName());
                        addData("Student.number", registration.getStudent().getNumber());
                        addData("Registration.number", registration.getNumber().toString());
                        addData("Person.name", registration.getStudent().getPerson().getName());
                        addData("Degree.code", registration.getDegree().getCode());
                        addData("Degree.presentationName", registration.getDegree().getPresentationNameI18N().getContent());
                        addData("RegistrationHistoryReport.statutes", entry.getStudentStatutesNamesAndDates());
                    }

                    private void addData(String key, Object data) {
                        addCell(ULisboaSpecificationsUtil.bundle("label." + key), data);
                    }

                });

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            builder.build(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();
    }

    @RequestMapping(value = "/exportbluerecord", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
    public @ResponseBody ResponseEntity<String> exportBlueRecordInfo(
            @RequestParam(value = "bean", required = false) final RegistrationHistoryReportParametersBean bean,
            final Model model) {

        final String reportId = UUID.randomUUID().toString();
        new Thread(() -> processReport(this::exportRegistrationsBlueRecordInformationToXLS, bean, reportId)).start();

        return new ResponseEntity<String>(reportId, HttpStatus.OK);
    }

    private byte[] exportRegistrationsBlueRecordInformationToXLS(RegistrationHistoryReportParametersBean bean) {
        final Collection<RegistrationHistoryReport> registrationsToExport = generateReport(bean, false);

        Collection<RegistrationDGESStateBean> candidacies = new ArrayList<>();
        for (RegistrationHistoryReport registrationHistory : registrationsToExport) {
            StudentCandidacy studentCandidacy = registrationHistory.getRegistration().getStudentCandidacy();
            if (studentCandidacy != null) {
                candidacies.add(RegistrationDGESStateBeanController.populateBean(studentCandidacy));
            }
        }

        final SpreadsheetBuilderForXLSX builder = new SpreadsheetBuilderForXLSX();
        builder.addSheet(ULisboaSpecificationsUtil.bundle("label.reports.registrationHistory.blueRecord"),
                new SheetData<RegistrationDGESStateBean>(candidacies) {

                    @Override
                    protected void makeLine(RegistrationDGESStateBean item) {
                        addData("HouseholdInformationForm.executionYear", item.getExecutionYear());
                        addData("Degree.degreeType", item.getDegreeTypeName());
                        addData("studentsListByCurricularCourse.degree", item.getDegreeCode());
                        addData("Degree.name", item.getDegreeName());
                        addData("ServiceRequestSlot.label.cycleType", item.getCycleName());
                        addData("RegistrationHistoryReport.curricularYear", item.getCurricularYear());
                        addData("OriginInformationForm.schoolLevel", item.getDegreeLevel());
                        addData("RegistrationHistoryReport.primaryBranch", item.getDegreeBranch());
                        addData("RegistrationHistoryReport.regimeType", item.getRegimeType());
                        addData("OriginInformationForm.institution", item.getInstitutionName());
                        addData("identification.number", item.getIdNumber());
                        addData("PersonalInformationForm.documentIdExpirationDate", item.getExpirationDateOfIdDoc());
                        addData("PersonalInformationForm.documentIdEmissionLocation", item.getEmissionLocationOfIdDoc());
                        addData("student", item.getName());
                        addData("PersonalInformationForm.maritalStatus", item.getMaritalStatus());
                        addData("is.registered", item.getRegistrationState());
                        addData("candidacy", item.getCandidacyState());
                        addData("FiliationForm.nationality", item.getNationality());
                        addData("FiliationForm.secondNationality", item.getSecondNationality());
                        addData("Person.birthYear", item.getBirthYear());
                        addData("FiliationForm.countryOfBirth", item.getCountryOfBirth());
                        addData("FiliationForm.districtOfBirth", item.getDistrictOfBirth());
                        addData("FiliationForm.districtSubdivisionOfBirth", item.getDistrictSubdivisionOfBirth());
                        addData("FiliationForm.parishOfBirth", item.getParishOfBirth());
                        addData("Person.gender", item.getGender());
                        addData("Registration.ingressionType", item.getIngressionType());
                        addData("PersonalInformationForm.ingressionOption", item.getPlacingOption());
                        addData("PersonalInformationForm.firstOptionDegreeDesignation.short", item.getFirstOptionDegree());
                        addData("PersonalInformationForm.firstOptionInstitution.short", item.getFirstOptionInstitution());
                        addData("ResidenceInformationForm.countryOfResidence", item.getCountryOfResidence());
                        addData("ResidenceInformationForm.districtOfResidence", item.getDistrictOfResidence());
                        addData("ResidenceInformationForm.districtSubdivisionOfResidence",
                                item.getDistrictSubdivisionOfResidence());
                        addData("ResidenceInformationForm.parishOfResidence", item.getParishOfResidence());
                        addData("ResidenceInformationForm.address", item.getAddressOfResidence());
                        addData("ResidenceInformationForm.areaCode", item.getAreaCodeOfResidence());
                        addData("ResidenceInformationForm.schoolTimeCountry", item.getCountryOfDislocated());
                        addData("ResidenceInformationForm.schoolTimeDistrictOfResidence", item.getDistrictOfDislocated());
                        addData("ResidenceInformationForm.schoolTimeDistrictSubdivisionOfResidence",
                                item.getDistrictSubdivisionOfDislocated());
                        addData("ResidenceInformationForm.schoolTimeParishOfResidence", item.getParishOfDislocated());
                        addData("ResidenceInformationForm.schoolTimeAddress", item.getAddressOfDislocated());
                        addData("ResidenceInformationForm.schoolTimeAreaCode", item.getAreaCodeOfDislocated());
                        addData("ResidenceInformationForm.dislocatedFromPermanentResidence", item.getIsDislocated());
                        addData("firstTimeCandidacy.fillResidenceInformation", item.getDislocatedResidenceType());
                        addData("PersonalInformationForm.profession", item.getProfession());
                        addData("PersonalInformationForm.professionTimeType.short", item.getProfessionTimeType());
                        addData("PersonalInformationForm.professionalCondition", item.getProfessionalCondition());
                        addData("PersonalInformationForm.professionType", item.getProfessionType());
                        addData("FiliationForm.fatherName", item.getFatherName());
                        addData("HouseholdInformationForm.fatherSchoolLevel.short", item.getFatherSchoolLevel());
                        addData("HouseholdInformationForm.fatherProfessionalCondition.short",
                                item.getFatherProfessionalCondition());
                        addData("HouseholdInformationForm.fatherProfessionType.short", item.getFatherProfessionType());
                        addData("FiliationForm.motherName", item.getMotherName());
                        addData("HouseholdInformationForm.motherSchoolLevel.short", item.getMotherSchoolLevel());
                        addData("HouseholdInformationForm.motherProfessionalCondition.short",
                                item.getMotherProfessionalCondition());
                        addData("HouseholdInformationForm.motherProfessionType.short", item.getMotherProfessionType());
                        addData("HouseholdInformationForm.householdSalarySpan.short", item.getSalarySpan());
                        addData("firstTimeCandidacy.fillDisabilities", item.getDisabilityType());
                        addData("DisabilitiesForm.needsDisabilitySupport.short", item.getNeedsDisabilitySupport());
                        addData("MotivationsExpectationsForm.universityDiscoveryMeansAnswers.short",
                                item.getUniversityDiscoveryString());
                        addData("MotivationsExpectationsForm.universityChoiceMotivationAnswers.short",
                                item.getUniversityChoiceString());
                        addData("OriginInformationForm.countryWhereFinishedPreviousCompleteDegree", item.getPrecedentCountry());
                        addData("OriginInformationForm.districtWhereFinishedPreviousCompleteDegree", item.getPrecedentDistrict());
                        addData("OriginInformationForm.districtSubdivisionWhereFinishedPreviousCompleteDegree",
                                item.getPrecedentDistrictSubdivision());
                        addData("OriginInformationForm.schoolLevel", item.getPrecedentSchoolLevel());
                        addData("OriginInformationForm.institution", item.getPrecedentInstitution());
                        addData("OriginInformationForm.degreeDesignation", item.getPrecedentDegreeDesignation());
                        addData("OriginInformationForm.degree.cycle", item.getPrecendentDegreeCycle());
                        addData("OriginInformationForm.conclusionGrade", item.getPrecedentConclusionGrade());
                        addData("OriginInformationForm.conclusionYear", item.getPrecedentConclusionYear());
                        addData("OriginInformationForm.highSchoolType", item.getPrecedentHighSchoolType());
                        addData("ContactsForm.institutionalEmail", item.getInstitutionalEmail());
                        addData("ContactsForm.personalEmail", item.getDefaultEmail());
                        addData("ContactsForm.phoneNumber", item.getPhone());
                        addData("ContactsForm.mobileNumber", item.getTelephone());
                        addData("SchoolSpecificData.vaccinationValidity", item.getVaccinationValidity());
                        addData("HouseholdInformationForm.grantOwnerType", item.getGrantOwnerType());
                        addData("HouseholdInformationForm.grantOwnerProviderName", item.getGrantOwnerProvider());
                    }

                    private void addData(String key, Object data) {
                        addCell(ULisboaSpecificationsUtil.bundle("label." + key), data);
                    }
                });

        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            builder.build(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();
    }

}
