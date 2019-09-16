/**
 *  Copyright © 2015 Universidade de Lisboa
 *
 *  This file is part of FenixEdu fenixedu-ulisboa-specifications.
 *
 *  FenixEdu fenixedu-ulisboa-specifications is free software: you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FenixEdu fenixedu-ulisboa-specifications is distributed in the hope that it will
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with FenixEdu fenixedu-ulisboa-specifications.
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package org.fenixedu.ulisboa.specifications.domain.student.access.importation;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.spaces.domain.Space;

import pt.ist.fenixframework.core.WriteOnReadError;

public class DgesStudentImportationProcess extends DgesStudentImportationProcess_Base {

    public static final Comparator<DgesStudentImportationProcess> COMPARATOR_BY_BEGIN_DATE =
            new Comparator<DgesStudentImportationProcess>() {

                @Override
                public int compare(final DgesStudentImportationProcess o1, final DgesStudentImportationProcess o2) {
                    return o1.getRequestDate().compareTo(o2.getRequestDate());
                }
            };

    protected DgesStudentImportationProcess() {
        super();
    }

    public DgesStudentImportationProcess(final ExecutionYear executionYear, final Space space, final EntryPhase entryPhase,
            final DgesStudentImportationFile dgesStudentImportationFile) {
        this();

        init(executionYear, space, entryPhase, dgesStudentImportationFile);
    }

    private void init(final ExecutionYear executionYear, final EntryPhase entryPhase) {
        String[] args = new String[0];
        if (executionYear == null) {
            throw new DomainException("error.DgesBaseProcess.execution.year.is.null", args);
        }
        String[] args1 = new String[0];
        if (entryPhase == null) {
            throw new DomainException("error.DgesBaseProcess.entry.phase.is.null", args1);
        }

        setExecutionYear(executionYear);
        setEntryPhase(entryPhase);
    }

    private void init(final ExecutionYear executionYear, final Space space, final EntryPhase entryPhase,
            final DgesStudentImportationFile dgesStudentImportationFile) {
        init(executionYear, entryPhase);

        String[] args = new String[0];

        if (space == null) {
            throw new DomainException("error.DgesStudentImportationProcess.campus.is.null", args);
        }
        String[] args1 = {};
        if (dgesStudentImportationFile == null) {
            throw new DomainException("error.DgesStudentImportationProcess.importation.file.is.null", args1);
        }

        setSpace(space);
        setDgesStudentImportationFile(dgesStudentImportationFile);
    }

    private transient StringBuilder sb = null;

    @Override
    public QueueJobResult execute() throws Exception {
        try {
            if (sb != null) {
                sb.append("\n").append("------------Starting new run after this point (maybe a rollback)---------\n");
            } else {
                sb = new StringBuilder();
            }

            importCandidates();
        } catch (WriteOnReadError e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }

        final QueueJobResult queueJobResult = new QueueJobResult();
        queueJobResult.setContentType("text/plain");
        queueJobResult.setContent(sb.toString().getBytes(StandardCharsets.UTF_8));

        return queueJobResult;
    }

    public void importCandidates() {
        Authenticate.mock(getPerson().getUser(), "TODO: CHANGE ME");
        try {
            final DgesStudentImportService service =
                    new DgesStudentImportService(getExecutionYear(), getSpace(), getEntryPhase());
            sb.append(service.importStudents(getDgesStudentImportationFile().getContent()));
        } finally {
            Authenticate.unmock();
        }
    }

    public static List<DgesStudentImportationProcess> readDoneJobs(final ExecutionYear executionYear) {
        return readAllJobs(executionYear).stream()
                .filter(process -> process instanceof DgesStudentImportationProcess && process.getDone())
                .collect(Collectors.toList());
    }

    public static List<DgesStudentImportationProcess> readUndoneJobs(final ExecutionYear executionYear) {
        return readAllJobs(executionYear).stream().filter(process -> !process.getDone()).collect(Collectors.toList());
    }

    public static List<DgesStudentImportationProcess> readAllJobs(final ExecutionYear executionYear) {
        //TODO why the instanceof verification
        return executionYear.getDgesStudentImportationProcessSet().stream()
                .filter(process -> process instanceof DgesStudentImportationProcess).collect(Collectors.toList());
    }

    public static boolean canRequestJob() {
        return QueueJob.getUndoneJobsForClass(DgesStudentImportationProcess.class).isEmpty();
    }

    @Override
    public String getFilename() {
        return "log.txt";
    }
}
