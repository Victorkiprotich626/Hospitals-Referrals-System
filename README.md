# Hospital Referrals Management and Tracking System

A multi-tenant hospital referrals management and tracking system built with Spring Boot, MySQL, Thymeleaf, Spring Security, and Flyway.

The system helps hospitals create, receive, route, track, and close patient referrals across facilities. It supports role-based access, tenant isolation, referral journey tracking, attachments, notifications, reporting, and deployment through Docker or Render.


## Core Objectives

- Provide a centralized platform for managing hospital-to-hospital referrals.
- Allow each hospital to operate as a separate tenant with isolated records.
- Enable super administrators to manage the whole platform.
- Enable hospital staff to create, receive, assign, forward, and close referrals.
- Track the complete referral journey, including multi-hop referrals from one hospital to another.
- Improve visibility through dashboards, search, filtering, reporting, timelines, and notifications.

## Technology Stack

| Layer | Technology |
| --- | --- |
| Backend | Spring Boot 3.5 |
| Language | Java 21 |
| UI | Thymeleaf, HTML, CSS |
| Security | Spring Security |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Build Tool | Maven |
| Testing | JUnit, Spring Boot Test, Spring Security Test, H2 |
| Containerization | Docker, Docker Compose |

## Main Features

### Multi-Tenant Hospital Management

- Shared-schema multi-tenancy using hospital-scoped records.
- Super admin can create, edit, and manage hospitals.
- Hospital admins and staff are scoped to their assigned hospital.
- Tenant-aware referral visibility prevents ordinary users from seeing unrelated hospital data.

### Authentication and Role-Based Access

- Secure login with Spring Security.
- Role-based dashboard routing after login.
- Remember-me login support.
- User accounts can be enabled or disabled.
- Bootstrap super admin account for initial access.

### Super Admin Workspace

- Platform dashboard.
- Hospital management.
- Hospital admin management.
- Global tenant administration.
- Foundation for platform-level reporting and governance.

### Hospital Admin Workspace

- Manage departments.
- Manage doctor profiles.
- Manage hospital users.
- Manage patient records.
- Create and track referrals.
- Route referrals internally.
- View notifications.
- Access referral timelines, attachments, and outcomes.

### Patient Registry

- Tenant-scoped patient records.
- System-generated patient numbers.
- Patient search and filtering.
- Patient demographics used during referral creation.
- Patient records remain hospital-scoped while referral journeys connect the cross-hospital case history.

### Referral Management

- Create outgoing referrals.
- Receive incoming referrals.
- Track incoming and outgoing referral queues.
- Search and filter referrals by status, direction, patient, hospital, assignment, subject, and journey.
- Route referrals to departments and doctors.
- Add notes to referrals.
- Update referral status.
- Close referrals with final outcomes.
- Forward referrals to another hospital when further specialist care is needed.

### Referral Journey Tracking

- Referrals can be linked into a journey chain.
- A hospital that participated in an earlier referral leg can view downstream progress in the journey.
- Example: Hospital A refers to Hospital B, then Hospital B forwards to Hospital C. Hospital A can still see the journey progress.
- Timeline views support both individual referral events and wider journey-level context.

### Referral Status and Outcome Workflow

Supported workflow concepts include:

| Area | Examples |
| --- | --- |
| Referral statuses | Submitted, Received, Under Review, Accepted, Rejected, Completed, Cancelled |
| Priorities | Low, Normal, High, Urgent |
| SLA states | On Track, Due Today, Overdue, Awaiting Response, Closed |
| Closure outcomes | Treated, Admitted, Redirected, Closed Without Treatment |

The system records important workflow events in the referral timeline, including status changes, routing, notes, attachments, forwarding, and final outcome updates.

### Role Workspaces

| Role | Main Responsibility |
| --- | --- |
| `SUPER_ADMIN` | Manages the whole platform, hospitals, and hospital admins. |
| `HOSPITAL_ADMIN` | Manages hospital setup, users, patients, departments, doctors, and referrals. |
| `REFERRAL_OFFICER` | Handles day-to-day referral operations, creation, review, routing, forwarding, and tracking. |
| `DOCTOR` | Reviews assigned referrals, views supporting documents, and adds clinical notes. |

