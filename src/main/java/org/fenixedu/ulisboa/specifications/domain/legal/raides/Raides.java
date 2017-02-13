package org.fenixedu.ulisboa.specifications.domain.legal.raides;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.District;
import org.fenixedu.academic.domain.DistrictSubdivision;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.GrantOwnerType;
import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacy.PersonalInformationBean;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.ulisboa.specifications.domain.exceptions.ULisboaSpecificationsDomainException;
import org.fenixedu.ulisboa.specifications.domain.legal.LegalReportContext;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.csv.XlsxExporter;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.process.DiplomadoService;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.process.IdentificacaoService;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.process.InscritoService;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.process.MobilidadeInternacionalService;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.report.RaidesRequestParameter;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.report.RaidesRequestPeriodParameter;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.xml.XmlToBaseFileWriter;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.xml.XmlZipFileWriter;
import org.fenixedu.ulisboa.specifications.domain.legal.report.LegalReport;
import org.fenixedu.ulisboa.specifications.domain.legal.report.LegalReportRequest;
import org.fenixedu.ulisboa.specifications.domain.legal.report.LegalReportResultFile;
import org.fenixedu.ulisboa.specifications.domain.legal.services.reportLog.transform.xls.XlsExporterLog;
import org.fenixedu.ulisboa.specifications.domain.student.curriculum.conclusion.RegistrationConclusionInformation;
import org.fenixedu.ulisboa.specifications.domain.student.curriculum.conclusion.RegistrationConclusionServices;
import org.fenixedu.ulisboa.specifications.util.ULisboaSpecificationsUtil;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import edu.emory.mathcs.backport.java.util.Collections;
import pt.ist.fenixframework.FenixFramework;

public class Raides {

    public static class AnoCurricular {
        public static final String ESTAGIO_FINAL_CODE = "ESTAGIO_FINAL_CODE";
        public static final String TRABALHO_PROJECTO_CODE = "TRABALHO_PROJECTO_CODE";
        public static final String DISSERTACAO_CODE = "DISSERTACAO_CODE";
        public static final String NAO_APLICAVEL_CODE = "NAO_APLICAVEL_CODE";

        public static Set<String> VALUES() {
            return Sets.newHashSet("1", "2", "3", "4", "5", "6", "7", ESTAGIO_FINAL_CODE, TRABALHO_PROJECTO_CODE,
                    DISSERTACAO_CODE, NAO_APLICAVEL_CODE);
        }

        public static LocalizedString LOCALIZED_NAME(final String key) {
            return ULisboaSpecificationsUtil.bundleI18N(AnoCurricular.class.getSimpleName() + "." + key);
        }
    }

    public static class RegimeFrequencia {

        public static final String ETD_CODE = "ETD_CODE";

        public static Set<String> VALUES() {
            Set<String> degreeCodes = Sets.newHashSet(
                    Lists.transform(Lists.newArrayList(Bennu.getInstance().getDegreesSet()), new Function<Degree, String>() {
                        @Override
                        public String apply(final Degree degree) {
                            return degree.getExternalId();
                        }
                    }));

            degreeCodes.add(ETD_CODE);

            return degreeCodes;
        }

        public static LocalizedString LOCALIZED_NAME(final String key) {
            if (ETD_CODE.equals(key)) {
                return ULisboaSpecificationsUtil.bundleI18N(RegimeFrequencia.class.getSimpleName() + "." + key);
            }

            final Degree degree = (Degree) FenixFramework.getDomainObject(key);
            return new LocalizedString(I18N.getLocale(), "[" + degree.getCode() + "] " + degree.getPresentationName());
        }

    }

    public static class Bolseiro {
        public static final GrantOwnerType NAO_BOLSEIRO = GrantOwnerType.STUDENT_WITHOUT_SCHOLARSHIP;
        public static final GrantOwnerType CANDIDATO_BOLSEIRO_ACCAO_SOCIAL =
                GrantOwnerType.HIGHER_EDUCATION_SAS_GRANT_OWNER_CANDIDATE;

        public static Set<String> VALUES() {
            final Set<String> result = Sets.newHashSet(NAO_BOLSEIRO.name(), CANDIDATO_BOLSEIRO_ACCAO_SOCIAL.name());
            for (final GrantOwnerType type : GrantOwnerType.values()) {
                result.add(type.name());
            }

            return result;
        }

        public static LocalizedString LOCALIZED_NAME(final String key) {
            if (Sets.newHashSet(NAO_BOLSEIRO, CANDIDATO_BOLSEIRO_ACCAO_SOCIAL).contains(key)) {
                return ULisboaSpecificationsUtil.bundleI18N(Bolseiro.class.getSimpleName() + "." + key);
            }

            return ULisboaSpecificationsUtil.bundleI18N("label.GrantOwnerType." + key);
        }

    }

    public static class Cursos {
        public static final String OUTRO = "0000";
    }

    public static class Estabelecimentos {
        public static final String OUTRO = "0000";
    }

    public static class ProgramaMobilidade {
        public static final String OUTRO_DOIS = "2";
        public static final String OUTRO_TRES = "3";
    }

    public static class ActividadeMobilidade {
        public static final String MOBILIDADE_ESTUDO = "1";
    }

    public static class NivelCursoOrigem {
        public static final String OUTRO = "4";
    }

    public static class Pais {
        public static final String OMISSAO = "PT";
    }

    public static class Ramo {
        public static final String TRONCO_COMUM = "1000017";
        public static final String OUTRO = "0000000";
    }

    public static class NivelEscolaridade {
        public static final String NAO_DISPONIVEL = "22";
    }

    public static class SituacaoProfissional {
        public static final String ALUNO = "17";
        public static final String NAO_DISPONIVEL = "19";
    }

