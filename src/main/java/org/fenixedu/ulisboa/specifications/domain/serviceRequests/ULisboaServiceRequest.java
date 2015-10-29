/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: diogo.simoes@qub-it.com
 *               jnpa@reitoria.ulisboa.pt
 *
 * 
 * This file is part of FenixEdu QubDocs.
 *
 * FenixEdu QubDocs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu QubDocs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu QubDocs.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fenixedu.ulisboa.specifications.domain.serviceRequests;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.AcademicProgram;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.documents.DocumentRequestGeneratedDocument;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituation;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.ServiceRequestType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.AcademicServiceRequestType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentPurposeTypeInstance;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.ExternalEnrolment;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academic.dto.serviceRequests.AcademicServiceRequestBean;
import org.fenixedu.academic.dto.serviceRequests.AcademicServiceRequestCreateBean;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.report.academicAdministrativeOffice.AdministrativeOfficeDocument;
import org.fenixedu.academic.util.report.ReportsUtils;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.ulisboa.specifications.domain.serviceRequests.validators.ULisboaServiceRequestValidator;
import org.fenixedu.ulisboa.specifications.dto.ServiceRequestPropertyBean;
import org.fenixedu.ulisboa.specifications.dto.ULisboaServiceRequestBean;
import org.fenixedu.ulisboa.specifications.service.reports.DocumentPrinter;
import org.fenixedu.ulisboa.specifications.util.Constants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DeletionListener;

import com.google.common.base.Strings;

public final class ULisboaServiceRequest extends ULisboaServiceRequest_Base implements ITreasuryServiceRequest {

    /**
     * TODOJN onde documentar isto
     * AcademicServiceRequest API used
     * get/set RequestDate
     * get/set AcademicServiceRequestYear
     * get/set ServiceRequestNumber
     * get/set AdministrativeOffice
     * get/set ServiceRequestType
     * get/set Registration
     * get/add AcademicServiceRequestSituation
     */

    /**
     * Constructors
     */

    protected ULisboaServiceRequest() {
        super();
    }

    protected ULisboaServiceRequest(ServiceRequestType serviceRequestType, Registration registration) {
        this();
        setServiceRequestType(serviceRequestType);
        initAcademicServiceRequest(registration);
        setRegistration(registration);
        setIsValid(true);
    }

    protected void initAcademicServiceRequest(Registration registration) {
        //Use the Academic Service Request init, because there is unaccessible methods
        AcademicServiceRequestCreateBean bean = new AcademicServiceRequestCreateBean(registration);
        bean.setRequestDate(new DateTime());
        bean.setRequestedCycle(registration.getDegreeType().getFirstOrderedCycleType());
        bean.setUrgentRequest(Boolean.FALSE);
        bean.setFreeProcessed(Boolean.FALSE);
        bean.setLanguage(I18N.getLocale());
        super.init(bean, registration.getDegree().getAdministrativeOffice());
    }

    @Atomic
    public static ULisboaServiceRequest createULisboaServiceRequest(ULisboaServiceRequestBean bean) {
        ULisboaServiceRequest request = new ULisboaServiceRequest(bean.getServiceRequestType(), bean.getRegistration());
        for (ServiceRequestPropertyBean propertyBean : bean.getServiceRequestPropertyBeans()) {
            ServiceRequestProperty property = ServiceRequestSlot.createProperty(propertyBean.getCode(), propertyBean.getValue());
            request.addServiceRequestProperties(property);
        }
        return request;
    }

    /**
     * Delete methods
     * Academic Service Request calls these two methods
     */

    @Override
    protected void disconnect() {
        for (ServiceRequestProperty property : getServiceRequestPropertiesSet()) {
            property.delete();
        }
        setRegistration(null);
        setServiceRequestType(null);
        super.disconnect();
    }

    @Override
    protected void checkRulesToDelete() {

        super.checkRulesToDelete();
    }

    /**
     * Implementation of interfaces
     * QubDocReports Service Request Interface
     * Treasury Service Request Interface
     */

