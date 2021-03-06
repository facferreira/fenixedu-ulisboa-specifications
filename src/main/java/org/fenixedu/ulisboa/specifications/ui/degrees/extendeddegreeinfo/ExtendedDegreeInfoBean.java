package org.fenixedu.ulisboa.specifications.ui.degrees.extendeddegreeinfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.IBean;
import org.fenixedu.bennu.TupleDataSourceBean;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

public class ExtendedDegreeInfoBean implements IBean {

    private ExecutionYear executionYear;
    private List<TupleDataSourceBean> executionYearOptions;
    private Degree degree;
    private List<TupleDataSourceBean> degreeOptions;
    private String degreeType;
    private String degreeAcron;
    private String degreeSitePublicUrl;
    private String degreeSiteManagementUrl;
    private String auditInfo;

    // DegreeInfo fields
    private LocalizedString name;
    private LocalizedString description;
    private LocalizedString history;
    private LocalizedString objectives;
    private LocalizedString designedFor;
    private LocalizedString professionalExits; // "Professional Exits"? LOL!!
    private LocalizedString operationalRegime;
    private LocalizedString gratuity;
    private LocalizedString additionalInfo;
    private LocalizedString links;
    private LocalizedString testIngression;
    private LocalizedString classifications;
    private LocalizedString accessRequisites;
    private LocalizedString candidacyDocuments;
    private Integer driftsInitial;
    private Integer driftsFirst;
    private Integer driftsSecond;
    private Double markMin;
    private Double markMax;
    private Double markAverage;
    private LocalizedString qualificationLevel;
    private LocalizedString recognitions;
    private LocalizedString prevailingScientificArea;

    // ExtendedDegreeInfo fields
    private LocalizedString scientificAreas;
    private LocalizedString studyProgrammeDuration;
    private LocalizedString studyRegime;
    private LocalizedString studyProgrammeRequirements;
    private LocalizedString higherEducationAccess;
    private LocalizedString professionalStatus;
    private LocalizedString supplementExtraInformation;
    private LocalizedString supplementOtherSources;

    // Read only
    private List<CourseGroupDegreeInfoBean> courseGroupInfos;