## Functional Modules

### Hospital and User Administration

- Hospital CRUD.
- Hospital admin CRUD.
- Hospital user CRUD.
- Role assignment.
- Department and doctor assignment.
- Activation and deactivation support.

### Department and Doctor Directory

- Create and manage departments.
- Create and manage doctor profiles.
- Link doctors to departments.
- Use departments and doctors for referral routing.
- Prevent deletion of directory records that are already tied to routed referrals.

### Attachments and Documents

- Upload referral documents.
- Categorize attachments, such as referral letter, lab result, imaging, and discharge summary.
- Store attachment metadata in the database.
- Store uploaded files on disk.
- Secure download and inline viewing for supported file types.
- Record attachment events in the referral timeline.

### In-App Notifications

- User-specific notification inbox.
- Unread notification count.
- Notifications for referral creation, routing, status changes, notes, attachments, and journey updates.
- Role-aware navigation from the notifications page back to each user's workspace.

### Reporting

- Viewer dashboard.
- Tenant-level operational reports.
- Referral filtering by status, date, and search text.
- CSV export support.
- Summary metrics for referral visibility.

## Project Structure

```text
src/main/java/com/example/referrals
├── common          Shared base models and helpers
├── config          Security and startup configuration
├── directory       Departments and doctors
├── hospital        Hospital tenant model
├── hospitaladmin   Hospital admin forms and services
├── notification    In-app notification domain
├── patient         Patient registry domain
├── referral        Referral, timeline, journey, and attachment domain
├── reporting       Tenant reporting service
├── security        Custom security users and login redirects
├── superadmin      Super admin forms and services
├── tenant          Tenant context handling
├── user            Application user model
└── web             MVC controllers
```

```text
src/main/resources
├── db/migration    Flyway database migrations
├── static          CSS and static assets
└── templates       Thymeleaf pages
```

## Local Development Setup

### Requirements

- Java 21
- Maven
- MySQL 8 or compatible MySQL database

### Database Setup

Create a MySQL database:

```sql
CREATE DATABASE hospital_referrals;
```

Set database environment variables if your database credentials differ from the defaults:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/hospital_referrals?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
```

Run the application:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

Flyway applies all database migrations automatically at startup.

## Environment Variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `SERVER_PORT` | Application port | `8080` |
| `SPRING_DATASOURCE_URL` | Full JDBC URL | Built from local MySQL defaults |
| `SPRING_DATASOURCE_USERNAME` | Database username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `root` |
| `DB_HOST` | Database host used when full JDBC URL is not set | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `hospital_referrals` |
| `DB_USERNAME` | Alternative database username | `root` |
| `DB_PASSWORD` | Alternative database password | `root` |
| `APP_BOOTSTRAP_SUPER_ADMIN_EMAIL` | Bootstrap super admin email | `superadmin@referrals.local` |
| `APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD` | Bootstrap super admin password | `ChangeMe123!` |
| `APP_STORAGE_ATTACHMENTS_DIR` | Referral attachment storage path | `storage/referral-attachments` |


## Key User Workflows

### Super Admin Flow

1. Create hospitals.
2. Create hospital admins.
3. Monitor platform setup from the super admin dashboard.

### Hospital Admin Flow

1. Create departments.
2. Create doctor profiles.
3. Create hospital users.
4. Register patients.
5. Create and manage referrals.
6. Route referrals to departments and doctors.
7. Track timelines, attachments, notifications, and outcomes.

### Referral Officer Flow

1. View referral queue.
2. Create outgoing referrals.
3. Review incoming referrals.
4. Route referrals internally.
5. Add notes and attachments.
6. Forward referrals to another hospital when needed.
7. Update referral status and closure outcome.

### Doctor Flow

1. View assigned referrals.
2. Review patient and referral details.
3. View supporting documents.
4. Add clinical notes.
5. Follow referral progress.