    public byte[] generateDocument() {
        try {
            byte[] data = DocumentPrinter.print(this).getData();
            //DocumentRequestGeneratedDocument.store(this, documents.iterator().next().getReportFileName() + ".pdf", data);
            return data;
        } catch (Exception e) {
            throw new DomainException("error.documentRequest.errorGeneratingDocument", e);
        }
    }

    @Override
    public boolean isToPrint() {
        return getServiceRequestType().isPrintable();
    }

    @Override
    public Person getPerson() {
        return hasRegistation() ? getRegistration().getPerson() : null;
    }

    @Override
    public boolean hasRegistation() {
        return getRegistration() != null;
    }

    @Override
    public Locale getLanguage() {
        return hasLanguage() ? findProperty(Constants.LANGUAGE).getLocale() : null;
    }

    @Override
    public boolean hasLanguage() {
        return hasProperty(Constants.LANGUAGE);
    }

    @Override
    public boolean isDetailed() {
        ServiceRequestProperty detailedProperty = findProperty(Constants.IS_DETAILED);
        return detailedProperty != null && detailedProperty.getBooleanValue() != null ? detailedProperty.getBooleanValue() : false;
    }

    @Override
    public Integer getNumberOfUnits() {
        return hasNumberOfUnits() ? findProperty(Constants.NUMBER_OF_UNITS).getInteger() : null;
    }

    @Override
    public boolean hasNumberOfUnits() {
        return hasProperty(Constants.NUMBER_OF_UNITS);
    }

    @Override
    public boolean isUrgent() {
        ServiceRequestProperty urgentProperty = findProperty(Constants.IS_URGENT);
        return urgentProperty != null && urgentProperty.getBooleanValue() != null ? urgentProperty.getBooleanValue() : false;
    }

    @Override
    public Integer getNumberOfPages() {
        return hasNumberOfPages() ? findProperty(Constants.NUMBER_OF_PAGES).getInteger() : null;
    }

    @Override
    public boolean hasNumberOfPages() {
        return hasProperty(Constants.NUMBER_OF_PAGES);
    }

    @Override
    public CycleType getCycleType() {
        return hasCycleType() ? findProperty(Constants.CYCLE_TYPE).getCycleType() : null;
    }

    @Override
    public boolean hasCycleType() {
        return hasProperty(Constants.CYCLE_TYPE);
    }

    @Override
    public ExecutionYear getExecutionYear() {
        return hasExecutionYear() ? findProperty(Constants.EXECUTION_YEAR).getExecutionYear() : null;
    }

    @Override
    public boolean hasExecutionYear() {
        return hasProperty(Constants.EXECUTION_YEAR);
    }

    @Override
    public String getDescription() {
        return getServiceRequestType().getName().getContent();
    }

    @Override
    public boolean isFor(ExecutionYear executionYear) {
        if (hasProperty(Constants.EXECUTION_YEAR)) {
            return findProperty(Constants.EXECUTION_YEAR).getExecutionYear().equals(executionYear);
        }
        return false;
    }

    @Override
    protected boolean hasMissingPersonalInfo() {
        return getPerson() == null || Strings.isNullOrEmpty(getPerson().getName())
                || (getPerson().getDateOfBirthYearMonthDay() == null) || Strings.isNullOrEmpty(getPerson().getDocumentIdNumber())
                || (getPerson().getIdDocumentType() == null);
    }

    public ServiceRequestProperty findProperty(String slotCode) {
        Optional<ServiceRequestProperty> property =
                getServiceRequestPropertiesSet().stream().filter(prop -> prop.getServiceRequestSlot().getCode().equals(slotCode))
                        .findFirst();
        if (property.isPresent()) {
            return property.get();
        }
        return null;
    }

    public boolean hasProperty(String slotCode) {
        return getServiceRequestPropertiesSet().stream()
                .filter(property -> property.getServiceRequestSlot().getCode().equals(slotCode)).findFirst().isPresent();
    }

    public List<AcademicServiceRequestSituation> getAcademicServiceRequestSituationOrderedList() {
        return getAcademicServiceRequestSituationsSet().stream()
                .sorted(AcademicServiceRequestSituation.COMPARATOR_BY_MOST_RECENT_SITUATION_DATE_AND_ID)
                .collect(Collectors.toList());
    }

    /**
     * Change State Methods
     */

