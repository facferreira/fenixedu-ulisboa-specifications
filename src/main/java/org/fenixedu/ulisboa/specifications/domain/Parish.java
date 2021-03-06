package org.fenixedu.ulisboa.specifications.domain;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.DistrictSubdivision;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class Parish extends Parish_Base {

    public Parish(String code, String name, DistrictSubdivision districtSubdivision) {
        super();

        if (StringUtils.isEmpty(code)) {
            throw new DomainException("label.error.emptyCode");
        }
        if (StringUtils.isEmpty(name)) {
            throw new DomainException("label.error.emptyName");
        }
        boolean codeExists = districtSubdivision.getParishSet().stream().anyMatch(p -> p.getCode().equals(code));
        if (codeExists) {
            throw new DomainException("label.error.uniqueParishCode");
        }
        setCode(code);
        setName(normalizeString(name));
        setDistrictSubdivision(districtSubdivision);

        setRoot(Bennu.getInstance());
    }

    public static Optional<Parish> findByName(DistrictSubdivision districtSubdivision, String name) {
        if (districtSubdivision == null) {
            return Optional.empty();
        }
        return districtSubdivision.getParishSet().stream().filter(p -> p.getName().equals(name)).findAny();
    }

    public static Optional<Parish> findByCode(DistrictSubdivision districtSubdivision, String code) {
        if (districtSubdivision == null) {
            return Optional.empty();
        }
        return districtSubdivision.getParishSet().stream().filter(p -> p.getCode().equals(code)).findAny();
    }

    private String normalizeString(String string) {
        if (!StringUtils.isEmpty(string)) {
            String[] split = string.split(" ");
            String output = "";
            for (int i = 0; i < split.length; i++) {
                if (i != 0) {
                    output += " ";
                }
                String part = split[i];
                output += part.substring(0, 1).toUpperCase();

                if (part.length() > 1) {
                    output += part.substring(1, part.length()).toLowerCase();
                }
            }
            return output;
        }
        return "";
    }
}
