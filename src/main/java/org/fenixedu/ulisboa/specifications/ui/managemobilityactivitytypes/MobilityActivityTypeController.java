/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: xpto@qub-it.com
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
package org.fenixedu.ulisboa.specifications.ui.managemobilityactivitytypes;

import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.academic.domain.student.mobility.MobilityActivityType;
import org.fenixedu.ulisboa.specifications.dto.student.mobility.MobilityActivityTypeBean;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsBaseController;
import org.fenixedu.ulisboa.specifications.ui.FenixeduUlisboaSpecificationsController;
import org.fenixedu.ulisboa.specifications.util.ULisboaConstants;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@SpringFunctionality(app = FenixeduUlisboaSpecificationsController.class, title = "label.title.manageMobilityActivityTypes",
        accessGroup = "logged")
@RequestMapping(MobilityActivityTypeController.CONTROLLER_URL)
public class MobilityActivityTypeController extends FenixeduUlisboaSpecificationsBaseController {

    public static final String CONTROLLER_URL = "/fenixedu-ulisboa-specifications/managemobilityactivitytypes/mobilityactivitytype";
    public static final String JSP_PATH = "fenixedu-ulisboa-specifications/managemobilityactivitytypes/mobilityactivitytype";

    @RequestMapping
    public String home(Model model) {
        return "forward:" + CONTROLLER_URL + _SEARCH_URI;
    }

    private static final String _SEARCH_URI = "/search";
    public static final String SEARCH_URL = CONTROLLER_URL + _SEARCH_URI;

    @RequestMapping(value = _SEARCH_URI)
    public String search(final Model model) {

        model.addAttribute("searchmobilityactivitytypeResultsDataSet", MobilityActivityType.findAll());
        return jspPage(_SEARCH_URI);
    }

    private static final String _READ_URI = "/read";
    public static final String READ_URL = CONTROLLER_URL + _READ_URI;

    @RequestMapping(value = _READ_URI + "/{oid}")
    public String read(@PathVariable("oid") MobilityActivityType mobilityActivityType, Model model) {
        model.addAttribute("mobilityActivityType", mobilityActivityType);

        return jspPage(_READ_URI);
    }

    private static final String _CREATE_URI = "/create";
    public static final String CREATE_URL = CONTROLLER_URL + _CREATE_URI;

    @RequestMapping(value = _CREATE_URI, method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("mobilityActivityTypeBeanJson", getBeanJson(new MobilityActivityTypeBean()));

        return jspPage(_CREATE_URI);
    }

    @RequestMapping(value = _CREATE_URI, method = RequestMethod.POST)
    public String create(@RequestParam(value = "bean", required = false) MobilityActivityTypeBean bean, final Model model,
            final RedirectAttributes redirectAttributes) {

        try {

            MobilityActivityType mobilityActivityType = MobilityActivityType.create(bean.getCode(),
                    new LocalizedString(ULisboaConstants.DEFAULT_LOCALE, bean.getName()), bean.isActive());

            return redirect(READ_URL + "/" + mobilityActivityType.getExternalId(), model, redirectAttributes);
        } catch (DomainException de) {
            addErrorMessage(de.getLocalizedMessage(), model);

            return create(model);
        }
    }

    private static final String _UPDATE_URI = "/update";
    public static final String UPDATE_URL = CONTROLLER_URL + _UPDATE_URI;

    @RequestMapping(value = _UPDATE_URI + "/{oid}", method = RequestMethod.GET)

    public String update(@PathVariable("oid") final MobilityActivityType mobilityActivityType, final Model model) {
        model.addAttribute("mobilityActivityType", mobilityActivityType);
        model.addAttribute("mobilityActivityTypeBeanJson", getBeanJson(new MobilityActivityTypeBean(mobilityActivityType)));

        return jspPage(_UPDATE_URI);
    }

    @RequestMapping(value = _UPDATE_URI + "/{oid}", method = RequestMethod.POST)
    public String update(@PathVariable("oid") final MobilityActivityType mobilityActivityType,
            @RequestParam(value = "bean", required = false) MobilityActivityTypeBean bean, final Model model,
            final RedirectAttributes redirectAttributes) {

        try {

            mobilityActivityType.edit(bean.getCode(), new LocalizedString(ULisboaConstants.DEFAULT_LOCALE, bean.getName()),
                    bean.isActive());
            
            return redirect(READ_URL + "/" + mobilityActivityType.getExternalId(), model, redirectAttributes);
        } catch (DomainException de) {
            addErrorMessage(de.getLocalizedMessage(), model);
            return update(mobilityActivityType, model);
        }
    }

    private static final String _DELETE_URI = "/delete";
    public static final String DELETE_URL = CONTROLLER_URL + _DELETE_URI;

    @RequestMapping(value = _DELETE_URI, method = RequestMethod.POST)
    public String delete(@RequestParam("id") final MobilityActivityType mobilityActivityType, final Model model,
            final RedirectAttributes redirectAttributes) {

        try {
            mobilityActivityType.delete();
            return redirect(SEARCH_URL, model, redirectAttributes);
        } catch (DomainException de) {
            addErrorMessage(de.getLocalizedMessage(), model);
            return read(mobilityActivityType, model);
        }
    }

    private String jspPage(final String page) {
        return JSP_PATH + "/" + page.substring(1, page.length());
    }

}
