import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteEmployee, getEmployee } from '../api/employeeApi.js';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { formatCurrency, formatDate } from '../utils/format.js';

export default function EmployeeDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [employee, setEmployee] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError('');
    getEmployee(id)
      .then((result) => {
        if (!ignore) setEmployee(result);
      })
      .catch((err) => {
        if (!ignore) setError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [id]);

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await deleteEmployee(id);
      navigate('/', { state: { flash: `Deleted ${employee.fullName}` } });
    } catch (err) {
      setDeleteError(err.message);
      setDeleting(false);
      setConfirming(false);
    }
  };

  if (loading) return <div className="card empty">Loading employee…</div>;

  if (error) {
    return (
      <div className="card empty">
        <h2>Employee not found</h2>
        <p className="muted">{error}</p>
        <Link className="btn btn-primary" to="/">Back to employees</Link>
      </div>
    );
  }

  const rows = [
    ['Employee ID', employee.id],
    ['Full name', employee.fullName],
    ['Email', employee.email],
    ['Phone number', employee.phoneNumber],
    ['Department', employee.department],
    ['Designation', employee.designation],
    ['Date of joining', formatDate(employee.dateOfJoining)],
    ['Salary', formatCurrency(employee.salary)],
    ['Employment status', <StatusBadge key="status" status={employee.status} />],
  ];

  return (
    <>
      <div className="page-head">
        <div>
          <Link className="back-link" to="/">← All employees</Link>
          <h1>{employee.fullName}</h1>
          <p className="muted">{employee.designation} · {employee.department}</p>
        </div>
        <div className="actions">
          <Link className="btn" to={`/employees/${employee.id}/edit`}>Edit</Link>
          <button type="button" className="btn btn-danger" onClick={() => setConfirming(true)}>Delete</button>
        </div>
      </div>

      {deleteError && <div className="alert alert-error" role="alert">{deleteError}</div>}

      <section className="card">
        <dl className="details">
          {rows.map(([label, value]) => (
            <div className="details-row" key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      </section>

      {confirming && (
        <ConfirmDialog
          title="Delete employee?"
          message={`${employee.fullName} (ID ${employee.id}) will be permanently removed. This cannot be undone.`}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setConfirming(false)}
        />
      )}
    </>
  );
}