    public static class Profissao {
        public static final String OUTRA_SITUACAO = "20";
        public static final String NAO_DISPONIVEL = "21";
    }

    public static class DocumentoIdentificacao {
        public static final String OUTRO = "7";
    }

    public static class TipoEstabSec {
        public static final String PUBLICO = "1";
    }

    public static final String DATE_FORMAT = "dd-MM-yyyy";

    protected Map<Student, TblIdentificacao> alunos = new LinkedHashMap<Student, TblIdentificacao>();
    protected Multimap<Student, TblInscrito> inscritos = LinkedListMultimap.create();
    protected Multimap<Student, TblDiplomado> diplomados = LinkedListMultimap.create();
    protected Multimap<Student, TblMobilidadeInternacional> mobilidadeInternacional = LinkedListMultimap.create();

    public Set<Registration> registrationList = new HashSet<Registration>();

    protected static IRaidesReportRequestDefaultData requestDefaultData;

    public void process(final RaidesInstance raidesInstance, final LegalReportRequest reportRequest) {
        final RaidesRequestParameter raidesRequestParameter = reportRequest.getParametersAs(RaidesRequestParameter.class);

        processEnrolled(reportRequest.getLegalReport(), raidesRequestParameter);
        processGraduated(reportRequest.getLegalReport(), raidesRequestParameter);
        processMobilidadeInternacional(reportRequest.getLegalReport(), raidesRequestParameter);

        // Excel
        XlsxExporter.write(reportRequest, this);

        // XML
        LegalReportResultFile xml = XmlToBaseFileWriter.write(reportRequest, raidesRequestParameter, this);

        // Log
        XlsExporterLog.write(reportRequest, LegalReportContext.getReport());

        // XML password protected zip
        XmlZipFileWriter.write(reportRequest, raidesRequestParameter, this, xml);

        reportRequest.markAsProcessed();
    }

    public void processMobilidadeInternacional(final LegalReport report, final RaidesRequestParameter raidesRequestParameter) {

        for (final RaidesRequestPeriodParameter enroledPeriod : raidesRequestParameter.getPeriodsForInternationalMobility()) {
            final ExecutionYear academicPeriod = enroledPeriod.getAcademicPeriod();
            for (final Degree degree : raidesRequestParameter.getDegrees()) {
                for (final Registration registration : degree.getRegistrationsSet()) {

                    final String[] messageArgs = formatArgs(registration, academicPeriod);

                    try {
                        if (!isAgreementPartOfMobilityReport(raidesRequestParameter, registration)) {
                            continue;
                        }

                        if (enroledPeriod.isEnrolledInAcademicPeriod()
                                && !isEnrolledInExecutionYear(enroledPeriod.getAcademicPeriod(), registration, false)) {
                            continue;
                        }

                        if (!hadEnrolmentsInPeriod(enroledPeriod.getInterval(), enroledPeriod.getAcademicPeriod(), registration,
                                raidesRequestParameter)) {
                            continue;
                        }

                        if (!isInEnrolledEctsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!isInEnrolledYearsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!containsStudentIdentification(registration.getStudent())) {
                            addStudent(report, raidesRequestParameter.getInstitution(), registration.getStudent(), registration,
                                    academicPeriod);
                        }

                        addMobilidadeInternacional(report, raidesRequestParameter, academicPeriod, registration);
                    } catch (final DomainException e) {
                        LegalReportContext.addError("",
                                i18n("error.Raides.unexpected.error.occured", concatArgs(messageArgs, e.getKey())));
                    } catch (final Throwable e) {
                        LegalReportContext.addError("", i18n("error.Raides.unexpected.error.occured",
                                concatArgs(messageArgs, ExceptionUtils.getFullStackTrace(e))));
                    }
                }
            }

        }
    }

    public void processGraduated(final LegalReport report, final RaidesRequestParameter raidesRequestParameter) {

        for (final RaidesRequestPeriodParameter enroledPeriod : raidesRequestParameter.getPeriodsForGraduated()) {
            final ExecutionYear academicPeriod = enroledPeriod.getAcademicPeriod();
            for (final Degree degree : raidesRequestParameter.getDegrees()) {
                for (final Registration registration : degree.getRegistrationsSet()) {

                    final String[] messageArgs = formatArgs(registration, academicPeriod);

                    try {
                        if (enroledPeriod.isEnrolledInAcademicPeriod()
                                && !isEnrolledInExecutionYear(enroledPeriod.getAcademicPeriod(), registration, true)) {
                            continue;
                        } else if (!enroledPeriod.isEnrolledInAcademicPeriod()
                                && isEnrolledInExecutionYear(enroledPeriod.getAcademicPeriod(), registration, true)) {
                            continue;
                        }

                        if (!hasConcludedInYear(registration, academicPeriod)) {
                            continue;
                        }

                        if (!hasConcludedInPeriod(enroledPeriod.getInterval(), academicPeriod, registration)) {
                            continue;
                        }

                        if (!isInEnrolledEctsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!isInEnrolledYearsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!containsStudentIdentification(registration.getStudent())) {
                            addStudent(report, raidesRequestParameter.getInstitution(), registration.getStudent(), registration,
                                    academicPeriod);
                        }

                        addGraduated(report, raidesRequestParameter, academicPeriod, registration);
                    } catch (final Throwable e) {
                        LegalReportContext.addError("", i18n("error.Raides.unexpected.error.occured",
                                concatArgs(messageArgs, ExceptionUtils.getFullStackTrace(e))));
                    }
                }
            }
        }
    }

