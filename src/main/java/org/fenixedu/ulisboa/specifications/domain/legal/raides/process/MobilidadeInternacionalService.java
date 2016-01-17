package org.fenixedu.ulisboa.specifications.domain.legal.raides.process;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Qualification;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.ulisboa.specifications.domain.legal.LegalReportContext;
import org.fenixedu.ulisboa.specifications.domain.legal.mapping.LegalMapping;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.Raides;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.TblMobilidadeInternacional;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.mapping.LegalMappingType;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.report.RaidesRequestParameter;
import org.fenixedu.ulisboa.specifications.domain.legal.report.LegalReport;

import com.google.common.base.Strings;

import net.fortuna.ical4j.model.parameter.Language;

public class MobilidadeInternacionalService extends RaidesService {

    public MobilidadeInternacionalService(final LegalReport report) {
        super(report);
    }

    public TblMobilidadeInternacional create(RaidesRequestParameter raidesRequestParameter,
            final ExecutionYear executionYear, final Registration registration) {
        final Unit institutionUnit = raidesRequestParameter.getInstitution();

        final TblMobilidadeInternacional bean = new TblMobilidadeInternacional();

        bean.setRegistration(registration);
        preencheInformacaoMatricula(report, bean, institutionUnit, executionYear, registration);

        final Qualification previousQualification = registration.getStudentCandidacy().getPreviousQualification();

        bean.setAnoCurricular(anoCurricular(registration, executionYear));
        bean.setPrimeiraVez(LegalMapping.find(report, LegalMappingType.BOOLEAN).translate(isFirstTimeOnDegree(registration, executionYear)));
        bean.setEctsInscrito(enrolledEcts(executionYear, registration).toString());
        bean.setRegimeFrequencia(regimeFrequencia(registration, executionYear));

        if (registration.getRegistrationProtocol() != null) {
            bean.setProgMobilidade(LegalMapping.find(report, LegalMappingType.INTERNATIONAL_MOBILITY_PROGRAM_AGREEMENT).translate(
                    registration.getRegistrationProtocol()));

            if (Raides.ProgramaMobilidade.OUTRO_TRES.equals(bean.getProgMobilidade())) {
                bean.setOutroPrograma(registration.getRegistrationProtocol().getName().getContent(Language.getDefaultLanguage()));
            }
        }

        if (registration.getRegistrationProtocol() != null && registration.getRegistrationProtocol().isMobility()) {
            MobilityRegistrationInformation mobilityInformation = registration.getIncomingMobility();
            if (mobilityInformation != null) {
                if (mobilityInformation.getMobilityActivityType() != null) {
                    bean.setTipoProgMobilidade(LegalMapping.find(report, LegalMappingType.INTERNATIONAL_MOBILITY_ACTIVITY).translate(
                            mobilityInformation.getMobilityActivityType()));
                } else {
                    bean.setTipoProgMobilidade(Raides.ActividadeMobilidade.MOBILIDADE_ESTUDO);
                }
                if (mobilityInformation.getProgramDuration() != null) {
                    bean.setDuracaoPrograma(LegalMapping.find(report, LegalMappingType.SCHOOL_PERIOD_DURATION).translate(
                            mobilityInformation.getProgramDuration()));
                }
            }
        }

        if (previousQualification.getSchoolLevel() != null) {
            bean.setNivelCursoOrigem(LegalMapping.find(report, LegalMappingType.MOBILITY_SCHOOL_LEVEL).translate(
                    previousQualification.getSchoolLevel()));
            if (Raides.NivelCursoOrigem.OUTRO.equals(bean.getNivelCursoOrigem())) {
                bean.setOutroNivelCurOrigem(previousQualification.getOtherSchoolLevel());
            }
            if (Raides.NivelCursoOrigem.OUTRO.equals(bean.getNivelCursoOrigem())
                    && Strings.isNullOrEmpty(bean.getOutroNivelCurOrigem())) {
                bean.setOutroNivelCurOrigem(previousQualification.getSchoolLevel().getLocalizedName());
            }
        }

        validaNivelCursoOrigem(executionYear, registration, bean);
        return bean;
    }

    protected void validaNivelCursoOrigem(final ExecutionYear executionYear, final Registration registration,
            final TblMobilidadeInternacional bean) {
        if (Strings.isNullOrEmpty(bean.getNivelCursoOrigem())) {
            LegalReportContext.addError(
                    "",
                    i18n("error.Raides.validation.mobility.provenance.school.level.empty",
                            String.valueOf(registration.getStudent().getNumber()), registration.getDegreeNameWithDescription(),
                            executionYear.getQualifiedName()));
            bean.markAsInvalid();
        } else if (Raides.NivelCursoOrigem.OUTRO.equals(bean.getNivelCursoOrigem())
                && Strings.isNullOrEmpty(bean.getOutroNivelCurOrigem())) {
            LegalReportContext.addError(
                    "",
                    i18n("error.Raides.validation.mobility.other.provenance.school.level.empty",
                            String.valueOf(registration.getStudent().getNumber()), registration.getDegreeNameWithDescription(),
                            executionYear.getQualifiedName()));
            bean.markAsInvalid();
        }
    }

    protected boolean isFirstTimeOnDegree(final Registration registration, final ExecutionYear executionYear) {
        if (!registration.getDegree().isEmpty()) {
            return isFirstTimeOnDegree(registration, executionYear);
        }
        if (registration.getRootRegistration() != registration) {
            return false;
        }

        return executionYear == registration.getStartExecutionYear();
    }
}
