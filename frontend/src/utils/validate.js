// Client-side validation. It mirrors the backend rules (EmployeeRequest.java) so users get instant feedback;
// the backend still validates everything again and its messages are shown if it disagrees.
export const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
export const PHONE_PATTERN = /^\+?[0-9][0-9\s-]{5,18}[0-9]$/;

export const STATUSES = ['ACTIVE', 'INACTIVE'];

export const EMPTY_EMPLOYEE = {
  fullName: '',
  email: '',
  phoneNumber: '',
  department: '',
  designation: '',
  dateOfJoining: '',
  salary: '',
  status: 'ACTIVE',
};

export function validateEmployee(values) {
  const errors = {};
  const text = (key) => String(values[key] ?? '').trim();

  if (!text('fullName')) errors.fullName = 'Full name is required';
  else if (text('fullName').length > 100) errors.fullName = 'Full name must be at most 100 characters';

  if (!text('email')) errors.email = 'Email is required';
  else if (text('email').length > 150) errors.email = 'Email must be at most 150 characters';
  else if (!EMAIL_PATTERN.test(text('email'))) errors.email = 'Enter a valid email address';

  if (!text('phoneNumber')) errors.phoneNumber = 'Phone number is required';
  else if (!PHONE_PATTERN.test(text('phoneNumber'))) {
    errors.phoneNumber = 'Use 7-20 digits; spaces, hyphens and a leading + are allowed';
  }

  if (!text('department')) errors.department = 'Department is required';
  else if (text('department').length > 100) errors.department = 'Department must be at most 100 characters';

  if (!text('designation')) errors.designation = 'Designation is required';
  else if (text('designation').length > 100) errors.designation = 'Designation must be at most 100 characters';

  if (!text('dateOfJoining')) errors.dateOfJoining = 'Date of joining is required';
  else if (!/^\d{4}-\d{2}-\d{2}$/.test(text('dateOfJoining'))) errors.dateOfJoining = 'Enter a valid date';

  const salaryText = text('salary');
  if (!salaryText) errors.salary = 'Salary is required';
  else if (Number.isNaN(Number(salaryText))) errors.salary = 'Salary must be a number';
  else if (Number(salaryText) <= 0) errors.salary = 'Salary must be greater than 0';
  else if (!/^\d{1,10}(\.\d{1,2})?$/.test(salaryText)) {
    errors.salary = 'Salary can have at most 10 digits and 2 decimal places';
  }

  if (!STATUSES.includes(values.status)) errors.status = 'Select Active or Inactive';

  return errors;
}

/** Converts form state into the JSON the API expects (trimmed strings, numeric salary). */
export function toPayload(values) {
  return {
    fullName: values.fullName.trim(),
    email: values.email.trim(),
    phoneNumber: values.phoneNumber.trim(),
    department: values.department.trim(),
    designation: values.designation.trim(),
    dateOfJoining: values.dateOfJoining,
    salary: Number(values.salary),
    status: values.status,
  };
}
