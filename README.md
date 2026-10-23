# 🚀 ProjectHub — Project Management Application

ProjectHub is a full-stack project management application designed to help teams organize projects, manage tasks, and collaborate through company-based workspaces. It includes user authentication, role-based access control, company management, and project management functionality.

🌐 **Live Demo:** [ProjectHub](https://projecthub-app.vercel.app/)

---

## ✨ Features

### 🔐 Authentication and Security
- User registration and login.
- Secure authentication and authorization using Spring Security.
- Role-based access control to manage permissions.

### 🏢 Company Management
- Create a company workspace or join an existing company using an invitation code.
- Organize users and projects within their respective company workspaces.
- Support for company-level administration.

### 👥 Role-Based Access Control

ProjectHub supports three roles:

| Role | Description |
|---|---|
| **Global Admin** | Has system-wide administrative access to manage users and oversee companies. |
| **Company Admin** | Manages their company's workspace, including members and company-level administration. |
| **Company Member** | Accesses projects and users according to their permissions and project involvement. |

### 📁 Project Management
- Create new projects.
- View project details and project listings.
- Update existing projects.
- Delete projects.

### ✅ Task Management
- Organize tasks associated with projects.
- Manage task-related information within the application.

### ☁️ Cloud Deployment
- Frontend deployed on Vercel.
- Backend deployed on Render.
- PostgreSQL database hosted on Neon.

---

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | React.js, Vite, JavaScript |
| Backend | Java, Spring Boot, Spring Security |
| Database | PostgreSQL |
| Data Access | Spring Data JPA, Hibernate |
| Build Tool | Maven |
| Deployment | Vercel, Render, Neon |

---

## 🏗️ Project Structure

```text
ProjectHub/
├── frontend/
│   ├── public/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
└── README.md
```

*The structure above is representative; adjust it if your repository contains additional folders or files.*

---

## ⚙️ Getting Started

Follow these steps to run ProjectHub locally.

### Prerequisites

Make sure you have the following installed:

- [Git](https://git-scm.com/)
- [Node.js and npm](https://nodejs.org/)
- [Java JDK](https://adoptium.net/) — use a version compatible with the backend's Maven configuration.
- [Apache Maven](https://maven.apache.org/) or the project's Maven Wrapper.
- A PostgreSQL database, either local or hosted.

### 1. Clone the Repository

```bash
git clone https://github.com/Hiresh-Goyal/ProjectHub
cd ProjectHub
```

### 2. Configure the Backend

Navigate to the backend directory:

```bash
cd backend
```

Configure your database connection and other environment-specific settings using the environment variables expected by your Spring Boot configuration.

For example, your environment may require:

```env
DB_URL=your_postgresql_connection_url
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
FRONTEND_URL=http://localhost:5173
```

Use the exact variable names referenced in your backend configuration. Keep real credentials in environment variables and **never commit secrets to GitHub**.

Start the backend using Maven:

```bash
mvn spring-boot:run
```

If the project includes the Maven Wrapper, you can use `./mvnw spring-boot:run` on Linux/macOS or `mvnw.cmd spring-boot:run` on Windows.

The backend's local URL and port depend on your application configuration. The frontend must be configured to point to the running backend.

### 3. Configure the Frontend

Open a new terminal and navigate to the frontend directory:

```bash
cd frontend
```

Install the dependencies:

```bash
npm install
```

Create a local `.env` file if your frontend configuration requires one. For a development setup using the Vite proxy, the API base URL may be:

```env
VITE_API_BASE_URL=/api/v1
```

Make sure this matches your frontend API configuration and the backend's local proxy target in `vite.config.js`.

Start the development server:

```bash
npm run dev
```

Vite will display the local frontend URL in the terminal, usually `http://localhost:5173`.

---

## 🔗 API Overview

The backend exposes REST APIs under the `/api/v1` base path.

The project management API includes endpoints for:

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/projects` | Retrieve projects |
| `GET` | `/api/v1/projects/{id}` | Retrieve a project by ID |
| `POST` | `/api/v1/projects` | Create a project |
| `PUT` | `/api/v1/projects/{id}` | Update a project |
| `DELETE` | `/api/v1/projects/{id}` | Delete a project |

These endpoints are subject to the application's authentication and authorization rules.

---

## 🔒 Security Considerations

- Never commit database credentials, authentication secrets, or production environment files.
- Configure CORS for the frontend origins that should be allowed to access the backend.
- Keep production environment variables in the deployment platform's settings.
- Ensure backend authorization rules protect company-specific data and restrict actions according to user roles.

---

## 📚 Learning Outcomes

Building ProjectHub provided hands-on experience with:

- Full-stack application development.
- Designing and implementing RESTful APIs.
- Authentication and role-based authorization.
- Relational database integration using PostgreSQL and JPA.
- Structuring a Spring Boot application into controller, service, repository, and DTO layers.
- Environment-based configuration and cloud deployment.
- Organizing application features around company-based workspaces.

---

## 🌐 Try ProjectHub

**Live Application:** https://projecthub-app.vercel.app/

Explore the application and its project management workflow.

---

## 👨‍💻 Contributing

Suggestions and improvements are welcome. To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a pull request describing your changes.

---

## 📄 License

A license has not yet been specified. Add a license file before presenting the project as open source, so others know how they may use, modify, and distribute the code.

---

**Built with ☕ Java, React, Spring Boot, and PostgreSQL.**