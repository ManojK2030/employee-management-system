# Employee Management System

A full-stack web application for managing employee records: add, view, edit, delete, search, filter, paginate and export to CSV.

| Layer    | Technology                                                                 |
|----------|----------------------------------------------------------------------------|
| Frontend | React 18 + Vite, React Router, plain CSS (responsive)                      |
| Backend  | Java 17, Spring Boot 3.3, Spring Data JPA, Bean Validation, Maven          |
| Database | MySQL 8                                                                    |
| Testing  | JUnit 5 + Mockito (service layer), MockMvc + H2 (API integration), Vitest (frontend) |
| Docs     | Swagger UI / OpenAPI (springdoc), Postman collection                       |
| Extras   | Docker Compose, CSV export                                                 |

## Features

- **Employee record**: Employee ID, Full Name, Email, Phone Number, Department, Designation, Date of Joining, Salary, Employment Status (Active / Inactive)
- Add, view, edit and list employees; **delete with a confirmation dialog**
- **Search** by name or email (case-insensitive, partial match, debounced)
- **Filter** by department and employment status (combinable with search)
- **Server-side pagination** with selectable page size
- **Responsive UI**: the table becomes a card list on phones
- Validation on both the client and the server; the server's field errors are shown next to the form fields
- Unique email (case-insensitive), enforced in the service *and* by a database unique key
- **Export to CSV** of the current search/filters
- Uniform JSON error responses (`400` validation, `404` not found, `409` duplicate email)

## Quick start with Docker (recommended)

**Prerequisite:** Docker Desktop (or Docker Engine + Compose v2).

```bash
docker compose up --build
```

The first build downloads dependencies and takes a few minutes. Then open:

| What        | URL                                   |
|-------------|---------------------------------------|
| Application | http://localhost:3000                 |
| REST API    | http://localhost:8080/api/employees   |
| Swagger UI  | http://localhost:8080/swagger-ui.html |
| MySQL       | `localhost:3307`, user `root`, password `root`, database `ems_db` |

On first start MySQL runs `database/schema.sql` and `database/seed.sql`, so the app opens with 30 sample employees.
To start again from an empty database: `docker compose down -v`.

## Run without Docker

### Prerequisites

- JDK 17 or later
- Maven 3.8+
- Node.js 18+ and npm
- MySQL 8 running locally

### 1. Database

Either let the backend create everything (the default, see step 2), or run the scripts yourself:

```bash
mysql -u root -p < database/schema.sql     # creates database ems_db and the employees table
mysql -u root -p < database/seed.sql       # optional: 30 sample employees
```

The backend connects with `root` / `root` on `localhost:3306` by default. If your MySQL password is different, set an environment variable (no code change needed):

```bash
# macOS / Linux
export DB_PASSWORD=your_password

# Windows PowerShell
$env:DB_PASSWORD = "your_password"
```

| Variable               | Default                                   | Purpose                              |
|------------------------|-------------------------------------------|--------------------------------------|
| `DB_URL`               | `jdbc:mysql://localhost:3306/ems_db?...`  | Full JDBC URL                        |
| `DB_USERNAME`          | `root`                                    | MySQL user                           |
| `DB_PASSWORD`          | `root`                                    | MySQL password                       |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Origins allowed to call the API  |

The JDBC URL contains `createDatabaseIfNotExist=true` and Hibernate runs with `ddl-auto=update`, so even with no scripts the database and table are created on first start.

### 2. Backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

### 3. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. In development Vite forwards `/api` requests to `http://localhost:8080`, so no CORS setup is needed. If your API runs elsewhere, set `VITE_PROXY_TARGET` before `npm run dev`.

## Running the tests

```bash
cd backend && mvn test        # unit tests + API integration tests (no MySQL needed, uses in-memory H2)
cd frontend && npm test       # validation and query-builder tests
```

- `EmployeeServiceImplTest` - service-layer business logic with Mockito: create / update / delete rules, duplicate email handling, email normalisation, pagination and sort validation, department list, CSV output and escaping.
- `EmployeeApiIntegrationTest` - HTTP -> controller -> service -> JPA -> H2: status codes, validation errors, search, filters, pagination, CSV export.

## API reference

Interactive documentation: **http://localhost:8080/swagger-ui.html** (OpenAPI JSON at `/v3/api-docs`).

| Method | Endpoint                      | Description                              | Success |
|--------|-------------------------------|------------------------------------------|---------|
| POST   | `/api/employees`              | Create an employee                       | 201     |
| GET    | `/api/employees`              | List with search, filters and pagination | 200     |
| GET    | `/api/employees/{id}`         | Get one employee                         | 200     |
| PUT    | `/api/employees/{id}`         | Update an employee (full replacement)    | 200     |
| DELETE | `/api/employees/{id}`         | Delete an employee                       | 204     |
| GET    | `/api/employees/departments`  | Distinct department names                | 200     |
| GET    | `/api/employees/export`       | CSV download (same filters as the list)  | 200     |