    @Atomic
    public void transitToProcessState() {
        if (getAcademicServiceRequestSituationType() != AcademicServiceRequestSituationType.NEW) {
            throw new DomainException("error.serviceRequests.ULisboaServiceRequest.invalid.changeState");
        }
        transitState(AcademicServiceRequestSituationType.PROCESSING, Constants.EMPTY_JUSTIFICATION.getContent());
        validate();
    }

    @Atomic
    public void transitToConcludedState() {
        if (getAcademicServiceRequestSituationType() != AcademicServiceRequestSituationType.PROCESSING) {
            throw new DomainException("error.serviceRequests.ULisboaServiceRequest.invalid.changeState");
        }
        transitState(AcademicServiceRequestSituationType.CONCLUDED, Constants.EMPTY_JUSTIFICATION.getContent());
        validate();
        //TODOJN create validator to check if has generated one time the document
        if (getServiceRequestType().getNotifyUponConclusion().booleanValue()) {
            sendMail();
        }
    }

    @Atomic
    public void transitToDeliverState() {
        if (getAcademicServiceRequestSituationType() != AcademicServiceRequestSituationType.CONCLUDED) {
            throw new DomainException("error.serviceRequests.ULisboaServiceRequest.invalid.changeState");
        }
        transitState(AcademicServiceRequestSituationType.DELIVERED, Constants.EMPTY_JUSTIFICATION.getContent());
        validate();
    }

    @Atomic
    public void transitToCancelState(String justification) {
        if (getAcademicServiceRequestSituationType() == AcademicServiceRequestSituationType.DELIVERED) {
            throw new DomainException("error.serviceRequests.ULisboaServiceRequest.invalid.changeState");
        }
        transitState(AcademicServiceRequestSituationType.CANCELLED, justification);
        validate();
    }

    @Atomic
    public void transitToRejectState(String justification) {
        if (getAcademicServiceRequestSituationType() != AcademicServiceRequestSituationType.DELIVERED) {
            throw new DomainException("error.serviceRequests.ULisboaServiceRequest.invalid.changeState");
        }
        transitState(AcademicServiceRequestSituationType.REJECTED, justification);
        validate();
    }

    @Atomic
    public void revertState(boolean notifyRevertAction) {
        if (notifyRevertAction) {
            sendMail();
        }
        AcademicServiceRequestSituation previousSituation = getAcademicServiceRequestSituationOrderedList().get(1);
        transitState(previousSituation.getAcademicServiceRequestSituationType(), previousSituation.getJustification());
        validate();
    }

    private void transitState(AcademicServiceRequestSituationType type, String justification) {
        if (type == AcademicServiceRequestSituationType.CANCELLED || type == AcademicServiceRequestSituationType.REJECTED) {
            Signal.emit(ITreasuryBridgeAPI.ACADEMIC_SERVICE_REQUEST_REJECT_OR_CANCEL_EVENT,
                    new DomainObjectEvent<ULisboaServiceRequest>(this));
        } else {
            Signal.emit(ITreasuryBridgeAPI.ACADEMIC_SERVICE_REQUEST_NEW_SITUATION_EVENT,
                    new DomainObjectEvent<ULisboaServiceRequest>(this));
        }
        AcademicServiceRequestBean bean = new AcademicServiceRequestBean(type, AccessControl.getPerson(), justification);
        createAcademicServiceRequestSituations(bean);
    }

    @Override
    public AcademicServiceRequestSituation getSituationByType(AcademicServiceRequestSituationType type) {
        List<AcademicServiceRequestSituation> situationsSet = getAcademicServiceRequestSituationOrderedList();
        for (int i = 0; i < situationsSet.size(); i++) {
            AcademicServiceRequestSituation situation = situationsSet.get(i);
            if (i != 0
                    || !isValidTransition(situationsSet.get(i - 1).getAcademicServiceRequestSituationType(),
                            situation.getAcademicServiceRequestSituationType())) {
                break;
            }
            if (situation.getAcademicServiceRequestSituationType() == type) {
                return situation;
            }
        }
        return null;
    }