    protected boolean isInEnrolledEctsLimit(final RaidesRequestPeriodParameter enroledPeriod, final Registration registration,
            final ExecutionYear academicPeriod) {
        if (!enroledPeriod.isEnrolmentEctsConstraint()) {
            return true;
        }

        final BigDecimal ectsEnrolled = BigDecimal.valueOf(registration.getEnrolmentsEcts(academicPeriod));

        if (enroledPeriod.getMinEnrolmentEcts() != null && enroledPeriod.getMinEnrolmentEcts().compareTo(ectsEnrolled) > 0) {
            return false;
        }

        if (enroledPeriod.getMaxEnrolmentEcts() != null && enroledPeriod.getMaxEnrolmentEcts().compareTo(ectsEnrolled) < 0) {
            return false;
        }

        return true;
    }

    protected boolean isInEnrolledYearsLimit(final RaidesRequestPeriodParameter enroledPeriod, final Registration registration,
            final ExecutionYear academicPeriod) {
        if (!enroledPeriod.isEnrolmentYearsConstraint()) {
            return true;
        }

        final int enrolledYears = registration.getEnrolmentsExecutionYears().size();

        if (enroledPeriod.getMinEnrolmentYears() != null && enroledPeriod.getMinEnrolmentYears().compareTo(enrolledYears) > 0) {
            return false;
        }

        if (enroledPeriod.getMaxEnrolmentYears() != null && enroledPeriod.getMaxEnrolmentYears().compareTo(enrolledYears) < 0) {
            return false;
        }

        return true;
    }

    protected boolean hasConcludedInPeriod(final Interval interval, final ExecutionYear academicPeriod,
            final Registration registration) {

        final Set<RegistrationConclusionInformation> informationConclusionSet =
                RegistrationConclusionServices.inferConclusion(registration);
        for (final RegistrationConclusionInformation rci : informationConclusionSet) {
            if (interval.contains(rci.getConclusionDate().toDateTimeAtStartOfDay())) {
                return true;
            }
        }

        return false;
    }

