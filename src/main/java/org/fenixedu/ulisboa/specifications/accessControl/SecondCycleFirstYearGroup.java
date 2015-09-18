package org.fenixedu.ulisboa.specifications.accessControl;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.ulisboa.specifications.domain.SecondCycleFirstYearPersistentGroup;
import org.joda.time.DateTime;

@Deprecated
@GroupOperator(SecondCycleFirstYearGroup.GROUP_OPERATOR)
public class SecondCycleFirstYearGroup extends CustomGroup {
    public static final String GROUP_OPERATOR = "secondCycleFirstYear";
    static DegreeType masterBolonha = DegreeType.matching(x -> x.getCode().equals("BOLONHA_MASTER_DEGREE")).get();

    @Override
    public String getPresentationName() {
        return GROUP_OPERATOR;
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return SecondCycleFirstYearPersistentGroup.getInstance();
    }

    @Override
    public Set<User> getMembers() {
        return ExecutionYear.readCurrentExecutionYear().getStudentsSet().stream().filter(r -> r.getDegreeType() == masterBolonha)
                .map(r -> r.getPerson().getUser()).collect(Collectors.toSet());
    }

    @Override
    public Set<User> getMembers(DateTime when) {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public boolean isMember(User user) {
        if (user == null) {
            return false;
        }
        Student student = user.getPerson().getStudent();
        if (student != null) {
            return isMemberStudent(student);
        }
        return false;
    }

    private boolean isMemberStudent(Student student) {
        return student
                .getActiveRegistrations()
                .stream()
                .anyMatch(
                        r -> r.getStartExecutionYear() == ExecutionYear.readCurrentExecutionYear()
                                && r.getDegreeType() == masterBolonha);
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof SecondCycleFirstYearGroup;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