    private boolean isValidTransition(AcademicServiceRequestSituationType previousType,
            AcademicServiceRequestSituationType currentType) {
        //Final states
        if (currentType == AcademicServiceRequestSituationType.DELIVERED
                || currentType == AcademicServiceRequestSituationType.CANCELLED
                || currentType == AcademicServiceRequestSituationType.REJECTED) {
            return true;
        }
        if (previousType == AcademicServiceRequestSituationType.PROCESSING
                && currentType == AcademicServiceRequestSituationType.CONCLUDED) {
            return true;
        }
        if (previousType == AcademicServiceRequestSituationType.NEW
                && currentType == AcademicServiceRequestSituationType.PROCESSING) {
            return true;
        }
        return false;
    }

    private void validate() {
        for (ULisboaServiceRequestValidator uLisboaServiceRequestValidator : getServiceRequestType()
                .getULisboaServiceRequestValidatorsSet()) {
            uLisboaServiceRequestValidator.validate(this);
        }
    }

    private void sendMail() {
        // TODOJN Auto-generated method stub

    }

    /**
     * Static services
     */

    public static Stream<ULisboaServiceRequest> findAll() {
        return Bennu.getInstance().getAcademicServiceRequestsSet().stream()
                .filter(request -> request instanceof ULisboaServiceRequest).map(ULisboaServiceRequest.class::cast);
    }

    public static Stream<ULisboaServiceRequest> findByRegistration(Registration registration) {
        return findAll().filter(request -> request.getRegistration().equals(registration));
    }

    public static Stream<ULisboaServiceRequest> findNewAcademicServiceRequests(Registration registration) {
        return findByRegistration(registration).filter(
                request -> request.getAcademicServiceRequestSituationType() == AcademicServiceRequestSituationType.NEW);
    }

    public static Stream<ULisboaServiceRequest> findProcessingAcademicServiceRequests(Registration registration) {
        return findByRegistration(registration).filter(
                request -> request.getAcademicServiceRequestSituationType() == AcademicServiceRequestSituationType.PROCESSING);
    }

    public static Stream<ULisboaServiceRequest> findToDeliverAcademicServiceRequests(Registration registration) {
        return findByRegistration(registration).filter(
                request -> request.getAcademicServiceRequestSituationType() == AcademicServiceRequestSituationType.DELIVERED);
    }

    /**
     * Delete Listener for Service Request Properties Relations
     * Delete Listener for Service Request Type
     */

    public static void setupListenerForPropertiesDeletion() {
        //Registration
        FenixFramework.getDomainModel().registerDeletionListener(Registration.class, new DeletionListener<Registration>() {

            @Override
            public void deleting(Registration registration) {
                for (ULisboaServiceRequest request : registration.getULisboaServiceRequestsSet()) {
                    request.setRegistration(null);
                    request.setIsValid(false);
                }
            }
        });
        //DocumentPurposeTypeInstance
        FenixFramework.getDomainModel().registerDeletionListener(DocumentPurposeTypeInstance.class,
                new DeletionListener<DocumentPurposeTypeInstance>() {

                    @Override
                    public void deleting(DocumentPurposeTypeInstance documentPurposeTypeInstance) {
                        for (ServiceRequestProperty property : documentPurposeTypeInstance.getServiceRequestPropertiesSet()) {
                            property.setDocumentPurposeTypeInstance(null);
                            property.getULisboaServiceRequest().setIsValid(false);
                        }
                    }
                });
        //Execution Year
        FenixFramework.getDomainModel().registerDeletionListener(ExecutionYear.class, new DeletionListener<ExecutionYear>() {

            @Override
            public void deleting(ExecutionYear executionYear) {
                for (ServiceRequestProperty property : executionYear.getServiceRequestPropertiesSet()) {
                    property.setExecutionYear(null);
                    property.getULisboaServiceRequest().setIsValid(false);
                }
            }
        });
        //Student Curricular Plan
        FenixFramework.getDomainModel().registerDeletionListener(StudentCurricularPlan.class,
                new DeletionListener<StudentCurricularPlan>() {

                    @Override
                    public void deleting(StudentCurricularPlan studentCurricularPlan) {
                        for (ServiceRequestProperty property : studentCurricularPlan.getServiceRequestPropertiesSet()) {
                            property.setStudentCurricularPlan(null);
                            property.getULisboaServiceRequest().setIsValid(false);
                        }
                    }
                });
        //ExternalEnrolment
        FenixFramework.getDomainModel().registerDeletionListener(ExternalEnrolment.class,
                new DeletionListener<ExternalEnrolment>() {

                    @Override
                    public void deleting(ExternalEnrolment externalEnrolment) {
                        for (ServiceRequestProperty property : externalEnrolment.getServiceRequestPropertiesSet()) {
                            property.removeExternalEnrolments(externalEnrolment);
                            property.getULisboaServiceRequest().setIsValid(false);
                        }
                    }
                });
        //CurriculumLine
        FenixFramework.getDomainModel().registerDeletionListener(CurriculumLine.class, new DeletionListener<CurriculumLine>() {

            @Override
            public void deleting(CurriculumLine curriculumLine) {
                for (ServiceRequestProperty property : curriculumLine.getServiceRequestPropertiesSet()) {
                    property.removeCurriculumLines(curriculumLine);
                    property.getULisboaServiceRequest().setIsValid(false);
                }

            }
        });
    }