    public ExtendedDegreeInfoBean() {
        setExecutionYear(ExecutionYear.findCurrent(null));
        setExecutionYearOptions(ExecutionYear.readNotClosedExecutionYears());

        final Set<Degree> allDegrees = new TreeSet<>((x, y) -> {

            int result = 0;

            final CycleType xFirstCycle = x.getDegreeType().getFirstOrderedCycleType();
            final CycleType yFirstCycle = y.getDegreeType().getFirstOrderedCycleType();
            if (xFirstCycle != null && yFirstCycle != null) {
                result = CycleType.COMPARATOR_BY_LESS_WEIGHT.compare(xFirstCycle, yFirstCycle);
            } else if (xFirstCycle != null) {
                result = -1;
            } else if (yFirstCycle != null) {
                result = 1;
            }

            if (result == 0) {
                result = x.getDegreeType().compareTo(y.getDegreeType());
            }

            if (result == 0) {
                if (x.getCode() != null && y.getCode() != null) {
                    result = x.getCode().compareTo(y.getCode());
                }
            }

            return result;

        });
        allDegrees.addAll(Bennu.getInstance().getDegreesSet());
        setDegree(allDegrees.stream().findFirst().orElse(null));
        setDegreeOptions(allDegrees);
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(final ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public List<TupleDataSourceBean> getExecutionYearOptions() {
        return executionYearOptions;
    }

    public void setExecutionYearOptions(final Collection<ExecutionYear> executionYearOptions) {
        this.executionYearOptions = executionYearOptions.stream().sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).map(ey -> {
            TupleDataSourceBean tupleDataSourceBean = new TupleDataSourceBean();
            tupleDataSourceBean.setId(ey.getExternalId());
            tupleDataSourceBean.setText(ey.getQualifiedName());
            return tupleDataSourceBean;
        }).collect(Collectors.toList());
    }

    public Degree getDegree() {
        return degree;
    }

    public void setDegree(final Degree degree) {
        this.degree = degree;
    }

    public List<TupleDataSourceBean> getDegreeOptions() {
        return degreeOptions;
    }

    public void setDegreeOptions(final Collection<Degree> degreeOptions) {
        this.degreeOptions = degreeOptions.stream().map(d -> {
            TupleDataSourceBean tupleDataSourceBean = new TupleDataSourceBean();
            tupleDataSourceBean.setId(d.getExternalId());
            tupleDataSourceBean
                    .setText((d.getCode() != null ? d.getCode() + " - " : "") + d.getPresentationName(getExecutionYear()));
            return tupleDataSourceBean;
        }).collect(Collectors.toList());
    }

    public String getDegreeType() {
        return degreeType;
    }

    public void setDegreeType(final String degreeType) {
        this.degreeType = degreeType;
    }

    public String getDegreeAcron() {
        return degreeAcron;
    }

    public void setDegreeAcron(final String degreeAcron) {
        this.degreeAcron = degreeAcron;
    }

    public String getDegreeSitePublicUrl() {
        return degreeSitePublicUrl;
    }

    public void setDegreeSitePublicUrl(String degreeSiteUrl) {
        this.degreeSitePublicUrl = degreeSiteUrl;
    }

    public String getDegreeSiteManagementUrl() {
        return degreeSiteManagementUrl;
    }

    public void setDegreeSiteManagementUrl(String degreeSiteManagementUrl) {
        this.degreeSiteManagementUrl = degreeSiteManagementUrl;
    }

    public String getAuditInfo() {
        return auditInfo;
    }

    public void setAuditInfo(String auditInfo) {
        this.auditInfo = auditInfo;
    }

    public LocalizedString getName() {
        return name;
    }

    public void setName(final LocalizedString name) {
        this.name = name;
    }

    public LocalizedString getDescription() {
        return description;
    }

    public void setDescription(final LocalizedString description) {
        this.description = description;
    }

    public LocalizedString getHistory() {
        return history;
    }

    public void setHistory(final LocalizedString history) {
        this.history = history;
    }

    public LocalizedString getObjectives() {
        return objectives;
    }

    public void setObjectives(final LocalizedString objectives) {
        this.objectives = objectives;
    }

    public LocalizedString getDesignedFor() {
        return designedFor;
    }

    public void setDesignedFor(final LocalizedString designedFor) {
        this.designedFor = designedFor;
    }

    public LocalizedString getProfessionalExits() {
        return professionalExits;
    }

    public void setProfessionalExits(final LocalizedString professionalExits) {
        this.professionalExits = professionalExits;
    }

    public LocalizedString getOperationalRegime() {
        return operationalRegime;
    }

    public void setOperationalRegime(final LocalizedString operationalRegime) {
        this.operationalRegime = operationalRegime;
    }

    public LocalizedString getGratuity() {
        return gratuity;
    }

    public void setGratuity(final LocalizedString gratuity) {
        this.gratuity = gratuity;
    }

    public LocalizedString getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(final LocalizedString additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public LocalizedString getLinks() {
        return links;
    }

    public void setLinks(final LocalizedString links) {
        this.links = links;
    }

    public LocalizedString getTestIngression() {
        return testIngression;
    }

    public void setTestIngression(final LocalizedString testIngression) {
        this.testIngression = testIngression;
    }

    public LocalizedString getClassifications() {
        return classifications;
    }

    public void setClassifications(final LocalizedString classifications) {
        this.classifications = classifications;
    }

    public LocalizedString getAccessRequisites() {
        return accessRequisites;
    }

    public void setAccessRequisites(final LocalizedString accessRequisites) {
        this.accessRequisites = accessRequisites;
    }

    public LocalizedString getCandidacyDocuments() {
        return candidacyDocuments;
    }

    public void setCandidacyDocuments(final LocalizedString candidacyDocuments) {
        this.candidacyDocuments = candidacyDocuments;
    }

    public Integer getDriftsInitial() {
        return driftsInitial;
    }

    public void setDriftsInitial(final Integer driftsInitial) {
        this.driftsInitial = driftsInitial;
    }

    public Integer getDriftsFirst() {
        return driftsFirst;
    }

    public void setDriftsFirst(final Integer driftsFirst) {
        this.driftsFirst = driftsFirst;
    }

    public Integer getDriftsSecond() {
        return driftsSecond;
    }

    public void setDriftsSecond(final Integer driftsSecond) {
        this.driftsSecond = driftsSecond;
    }

    public Double getMarkMin() {
        return markMin;
    }

    public void setMarkMin(final Double markMin) {
        this.markMin = markMin;
    }

    public Double getMarkMax() {
        return markMax;
    }

    public void setMarkMax(final Double markMax) {
        this.markMax = markMax;
    }

    public Double getMarkAverage() {
        return markAverage;
    }

    public void setMarkAverage(final Double markAverage) {
        this.markAverage = markAverage;
    }

    public LocalizedString getQualificationLevel() {
        return qualificationLevel;
    }

    public void setQualificationLevel(final LocalizedString qualificationLevel) {
        this.qualificationLevel = qualificationLevel;
    }

    public LocalizedString getRecognitions() {
        return recognitions;
    }

    public void setRecognitions(final LocalizedString recognitions) {
        this.recognitions = recognitions;
    }

    public LocalizedString getPrevailingScientificArea() {
        return prevailingScientificArea;
    }

    public void setPrevailingScientificArea(LocalizedString prevailingScientificArea) {
        this.prevailingScientificArea = prevailingScientificArea;
    }

    public LocalizedString getScientificAreas() {
        return scientificAreas;
    }

    public void setScientificAreas(final LocalizedString scientificAreas) {
        this.scientificAreas = scientificAreas;
    }

    public LocalizedString getStudyProgrammeDuration() {
        return studyProgrammeDuration;
    }

    public void setStudyProgrammeDuration(final LocalizedString studyProgrammeDuration) {
        this.studyProgrammeDuration = studyProgrammeDuration;
    }

    public LocalizedString getStudyRegime() {
        return studyRegime;
    }

    public void setStudyRegime(final LocalizedString studyRegime) {
        this.studyRegime = studyRegime;
    }

    public LocalizedString getStudyProgrammeRequirements() {
        return studyProgrammeRequirements;
    }

    public void setStudyProgrammeRequirements(final LocalizedString studyProgrammeRequirements) {
        this.studyProgrammeRequirements = studyProgrammeRequirements;
    }

    public LocalizedString getHigherEducationAccess() {
        return higherEducationAccess;
    }

    public void setHigherEducationAccess(final LocalizedString higherEducationAccess) {
        this.higherEducationAccess = higherEducationAccess;
    }

    public LocalizedString getProfessionalStatus() {
        return professionalStatus;
    }

    public void setProfessionalStatus(final LocalizedString professionalStatus) {
        this.professionalStatus = professionalStatus;
    }

    public LocalizedString getSupplementExtraInformation() {
        return supplementExtraInformation;
    }

    public void setSupplementExtraInformation(final LocalizedString supplementExtraInformation) {
        this.supplementExtraInformation = supplementExtraInformation;
    }

    public LocalizedString getSupplementOtherSources() {
        return supplementOtherSources;
    }

    public void setSupplementOtherSources(final LocalizedString supplementOtherSources) {
        this.supplementOtherSources = supplementOtherSources;
    }

    public List<CourseGroupDegreeInfoBean> getCourseGroupInfos() {
        return courseGroupInfos;
    }

    public void setCourseGroupInfos(final List<CourseGroupDegreeInfoBean> courseGroupInfos) {
        this.courseGroupInfos = courseGroupInfos;
    }

}
