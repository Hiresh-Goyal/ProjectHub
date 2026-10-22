import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft, Building2, ChevronRight, ClipboardList, FolderKanban,
  Search, ShieldCheck, Users, UserRound, Activity, CircleAlert,
} from 'lucide-react';
import { getCompanyId, getProjectCompanyId } from '../api';

function labelize(value = '') {
  return String(value).toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

function Avatar({ name = '' }) {
  const initials = String(name).trim().split(/[\s@._-]+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'U';
  return <span className="avatar">{initials}</span>;
}

function EmptyState({ title, message }) {
  return <div className="empty-state"><div className="empty-icon"><Building2 size={24} /></div><h3>{title}</h3><p>{message}</p></div>;
}

function RoleChip({ role }) {
  if (!role) return <span className="muted-text">Not assigned</span>;
  return <span className={`role-chip ${role === 'COMPANY_ADMIN' ? 'role-admin' : ''}`}>{labelize(role)}</span>;
}

export default function CompanyExplorerPage({
  users = [], projects = [], tasks = [], query = '', onEditUser, onGrantAdmin, onDeleteUser,
}) {
  const [selectedCompanyId, setSelectedCompanyId] = useState(null);
  const [detailTab, setDetailTab] = useState('overview');
  const normalizedQuery = query.trim().toLowerCase();

  // Company records are derived from the all-users response. Every associated user
  // carries companyId/companyName/companyRole; unassigned users are intentionally omitted.
  const companyIndex = useMemo(() => {
    const index = new Map();
    users.forEach((user) => {
      const id = getCompanyId(user);
      if (id === null) return;
      if (!index.has(id)) {
        index.set(id, {
          id,
          name: user.companyName || `Company #${id}`,
          users: [],
          admin: null,
          createdAt: null,
        });
      }
      const company = index.get(id);
      if (!company.name && user.companyName) company.name = user.companyName;
      company.users.push(user);
      if (user.companyRole === 'COMPANY_ADMIN') company.admin = user;
    });
    return [...index.values()].sort((a, b) => a.name.localeCompare(b.name));
  }, [users]);

  useEffect(() => {
    if (selectedCompanyId !== null && !companyIndex.some((company) => Number(company.id) === Number(selectedCompanyId))) {
      setSelectedCompanyId(null);
      setDetailTab('overview');
    }
  }, [companyIndex, selectedCompanyId]);

  const hasProjectCompanyMetadata = projects.length === 0 || projects.every((project) => getProjectCompanyId(project) !== null);
  const selectedCompany = companyIndex.find((company) => Number(company.id) === Number(selectedCompanyId)) || null;
  const companyProjects = useMemo(() => {
    if (!selectedCompany || !hasProjectCompanyMetadata) return [];
    return projects.filter((project) => getProjectCompanyId(project) === Number(selectedCompany.id));
  }, [projects, selectedCompany, hasProjectCompanyMetadata]);
  const companyProjectIds = useMemo(() => new Set(companyProjects.map((project) => Number(project.id))), [companyProjects]);
  const companyTasks = useMemo(() => {
    if (!selectedCompany || !hasProjectCompanyMetadata) return [];
    return tasks.filter((task) => companyProjectIds.has(Number(task.projectId)));
  }, [tasks, companyProjectIds, selectedCompany, hasProjectCompanyMetadata]);
  const noCompanyMetadataInUsers = users.length > 0 && !users.some((user) => Object.prototype.hasOwnProperty.call(user || {}, 'companyId') || Object.prototype.hasOwnProperty.call(user || {}, 'companyID'));

  const filteredCompanies = companyIndex.filter((company) => {
    const haystack = `${company.name} ${company.id} ${company.admin?.name || ''} ${company.admin?.email || ''}`.toLowerCase();
    return haystack.includes(normalizedQuery);
  });
  const filteredCompanyUsers = (selectedCompany?.users || []).filter((user) =>
    `${user.name || ''} ${user.email || ''} ${user.companyRole || ''} ${user.id || ''}`.toLowerCase().includes(normalizedQuery));
  const filteredCompanyProjects = companyProjects.filter((project) =>
    `${project.name || ''} ${project.status || ''} ${project.id || ''}`.toLowerCase().includes(normalizedQuery));
  const filteredCompanyTasks = companyTasks.filter((task) => {
    const project = companyProjects.find((item) => Number(item.id) === Number(task.projectId));
    return `${task.title || ''} ${task.description || ''} ${task.status || ''} ${task.priority || ''} ${project?.name || ''}`.toLowerCase().includes(normalizedQuery);
  });

  const openCompany = (companyId) => {
    setSelectedCompanyId(Number(companyId));
    setDetailTab('overview');
  };
  const backToCompanies = () => {
    setSelectedCompanyId(null);
    setDetailTab('overview');
  };

  if (noCompanyMetadataInUsers) {
    return <div className="company-explorer-page">
      <div className="section-heading"><div><div className="form-eyebrow">PLATFORM ADMINISTRATION</div><h1>Companies</h1><p>Explore companies and open their users, projects, and tasks.</p></div></div>
      <div className="helper-callout page-callout"><CircleAlert size={18} /><span>The users response does not include <code>companyId</code>/<code>companyID</code>. Add <code>companyId</code>, <code>companyName</code>, and <code>companyRole</code> to <code>UserResponse</code> and map them before using the company explorer.</span></div>
    </div>;
  }

  if (!selectedCompany) {
    return <div className="company-explorer-page">
      <SectionHeader
        eyebrow="PLATFORM ADMINISTRATION"
        title="Companies"
        description="Select a company to inspect its people, projects, and tasks without mixing it with other workspaces."
        count={companyIndex.length}
      />
      <div className="company-explorer-summary">
        <div><span className="explorer-summary-icon"><Building2 size={18} /></span><strong>{companyIndex.length}</strong><span>companies</span></div>
        <div><span className="explorer-summary-icon"><Users size={18} /></span><strong>{users.filter((user) => getCompanyId(user) !== null).length}</strong><span>associated users</span></div>
        <div><span className="explorer-summary-icon"><UserRound size={18} /></span><strong>{users.filter((user) => getCompanyId(user) === null).length}</strong><span>unassigned users</span></div>
      </div>
      {!hasProjectCompanyMetadata && <div className="helper-callout page-callout"><CircleAlert size={18} /><span>Company cards can be built from <code>UserResponse</code>, but project/task grouping needs project responses to include <code>companyId</code>. Until then, project and task counts are shown as unavailable rather than guessed.</span></div>}
      {filteredCompanies.length ? <div className="company-directory-grid">{filteredCompanies.map((company) => {
        const projectCount = hasProjectCompanyMetadata ? projects.filter((project) => getProjectCompanyId(project) === Number(company.id)).length : null;
        const companyProjectIdSet = new Set(hasProjectCompanyMetadata ? projects.filter((project) => getProjectCompanyId(project) === Number(company.id)).map((project) => Number(project.id)) : []);
        const taskCount = hasProjectCompanyMetadata ? tasks.filter((task) => companyProjectIdSet.has(Number(task.projectId))).length : null;
        return <button className="company-directory-card" key={company.id} onClick={() => openCompany(company.id)}>
          <div className="company-directory-top"><span className="company-directory-icon"><Building2 size={21} /></span><span className="company-directory-id">Company #{company.id}</span></div>
          <h2>{company.name}</h2>
          <p className="company-directory-admin"><ShieldCheck size={14} />{company.admin?.name || 'Administrator not listed'}{company.admin?.email ? <small>{company.admin.email}</small> : null}</p>
          <div className="company-directory-metrics"><div><Users size={15} /><strong>{company.users.length}</strong><span>users</span></div><div><FolderKanban size={15} /><strong>{projectCount === null ? '—' : projectCount}</strong><span>projects</span></div><div><ClipboardList size={15} /><strong>{taskCount === null ? '—' : taskCount}</strong><span>tasks</span></div></div>
          <div className="company-directory-footer"><span>Open company workspace</span><ChevronRight size={17} /></div>
        </button>;
      })}</div> : <EmptyState title={normalizedQuery ? 'No companies match your search' : 'No companies found'} message={normalizedQuery ? 'Try another company name, ID, or administrator.' : 'Companies appear here when at least one user is associated with them.'} />}
    </div>;
  }

  const companyStatusCounts = companyTasks.reduce((counts, task) => {
    const status = String(task.status || 'UNKNOWN').toUpperCase();
    counts[status] = (counts[status] || 0) + 1;
    return counts;
  }, {});

  return <div className="company-explorer-page">
    <div className="company-explorer-back-row"><button className="btn btn-secondary btn-small" onClick={backToCompanies}><ArrowLeft size={15} /> All companies</button><span>Platform administration <ChevronRight size={13} /> {selectedCompany.name}</span></div>
    <div className="company-explorer-hero">
      <div className="company-explorer-hero-icon"><Building2 size={27} /></div>
      <div className="company-explorer-hero-main"><div className="form-eyebrow">COMPANY WORKSPACE</div><h1>{selectedCompany.name}</h1><p>Company ID #{selectedCompany.id}</p></div>
      <div className="company-explorer-owner"><span>Company administrator</span><strong>{selectedCompany.admin?.name || 'Not listed'}</strong><small>{selectedCompany.admin?.email || 'No administrator found in the user list'}</small></div>
    </div>
    <div className="company-explorer-tabs" role="tablist" aria-label="Company sections">
      {[['overview', 'Overview', Activity], ['users', 'Users', Users], ['projects', 'Projects', FolderKanban], ['tasks', 'Tasks', ClipboardList]].map(([id, label, Icon]) => <button key={id} role="tab" aria-selected={detailTab === id} className={detailTab === id ? 'company-explorer-tab active' : 'company-explorer-tab'} onClick={() => setDetailTab(id)}><Icon size={16} />{label}{id === 'users' && <span>{selectedCompany.users.length}</span>}{id === 'projects' && <span>{hasProjectCompanyMetadata ? companyProjects.length : '—'}</span>}{id === 'tasks' && <span>{hasProjectCompanyMetadata ? companyTasks.length : '—'}</span>}</button>)}
    </div>

    {detailTab === 'overview' && <>
      {!hasProjectCompanyMetadata && <div className="helper-callout page-callout"><CircleAlert size={18} /><span>Project and task counts are unavailable because the project API response does not include <code>companyId</code>. Add <code>companyId</code> to <code>ProjectResponse</code> and populate it from <code>project.getCompany().getId()</code> in <code>ProjectMapper</code>.</span></div>}
      <div className="company-explorer-stats">
        <Metric icon={Users} label="Company users" value={selectedCompany.users.length} detail="Accounts associated with this company" />
        <Metric icon={FolderKanban} label="Projects" value={hasProjectCompanyMetadata ? companyProjects.length : '—'} detail={hasProjectCompanyMetadata ? 'Projects assigned to this company' : 'Requires ProjectResponse.companyId'} />
        <Metric icon={ClipboardList} label="Tasks" value={hasProjectCompanyMetadata ? companyTasks.length : '—'} detail={hasProjectCompanyMetadata ? 'Tasks from this company’s projects' : 'Requires project-to-company mapping'} />
        <Metric icon={ShieldCheck} label="Administrator" value={selectedCompany.admin ? 'Assigned' : 'Missing'} detail={selectedCompany.admin?.name || 'No COMPANY_ADMIN in the returned users'} />
      </div>
      <div className="company-explorer-overview-grid">
        <section className="surface-card company-explorer-panel"><div className="card-heading"><div><span className="card-eyebrow">PEOPLE</span><h2>Company users</h2><p>Users assigned to this company.</p></div><Users size={19} className="muted-icon" /></div>
          {selectedCompany.users.slice(0, 5).map((user) => <div className="company-explorer-person" key={user.id}><Avatar name={user.name} /><div><strong>{user.name}</strong><small>{user.email}</small></div><RoleChip role={user.companyRole} /></div>)}
          <button className="subtle-link" onClick={() => setDetailTab('users')}>View all users <ChevronRight size={15} /></button>
        </section>
        <section className="surface-card company-explorer-panel"><div className="card-heading"><div><span className="card-eyebrow">WORK</span><h2>Projects and tasks</h2><p>Work connected to this company's projects.</p></div><FolderKanban size={19} className="muted-icon" /></div>
          <div className="company-explorer-work-line"><span>Projects</span><strong>{hasProjectCompanyMetadata ? companyProjects.length : 'Unavailable'}</strong></div>
          <div className="company-explorer-work-line"><span>Tasks</span><strong>{hasProjectCompanyMetadata ? companyTasks.length : 'Unavailable'}</strong></div>
          <div className="company-explorer-work-line"><span>Open tasks</span><strong>{hasProjectCompanyMetadata ? companyTasks.filter((task) => String(task.status).toUpperCase() !== 'COMPLETED').length : '—'}</strong></div>
          <button className="subtle-link" onClick={() => setDetailTab('projects')}>View projects <ChevronRight size={15} /></button>
        </section>
      </div>
    </>}

    {detailTab === 'users' && <section className="surface-card users-table-card company-explorer-table-card"><div className="card-heading"><div><span className="card-eyebrow">COMPANY DIRECTORY</span><h2>Users in {selectedCompany.name}</h2><p>Only users whose company ID matches #{selectedCompany.id} are shown.</p></div><span className="admin-total"><Users size={17} /><strong>{filteredCompanyUsers.length}</strong><span>users</span></span></div>
      <div className="table-wrap"><table className="data-table users-table"><thead><tr><th>User</th><th>User ID</th><th>Company role</th><th>Created</th><th>Actions</th></tr></thead><tbody>{filteredCompanyUsers.map((user) => <tr key={user.id}><td><div className="admin-user-cell"><Avatar name={user.name} /><div><strong>{user.name}</strong><small>{user.email}</small></div></div></td><td><span className="member-id">#{user.id}</span></td><td><RoleChip role={user.companyRole} /></td><td>{formatDate(user.createdAt)}</td><td><div className="table-actions"><button className="btn btn-secondary btn-small" onClick={() => onEditUser(user)}>Edit</button><button className="btn btn-secondary btn-small" onClick={() => onGrantAdmin(user)}><ShieldCheck size={14} /> Grant admin</button><button className="icon-button danger-hover" aria-label={`Delete ${user.name}`} title="Delete user" onClick={() => onDeleteUser(user)}><Users size={15} /></button></div></td></tr>)}</tbody></table></div>
      {!filteredCompanyUsers.length && <EmptyState title={normalizedQuery ? 'No matching users' : 'No company users'} message={normalizedQuery ? 'Try another name, email, role, or ID.' : 'No users are associated with this company.'} />}
    </section>}

    {detailTab === 'projects' && <section className="surface-card users-table-card company-explorer-table-card"><div className="card-heading"><div><span className="card-eyebrow">COMPANY PROJECTS</span><h2>Projects in {selectedCompany.name}</h2><p>Projects are grouped using the company ID returned with each project.</p></div><span className="admin-total"><FolderKanban size={17} /><strong>{hasProjectCompanyMetadata ? filteredCompanyProjects.length : '—'}</strong><span>projects</span></span></div>
      {!hasProjectCompanyMetadata ? <div className="helper-callout"><CircleAlert size={18} /><span>The current project response has no company ID. Add <code>companyId</code> to <code>ProjectResponse</code> and map it from the project entity before project lists can be safely grouped by company.</span></div> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Project</th><th>Project ID</th><th>Status</th><th>Tasks</th></tr></thead><tbody>{filteredCompanyProjects.map((project) => <tr key={project.id}><td><div className="admin-user-cell"><span className="company-project-icon"><FolderKanban size={17} /></span><div><strong>{project.name}</strong><small>{project.companyName || selectedCompany.name}</small></div></div></td><td>#{project.id}</td><td><span className={`status-pill status-${String(project.status || '').toLowerCase().replaceAll('_', '-')}`}><i />{labelize(project.status || 'Unknown')}</span></td><td>{tasks.filter((task) => Number(task.projectId) === Number(project.id)).length}</td></tr>)}</tbody></table></div>}
      {hasProjectCompanyMetadata && !filteredCompanyProjects.length && <EmptyState title={normalizedQuery ? 'No matching projects' : 'No projects yet'} message={normalizedQuery ? 'Try another project name or status.' : 'This company has no projects in the current response.'} />}
    </section>}

    {detailTab === 'tasks' && <section className="surface-card users-table-card company-explorer-table-card"><div className="card-heading"><div><span className="card-eyebrow">COMPANY TASKS</span><h2>Tasks in {selectedCompany.name}</h2><p>Tasks are linked to the company's projects through project ID.</p></div><span className="admin-total"><ClipboardList size={17} /><strong>{hasProjectCompanyMetadata ? filteredCompanyTasks.length : '—'}</strong><span>tasks</span></span></div>
      {!hasProjectCompanyMetadata ? <div className="helper-callout"><CircleAlert size={18} /><span>Tasks can be grouped by company only after the project response supplies <code>companyId</code>. This prevents accidentally showing another company's tasks under the wrong company.</span></div> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Task</th><th>Project</th><th>Status</th><th>Priority</th><th>Due date</th></tr></thead><tbody>{filteredCompanyTasks.map((task) => { const project = companyProjects.find((item) => Number(item.id) === Number(task.projectId)); return <tr key={task.id}><td><div className="admin-user-cell"><span className="company-task-dot" /><div><strong>{task.title}</strong><small>{task.description || 'No description'}</small></div></div></td><td>{project?.name || `Project #${task.projectId}`}</td><td><span className={`status-pill status-${String(task.status || '').toLowerCase().replaceAll('_', '-')}`}><i />{labelize(task.status || 'Unknown')}</span></td><td>{labelize(task.priority || '')}</td><td>{formatDate(task.dueDate)}</td></tr>; })}</tbody></table></div>}
      {hasProjectCompanyMetadata && !filteredCompanyTasks.length && <EmptyState title={normalizedQuery ? 'No matching tasks' : 'No tasks yet'} message={normalizedQuery ? 'Try another task title, project, or status.' : 'This company has no tasks in its projects.'} />}
    </section>}
  </div>;
}

function SectionHeader({ eyebrow, title, description, count }) {
  return <div className="section-heading"><div><div className="form-eyebrow">{eyebrow}</div><h1>{title}</h1><p>{description}</p></div><div className="admin-total"><Building2 size={17} /><strong>{count}</strong><span>{count === 1 ? 'company' : 'companies'}</span></div></div>;
}

function Metric({ icon: Icon, label, value, detail }) {
  return <div className="stat-card company-explorer-metric"><div className="stat-top"><span className="stat-icon"><Icon size={19} /></span></div><div className="stat-value">{value}</div><div className="stat-label">{label}</div><p>{detail}</p></div>;
}
