package org.fenixedu.ulisboa.specifications.ui.blue_record;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.BennuSpringController;
import org.fenixedu.ulisboa.specifications.domain.PersonUlisboaSpecifications;
import org.fenixedu.ulisboa.specifications.domain.legal.raides.Raides;
import org.fenixedu.ulisboa.specifications.domain.student.access.StudentAccessServices;
import org.fenixedu.ulisboa.specifications.ui.firstTimeCandidacy.CgdDataAuthorizationController;
import org.fenixedu.ulisboa.specifications.ui.firstTimeCandidacy.DocumentsPrintController;
import org.fenixedu.ulisboa.specifications.ui.firstTimeCandidacy.FirstTimeCandidacyController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@BennuSpringController(value = BlueRecordEntryPoint.class)
@RequestMapping(CgdDataAuthorizationControllerBlueRecord.CONTROLLER_URL)
public class CgdDataAuthorizationControllerBlueRecord extends CgdDataAuthorizationController {

    public static final String CONTROLLER_URL =
            "/fenixedu-ulisboa-specifications/blueRecord/{executionYearId}/cgddataauthorization";

    @Override
    public String back(@PathVariable("executionYearId") final ExecutionYear executionYear, final Model model, RedirectAttributes redirectAttributes) {
        addControllerURLToModel(executionYear, model);
        String url = MotivationsExpectationsFormControllerBlueRecord.CONTROLLER_URL;
        return redirect(urlWithExecutionYear(url, executionYear), model, redirectAttributes);
    }

    @Override
    public String cgddataauthorization(@PathVariable("executionYearId") final ExecutionYear executionYear, final Model model,
            final RedirectAttributes redirectAttributes) {
        addControllerURLToModel(executionYear, model);
        if (isFormIsFilled(executionYear, model)) {
            return nextScreen(executionYear, model, redirectAttributes);
        }

        return "fenixedu-ulisboa-specifications/firsttimecandidacy/cgddataauthorization";
    }

    protected String nextScreen(final ExecutionYear executionYear, final Model model,
            final RedirectAttributes redirectAttributes) {
        return redirect(urlWithExecutionYear(BlueRecordEnd.CONTROLLER_URL, executionYear), model, redirectAttributes);
    }

    @RequestMapping(value = "/authorize")
    public String cgddataauthorizationToAuthorize(@PathVariable("executionYearId") final ExecutionYear executionYear,
            final Model model, final RedirectAttributes redirectAttributes) {
        addControllerURLToModel(executionYear, model);
        if (isFormIsFilled(executionYear, model)) {
            return nextScreen(executionYear, model, redirectAttributes);
        }

        authorizeSharingDataWithCGD(true);

        final Registration registration = findFirstTimeRegistration(executionYear);
        boolean wsCallSuccess = StudentAccessServices.triggerSyncRegistrationToExternal(registration);

        if (wsCallSuccess) {
            return redirect(DocumentsPrintController.CONTROLLER_URL, model, redirectAttributes);
        } else {
            return redirect(DocumentsPrintController.WITH_MODEL43_URL, model, redirectAttributes);
        }
    }

    @RequestMapping(value = "/unauthorize")
    public String cgddataauthorizationToUnauthorize(@PathVariable("executionYearId") final ExecutionYear executionYear,
            final Model model, final RedirectAttributes redirectAttributes) {
        addControllerURLToModel(executionYear, model);
        if (isFormIsFilled(executionYear, model)) {
            return nextScreen(executionYear, model, redirectAttributes);
        }

        authorizeSharingDataWithCGD(false);

        return redirect(DocumentsPrintController.WITH_MODEL43_URL, model, redirectAttributes);
    }

    @Override
    protected String getControllerURL() {
        return CONTROLLER_URL;
    }

    @Override
    public boolean isFormIsFilled(final ExecutionYear executionYear, final Student student) {
        final Registration firstTimeRegistration = findFirstTimeRegistration(executionYear);
        if(firstTimeRegistration == null) {
            return true;
        }
        
        if(firstTimeRegistration.getPerson().getPersonUlisboaSpecifications() == null) {
            return true;
        }
        
        return firstTimeRegistration.getPerson().getPersonUlisboaSpecifications().getSharingDataWithCGDAnswered();
    }

    @Override
    protected Student getStudent(Model model) {
        return AccessControl.getPerson().getStudent();
    }
    
    private Registration findFirstTimeRegistration(final ExecutionYear executionYear) {
        final List<Registration> registrations =
                Raides.findActiveFirstTimeRegistrationsOrWithEnrolments(executionYear, AccessControl.getPerson().getStudent());
        return registrations.stream().filter(r -> r.getRegistrationYear() == executionYear).findFirst().orElse(null);
    }

}