    public static void setupListenerForServiceRequestTypeDeletion() {
        FenixFramework.getDomainModel().registerDeletionListener(ServiceRequestType.class,
                new DeletionListener<ServiceRequestType>() {
                    @Override
                    public void deleting(ServiceRequestType serviceRequestType) {
                        serviceRequestType.getULisboaServiceRequestValidatorsSet().clear();
                        serviceRequestType.getServiceRequestSlotsSet().clear();
                    }
                });
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
     * ****************
     * <Deprecated API>
     * ****************
     */
    @Deprecated
    @Override
    public void edit(AcademicServiceRequestBean academicServiceRequestBean) {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.workFlow");
    }

    @Deprecated
    @Override
    protected String getDescription(AcademicServiceRequestType academicServiceRequestType, String specificServiceType) {
        return getDescription();
    }

    @Deprecated
    @Override
    protected String getDescription(AcademicServiceRequestType academicServiceRequestType) {
        return getDescription();
    }

    @Deprecated
    @Override
    public boolean isPayedUponCreation() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isPossibleToSendToOtherEntity() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isManagedWithRectorateSubmissionBatch() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public EventType getEventType() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public AcademicServiceRequestType getAcademicServiceRequestType() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean hasPersonalInfo() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public AcademicProgram getAcademicProgram() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected void checkRulesToChangeState(AcademicServiceRequestSituationType situationType) {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isDownloadPossible() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected void internalChangeState(AcademicServiceRequestBean academicServiceRequestBean) {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected void verifyIsToDeliveredAndIsPayed(AcademicServiceRequestBean academicServiceRequestBean) {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected void verifyIsToProcessAndHasPersonalInfo(AcademicServiceRequestBean academicServiceRequestBean) {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isPiggyBackedOnRegistry() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isCanGenerateRegistryCode() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isRequestForPerson() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isRequestForPhd() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isRequestForRegistration() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected void internalRevertToProcessingState() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public void revertToProcessingState() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isDiploma() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isPastDiploma() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isRegistryDiploma() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isDiplomaSupplement() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isBatchSet() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    public boolean isRequestedWithCycle() {
        return hasCycleType();
    }

    @Deprecated
    @Override
    public boolean hasRegistryCode() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected List<AcademicServiceRequestSituationType> getNewSituationAcceptedSituationsTypes() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected List<AcademicServiceRequestSituationType> getProcessingSituationAcceptedSituationsTypes() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected List<AcademicServiceRequestSituationType> getSentToExternalEntitySituationAcceptedSituationsTypes() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected List<AcademicServiceRequestSituationType> getReceivedFromExternalEntitySituationAcceptedSituationsTypes() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    @Deprecated
    @Override
    protected List<AcademicServiceRequestSituationType> getConcludedSituationAcceptedSituationsTypes() {
        throw new DomainException("error.serviceRequests.ULisboaServiceRequest.deprecated.method");
    }

    /*
     * *****************
     * </Deprecated API>
     * *****************
     */

}
