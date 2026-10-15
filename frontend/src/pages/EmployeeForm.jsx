import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError, createEmployee, getDepartments, getEmployee, updateEmployee } from '../api/employeeApi.js';
import { EMPTY_EMPLOYEE, toPayload, validateEmployee } from '../utils/validate.js';

function Field({ label, name, error, hint, children }) {
  return (
    <div className={`field ${error ? 'has-error' : ''}`}>
      <label htmlFor={name}>{label}</label>
      {children}
      {hint && !error && <small className="muted">{hint}</small>}
      {error && <small className="error-text" id={`${name}-error`}>{error}</small>}
    </div>
  );
}

/** One component for both "Add Employee" (/employees/new) and "Edit Employee" (/employees/:id/edit). */
export default function EmployeeForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [values, setValues] = useState(EMPTY_EMPLOYEE);
  const [errors, setErrors] = useState({});
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(isEdit);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    getDepartments().then(setDepartments).catch(() => setDepartments([]));
  }, []);

  useEffect(() => {
    if (!isEdit) return undefined;
    let ignore = false;
    setLoading(true);
    getEmployee(id)
      .then((employee) => {
        if (ignore) return;
        setValues({
          fullName: employee.fullName,
          email: employee.email,
          phoneNumber: employee.phoneNumber,
          department: employee.department,
          designation: employee.designation,
          dateOfJoining: employee.dateOfJoining,
          salary: String(employee.salary),
          status: employee.status,
        });
      })
      .catch((err) => {
        if (!ignore) setLoadError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [id, isEdit]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    // Clear a field's error as soon as the user edits it.
    setErrors((current) => (current[name] ? { ...current, [name]: undefined } : current));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitError('');

    const found = validateEmployee(values);
    if (Object.keys(found).length > 0) {
      setErrors(found);
      document.getElementById(Object.keys(found)[0])?.focus();
      return;
    }

    setSubmitting(true);
    try {
      const payload = toPayload(values);
      const saved = isEdit ? await updateEmployee(id, payload) : await createEmployee(payload);
      navigate('/', {
        state: { flash: isEdit ? `Updated ${saved.fullName}` : `Added ${saved.fullName} (ID ${saved.id})` },
      });
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setErrors(err.fieldErrors); // e.g. "email already exists" from the server
      } else {
        setSubmitError(err.message);
      }
      setSubmitting(false);
    }
  };

  if (loading) return <div className="card empty">Loading employee…</div>;

  if (loadError) {
    return (
      <div className="card empty">
        <h2>Could not load employee</h2>
        <p className="muted">{loadError}</p>
        <Link className="btn btn-primary" to="/">Back to employees</Link>
      </div>
    );
  }

  const input = (name, props = {}) => (
    <input
      id={name}
      name={name}
      value={values[name]}
      onChange={handleChange}
      aria-invalid={Boolean(errors[name])}
      aria-describedby={errors[name] ? `${name}-error` : undefined}
      {...props}
    />
  );

  return (
    <>
      <div className="page-head">
        <div>
          <h1>{isEdit ? 'Edit Employee' : 'Add Employee'}</h1>
          {isEdit && <p className="muted">Employee ID {id}</p>}
        </div>
      </div>

      {submitError && <div className="alert alert-error" role="alert">{submitError}</div>}

      <form className="card form-grid" onSubmit={handleSubmit} noValidate>
        <Field label="Full name *" name="fullName" error={errors.fullName}>
          {input('fullName', { type: 'text', maxLength: 100, autoComplete: 'off' })}
        </Field>

        <Field label="Email *" name="email" error={errors.email}>
          {input('email', { type: 'email', maxLength: 150, autoComplete: 'off' })}
        </Field>

        <Field label="Phone number *" name="phoneNumber" error={errors.phoneNumber} hint="e.g. +91 98765 43210">
          {input('phoneNumber', { type: 'tel', maxLength: 20 })}
        </Field>

        <Field label="Department *" name="department" error={errors.department} hint="Pick an existing one or type a new one">
          {input('department', { type: 'text', maxLength: 100, list: 'department-options', autoComplete: 'off' })}
          <datalist id="department-options">
            {departments.map((name) => <option key={name} value={name} />)}
          </datalist>
        </Field>

        <Field label="Designation *" name="designation" error={errors.designation}>
          {input('designation', { type: 'text', maxLength: 100, autoComplete: 'off' })}
        </Field>

        <Field label="Date of joining *" name="dateOfJoining" error={errors.dateOfJoining}>
          {input('dateOfJoining', { type: 'date' })}
        </Field>

        <Field label="Salary (INR) *" name="salary" error={errors.salary}>
          {input('salary', { type: 'number', min: '0', step: '0.01', inputMode: 'decimal' })}
        </Field>

        <Field label="Employment status *" name="status" error={errors.status}>
          <select id="status" name="status" value={values.status} onChange={handleChange}>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </Field>

        <div className="form-actions">
          <Link className="btn" to={isEdit ? `/employees/${id}` : '/'}>Cancel</Link>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Add employee'}
          </button>
        </div>
      </form>
    </>
  );
}
