package org.jasig.ssp.service.impl;

import org.jasig.ssp.transferobject.reports.JournalCaseNotesStudentReportTO;

import java.util.Comparator;
import java.util.Objects;

public class StudentNameCompator implements Comparator<JournalCaseNotesStudentReportTO> {
  @Override
  public int compare(JournalCaseNotesStudentReportTO p1, JournalCaseNotesStudentReportTO p2) {

      int value = p1.getLastName().compareToIgnoreCase(p2.getLastName());

      if(value != 0) return value;

      value = p1.getFirstName().compareToIgnoreCase(p2.getFirstName());

      if(value != 0) return value;

      if(Objects.isNull(p1.getMiddleName()) && Objects.isNull(p2.getMiddleName())) return 0;

      if(Objects.isNull(p1.getMiddleName())) return -1;

      if(Objects.isNull(p2.getMiddleName())) return 1;

      return p1.getMiddleName().compareToIgnoreCase(p2.getMiddleName());
    }
}
