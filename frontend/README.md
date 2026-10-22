# ProjectHub Frontend

Responsive React + Vite interface for the Spring Boot ProjectHub backend supplied as `backend(20261022-080909).zip`.

## Features

- Sign in and account registration through the backend's stateless HTTP Basic authentication.
- First-run company setup: create a company or join an existing company using an invitation code.
- Company page with current-company details, invitation-code rotation, and company-administrator transfer where the backend allows it.
- Overview dashboard with project/task totals, completion progress, overdue and high-priority work, and recent tasks.
- Project directory with status filtering; create, edit, delete, and member management.
- Task management with status and priority filters, due dates, status-only updates for members, and owner/admin edit/delete controls.
- My Work page for tasks assigned to the signed-in user.
- Task assignee visibility for users permitted to view a task; assignment changes are restricted by backend permissions.
- Global admin user management: list/edit users, promote global administrators, and delete accounts.
- Account profile and self-service password changes through `/users/me` and `/users/{id}/password`.
- Responsive sidebar, global search, `Ctrl+K` / `⌘K` shortcut, confirmation prompts, inline errors, and toast notifications.

## Requirements

- Node.js 18+ (Node.js 20 or 22 recommended).
- npm.
- PostgreSQL configured for the backend.
- Spring Boot backend running at `http://localhost:8080`.

## Run locally on Windows

1. Start PostgreSQL and the backend from IntelliJ IDEA. Wait until Spring Boot starts successfully.
2. Extract this ZIP and open a terminal in `projecthub-frontend`.
3. Install dependencies:

   ```powershell
   npm install
   ```

4. Start the frontend:

   ```powershell
   npm run dev
   ```

5. Open `http://127.0.0.1:5173`.

The Vite development server proxies `/api/*` to `http://localhost:8080`, so local development uses the same browser origin for API calls. `vite.config.js` is only for the local development server; this proxy is not used by a Vercel production deployment. The `.env.example` value `/api/v1` is the correct local value when using that proxy.

## Deploy to Vercel

1. Import the GitHub repository into Vercel.
2. Set the Vercel project's **Root Directory** to `frontend` (the folder containing this `package.json`).
3. Use the Vite defaults: build command `npm run build` and output directory `dist`.
4. Under **Settings → Environment Variables**, add `VITE_API_BASE_URL` with the full Render backend URL plus `/api/v1`, for example `https://your-render-service.onrender.com/api/v1`. Replace the example host with your actual Render service URL. Set it for Production and any Preview environments you use.
5. Redeploy after adding or changing the variable because Vite embeds `VITE_*` variables at build time.

Do not set the production value to `/api/v1`: that relative path is for local development through Vite's proxy. If the production variable is missing or remains relative, the app displays a configuration error instead of silently calling the Vercel domain for API requests. `VITE_*` values are public in the built frontend; never put database credentials or other secrets in them. The deployed backend must also permit the Vercel origin through its CORS configuration.

## Company onboarding

Registration creates a user account first. After sign-in, users without a company are shown options to create a company or join one using an invitation code. When a company is created, the returned invitation code is shown once; copy and save it. The backend does not provide an endpoint for retrieving the current raw invitation code, so rotating the code is the recovery path if it is lost.

The frontend reads `companyId` (and tolerates `companyID`), `companyName`, and `companyRole` from `GET /api/v1/users/me` and each user returned by `GET /api/v1/users`. If all three current-user fields are `null`, ordinary users see the onboarding choice to create or join a company. A partially populated company context is treated as an API/data error rather than silently sending the user through onboarding. The project-member picker filters to users with the same company ID when the directory includes company fields; the backend must still enforce company membership. For compatibility with an older response that omits all company fields, the frontend falls back to `/companies/me` and leaves final eligibility enforcement to the backend.

## API compatibility

- API prefix: `/api/v1`.
- Authentication: stateless HTTP Basic; credentials are kept in session storage for the current browser tab.
- Current user: `GET /users/me`.
- Global admin detection: protected `GET /admin` endpoint.
- Company details: `GET /companies/me` returns `id`, `companyName`, `companyOwnerName`, and `createdAt`.
- Create company: `POST /companies` with `{ "companyName": "Example Company" }`.
- Join company: `POST /companies/join` with `{ "invitationCode": "..." }`.
- Rotate invitation code: `PATCH /companies/me/invitation-code`.
- Transfer company administration: `PUT /companies/me/owner` with `{ "id": 2 }`.
- Project request: `{ "name": "Project Alpha", "status": "ACTIVE" }`.
- Project status values: `PLANNING`, `ACTIVE`, `COMPLETED`, `ON_HOLD`.
- Task request: `{ "title", "description", "status", "priority", "dueDate", "projectId" }`.
- Task status values: `TODO`, `IN_PROGRESS`, `COMPLETED`.
- Task priority values: `LOW`, `MEDIUM`, `HIGH`.
- Status-only task update: `PATCH /tasks/{id}/status` with `{ "status": "IN_PROGRESS" }`.
- Add a project member: `POST /projects/{projectId}/members` with `{ "userId": 2 }`.
- Assign a task: `POST /tasks/{taskId}/assignees` with `{ "projectMemberId": 3 }`.

The frontend shows or hides actions for usability only; all actual authorization must remain enforced by the backend.

## Build

```powershell
npm run build
```

Vite outputs production files to `dist/`.

## Company-aware user selection and global company explorer

The Company Explorer tab is available to a global `ROLE_ADMIN`. It groups companies by the `companyId`/`companyID`, `companyName`, and `companyRole` values returned in each user record. From a company card, the administrator can inspect that company's users, projects, and tasks in separate tabs. Project/task counts are intentionally not guessed when project-company metadata is missing.

### Required response fields

`UserResponse` should include:

- `companyId` (or `companyID`)
- `companyName`
- `companyRole`

`ProjectResponse` should include enough relationship metadata for safe filtering and permission-aware actions:

- `companyId` (or `companyID`)
- `companyName` (optional display field)
- `ownerId` (or `ownerID`)

Map these project fields from the entity, for example `project.getCompany().getId()`, `project.getCompany().getName()`, and `project.getOwner().getId()`. The Company Explorer uses the company ID to group projects, then groups tasks by their existing `projectId`. The `ownerId` is used to decide whether the signed-in user owns a project, instead of assuming that every project they are not a member of is theirs.

The currently shared backend response shape for `ProjectResponse` contains only `id`, `name`, and `status`; until the extra fields are added to `ProjectResponse` and `ProjectMapper`, the frontend shows a notice that project/task company grouping is unavailable. User rows will still be grouped using the company fields in `UserResponse`.

### Member directory behavior

Project-member selection uses the users returned from `GET /api/v1/users`, then filters to the target project's company. This matches the backend's different directory responses for global administrators, company administrators, and project owners. Because the project-owner directory can be a subset (members already included in company projects), the dialog also offers manual user-ID entry; the backend remains responsible for validating the user's company and the requester's permissions.
