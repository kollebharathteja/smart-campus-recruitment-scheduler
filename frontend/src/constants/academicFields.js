// Canonical labels for the extra academic percentages/CGPAs companies commonly ask for,
// beyond the fixed Student.cgpa field (which is treated as UG/Degree CGPA elsewhere in the
// app). These exact strings are also what the Excel import template's columns produce, so a
// row imported from Excel and a row typed by hand always end up keyed the same way in
// Student.additionalDetails — which is what a drive's custom eligibility criteria matches
// against by exact label.
//
// A field only gets checked at eligibility time if a drive's admin explicitly added a
// criterion for it (see RecruitmentDrives.jsx "Custom eligibility criteria"). So a company
// that only cares about Degree CGPA never forces a student to fill in 10th/12th/PG — those
// stay optional unless someone actually asks for them.
export const ACADEMIC_DETAIL_FIELDS = [
  { label: '10th Percentage', hint: '10th / SSC %' },
  { label: '12th Percentage', hint: '12th / Inter %' },
  { label: 'UG CGPA', hint: 'Degree / undergraduate CGPA' },
  { label: 'PG CGPA', hint: 'Postgraduate CGPA' }
];
