# Hospital Referrals Management System

This repository contains the first increment of a multi-tenant hospital referrals management and tracking system built with Spring Boot, MySQL, and Thymeleaf.

## What is included in this increment

- Shared-schema multi-tenant foundation with hospital-scoped tenancy.
- Spring Security login and role-based access control.
- Seeded `SUPER_ADMIN` bootstrap user.
- Super admin dashboard.
- Super admin CRUD for hospitals.
- Super admin CRUD for hospital administrators.
- Hospital directory management for departments and doctors.
- Tenant-scoped patient registry for hospital admins.
- Cross-hospital referral creation and tracking.
- Internal routing of incoming referrals to departments and doctors.
- Hospital-admin-managed user accounts for referral officers, doctors, and viewers.
- Dedicated referral officer and doctor role workspaces.
- Dedicated read-only viewer reporting workspace with CSV export.
- Referral attachments with secure upload and download.
- In-app notifications with unread counts and notification inboxes.
- Referral timeline with status changes and notes.
- Flyway-based schema management.

## Initial roles

- `SUPER_ADMIN`
- `HOSPITAL_ADMIN`
- `REFERRAL_OFFICER`
- `DOCTOR`
- `VIEWER`

The system now has working UI flows for `SUPER_ADMIN`, `HOSPITAL_ADMIN`, `REFERRAL_OFFICER`, `DOCTOR`, and `VIEWER`.

## Default bootstrap account

Configured in `src/main/resources/application.yml`:

- Email: `superadmin@referrals.local`
- Password: `ChangeMe123!`

Change this before using the system outside local development.

## Local database setup

1. Create a MySQL database named `hospital_referrals`.
2. Set the datasource environment variables if you are not using the defaults:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
3. Run the application with Maven:

```bash
mvn spring-boot:run
```

Flyway will create the schema automatically.

## Docker setup

This project now includes:

- `Dockerfile` for the Spring Boot application
- `docker-compose.yml` for the app and MySQL
- `.env.example` with the main runtime variables

### Quick start

1. Copy `.env.example` to `.env` if you want to customize ports, database credentials, or the bootstrap admin account.
2. Start the containers:

```bash
docker compose up --build
```

3. Open the application at `http://localhost:8080`
4. MySQL will be available on host port `3307` by default.

### Default Docker credentials

- App URL: `http://localhost:8080`
- Database: `hospital_referrals`
- Database user: `referrals`
- Database password: `referrals`
- Super admin email: `superadmin@referrals.local`
- Super admin password: `ChangeMe123!`

### Useful Docker commands

```bash
docker compose up --build
docker compose down
docker compose down -v
```

`docker compose down -v` also removes the MySQL and attachment volumes.

## Render deployment

This repository now includes a Render Blueprint in [render.yaml](C:/Users/jafxq/Documents/Codex/2026-04-22-i-want-you-to-help-me/render.yaml) that defines:

- `hospital-referrals-app` as a Docker-based web service
- `hospital-referrals-db` as a Docker-based private MySQL service
- persistent disk storage for MySQL data
- persistent disk storage for referral attachments

### Render notes

- Render does not use `docker-compose.yml` for a web service deploy.
- The application must connect to a separate private MySQL service over Render's private network.
- The Blueprint uses `fromService` references so the app receives the MySQL host, port, database name, username, and password automatically.

### How to use on Render

1. Push this repository with `render.yaml` and `mysql-render/Dockerfile`.
2. In Render, create a new Blueprint from the repo.
3. When prompted, provide secret values for:
   - `MYSQL_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
   - `APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD`
4. Deploy the Blueprint.

### Services created by the Blueprint

- `hospital-referrals-db`
  - Type: Private Service
  - Runtime: Docker
  - Image base: MySQL 8.4
- `hospital-referrals-app`
  - Type: Web Service
  - Runtime: Docker
  - Health check: `/actuator/health`

### Important

- The Blueprint is configured for the `frankfurt` region. Change this in `render.yaml` if you want another Render region.
- Both services use the `starter` plan because private services and persistent disks are not a free-tier-style setup.

## Recommended next increments

1. Email and SMS delivery channels for important notifications.
2. Password reset, email integration, and account hardening.
3. Attachment review workflows and approval states.
4. Escalation rules for overdue or unacknowledged referrals.
5. Extended file validation, previews, and image/PDF rendering.
6. Global super-admin reporting across all tenants.