    public void processEnrolled(final LegalReport report, final RaidesRequestParameter raidesRequestParameter) {

        for (final RaidesRequestPeriodParameter enroledPeriod : raidesRequestParameter.getPeriodsForEnrolled()) {
            final ExecutionYear academicPeriod = enroledPeriod.getAcademicPeriod();
            for (final Degree degree : raidesRequestParameter.getDegrees()) {

                for (final Registration registration : degree.getRegistrationsSet()) {

                    final String[] messageArgs = formatArgs(registration, academicPeriod);

                    try {
                        if (!isAgreementPartOfEnrolledReport(raidesRequestParameter, registration)) {
                            continue;
                        }

                        if (enroledPeriod.isEnrolledInAcademicPeriod()
                                && !isEnrolledInExecutionYear(enroledPeriod.getAcademicPeriod(), registration, false)) {
                            continue;
                        }

                        if (!hadEnrolmentsInPeriod(enroledPeriod.getInterval(), enroledPeriod.getAcademicPeriod(), registration,
                                raidesRequestParameter)) {
                            continue;
                        }

                        if (!isInEnrolledEctsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!isInEnrolledYearsLimit(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!isActiveAtPeriod(enroledPeriod, registration, academicPeriod)) {
                            continue;
                        }

                        if (!hasEnrolmentsInSecondMomentAndIsNotConcluded(enroledPeriod.getInterval(),
                                enroledPeriod.getAcademicPeriod(), registration, raidesRequestParameter)) {
                            continue;
                        }

                        if (!containsStudentIdentification(registration.getStudent())) {
                            addStudent(report, raidesRequestParameter.getInstitution(), registration.getStudent(), registration,
                                    academicPeriod);
                        }

                        if (!academicPeriod.isCurrent() && isEnrolledInDissertation(registration, academicPeriod)
                                && isEnrolled(registration, academicPeriod)) {
                            continue;
                        }

                        addEnrolledStudent(report, raidesRequestParameter, academicPeriod, registration);

                    } catch (final DomainException e) {
                        LegalReportContext.addError("",
                                i18n("error.Raides.unexpected.error.occured", concatArgs(messageArgs, e.getKey())));
                    } catch (final Throwable e) {
                        LegalReportContext.addError("", i18n("error.Raides.unexpected.error.occured",
                                concatArgs(messageArgs, ExceptionUtils.getFullStackTrace(e))));
                    }
                }
            }
        }
    }

    protected boolean isActiveAtPeriod(final RaidesRequestPeriodParameter enroledPeriod, final Registration registration,
            final ExecutionYear academicPeriod) {
        final RegistrationState stateInDate = registration.getStateInDate(enroledPeriod.getEnd());
        return stateInDate != null && (stateInDate.isActive() || stateInDate.getStateType() == RegistrationStateType.CONCLUDED);
    }

    protected boolean isEnrolledInDissertation(Registration registration, ExecutionYear academicPeriod) {
        Collection<Enrolment> enrolments = registration.getEnrolments(academicPeriod);

        for (final Enrolment enrolment : enrolments) {
            if (enrolment.isDissertation()) {
                return true;
            }
        }

        return false;
    }

    protected boolean isEnrolled(Registration registration, ExecutionYear academicPeriod) {
        Collection<Enrolment> enrolments = registration.getEnrolments(academicPeriod);

        for (final Enrolment enrolment : enrolments) {
            if (!enrolment.isAnnulled()) {
                return true;
            }
        }

        return false;
    }

    protected boolean hasEnrolmentsInSecondMomentAndIsNotConcluded(final Interval interval, final ExecutionYear executionYear,
            final Registration registration, final RaidesRequestParameter raidesRequestParameter) {
        if (!isInSecondMoment(raidesRequestParameter)) {
            return true;
        }

        if (hasConcludedInYear(registration, executionYear)) {
            final Set<RegistrationConclusionInformation> informationConclusionSet =
                    RegistrationConclusionServices.inferConclusion(registration);
            boolean hadConcludedBeforeIntervalEnd = false;
            for (final RegistrationConclusionInformation rci : informationConclusionSet) {
                if (rci.isScholarPart()) {
                    continue;
                }

                if (isIntegratedMasterDegree(registration))

                    if (!rci.getConclusionDate().isAfter(interval.getEnd().toLocalDate())) {
                        hadConcludedBeforeIntervalEnd = true;
                    }
            }

            if (hadConcludedBeforeIntervalEnd) {
                return false;
            }
        }

        final Collection<CurriculumLine> allCurriculumLines = Raides.getAllCurriculumLines(registration);
        for (final CurriculumLine curriculumLine : allCurriculumLines) {
            if (curriculumLine.getExecutionYear() != executionYear) {
                continue;
            }

            if (!curriculumLine.isEnrolment()) {
                continue;
            }

            final Enrolment enrolment = (Enrolment) curriculumLine;

            if (enrolment.isExtraCurricular()) {
                continue;
            }

            if (enrolment.isDissertation() && enrolment.isEnroled()) {
                return true;
            }

            if (enrolment.isDissertation() && enrolment.isApproved()
                    && interval.contains(enrolment.getApprovementDate().toLocalDate().toDateTimeAtStartOfDay())) {
                return true;
            }

            if (enrolment.getExecutionPeriod().getSemester().intValue() == 2) {
                return true;
            }

            if (enrolment.getCurricularCourse().isAnual(executionYear)) {
                return true;
            }
        }

        return false;
    }

    protected boolean hadEnrolmentsInPeriod(final Interval interval, final ExecutionYear executionYear,
            final Registration registration, final RaidesRequestParameter raidesRequestParameter) {
        final Collection<CurriculumLine> allCurriculumLines = Raides.getAllCurriculumLines(registration);
        for (final CurriculumLine curriculumLine : allCurriculumLines) {
            if (curriculumLine.getExecutionYear() != executionYear) {
                continue;
            }

            if (!curriculumLine.isEnrolment()) {
                continue;
            }

            final Enrolment enrolment = (Enrolment) curriculumLine;

            if (enrolment.isExtraCurricular()) {
                continue;
            }

            if (interval.contains(enrolment.getCreationDateDateTime())) {
                return true;
            }

            if (enrolment.isDissertation() && enrolment.isEnroled()) {
                return true;
            }

            if (enrolment.isDissertation() && enrolment.isApproved()
                    && interval.contains(enrolment.getApprovementDate().toLocalDate().toDateTimeAtStartOfDay())) {
                return true;
            }
        }

        final LocalDate enrolmentDate = getEnrolmentDate(registration, executionYear);
        return enrolmentDate != null && interval.contains(enrolmentDate.toDateTimeAtStartOfDay());
    }

    protected void addEnrolledStudent(final LegalReport report, final RaidesRequestParameter raidesRequestParameter,
            final ExecutionYear executionYear, final Registration registration) {
        final TblInscrito tblInscrito = (new InscritoService(report)).create(raidesRequestParameter, executionYear, registration);
        inscritos.put(registration.getStudent(), tblInscrito);
        registrationList.add(registration);
    }

    protected void addGraduated(final LegalReport report, final RaidesRequestParameter raidesRequestParameter,
            final ExecutionYear executionYear, final Registration registration) {
        final TblDiplomado diplomado = (new DiplomadoService(report)).create(raidesRequestParameter, executionYear, registration);
        diplomados.put(registration.getStudent(), diplomado);
        registrationList.add(registration);
    }

    protected void addMobilidadeInternacional(final LegalReport report, final RaidesRequestParameter raidesRequestParameter,
            final ExecutionYear executionYear, final Registration registration) {
        final TblMobilidadeInternacional mobilidadeInternacional =
                (new MobilidadeInternacionalService(report)).create(raidesRequestParameter, executionYear, registration);
        this.mobilidadeInternacional.put(registration.getStudent(), mobilidadeInternacional);
        registrationList.add(registration);
    }

    protected TblIdentificacao addStudent(final LegalReport report, final Unit institution, final Student student,
            final Registration registration, final ExecutionYear executionYear) {
        final TblIdentificacao aluno =
                (new IdentificacaoService(report)).create(institution, student, registration, executionYear);
        alunos.put(student, aluno);

        return aluno;
    }

    protected boolean hasConcludedInYear(final Registration registration, final ExecutionYear executionYear) {
        final Set<RegistrationConclusionInformation> informationConclusionSet =
                RegistrationConclusionServices.inferConclusion(registration);
        for (final RegistrationConclusionInformation rci : informationConclusionSet) {
            if (!rci.isScholarPart() && rci.getConclusionYear() == executionYear) {
                return true;
            }
        }

        return hadScholarPartApprovement(registration, executionYear);
    }

    public static boolean hadScholarPartApprovement(final Registration registration, final ExecutionYear executionYear) {
        final Set<RegistrationConclusionInformation> informationConclusionSet =
                RegistrationConclusionServices.inferConclusion(registration);
        for (final RegistrationConclusionInformation rci : informationConclusionSet) {
            if (rci.isScholarPart() && scholarPartConclusionYear(rci) == executionYear) {
                return true;
            }
        }

        return false;
    }

    public static ExecutionYear scholarPartConclusionYear(final RegistrationConclusionInformation rci) {
        if (!Raides.isMasterDegreeOrDoctoralDegree(rci.getRegistrationConclusionBean().getRegistration())) {
            return rci.getConclusionYear();
        }

        if (!rci.getRegistrationConclusionBean().isConclusionProcessed()) {
            return rci.getConclusionYear();
        }

        final ExecutionYear conclusionYearByDate = ExecutionYear.readByDateTime(rci.getConclusionDate());
        final ExecutionYear conclusionYearByInformation = rci.getConclusionYear();

        if (conclusionYearByDate.isBefore(conclusionYearByInformation)) {
            return conclusionYearByDate;
        }

        return rci.getConclusionYear();
    }

    protected boolean containsStudentIdentification(final Student student) {
        return alunos.containsKey(student);
    }

    public static boolean isAgreementPartOfMobilityReport(final RaidesRequestParameter raidesRequestParameter,
            final Registration registration) {
        return raidesRequestParameter.getAgreementsForMobility().contains(registration.getRegistrationProtocol());
    }

    protected boolean isAgreementPartOfEnrolledReport(final RaidesRequestParameter raidesRequestParameter,
            final Registration registration) {
        return raidesRequestParameter.getAgreementsForEnrolled().contains(registration.getRegistrationProtocol());
    }

    protected boolean isEnrolledInExecutionYear(final ExecutionYear executionYear, final Registration registration,
            final boolean lookupAtRegistrationDataByExecutionYear) {
        final Set<ExecutionYear> executionYearsSet = filterExtraCurricularCourses(registration.getEnrolments(executionYear))
                .stream().map(r -> r.getExecutionYear()).collect(Collectors.toSet());

        if (!lookupAtRegistrationDataByExecutionYear) {
            return !executionYearsSet.isEmpty();
        } else if (!executionYearsSet.isEmpty()) {
            return true;
        }

        final RegistrationDataByExecutionYear registrationData = getRegistrationDataByExecutionYear(registration, executionYear);
        final boolean withHistoric = registrationData != null && registrationData.getEnrolmentDate() != null;

        if (withHistoric) {
            LegalReportContext.addWarn("", i18n("warn.Raides.enrolledInExecutionYear.with.historic.but.not.enrolments",
                    formatArgs(registration, executionYear)));
        }

        return withHistoric;
    }

    protected Collection<Enrolment> filterExtraCurricularCourses(final Collection<Enrolment> enrolments) {
        final Set<Enrolment> result = Sets.newHashSet();
        for (final Enrolment enrolment : enrolments) {
            if (!enrolment.isExtraCurricular()) {
                result.add(enrolment);
            }
        }

        return result;
    }

    private final boolean isInFirstMoment(final RaidesRequestParameter raidesRequestParameter) {
        return "1".equals(raidesRequestParameter.getMoment());
    }

    private final boolean isInSecondMoment(final RaidesRequestParameter raidesRequestParameter) {
        return "2".equals(raidesRequestParameter.getMoment());
    }

    protected boolean hadEnrolmentsInPeriod(final LocalDate begin, final LocalDate end, final Registration registration) {
        return false;
    }

    public static boolean isDegreeChange(final RaidesRequestParameter raidesRequestParameter,
            final IngressionType registrationIngression) {
        return raidesRequestParameter.getIngressionsForDegreeChange().contains(registrationIngression);
    }

    public static boolean isDegreeTransfer(final RaidesRequestParameter raidesRequestParameter,
            final IngressionType registrationIngression) {
        return raidesRequestParameter.getIngressionsForDegreeTransfer().contains(registrationIngression);
    }

    public static boolean isDegreeChange(final RaidesInstance raidesInstance, final IngressionType registrationIngression) {
        return raidesInstance.getDegreeChangeIngressionsSet().contains(registrationIngression);
    }

    public static boolean isDegreeTransfer(final RaidesInstance raidesInstance, final IngressionType registrationIngression) {
        return raidesInstance.getDegreeTransferIngressionsSet().contains(registrationIngression);
    }

    public static boolean isDegreeChangeOrTransfer(final RaidesInstance raidesInstance,
            final IngressionType registrationIngression) {
        return isDegreeChange(raidesInstance, registrationIngression) || isDegreeTransfer(raidesInstance, registrationIngression);
    }

    public static boolean isGeneralAccessRegime(final RaidesRequestParameter raidesRequestParameter,
            final IngressionType registrationIngression) {
        return raidesRequestParameter.getIngressionsForGeneralAccessRegime().contains(registrationIngression);
    }

    public Set<Student> studentsToReport() {
        return alunos.keySet();
    }

    public TblIdentificacao identificacaoForStudent(final Student student) {
        return alunos.get(student);
    }

    public List<TblIdentificacao> getAllIdentifications() {
        return Lists.newArrayList(alunos.values());
    }

    public Collection<TblInscrito> inscricoesForStudent(final Student student,
            final RaidesRequestParameter raidesRequestParameter) {
        if (!raidesRequestParameter.isFilterEntriesWithErrors()) {
            return inscritos.get(student);
        }

        final Collection<TblInscrito> filteredResult = Lists.newArrayList();

        for (final TblInscrito tblInscrito : inscritos.get(student)) {
            if (tblInscrito.isValid()) {
                filteredResult.add(tblInscrito);
            }
        }

        return filteredResult;
    }

    public Collection<TblInscrito> getAllInscritos() {
        return inscritos.values();
    }

    public Collection<TblDiplomado> diplomadosForStudent(final Student student,
            final RaidesRequestParameter raidesRequestParameter) {
        if (!raidesRequestParameter.isFilterEntriesWithErrors()) {
            return diplomados.get(student);
        }

        final Collection<TblDiplomado> filteredResult = Lists.newArrayList();

        for (final TblDiplomado tblDiplomado : diplomados.get(student)) {
            if (tblDiplomado.isValid()) {
                filteredResult.add(tblDiplomado);
            }
        }

        return filteredResult;
    }

    public Collection<TblDiplomado> getAllDiplomados() {
        return diplomados.values();
    }

    public TblMobilidadeInternacional mobilidadeInternacionalForStudent(final Student student,
            final RaidesRequestParameter raidesRequestParameter) {
        if (mobilidadeInternacional.get(student).isEmpty()) {
            return null;
        }

        if (mobilidadeInternacional.get(student).size() > 1) {
            throw new RuntimeException("error.Raides.student.with.more.than.one.mobility");
        }

        final TblMobilidadeInternacional tblMobilidadeInternacional = mobilidadeInternacional.get(student).iterator().next();
        if (raidesRequestParameter.isFilterEntriesWithErrors() && !tblMobilidadeInternacional.isValid()) {
            return null;
        }

        return tblMobilidadeInternacional;
    }

    public Collection<TblMobilidadeInternacional> getAllMobilidadeInternacional() {
        return this.mobilidadeInternacional.values();
    }

    public static void defineRaidesReportRequestDefaultData(final IRaidesReportRequestDefaultData requestDefaultData) {
        Raides.requestDefaultData = requestDefaultData;
    }

    public static void fillRaidesRequestDefaultData(final RaidesRequestParameter raidesRequestParameter) {
        requestDefaultData = new RaidesReportRequestDefaultData();

        Raides.requestDefaultData.fill(raidesRequestParameter);
    }

    //Returning the bean which contains information relating to both the precedent and the completed qualifications
    public Set<PersonalInformationBean> getAllPrecedentDegreeInformations() {
        final Set<PersonalInformationBean> result = new HashSet<PersonalInformationBean>();
        List<Registration> registrations = new ArrayList<Registration>(registrationList);
        Collections.sort(registrations, new Comparator<Registration>() {

            @Override
            public int compare(Registration o1, Registration o2) {
                return o1.getStudent().getName().compareTo(o2.getStudent().getName());
            }
        });

        for (final Registration registration : registrations) {
            result.add(new PersonalInformationBean(registration.getStudentCandidacy().getPrecedentDegreeInformation()));
        }

        return result;
    }

    public boolean isInEnrolledData(final Registration registration) {
        for (RaidesData data : inscritos.values()) {
            if (data.getRegistration() == registration) {
                return true;
            }
        }

        return false;
    }

    public boolean isInGraduated(final Registration registration) {
        for (final RaidesData data : diplomados.values()) {
            if (data.getRegistration() == registration) {
                return true;
            }
        }

        return false;
    }

    public boolean isInInternacionalMobility(final Registration registration) {
        for (final RaidesData data : mobilidadeInternacional.values()) {
            if (data.getRegistration() == registration) {
                return true;
            }
        }

        return false;
    }

    public static Country countryOfResidence(final Registration registration, final ExecutionYear executionYear) {
        final PersonalIngressionData pid = registration.getStudent().getPersonalIngressionDataByExecutionYear(executionYear);

        if (pid != null) {
            if (pid.getCountryOfResidence() != null) {
                return pid.getCountryOfResidence();
            }
        }

        if (registration.getPerson().getDefaultPhysicalAddress() != null
                && registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence() != null) {
            return registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence();

        }

        return null;
    }

    public static DistrictSubdivision districtSubdivisionOfResidence(final Registration registration,
            final ExecutionYear executionYear) {
        final PersonalIngressionData pid = registration.getStudent().getPersonalIngressionDataByExecutionYear(executionYear);

        if (pid != null) {
            if (pid.getDistrictSubdivisionOfResidence() != null) {
                return pid.getDistrictSubdivisionOfResidence();
            }
        }

        if (registration.getPerson().getDefaultPhysicalAddress() != null
                && registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence() != null
                && registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence().isDefaultCountry()
                && !Strings.isNullOrEmpty(registration.getPerson().getDefaultPhysicalAddress().getDistrictOfResidence())
                && !Strings.isNullOrEmpty(
                        registration.getPerson().getDefaultPhysicalAddress().getDistrictSubdivisionOfResidence())) {
            final District district =
                    findDistrictByName(registration.getPerson().getDefaultPhysicalAddress().getDistrictOfResidence());

            final DistrictSubdivision districtSubdivision = findDistrictSubdivisionByName(district,
                    registration.getPerson().getDefaultPhysicalAddress().getDistrictSubdivisionOfResidence());

            return districtSubdivision;
        }

        return null;
    }

    public static District districtOfResidence(final Registration registration, final ExecutionYear executionYear) {
        final PersonalIngressionData pid = registration.getStudent().getPersonalIngressionDataByExecutionYear(executionYear);

        if (pid != null) {
            if (pid.getDistrictSubdivisionOfResidence() != null) {
                return pid.getDistrictSubdivisionOfResidence().getDistrict();
            }
        }

        if (registration.getPerson().getDefaultPhysicalAddress() != null
                && registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence() != null
                && registration.getPerson().getDefaultPhysicalAddress().getCountryOfResidence().isDefaultCountry()
                && !Strings.isNullOrEmpty(registration.getPerson().getDefaultPhysicalAddress().getDistrictOfResidence())
                && !Strings.isNullOrEmpty(
                        registration.getPerson().getDefaultPhysicalAddress().getDistrictSubdivisionOfResidence())) {
            final District district =
                    findDistrictByName(registration.getPerson().getDefaultPhysicalAddress().getDistrictOfResidence());

            return district;
        }

        return null;
    }

    public static boolean isMasterDegreeOrDoctoralDegree(final Registration registration) {
        return (registration.getDegreeType().isSecondCycle() && !isIntegratedMasterDegree(registration))
                || registration.getDegreeType().isThirdCycle();
    }

    public static boolean isIntegratedMasterDegree(final Registration registration) {
        return registration.getDegreeType().isIntegratedMasterDegree();
    }

    public static boolean isDoctoralDegree(final Registration registration) {
        return registration.getDegreeType().isThirdCycle();
    }

    public static boolean isSpecializationDegree(final Registration registration) {
        return registration.getDegreeType().isSpecializationDegree();
    }

    public static PersonalIngressionData personalIngressionData(Registration registration, ExecutionYear executionYear) {
        PersonalIngressionData personalIngressionDataByExecutionYear =
                registration.getStudent().getPersonalIngressionDataByExecutionYear(executionYear);

        if (personalIngressionDataByExecutionYear != null) {
            return personalIngressionDataByExecutionYear;
        }

        for (ExecutionYear ex = executionYear; ex.getPreviousExecutionYear() != null; ex = ex.getPreviousExecutionYear()) {
            if (registration.getStudent().getPersonalIngressionDataByExecutionYear(ex) != null) {
                LegalReportContext.addWarn("", i18n("warn.Raides.validation.using.personal.ingression.data.from.previous.year",
                        formatArgs(registration, executionYear)));

                return registration.getStudent().getPersonalIngressionDataByExecutionYear(ex);
            }
        }

        return null;
    }

    private static String i18n(String key, String... arguments) {
        return ULisboaSpecificationsUtil.bundle(key, arguments);
    }

    // @formatter:off
    /* *******************
     * ADDED ON BLUE RECORD
     * *******************
     */
    // @formatter:on

    /**
     * Check if the previous completed qualification fields are filled
     */
    public static Set<String> verifyCompletePrecedentDegreeInformationFieldsFilled(final Registration registration) {
        final PrecedentDegreeInformation pdi = registration.getStudentCandidacy().getPrecedentDegreeInformation();

        final Set<String> result = new HashSet<String>();

        if (Strings.isNullOrEmpty(pdi.getConclusionGrade())) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.conclusion.grade.required");
        }

        if (pdi.getConclusionYear() == null) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.conclusion.year.required");
        }

