# Fintrac

Fintrac is a full-stack expense tracking and adaptive budgeting platform for managing monthly budgets, daily expenses, weekly spending patterns, and emergency budget adjustments.

## Live Links

- GitHub: https://github.com/Yashkr29/Fintrac


## Demo Credentials

Use these credentials for portfolio demos after running `database/demo_seed.sql` or creating the same user through signup:

- Username: `demo`
- Password: `Demo@123`

## Features

- JWT-based authentication with protected user data
- Income and expense tracking
- Category-wise spending analysis
- Monthly budget planning
- Adaptive budget adjustment for emergency expenses
- Dashboard summaries for income, expenses, savings, and remaining budget
- Alerts for budget warning and over-budget states
- Monthly reports with category, merchant, and weekly charts
- CSV export for monthly report summaries

## Tech Stack

Frontend:
- React
- Vite
- Tailwind CSS
- Recharts
- Axios

Backend:
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- MySQL
- Maven

Database:
- MySQL 8+
- Schema file: `database/schema.sql`

## Folder Structure

```text
Fintrac/
  backend/      Spring Boot REST API
  frontend/     React/Vite client
  database/     MySQL schema and database setup files
  screenshots/  Portfolio screenshots
```

Generated folders such as `frontend/node_modules`, `frontend/dist`, `frontend/.vite`, and `backend/target` are ignored by Git.

## Environment Variables

Frontend example: `frontend/.env.example`

```env
VITE_API_URL=https://your-backend-service.onrender.com/api
VITE_API_PROXY_TARGET=http://localhost:8080
```

Backend example: `backend/.env.example`

```env
SPRING_DATASOURCE_URL=jdbc:mysql://your-hosted-mysql-host:3306/fintrac?useSSL=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_mysql_user
SPRING_DATASOURCE_PASSWORD=your_mysql_password
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
JWT_EXPIRATION=86400000
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app,http://localhost:3000
PORT=8080
```

## Local Setup

1. Create MySQL database and run `database/schema.sql`.
2. Configure backend environment variables or update local values in `application.properties`.
3. Start the backend:

```bash
cd backend
mvn spring-boot:run
```

4. Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

5. Open `http://localhost:3000`.

## Build And Test

Frontend production build:

```bash
cd frontend
npm run build
```

Backend tests:

```bash
cd backend
mvn test
```

Backend package:

```bash
cd backend
mvn package
```

## Deployment

Frontend on Vercel:
- Root: repository root
- Build command: `cd frontend && npm install && npm run build`
- Output directory: `frontend/dist`
- Required env: `VITE_API_URL`

Backend on Render/Railway:
- Use `backend/Dockerfile`
- Required env: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, `PORT`

Database:
- Use hosted MySQL
- Run `database/schema.sql`
- Optionally run `database/demo_seed.sql` for portfolio walkthrough credentials

## Portfolio Card

```text
Fintrac - Expense Tracking and Adaptive Budgeting Platform
Full-stack React + Spring Boot app with JWT authentication, MySQL persistence, budget alerts, emergency expense adjustment, and analytics dashboards.
GitHub: https://github.com/Yashkr29/Fintrac
Demo: https://fintrac.vercel.app
```

## Screenshots

Place final screenshots in `screenshots/`:

- Login
- Dashboard
- Transactions
- Budget planner
- Reports and analytics
