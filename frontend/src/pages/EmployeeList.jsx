import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { deleteEmployee, exportUrl, getDepartments, listEmployees } from '../api/employeeApi.js';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Pagination from '../components/Pagination.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import useDebounce from '../hooks/useDebounce.js';
import { formatCurrency, formatDate } from '../utils/format.js';

const PAGE_SIZES = [5, 10, 20, 50];

export default function EmployeeList() {
  const location = useLocation();
  const navigate = useNavigate();

  const [search, setSearch] = useState('');
  const [department, setDepartment] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [data, setData] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [flash, setFlash] = useState(location.state?.flash || '');

  const debouncedSearch = useDebounce(search, 350);

  // Show the "created / updated / deleted" message once, then clear it from the history entry
  // so a browser refresh does not show it again.
  useEffect(() => {
    if (location.state?.flash) navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!flash) return undefined;
    const timer = setTimeout(() => setFlash(''), 4000);
    return () => clearTimeout(timer);
  }, [flash]);

  useEffect(() => {
    getDepartments().then(setDepartments).catch(() => setDepartments([]));
  }, [reloadKey]);

  // `ignore` stops a slow, outdated response from overwriting a newer one.
  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError('');
    listEmployees({ search: debouncedSearch, department, status, page, size })
      .then((result) => {
        if (!ignore) setData(result);
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
  }, [debouncedSearch, department, status, page, size, reloadKey]);

  const changeFilter = (setter) => (event) => {
    setter(event.target.value);
    setPage(0);
  };

  const clearFilters = () => {
    setSearch('');
    setDepartment('');
    setStatus('');
    setPage(0);
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await deleteEmployee(toDelete.id);
      setFlash(`Deleted ${toDelete.fullName}`);
      // Deleting the last row of a page would leave us on an empty page, so step back one page.
      if (data.content.length === 1 && page > 0) setPage(page - 1);
      setReloadKey((key) => key + 1);
    } catch (err) {
      setError(err.message);
    } finally {
      setDeleting(false);
      setToDelete(null);
    }
  };

  const employees = data?.content ?? [];
  const filtersActive = Boolean(search || department || status);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Employees</h1>
          <p className="muted">
            {data ? `${data.totalElements} ${data.totalElements === 1 ? 'employee' : 'employees'}` : ' '}
            {filtersActive ? ' match your filters' : ' in total'}
          </p>
        </div>
        <div className="actions">
          <a className="btn" href={exportUrl({ search: debouncedSearch, department, status })} download>
            Export CSV
          </a>
          <Link className="btn btn-primary" to="/employees/new">
            + Add Employee
          </Link>
        </div>
      </div>

      {flash && <div className="alert alert-success" role="status">{flash}</div>}
      {error && <div className="alert alert-error" role="alert">{error}</div>}

      <section className="card filters" aria-label="Search and filters">
        <label className="field grow">
          <span>Search</span>
          <input
            type="search"
            placeholder="Search by name or email"
            value={search}
            onChange={changeFilter(setSearch)}
          />
        </label>

        <label className="field">
          <span>Department</span>
          <select value={department} onChange={changeFilter(setDepartment)}>
            <option value="">All departments</option>
            {departments.map((name) => (
              <option key={name} value={name}>{name}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Status</span>
          <select value={status} onChange={changeFilter(setStatus)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </label>

        <label className="field">
          <span>Per page</span>
          <select
            value={size}
            onChange={(event) => {
              setSize(Number(event.target.value));
              setPage(0);
            }}
          >
            {PAGE_SIZES.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </label>

        {filtersActive && (
          <button type="button" className="btn btn-link" onClick={clearFilters}>
            Clear filters
          </button>
        )}
      </section>

      <section className={`card table-card ${loading ? 'is-loading' : ''}`} aria-busy={loading}>
        {!data && loading && <p className="empty">Loading employees…</p>}

        {data && employees.length === 0 && (
          <div className="empty">
            <h2>{filtersActive ? 'No employees match your filters' : 'No employees yet'}</h2>
            <p className="muted">
              {filtersActive ? 'Try a different search or clear the filters.' : 'Add your first employee to get started.'}
            </p>
            {!filtersActive && (
              <Link className="btn btn-primary" to="/employees/new">+ Add Employee</Link>
            )}
          </div>
        )}

        {employees.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Employee</th>
                <th>Department</th>
                <th>Designation</th>
                <th>Joined</th>
                <th className="num">Salary</th>
                <th>Status</th>
                <th className="actions-col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((employee) => (
                <tr key={employee.id}>
                  <td data-label="ID">{employee.id}</td>
                  <td data-label="Employee">
                    <Link className="employee-link" to={`/employees/${employee.id}`}>
                      {employee.fullName}
                    </Link>
                    <div className="muted small">{employee.email}</div>
                  </td>
                  <td data-label="Department">{employee.department}</td>
                  <td data-label="Designation">{employee.designation}</td>
                  <td data-label="Joined">{formatDate(employee.dateOfJoining)}</td>
                  <td data-label="Salary" className="num">{formatCurrency(employee.salary)}</td>
                  <td data-label="Status"><StatusBadge status={employee.status} /></td>
                  <td data-label="Actions" className="actions-col">
                    <div className="row-actions">
                      <Link className="btn btn-sm" to={`/employees/${employee.id}`}>View</Link>
                      <Link className="btn btn-sm" to={`/employees/${employee.id}/edit`}>Edit</Link>
                      <button type="button" className="btn btn-sm btn-danger-outline" onClick={() => setToDelete(employee)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={data.size}
          onPageChange={setPage}
        />
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete employee?"
          message={`${toDelete.fullName} (ID ${toDelete.id}) will be permanently removed. This cannot be undone.`}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </>
  );
}