        if (pdi.getSchoolLevel() == null) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.school.level.required");
        }

        if (pdi.getSchoolLevel() == SchoolLevelType.OTHER && Strings.isNullOrEmpty(pdi.getOtherSchoolLevel())) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.school.level.required");
        }

        if (pdi.getCountry() == null) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.country.required");
        }

        if (pdi.getInstitution() == null) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.institution.required");
        }

        if (Strings.isNullOrEmpty(pdi.getDegreeDesignation())) {
            result.add("error.Raides.verifyCompletePrecedentDegreeInformationFieldsFilled.degreeDesignation.required");
        }

        return result;
    }

    public static boolean isCompletePrecedentDegreeInformationFieldsFilled(final Registration registration) {
        return verifyCompletePrecedentDegreeInformationFieldsFilled(registration).isEmpty();
    }

    public static boolean isCompletePrecedentDegreeInformationFieldsToBeFilledByStudent(final Registration registration) {
        final PrecedentDegreeInformation pdi = registration.getStudentCandidacy().getPrecedentDegreeInformation();

        if (pdi.getSchoolLevel() != null && !pdi.getSchoolLevel().isHigherEducation()) {
            return false;
        }

        return pdi.getInstitution() == null || Strings.isNullOrEmpty(pdi.getDegreeDesignation());
    }

    private static boolean isPreviousDegreePrecedentDegreeInformationRequired(final RaidesInstance raidesInstance,
            final Registration registration) {
        // For now use degree transfer
        return isDegreeChangeOrTransfer(raidesInstance, registration.getIngressionType());
    }

    /**
     * In case of degree transfer, degree change and mobility check if previous degree or origin
     * information is filled
     */

    public static Set<String> verifyPreviousDegreePrecedentDegreeInformationFieldsFilled(final RaidesInstance raidesInstance,
            final Registration registration) {
        final Set<String> result = new HashSet<String>();

        if (isPreviousDegreePrecedentDegreeInformationRequired(raidesInstance, registration)) {
            return result;
        }

        final PrecedentDegreeInformation pdi = registration.getStudentCandidacy().getPrecedentDegreeInformation();

        if (Strings.isNullOrEmpty(pdi.getPrecedentDegreeDesignation())) {
            result.add(
                    "error.Raides.verifyPreviousDegreePrecedentDegreeInformationFieldsFilled.precedentDegreeDesignation.required");
        }

        if (pdi.getPrecedentSchoolLevel() == null) {
            result.add("error.Raides.verifyPreviousDegreePrecedentDegreeInformationFieldsFilled.precedentSchoolLevel.required");
        }

        if (pdi.getPrecedentInstitution() == null) {
            result.add("error.Raides.verifyPreviousDegreePrecedentDegreeInformationFieldsFilled.precedentInstitution.required");
        }

        if (pdi.getPrecedentCountry() == null) {
            result.add("error.Raides.verifyPreviousDegreePrecedentDegreeInformationFieldsFilled.precedentCountry.required");
        }

        return result;
    }

    public static List<Registration> findActiveFirstTimeRegistrationsOrWithEnrolments(final ExecutionYear executionYear,
            final Student student) {
        final List<Registration> result = Lists.newArrayList();

        for (final Registration registration : student.getRegistrationsSet()) {
            if (!registration.isActive()) {
                continue;
            }

            if (registration.getEnrolments(executionYear).isEmpty() && registration.getRegistrationYear() != executionYear) {
                continue;
            }

            result.add(registration);
        }

        Collections.sort(result, Registration.COMPARATOR_BY_START_DATE);

        return result;
    }

    public static boolean isPreviousDegreePrecedentDegreeInformationFieldsFilled(final RaidesInstance raidesInstance,
            final Registration registration) {
        return verifyPreviousDegreePrecedentDegreeInformationFieldsFilled(raidesInstance, registration).isEmpty();
    }

    public static boolean isPreviousDegreePrecedentDegreeInformationFieldsToBeFilledByStudent(final RaidesInstance raidesInstance,
            final Registration registration) {
        final PrecedentDegreeInformation pdi = registration.getStudentCandidacy().getPrecedentDegreeInformation();
        if (!isPreviousDegreePrecedentDegreeInformationRequired(raidesInstance, registration)) {
            return false;
        }

        return pdi.getPrecedentInstitution() == null;
    }

    public static Set<Degree> getPrecedentDegreesUntilRoot(final Degree degree) {
        final Set<Degree> result = Sets.newHashSet();
        result.addAll(degree.getPrecedentDegreesSet());

        for (final Degree it : degree.getPrecedentDegreesSet()) {
            result.addAll(getPrecedentDegreesUntilRoot(it));
        }

        return result;
    }

    public static Collection<Registration> getPrecedentDegreeRegistrations(final Registration registration) {

        final Set<Degree> precedentDegreesUntilRoot = getPrecedentDegreesUntilRoot(registration.getDegree());
        final Set<Registration> result = Sets.newHashSet();
        for (final Registration it : registration.getStudent().getRegistrationsSet()) {

            if (registration == it) {
                continue;
            }

            if (it.isCanceled() || it.isConcluded() || it.hasConcluded()) {
                continue;
            }

            if (precedentDegreesUntilRoot.contains(it.getDegree())) {
                result.add(it);
            }
        }

        return result;
    }

    public static Collection<CurriculumLine> getAllCurriculumLines(final Registration registration) {
        Set<CurriculumLine> curriculumLines = new HashSet<CurriculumLine>();
        Collection<StudentCurricularPlan> studentCurricularPlans = registration.getStudentCurricularPlansSet();
        for (StudentCurricularPlan studentCurricularPlan : studentCurricularPlans) {
            curriculumLines.addAll(studentCurricularPlan.getAllCurriculumLines());
        }

        return curriculumLines;
    }

    public static Collection<ExecutionYear> getEnrolmentYearsIncludingPrecedentRegistrations(final Registration registration) {
        return getEnrolmentYearsIncludingPrecedentRegistrations(registration, null);
    }

    /**
     * 
     * @param untilExecutionYear is inclusive. null does not apply any filtering
     * 
     * @return
     */
    public static Collection<ExecutionYear> getEnrolmentYearsIncludingPrecedentRegistrations(final Registration registration,
            final ExecutionYear untilExecutionYear) {

        final Set<Registration> registrations = Sets.newHashSet();
        registrations.add(registration);
        registrations.addAll(getPrecedentDegreeRegistrations(registration));

        final Set<ExecutionYear> result = Sets.newHashSet();
        for (final Registration it : registrations) {
            result.addAll(it.getEnrolmentsExecutionYears());
            result.addAll(it.getRegistrationDataByExecutionYearSet().stream().filter(rd -> rd.getEnrolmentDate() != null)
                    .map(rd -> rd.getExecutionYear()).collect(Collectors.toSet()));
        }

        if (untilExecutionYear == null) {
            return result;
        }

        return result.stream().filter(e -> e.isBeforeOrEquals(untilExecutionYear)).collect(Collectors.toSet());
    }

    /**
     * Returns the root registration.
     * 
     * @return This registration if does not have precedent or the oldest precendent registration
     */
    public static Registration getRootRegistration(final Registration registration) {
        final SortedSet<Registration> registrations = Sets.newTreeSet(Registration.COMPARATOR_BY_START_DATE);
        registrations.add(registration);
        registrations.addAll(getPrecedentDegreeRegistrations(registration));

        return registrations.first();
    }

    public static LocalDate getEnrolmentDate(final Registration registration, final ExecutionYear executionYear) {
        if (getRegistrationDataByExecutionYear(registration, executionYear) == null) {
            return null;
        }

        return getRegistrationDataByExecutionYear(registration, executionYear).getEnrolmentDate();
    }

    public static RegistrationDataByExecutionYear getRegistrationDataByExecutionYear(final Registration registration,
            final ExecutionYear year) {
        for (RegistrationDataByExecutionYear registrationData : registration.getRegistrationDataByExecutionYearSet()) {
            if (registrationData.getExecutionYear().equals(year)) {
                return registrationData;
            }
        }
        return null;
    }

    public static District findDistrictByName(final String name) {
        final String n = name;
        for (final District district : Bennu.getInstance().getDistrictsSet()) {
            if (district.getName().equals(n.trim())) {
                return district;
            }
        }

        return null;
    }

    public static DistrictSubdivision findDistrictSubdivisionByName(final District district, final String name) {
        DistrictSubdivision result = null;
        String n = name.trim();
        if (district != null && !Strings.isNullOrEmpty(n)) {
            for (final DistrictSubdivision iter : Bennu.getInstance().getDistrictSubdivisionsSet()) {
                if (iter.getDistrict().equals(district) && n.toLowerCase().equals(iter.getName().toLowerCase())) {
                    if (result != null) {
                        throw new ULisboaSpecificationsDomainException("error.DistrictSubdivision.found.duplicate",
                                district.getCode(), name, result.toString(), iter.toString());
                    }
                    result = iter;
                }
            }
        }

        return result;
    }

    public static RegistrationProtocol findRegistrationProtocolByCode(final String code) {
        return Bennu.getInstance().getRegistrationProtocolsSet().stream().filter(r -> r.getCode().equals(code)).findAny().get();
    }

    //TODO: move to RaidesLegalReportUtil?
    public static String[] formatArgs(Registration registration, ExecutionYear executionYear) {
        return new String[] {

                String.valueOf(registration.getStudent().getNumber()),

                String.valueOf(registration.getNumber()),

                registration.getDegree().getCode(),

                registration.getDegreeNameWithDescription(),

                executionYear == null ? "" : executionYear.getQualifiedName()

        };
    }

    private static String[] concatArgs(String[] left, String... right) {
        final List<String> result = new ArrayList<String>();
        result.addAll(Arrays.asList(left));
        result.addAll(Arrays.asList(right));

        return result.toArray(new String[] {});
    }

}