**List query parameters** (all optional): `search` (name or email), `department`, `status` (`ACTIVE` / `INACTIVE`), `page` (default 0), `size` (default 10, max 100), `sortBy` (`id`, `fullName`, `email`, `department`, `designation`, `dateOfJoining`, `salary`, `status`; default `id`), `sortDir` (`asc` / `desc`, default `desc`).

**Create / update body**

```json
{
  "fullName": "Priya Sharma",
  "email": "priya.sharma@example.com",
  "phoneNumber": "+91 98765 43210",
  "department": "Engineering",
  "designation": "Backend Developer",
  "dateOfJoining": "2024-07-01",
  "salary": 850000.00,
  "status": "ACTIVE"
}
```

**Validation rules**: all fields required; email must be valid and unique; phone is 7-20 digits (spaces, hyphens and a leading `+` allowed); salary greater than 0 with at most 2 decimals; status is `ACTIVE` or `INACTIVE`; date format `yyyy-MM-dd`.

**Error format** (every error has this shape)

```json
{
  "timestamp": "2026-10-15T08:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/employees",
  "fieldErrors": { "email": "Email must be a valid address" }
}
```

### Sample requests (curl)

```bash
# create
curl -X POST http://localhost:8080/api/employees -H "Content-Type: application/json" -d '{
  "fullName":"Priya Sharma","email":"priya.sharma@example.com","phoneNumber":"+91 98765 43210",
  "department":"Engineering","designation":"Backend Developer","dateOfJoining":"2024-07-01",
  "salary":850000,"status":"ACTIVE"}'

# list: page 2, 5 per page, Engineering, active only, name/email contains "an"
curl "http://localhost:8080/api/employees?page=1&size=5&department=Engineering&status=ACTIVE&search=an"

# get / delete
curl http://localhost:8080/api/employees/1
curl -X DELETE http://localhost:8080/api/employees/1

# CSV export
curl -OJ "http://localhost:8080/api/employees/export?department=Finance"
```

A ready-to-import **Postman collection** is in [`postman/Employee-Management-System.postman_collection.json`](postman/Employee-Management-System.postman_collection.json). Run it top to bottom: the create request saves the new ID in a variable that the following requests use, and the requests include test scripts.

## Project structure

```
employee-management-system/
├── backend/                         Spring Boot API
│   ├── src/main/java/com/ems/
│   │   ├── controller/              REST endpoints (HTTP only, no business logic)
│   │   ├── service/                 business rules (EmployeeService + implementation)
│   │   ├── repository/              Spring Data JPA + dynamic search specifications
│   │   ├── entity/                  JPA entity and enum
│   │   ├── dto/                     request / response / page / error objects
│   │   ├── mapper/                  entity -> DTO
│   │   ├── exception/               custom exceptions + global handler
│   │   └── config/                  CORS and OpenAPI
│   └── src/test/java/com/ems/       unit and integration tests
├── frontend/                        React app (Vite)
│   └── src/{pages,components,api,hooks,utils}
├── database/                        schema.sql, seed.sql
├── postman/                         Postman collection
├── docs/screenshots/                screenshots of the running application
└── docker-compose.yml               MySQL + backend + frontend
```

## Design notes

- **Layering**: controller -> service -> repository. The controller only maps HTTP to method calls; validation, duplicate checks and paging rules live in the service, which is why the unit tests target it.
- **DTOs instead of exposing the entity**, so the API shape is independent of the table and each request is validated with Bean Validation.
- **Dynamic search with JPA Specifications**: optional filters are combined with `AND` in one query, `LIKE` wildcards typed by the user are escaped, and the database does the paging.
- **Safe sorting**: `sortBy` is checked against a whitelist and `id` is added as a tie-breaker so rows never jump between pages.
- **Email uniqueness** is checked in the service for a friendly `409`, and backed by a unique key in the database for concurrent requests.
- **Money** is stored as `DECIMAL(12,2)` / `BigDecimal`, never floating point.
- **CSV export** guards against spreadsheet formula injection (values starting with `=`, `+`, `-`, `@` are prefixed with `'`) and includes a UTF-8 BOM so Excel shows non-English names correctly.
- **Frontend**: all HTTP calls are in `api/employeeApi.js`; list requests are debounced and out-of-date responses are ignored, so fast typing never shows stale results.

## Screenshots

Screenshots of the running application are in [`docs/screenshots`](docs/screenshots):

| Employee list (search, filters, pagination) | Add / edit form | Employee details | Delete confirmation |
|---|---|---|---|
| `01-employee-list.png` | `02-add-employee.png` | `03-employee-details.png` | `04-delete-confirmation.png` |

## Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'root'` | Set `DB_USERNAME` / `DB_PASSWORD` to your MySQL credentials (see above). |
| `Communications link failure` | MySQL is not running or not on port 3306 (set `DB_URL` if it differs). |
| Port 8080 / 5173 / 3000 already in use | Stop the other process, or change `server.port` / the Docker port mappings. |
| Frontend shows "Cannot reach the server" | The backend is not running on port 8080. |
| Docker: backend restarts a few times at first | It is waiting for MySQL to finish initialising; it connects automatically. |

## Not included

Login / authentication and department management (optional items in the brief) were left out to keep the scope focused on the required CRUD functionality.
