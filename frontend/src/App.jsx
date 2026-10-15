import { Link, Route, Routes } from 'react-router-dom';
import EmployeeList from './pages/EmployeeList.jsx';
import EmployeeForm from './pages/EmployeeForm.jsx';
import EmployeeDetails from './pages/EmployeeDetails.jsx';

function NotFound() {
  return (
    <div className="card empty">
      <h2>Page not found</h2>
      <p>The page you are looking for does not exist.</p>
      <Link className="btn btn-primary" to="/">Back to employees</Link>
    </div>
  );
}

export default function App() {
  return (
    <>
      <header className="app-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">EMS</span>
            Employee Management
          </Link>
        </div>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<EmployeeList />} />
          <Route path="/employees/new" element={<EmployeeForm />} />
          <Route path="/employees/:id" element={<EmployeeDetails />} />
          <Route path="/employees/:id/edit" element={<EmployeeForm />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}
