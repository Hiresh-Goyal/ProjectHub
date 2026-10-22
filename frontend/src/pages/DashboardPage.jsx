import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity, AlertCircle, ArrowRight, Building2, CalendarDays, Check, CheckCircle2, ChevronDown, CircleHelp,
  Clock3, Command, FolderKanban, Gauge, LayoutDashboard, ListTodo, LogOut, Menu, Plus, RefreshCw, Search,
  Settings2, Shield, ShieldCheck, Trash2, UserRound, UserPlus, Users,
  X, Layers3, Pencil, LockKeyhole, ClipboardList,
} from 'lucide-react';
import { api, dedupeById, getCompanyId, getProjectCompanyId, getProjectOwnerId, hasCompanyFields } from '../api';
import { useAuth } from '../auth/AuthContext';
import Modal from '../components/Modal';
import CompanySetupPage from './CompanySetupPage';
import CompanyPage from './CompanyPage';
import CompanyExplorerPage from './CompanyExplorerPage';

const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'COMPLETED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];
const PROJECT_STATUSES = ['PLANNING', 'ACTIVE', 'COMPLETED', 'ON_HOLD'];

function labelize(value = '') {
  return String(value).toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}
function formatDate(value) {
  if (!value) return 'No due date';
  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}
function sameEmail(a, b) { return String(a || '').toLowerCase() === String(b || '').toLowerCase(); }
function dedupeTasks(tasks) { return dedupeById(tasks); }
function getInitials(name = '') {
  return String(name).split(/[\s@._-]+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'U';
}
function statusClass(value = '') { return `status-${String(value).toLowerCase().replaceAll('_', '-')}`; }
function priorityClass(value = '') { return `priority-${String(value).toLowerCase()}`; }

function EmptyState({ icon: Icon = ClipboardList, title, message, action = null }) {
  return <div className="empty-state"><div className="empty-icon"><Icon size={24} /></div><h3>{title}</h3><p>{message}</p>{action}</div>;
}
function SectionHeading({ eyebrow, title, description, action = null }) {
  return <div className="section-heading"><div><div className="form-eyebrow">{eyebrow}</div><h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div>;
}
function StatCard({ icon: Icon, label, value, hint, accent = 'blue' }) {
  return <div className={`stat-card accent-${accent}`}><div className="stat-top"><span className="stat-icon"><Icon size={19} /></span><span className="stat-caption">{hint}</span></div><div className="stat-value">{value}</div><div className="stat-label">{label}</div></div>;
}
function Avatar({ name, size = 'md' }) { return <span className={`avatar avatar-${size}`}>{getInitials(name)}</span>; }
function StatusPill({ value }) { return <span className={`status-pill ${statusClass(value)}`}><i />{labelize(value || 'Unknown')}</span>; }
function PriorityPill({ value }) { return <span className={`priority-pill ${priorityClass(value)}`}>{labelize(value || 'Normal')}</span>; }
function LoadingBlock({ label = 'Loading workspace…' }) { return <div className="page-loading"><span className="spinner spinner-dark" /><span>{label}</span></div>; }

function ProjectForm({ project, onSave, onClose }) {
  const [name, setName] = useState(project?.name || '');
  const [status, setStatus] = useState(project?.status || 'ACTIVE');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try { await onSave({ name: name.trim(), status }, project); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  return <Modal title={project ? 'Edit project' : 'Create a project'} subtitle={project ? 'Update the details for this workspace.' : 'Give your team a clear space to organize work.'} onClose={onClose}>
    <form className="stack-form" onSubmit={submit}>
      <label className="field"><span>Project name</span><input autoFocus required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Website refresh" /></label>
      <label className="field"><span>Status</span><select value={status} onChange={(e) => setStatus(e.target.value)}>{PROJECT_STATUSES.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}</select></label>
      {error && <div className="form-error">{error}</div>}
      <div className="modal-actions"><button type="button" className="btn btn-quiet" onClick={onClose}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? <span className="spinner" /> : project ? 'Save changes' : 'Create project'}</button></div>
    </form>
  </Modal>;
}

function TaskForm({ task, projects, onSave, onClose }) {
  const [title, setTitle] = useState(task?.title || '');
  const [description, setDescription] = useState(task?.description || '');
  const [status, setStatus] = useState(task?.status || 'TODO');
  const [priority, setPriority] = useState(task?.priority || 'MEDIUM');
  const [dueDate, setDueDate] = useState(task?.dueDate || '');
  const [projectId, setProjectId] = useState(String(task?.projectId ?? projects[0]?.id ?? ''));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      await onSave({ title: title.trim(), description: description.trim(), status, priority, dueDate: dueDate || null, projectId: Number(projectId) }, task);
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  return <Modal title={task ? 'Edit task' : 'Create a task'} subtitle={task ? 'Keep the details current.' : 'Break the project into a clear next action.'} onClose={onClose} size="lg">
    <form className="stack-form" onSubmit={submit}>
      <label className="field"><span>Task title</span><input autoFocus required maxLength={150} value={title} onChange={(e) => setTitle(e.target.value)} placeholder="What needs to get done?" /></label>
      <label className="field"><span>Description <small>Optional</small></span><textarea maxLength={1000} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Add context, notes, or acceptance criteria…" /></label>
      <div className="form-grid-2">
        <label className="field"><span>Project</span><select required value={projectId} onChange={(e) => setProjectId(e.target.value)}>{projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
        <label className="field"><span>Due date</span><input type="date" value={dueDate || ''} onChange={(e) => setDueDate(e.target.value)} /></label>
        <label className="field"><span>Status</span><select value={status} onChange={(e) => setStatus(e.target.value)}>{TASK_STATUSES.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}</select></label>
        <label className="field"><span>Priority</span><select value={priority} onChange={(e) => setPriority(e.target.value)}>{PRIORITIES.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}</select></label>
      </div>
      {error && <div className="form-error">{error}</div>}
      <div className="modal-actions"><button type="button" className="btn btn-quiet" onClick={onClose}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy || !projects.length}>{busy ? <span className="spinner" /> : task ? 'Save changes' : 'Create task'}</button></div>
    </form>
  </Modal>;
}

function ProjectMembersModal({ project, users = [], canSelectUsers = false, onClose, notify, onChanged }) {
  const [members, setMembers] = useState([]);
  const [userId, setUserId] = useState('');
  const [manualUserId, setManualUserId] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const loadMembers = useCallback(async () => {
    setLoading(true); setError('');
    try { setMembers(await api.get(`/projects/${project.id}/members`)); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [project.id]);
  useEffect(() => { loadMembers(); }, [loadMembers]);
  const availableUsers = users.filter((user) => !members.some((member) => Number(member.userId) === Number(user.id)));
  const addMember = async (event) => {
    event.preventDefault();
    if (!Number(userId) || Number(userId) <= 0) { setError('Enter a valid numeric user ID.'); return; }
    setBusy(true); setError('');
    try {
      await api.post(`/projects/${project.id}/members`, { userId: Number(userId) });
      setUserId(''); notify('success', 'Member added to the project.'); await loadMembers(); await onChanged();
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  const removeMember = async (member) => {
    if (!window.confirm(`Remove ${member.userName} from ${project.name}? Their task assignments in this project will also be removed.`)) return;
    try { await api.delete(`/projects/${project.id}/members/${member.userId}`); notify('success', `${member.userName} was removed.`); await loadMembers(); await onChanged(); }
    catch (err) { notify('error', err.message); }
  };
  return <Modal title="Project members" subtitle={`Manage access for ${project.name}.`} onClose={onClose} size="lg">
    <form className="inline-add-form" onSubmit={addMember}><label className="field"><span>{canSelectUsers && !manualUserId && availableUsers.length ? "Choose a user" : "Add by user ID"}</span>{canSelectUsers && !manualUserId && availableUsers.length ? <select required value={userId} onChange={(e) => setUserId(e.target.value)}><option value="">Choose a user…</option>{availableUsers.map((user) => <option key={user.id} value={user.id}>{user.name} · {user.email} (#{user.id})</option>)}</select> : <input inputMode="numeric" required value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="Enter teammate's user ID" />}</label><button className="btn btn-primary" type="submit" disabled={busy}>{busy ? <span className="spinner" /> : <><UserPlus size={16} /> Add member</>}</button></form>
    {canSelectUsers && availableUsers.length > 0 && <button type="button" className="text-button member-directory-toggle" onClick={() => { setManualUserId((value) => !value); setUserId(''); }}>{manualUserId ? 'Choose from the available users' : "Can't find the teammate? Enter a user ID"}</button>}
    {!canSelectUsers && <p className="helper-note">The user directory is not available for this role. Enter the teammate's user ID; the backend will validate company membership and project permissions.</p>}
    {canSelectUsers && !availableUsers.length && <p className="helper-note">No eligible users are included in your current directory response. Enter a user ID instead; the backend will validate the target's company membership.</p>}
    {canSelectUsers && availableUsers.length > 0 && <p className="helper-note">Only users in this project's company are selectable. The backend validates company membership again when saving.</p>}
    {error && <div className="form-error member-error">{error}</div>}
    <div className="member-list">
      {loading ? <LoadingBlock label="Loading members…" /> : members.length ? members.map((member) => <div className="member-row" key={member.id}><Avatar name={member.userName} /><div className="member-main"><strong>{member.userName}</strong><small>{member.userEmail || `User ID ${member.userId}`}</small></div><span className="member-id">ID {member.userId}</span><button className="icon-button danger-hover" aria-label={`Remove ${member.userName}`} title="Remove member" onClick={() => removeMember(member)}><Trash2 size={16} /></button></div>) : !error ? <EmptyState icon={Users} title="No members yet" message="Add a teammate by their user ID." /> : null}
    </div>
  </Modal>;
}

function TaskAssignmentsModal({ task, project, canManage, onClose, notify, onChanged }) {
  const [assignments, setAssignments] = useState([]);
  const [members, setMembers] = useState([]);
  const [projectMemberId, setProjectMemberId] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const load = useCallback(async () => {
    setLoading(true); setError('');
    const [assignResult, membersResult] = await Promise.allSettled([
      api.get(`/tasks/${task.id}/assignees`),
      canManage ? api.get(`/projects/${project.id}/members`) : Promise.resolve([]),
    ]);
    if (assignResult.status === 'fulfilled') setAssignments(assignResult.value || []);
    else setError(assignResult.reason.message);
    if (membersResult.status === 'fulfilled') setMembers(membersResult.value || []);
    else if (canManage) setError(membersResult.reason.message);
    setLoading(false);
  }, [task.id, project.id, canManage]);
  useEffect(() => { load(); }, [load]);
  const assign = async (event) => {
    event.preventDefault();
    if (!projectMemberId) return;
    setBusy(true); setError('');
    try {
      await api.post(`/tasks/${task.id}/assignees`, { projectMemberId: Number(projectMemberId) });
      setProjectMemberId(''); notify('success', 'Task assigned successfully.'); await load(); await onChanged();
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  const remove = async (assignment) => {
    if (!window.confirm(`Remove ${assignment.userName} from this task?`)) return;
    try { await api.delete(`/task-assignments/${assignment.id}`); notify('success', 'Task assignment removed.'); await load(); await onChanged(); }
    catch (err) { notify('error', err.message); }
  };
  const available = members.filter((member) => !assignments.some((assignment) => Number(assignment.projectMemberId) === Number(member.id)));
  return <Modal title="Task assignees" subtitle={`${task.title} · ${project.name}`} onClose={onClose} size="lg">
    {canManage && <form className="inline-add-form" onSubmit={assign}><label className="field"><span>Assign a project member</span><select value={projectMemberId} onChange={(e) => setProjectMemberId(e.target.value)}><option value="">Choose a member…</option>{available.map((member) => <option key={member.id} value={member.id}>{member.userName} · ID {member.userId}</option>)}</select></label><button className="btn btn-primary" type="submit" disabled={busy || !projectMemberId}>{busy ? <span className="spinner" /> : <><UserPlus size={16} /> Assign</>}</button></form>}
    {!canManage && <div className="helper-callout"><ShieldCheck size={17} /> You can view everyone assigned to this task. Project owners, company administrators, and global administrators can change assignments.</div>}
    {error && <div className="form-error member-error">{error}</div>}
    <div className="member-list">
      {loading ? <LoadingBlock label="Loading assignments…" /> : assignments.length ? assignments.map((assignment) => <div className="member-row" key={assignment.id}><Avatar name={assignment.userName} /><div className="member-main"><strong>{assignment.userName}</strong><small>Assigned {assignment.createdAt ? new Date(assignment.createdAt).toLocaleDateString() : 'recently'}</small></div><span className="member-id">User ID {assignment.userId}</span>{canManage && <button className="icon-button danger-hover" title="Remove assignment" aria-label={`Unassign ${assignment.userName}`} onClick={() => remove(assignment)}><Trash2 size={16} /></button>}</div>) : !error ? <EmptyState icon={Users} title="No one assigned yet" message={canManage ? 'Choose a project member above to assign this task.' : 'The task has no assignees yet.'} /> : null}
    </div>
  </Modal>;
}

function TaskDetailsModal({ task, project, canManage, onClose, onEdit, onAssignees, onStatusChange }) {
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const handleStatusChange = async (event) => {
    const nextStatus = event.target.value;
    setUpdatingStatus(true);
    const succeeded = await onStatusChange(task, nextStatus);
    setUpdatingStatus(false);
    if (succeeded) {
      onClose();
    }
  };

  return <Modal title={task.title} subtitle={`Task #${task.id} · ${project?.name || `Project #${task.projectId}`}`} onClose={onClose} size="lg">
    <div className="task-details-content">
      <div className="task-detail-badges"><StatusPill value={task.status} /><PriorityPill value={task.priority} /></div>
      <section className="task-detail-section"><span className="card-eyebrow">DESCRIPTION</span><p className="task-detail-description">{task.description?.trim() || 'No description has been added for this task yet.'}</p></section>
      <div className="task-detail-meta-grid">
        <div><span><FolderKanban size={15} /> Project</span><strong>{project?.name || `Project #${task.projectId}`}</strong></div>
        <div><span><CalendarDays size={15} /> Due date</span><strong>{formatDate(task.dueDate)}</strong></div>
        <div><span><Activity size={15} /> Status</span><strong>{labelize(task.status)}</strong></div>
        <div><span><Gauge size={15} /> Priority</span><strong>{labelize(task.priority)}</strong></div>
      </div>
      <label className="field task-detail-status"><span>Update status</span><select value={task.status} onChange={handleStatusChange} disabled={updatingStatus}>{TASK_STATUSES.map((status) => <option key={status} value={status}>{labelize(status)}</option>)}</select>{updatingStatus && <small className="field-hint">Updating task status…</small>}</label>
      <div className="modal-actions task-detail-actions"><button type="button" className="btn btn-secondary" onClick={onAssignees}><Users size={16} /> View assignees</button><span className="modal-action-spacer" />{canManage && <button type="button" className="btn btn-secondary" onClick={onEdit}><Pencil size={15} /> Edit task</button>}<button type="button" className="btn btn-primary" onClick={onClose}>Close</button></div>
    </div>
  </Modal>;
}

function UserEditModal({ user, onSave, onClose }) {
  const [name, setName] = useState(user.name || '');
  const [email, setEmail] = useState(user.email || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try { await onSave({ name: name.trim(), email: email.trim().toLowerCase() }, user); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  return <Modal title="Edit user" subtitle={`User ID ${user.id}`} onClose={onClose}><form className="stack-form" onSubmit={submit}><label className="field"><span>Name</span><input required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></label><label className="field"><span>Email</span><input type="email" required maxLength={200} value={email} onChange={(e) => setEmail(e.target.value)} /></label>{error && <div className="form-error">{error}</div>}<div className="modal-actions"><button type="button" className="btn btn-quiet" onClick={onClose}>Cancel</button><button className="btn btn-primary" type="submit" disabled={busy}>{busy ? <span className="spinner" /> : 'Save user'}</button></div></form></Modal>;
}

function PasswordModal({ userId, onClose, onSuccess }) {
  const [currentPass, setCurrentPass] = useState('');
  const [newPass, setNewPass] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try { await api.patch(`/users/${userId}/password`, { currentPass, newPass }); onSuccess(); }
    catch (err) { setError(err.message); }
    finally { setBusy(false); }
  };
  return <Modal title="Change password" subtitle="Use your current password to confirm the change." onClose={onClose}><form className="stack-form" onSubmit={submit}><label className="field"><span>Current password</span><input type="password" autoComplete="current-password" required maxLength={50} value={currentPass} onChange={(e) => setCurrentPass(e.target.value)} /></label><label className="field"><span>New password</span><input type="password" autoComplete="new-password" required minLength={8} maxLength={50} value={newPass} onChange={(e) => setNewPass(e.target.value)} /></label>{error && <div className="form-error">{error}</div>}<div className="modal-actions"><button type="button" className="btn btn-quiet" onClick={onClose}>Cancel</button><button className="btn btn-primary" type="submit" disabled={busy}>{busy ? <span className="spinner" /> : 'Update password'}</button></div></form></Modal>;
}

export default function DashboardPage() {
  const { session, logout, updateSession } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [assignedTasks, setAssignedTasks] = useState([]);
  const [users, setUsers] = useState([]);
  const [company, setCompany] = useState(null);
  const [companyChecked, setCompanyChecked] = useState(false);
  const [companyAdmin, setCompanyAdmin] = useState(false);
  const [lastInvitationCode, setLastInvitationCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [query, setQuery] = useState('');
  const [taskStatusFilter, setTaskStatusFilter] = useState('ALL');
  const [taskPriorityFilter, setTaskPriorityFilter] = useState('ALL');
  const [projectStatusFilter, setProjectStatusFilter] = useState('ALL');
  const [modal, setModal] = useState(null);
  const [toast, setToast] = useState(null);
  const [account, setAccount] = useState({ name: session?.displayName || '', email: session?.email || '' });
  const [accountLoading, setAccountLoading] = useState(false);
  const [accountError, setAccountError] = useState('');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const searchInputRef = useRef(null);
  const toastTimerRef = useRef(null);
  const sessionRef = useRef(session);

  useEffect(() => { sessionRef.current = session; }, [session]);

  const notify = useCallback((type, message) => {
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    setToast({ type, message, key: Date.now() + Math.random() });
    toastTimerRef.current = window.setTimeout(() => setToast(null), 4800);
  }, []);

  useEffect(() => () => {
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
  }, []);

  const refreshData = useCallback(async (quiet = false) => {
    const activeSession = sessionRef.current;
    if (!activeSession?.authHeader) return;
    if (quiet) setRefreshing(true); else setLoading(true);
    setLoadError('');

    let currentUser;
    try {
      currentUser = await api.get('/users/me');
    } catch (error) {
      setLoadError(`Could not load your account: ${error.message}`);
      if (error.status === 401) {
        notify('error', 'Your session expired. Please sign in again.');
        logout();
      }
      if (quiet) setRefreshing(false); else setLoading(false);
      return;
    }

    // UserResponse is the authoritative source for onboarding and company role.
    // getCompanyId supports both `companyId` and Java's possible `companyID` JSON name.
    let currentCompanyId = getCompanyId(currentUser);
    let currentCompanyName = currentUser.companyName ?? null;
    let currentCompanyRole = currentUser.companyRole ?? null;
    const userResponseHasCompanyFields = hasCompanyFields(currentUser);

    let currentCompany = null;
    if (!userResponseHasCompanyFields) {
      // Compatibility with an older backend response that predates company fields.
      try {
        currentCompany = await api.get('/companies/me');
        currentCompanyId = getCompanyId(currentCompany) ?? (currentCompany.id == null ? null : Number(currentCompany.id));
        currentCompanyName = currentCompany.companyName ?? null;
      } catch (error) {
        if (error.status !== 404) {
          setLoadError(`Could not load company details: ${error.message}`);
          setCompanyChecked(true);
          if (quiet) setRefreshing(false); else setLoading(false);
          return;
        }
        currentCompanyId = null;
      }
    } else if (currentCompanyId !== null) {
      try {
        currentCompany = await api.get('/companies/me');
        const responseCompanyId = currentCompany?.id == null ? null : Number(currentCompany.id);
        if (responseCompanyId !== null && responseCompanyId !== currentCompanyId) {
          throw new Error('The company details do not match your account company. Refresh or contact an administrator.');
        }
        currentCompanyName = currentCompanyName ?? currentCompany?.companyName ?? null;
      } catch (error) {
        setLoadError(`Could not load company details: ${error.message}`);
        setCompanyChecked(true);
        if (quiet) setRefreshing(false); else setLoading(false);
        return;
      }
    }

    if (userResponseHasCompanyFields) {
      const allCompanyFieldsNull = currentCompanyId === null && currentCompanyName === null && currentCompanyRole === null;
      const completeCompanyContext = currentCompanyId !== null && Boolean(currentCompanyName) && currentCompanyRole !== null;
      if (!allCompanyFieldsNull && !completeCompanyContext) {
        setLoadError('Your account has incomplete company information. Refresh your account or contact an administrator.');
        setCompanyChecked(true);
        if (quiet) setRefreshing(false); else setLoading(false);
        return;
      }
    }

    const companyContext = {
      userId: currentUser.id == null ? activeSession.userId : Number(currentUser.id),
      displayName: currentUser.name || activeSession.displayName,
      email: currentUser.email || activeSession.email,
      companyId: currentCompanyId,
      companyName: currentCompanyName,
      companyRole: currentCompanyRole,
    };
    const contextChanged =
      Number(activeSession?.userId) !== Number(companyContext.userId) ||
      activeSession?.displayName !== companyContext.displayName ||
      String(activeSession?.email || '').toLowerCase() !== String(companyContext.email || '').toLowerCase() ||
      getCompanyId(activeSession) !== companyContext.companyId ||
      (activeSession?.companyName ?? null) !== companyContext.companyName ||
      (activeSession?.companyRole ?? null) !== companyContext.companyRole;
    if (contextChanged) updateSession(companyContext);

    const hasCompany = currentCompanyId !== null;
    setCompany(currentCompany);
    setCompanyChecked(true);
    setCompanyAdmin(currentCompanyRole === 'COMPANY_ADMIN');

    // Users without a company see onboarding. A global platform admin may still inspect
    // platform-wide resources without having a company themselves.
    if (!hasCompany && !activeSession.isAdmin) {
      setProjects([]);
      setTasks([]);
      setAssignedTasks([]);
      setUsers([]);
      setCompanyAdmin(false);
      if (quiet) setRefreshing(false); else setLoading(false);
      return;
    }

    const [projectsResult, tasksResult, assignedResult, directoryResult] = await Promise.allSettled([
      api.get('/projects'),
      api.get('/tasks'),
      hasCompany ? api.get('/users/me/assigned-tasks') : Promise.resolve([]),
      api.get('/users'),
    ]);

    if (projectsResult.status === 'fulfilled') setProjects(dedupeById(projectsResult.value || []));
    else setLoadError((current) => current || projectsResult.reason.message);
    if (tasksResult.status === 'fulfilled') setTasks(dedupeTasks(tasksResult.value || []));
    else setLoadError((current) => current || tasksResult.reason.message);
    if (assignedResult.status === 'fulfilled') setAssignedTasks(assignedResult.value || []);
    else {
      setAssignedTasks([]);
      setLoadError((current) => current || `Could not load your assignments: ${assignedResult.reason.message}`);
    }

    if (directoryResult.status === 'fulfilled') {
      setUsers(dedupeById(directoryResult.value || []));
      // Compatibility for older UserResponse versions without company fields. On the updated
      // backend, companyRole above is the source of truth and this fallback is not used.
      if (!userResponseHasCompanyFields) {
        const legacyCompanyAdmin = Boolean(
          currentCompany && (
            !activeSession.isAdmin ||
            (currentCompany.companyOwnerName && currentCompany.companyOwnerName === companyContext.displayName)
          )
        );
        setCompanyAdmin(legacyCompanyAdmin);
      }
    } else {
      setUsers([]);
      if (!userResponseHasCompanyFields && directoryResult.reason.status === 403) setCompanyAdmin(false);
      // A 403 is expected for ordinary company members; /users is restricted by the service.
      if (directoryResult.reason.status !== 403 || activeSession.isAdmin) {
        setLoadError((current) => current || `Could not load the user directory: ${directoryResult.reason.message}`);
      }
    }

    const results = [projectsResult, tasksResult, assignedResult, directoryResult];
    if (results.some((result) => result.status === 'rejected' && result.reason.status === 401)) {
      notify('error', 'Your session expired. Please sign in again.');
      logout();
    }
    if (quiet) setRefreshing(false); else setLoading(false);
  }, [session?.authHeader, session?.isAdmin, updateSession, logout, notify]);
  useEffect(() => { refreshData(); }, [refreshData]);

  useEffect(() => {
    setAccount({ name: session?.displayName || '', email: session?.email || '' });
  }, [session?.userId, session?.displayName, session?.email]);

  useEffect(() => {
    const onShortcut = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && searchInputRef.current) {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
      if (event.key === 'Escape') {
        setProfileMenuOpen(false);
        if (document.activeElement === searchInputRef.current) {
          setQuery('');
          searchInputRef.current?.blur();
        }
      }
    };
    window.addEventListener('keydown', onShortcut);
    return () => window.removeEventListener('keydown', onShortcut);
  }, []);

  const canManageProject = useCallback((project) => {
    if (!project) return false;
    if (session?.isAdmin) return true;

    const currentCompanyId = getCompanyId(session);
    const projectCompanyId = getProjectCompanyId(project);
    // Non-admin project lists are already scoped by the backend. When the response includes
    // companyId, validate it explicitly; for compatibility, a missing value is tolerated only
    // for users who already have a company context.
    const sameCompany = projectCompanyId === null
      ? currentCompanyId !== null
      : currentCompanyId !== null && projectCompanyId === currentCompanyId;
    if (!sameCompany) return false;
    if (companyAdmin) return true;

    // Project ownership must come from the API rather than assuming every non-member owns it.
    return getProjectOwnerId(project) === Number(session?.userId);
  }, [session?.isAdmin, session?.userId, session?.companyId, session?.companyID, companyAdmin]);
  const projectById = useMemo(() => new Map(projects.map((project) => [Number(project.id), project])), [projects]);
  const manageableProjects = useMemo(() => projects.filter((project) => canManageProject(project)), [projects, canManageProject]);
  const filteredProjects = useMemo(() => projects.filter((project) => {
    const matchesQuery = `${project.name || ''} ${project.status || ''}`.toLowerCase().includes(query.toLowerCase());
    const matchesStatus = projectStatusFilter === 'ALL' || project.status?.toUpperCase() === projectStatusFilter;
    return matchesQuery && matchesStatus;
  }), [projects, query, projectStatusFilter]);
  const filteredTasks = useMemo(() => tasks.filter((task) => {
    const project = projectById.get(Number(task.projectId));
    const matchesQuery = `${task.title || ''} ${task.description || ''} ${project?.name || ''}`.toLowerCase().includes(query.toLowerCase());
    const matchesStatus = taskStatusFilter === 'ALL' || task.status?.toUpperCase() === taskStatusFilter;
    const matchesPriority = taskPriorityFilter === 'ALL' || task.priority?.toUpperCase() === taskPriorityFilter;
    return matchesQuery && matchesStatus && matchesPriority;
  }), [tasks, projectById, query, taskStatusFilter, taskPriorityFilter]);
  const filteredAssignedTasks = useMemo(() => assignedTasks.filter((assignment) => {
    const task = tasks.find((item) => Number(item.id) === Number(assignment.taskId));
    const searchable = `${assignment.taskName || task?.title || ''} ${assignment.projectName || ''} ${task?.description || ''}`;
    return searchable.toLowerCase().includes(query.toLowerCase());
  }), [assignedTasks, tasks, query]);

  const completedTasks = tasks.filter((task) => String(task.status).toUpperCase() === 'COMPLETED').length;
  const openTasks = tasks.filter((task) => String(task.status).toUpperCase() !== 'COMPLETED').length;
  const highPriorityTasks = tasks.filter((task) => String(task.priority).toUpperCase() === 'HIGH' && String(task.status).toUpperCase() !== 'COMPLETED').length;
  const overdueTasks = tasks.filter((task) => task.dueDate && task.dueDate < new Date().toISOString().slice(0, 10) && String(task.status).toUpperCase() !== 'COMPLETED');
  const attentionTasks = [...overdueTasks, ...tasks.filter((task) => String(task.priority).toUpperCase() === 'HIGH' && String(task.status).toUpperCase() !== 'COMPLETED' && !overdueTasks.some((item) => Number(item.id) === Number(task.id)))];
  const progress = tasks.length ? Math.round((completedTasks / tasks.length) * 100) : 0;

  const saveProject = async (body, existing) => {
    if (existing) await api.put(`/projects/${existing.id}`, body);
    else await api.post('/projects', body);
    setModal(null); notify('success', existing ? 'Project updated.' : 'Project created.'); await refreshData(true);
  };
  const deleteProject = async (project) => {
    if (!window.confirm(`Delete “${project.name}”? This also removes its tasks, memberships, and task assignments.`)) return;
    try { await api.delete(`/projects/${project.id}`); notify('success', 'Project deleted.'); await refreshData(true); }
    catch (error) { notify('error', error.message); }
  };
  const saveTask = async (body, existing) => {
    if (existing) await api.put(`/tasks/${existing.id}`, body);
    else await api.post('/tasks', body);
    setModal(null); notify('success', existing ? 'Task updated.' : 'Task created.'); await refreshData(true);
  };
  const changeTaskStatus = async (task, status) => {
    try { await api.patch(`/tasks/${task.id}/status`, { status }); notify('success', `Task moved to ${labelize(status)}.`); await refreshData(true); return true; }
    catch (error) { notify('error', error.message); return false; }
  };
  const deleteTask = async (task) => {
    if (!window.confirm(`Delete task “${task.title}”? This cannot be undone.`)) return;
    try { await api.delete(`/tasks/${task.id}`); notify('success', 'Task deleted.'); await refreshData(true); }
    catch (error) { notify('error', error.message); }
  };

  const openAccount = async () => {
    setActiveTab('account'); setMobileNavOpen(false); setAccountError('');
    setAccountLoading(true);
    try {
      const data = await api.get('/users/me');
      setAccount({ name: data.name || '', email: data.email || '' });
      updateSession({
        userId: data.id,
        displayName: data.name || session.displayName,
        email: data.email || session.email,
        companyId: getCompanyId(data),
        companyName: data.companyName ?? null,
        companyRole: data.companyRole ?? null,
      });
    } catch (error) { setAccountError(error.message); }
    finally { setAccountLoading(false); }
  };

  const saveAccount = async (event) => {
    event.preventDefault(); setAccountError('');
    const id = Number(session?.userId);
    if (!id) { setAccountError('Your account ID could not be loaded. Sign out and sign in again.'); return; }
    setAccountLoading(true);
    try {
      const oldEmail = session.email;
      const body = { name: account.name.trim(), email: account.email.trim().toLowerCase() };
      await api.put(`/users/${id}`, body);
      if (!sameEmail(oldEmail, body.email)) {
        window.alert('Your email was updated. Sign in again with the new email address and your existing password.');
        logout();
        return;
      }
      updateSession({ userId: id, displayName: body.name, email: body.email });
      notify('success', 'Profile updated.');
    } catch (error) { setAccountError(error.message); }
    finally { setAccountLoading(false); }
  };

  const saveUser = async (body, user) => {
    const editingSelf = sameEmail(user.email, session.email);
    const emailChanged = editingSelf && !sameEmail(user.email, body.email);
    await api.put(`/users/${user.id}`, body);
    setModal(null);
    if (emailChanged) {
      window.alert('Your email was updated. Sign in again with your new email address and existing password.');
      logout();
      return;
    }
    notify('success', 'User details updated.');
    if (editingSelf) updateSession({ displayName: body.name });
    await refreshData(true);
  };
  const grantAdmin = async (user) => {
    if (!window.confirm(`Grant administrator access to ${user.name} (${user.email})?`)) return;
    try { await api.post(`/admin/makeAdmin/${user.id}`); notify('success', `Administrator access granted to ${user.name}.`); }
    catch (error) { notify(error.status === 409 ? 'info' : 'error', error.status === 409 ? `${user.name} already has administrator access.` : error.message); }
  };
  const deleteUser = async (user) => {
    if (!window.confirm(`Delete user ${user.name} (${user.email})? This cannot be undone.`)) return;
    try { await api.delete(`/users/${user.id}`); notify('success', 'User deleted.'); await refreshData(true); }
    catch (error) { notify('error', error.message); }
  };
  const changePasswordSuccess = () => {
    setModal(null);
    window.alert('Password updated successfully. Sign in again using your new password.');
    logout();
  };

  const completeCompanySetup = async ({ company: createdCompany, invitationCode = '', created = false, joined = false }) => {
    if (createdCompany) setCompany(createdCompany);
    if (invitationCode) setLastInvitationCode(invitationCode);
    await refreshData(true);
    setActiveTab('overview');
    setQuery('');
    if (created) notify('success', 'Company created. Save the invitation code so teammates can join.');
    else if (joined) notify('success', 'You joined the company successfully.');
  };

  const rotateCompanyInvitation = async () => {
    const result = await api.patch('/companies/me/invitation-code');
    const code = typeof result === 'string' ? result.replace(/^"|"$/g, '') : String(result?.invitationCode || '');
    if (!code) throw new Error('The backend did not return an invitation code.');
    setLastInvitationCode(code);
    notify('success', 'Invitation code rotated. The previous code is no longer valid.');
  };

  const transferCompanyAdmin = async (userId) => {
    await api.put('/companies/me/owner', { id: Number(userId) });
    setLastInvitationCode('');
    notify('success', 'Company administrator role transferred. Your company role is now member.');
    await refreshData(true);
  };

  const navItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'projects', label: 'Projects', icon: FolderKanban, count: projects.length },
    { id: 'tasks', label: 'Tasks', icon: ListTodo, count: tasks.length },
    { id: 'my-work', label: 'My work', icon: CheckCircle2, count: assignedTasks.length },
    { id: 'company', label: 'Company', icon: Building2 },
  ];
  if (session?.isAdmin) {
    navItems.push({ id: 'companies', label: 'Companies', icon: Building2, count: new Set(users.map(getCompanyId).filter((id) => id !== null)).size });
    navItems.push({ id: 'admin', label: 'Admin', icon: ShieldCheck, count: users.length });
  }
  navItems.push({ id: 'account', label: 'Account settings', icon: Settings2 });

  const selectTab = (id) => {
    setActiveTab(id);
    setMobileNavOpen(false);
    setQuery('');
    if (id !== 'tasks') { setTaskStatusFilter('ALL'); setTaskPriorityFilter('ALL'); }
    if (id !== 'projects') setProjectStatusFilter('ALL');
  };
  const searchPlaceholder = ({
    overview: 'Search projects and tasks…',
    projects: 'Search projects…',
    tasks: 'Search tasks…',
    'my-work': 'Search assigned work…',
    company: 'Search company…',
    companies: 'Search companies and company records…',
    admin: 'Search users…',
  })[activeTab] || 'Search…';
  const welcomeName = (session?.displayName || session?.email?.split('@')[0] || 'there').split(' ')[0];

  const taskRow = (task) => {
    const project = projectById.get(Number(task.projectId));
    const canManage = canManageProject(project);
    return <tr key={task.id}>
      <td><div className="task-title-cell"><span className={`task-check ${String(task.status).toUpperCase() === 'COMPLETED' ? 'checked' : ''}`}>{String(task.status).toUpperCase() === 'COMPLETED' && <Check size={12} />}</span><div><button type="button" className="task-title-link" onClick={() => setModal({ type: 'task-details', task, project, canManage })}>{task.title}</button>{task.description && <small>{task.description}</small>}</div></div></td>
      <td><span className="project-link-label"><span className="tiny-project-dot" />{project?.name || `Project #${task.projectId}`}</span></td>
      <td><label className="table-status-control"><select aria-label={`Change status of ${task.title}`} value={task.status} onChange={(e) => changeTaskStatus(task, e.target.value)}>{TASK_STATUSES.map((status) => <option key={status} value={status}>{labelize(status)}</option>)}</select></label></td><td><PriorityPill value={task.priority} /></td><td><span className={`date-cell ${task.dueDate && task.dueDate < new Date().toISOString().slice(0, 10) && String(task.status).toUpperCase() !== 'COMPLETED' ? 'date-overdue' : ''}`}><CalendarDays size={14} />{formatDate(task.dueDate)}</span></td>
      <td><div className="table-actions"><button className="icon-button" title={canManage ? "Manage assignees" : "View assignees"} aria-label={canManage ? "Manage assignees" : "View assignees"} onClick={() => setModal({ type: 'assignments', task, project: project || { id: task.projectId, name: `Project #${task.projectId}` }, canManage })}><Users size={16} /></button>{canManage && <><button className="icon-button" title="Edit task" aria-label="Edit task" onClick={() => setModal({ type: 'task', task })}><Pencil size={15} /></button><button className="icon-button danger-hover" title="Delete task" aria-label="Delete task" onClick={() => deleteTask(task)}><Trash2 size={15} /></button></>}</div></td>
    </tr>;
  };

  if (loading) return <div className="app-loading"><div className="brand"><span className="brand-mark"><Layers3 size={21} /></span><span>project<span>hub</span></span></div><LoadingBlock /></div>;

  if (companyChecked && !company && !session?.isAdmin) {
    return <CompanySetupPage displayName={session?.displayName || session?.email?.split('@')[0]} onComplete={completeCompanySetup} onSignOut={logout} />;
  }

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}>
      <div className="sidebar-brand"><div className="brand brand-light"><span className="brand-mark"><Layers3 size={21} strokeWidth={2.5} /></span><span>project<span>hub</span></span></div><button className="icon-button sidebar-close" aria-label="Close menu" onClick={() => setMobileNavOpen(false)}><X size={18} /></button></div>
      <button type="button" className="workspace-switcher" onClick={() => selectTab('projects')} title="Browse projects"><div className="workspace-icon"><Command size={18} /></div><div className="workspace-label"><strong>{company?.companyName || session?.companyName || (session?.isAdmin ? 'Platform workspace' : 'My workspace')}</strong><small>{company ? `${projects.length} ${projects.length === 1 ? 'project' : 'projects'}` : 'Company setup required'}</small></div><ChevronDown size={15} /></button>
      <div className="sidebar-section-label">WORKSPACE</div>
      <nav className="sidebar-nav">{navItems.map((item) => <button className={`nav-item ${activeTab === item.id ? 'nav-item-active' : ''}`} key={item.id} onClick={() => selectTab(item.id)}><item.icon size={18} strokeWidth={1.8} /><span>{item.label}</span>{item.count !== undefined && <span className="nav-count">{item.count}</span>}</button>)}</nav>
      <div className="sidebar-bottom"><button className="sidebar-user" onClick={openAccount} title="Open account settings"><Avatar name={session?.displayName || session?.email} /><span className="sidebar-user-text"><strong>{session?.displayName || session?.email?.split('@')[0]}</strong><small>{session?.isAdmin ? 'Global administrator' : companyAdmin ? 'Company administrator' : 'Workspace member'}</small></span><Settings2 size={16} /></button></div>
    </aside>
    {mobileNavOpen && <button className="mobile-nav-scrim" aria-label="Close navigation" onClick={() => setMobileNavOpen(false)} />}

    <main className="main-panel">
      <header className="topbar"><div className="topbar-left"><button className="icon-button mobile-menu-toggle" aria-label="Open menu" onClick={() => setMobileNavOpen(true)}><Menu size={20} /></button><div className="breadcrumbs"><span>Workspace</span><span className="breadcrumb-separator">/</span><strong>{navItems.find((item) => item.id === activeTab)?.label || 'Overview'}</strong></div></div><div className="topbar-right">{activeTab !== 'account' && <div className="global-search" role="search"><Search size={16} /><input ref={searchInputRef} value={query} onChange={(e) => setQuery(e.target.value)} placeholder={searchPlaceholder} aria-label={`Search ${navItems.find((item) => item.id === activeTab)?.label || 'workspace'}`} /><kbd>Ctrl/⌘ K</kbd>{query && <button type="button" className="search-clear" aria-label="Clear search" title="Clear search" onClick={() => setQuery('')}><X size={13} /></button>}</div>}<button className={`icon-button refresh-button ${refreshing ? 'spinning' : ''}`} title="Refresh workspace" aria-label="Refresh workspace" disabled={refreshing} onClick={() => refreshData(true)}><RefreshCw size={17} /></button><span className="topbar-divider" /><div className="profile-menu-wrap" onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setProfileMenuOpen(false); }}><button className="profile-chip" aria-haspopup="menu" aria-expanded={profileMenuOpen} onClick={() => setProfileMenuOpen((open) => !open)}><Avatar name={session?.displayName || session?.email} /><span><strong>{session?.displayName || session?.email?.split('@')[0]}</strong><small>{session?.isAdmin ? 'Global admin' : companyAdmin ? 'Company admin' : 'Member'}</small></span><ChevronDown size={15} /></button>{profileMenuOpen && <div className="profile-menu" role="menu"><button role="menuitem" onClick={() => { setProfileMenuOpen(false); openAccount(); }}><UserRound size={16} /><span><strong>Account settings</strong><small>Profile and password</small></span></button><button role="menuitem" onClick={() => { setProfileMenuOpen(false); refreshData(true); }}><RefreshCw size={16} /><span><strong>Refresh workspace</strong><small>Reload projects and tasks</small></span></button><div className="profile-menu-divider" /><button role="menuitem" className="profile-menu-logout" onClick={() => { setProfileMenuOpen(false); logout(); }}><LogOut size={16} /><span><strong>Sign out</strong><small>End this browser-tab session</small></span></button></div>}</div></div></header>
      {loadError && <div className="inline-warning"><AlertCircle size={17} /><span>{loadError}</span><button onClick={() => refreshData(true)}>Try again</button><button className="icon-button" aria-label="Dismiss error" onClick={() => setLoadError('')}><X size={15} /></button></div>}

      <div className="page-content">
        {activeTab === 'overview' && <>
          <div className="welcome-row"><div><div className="date-eyebrow"><CalendarDays size={15} /> {new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</div><h1>Good to see you, {welcomeName}<span className="wave">✦</span></h1><p>Here's what's happening across your workspace today.</p></div><button className="btn btn-primary" onClick={() => setModal({ type: 'task' })} disabled={!manageableProjects.length}><Plus size={17} /> New task</button></div>
          <div className="stats-grid"><StatCard icon={FolderKanban} label="Projects in view" value={projects.length} hint="Across your workspace" accent="blue" /><StatCard icon={ListTodo} label="Open tasks" value={openTasks} hint={`${completedTasks} completed`} accent="violet" /><StatCard icon={Gauge} label="Task completion" value={`${progress}%`} hint="Overall progress" accent="green" /><StatCard icon={Activity} label="Needs attention" value={highPriorityTasks + overdueTasks.length} hint={`${highPriorityTasks} high priority · ${overdueTasks.length} overdue`} accent="orange" /></div>
          <div className="overview-grid"><section className="surface-card progress-card"><div className="card-heading"><div><span className="card-eyebrow">WORK IN MOTION</span><h2>Task overview</h2><p>Keep an eye on progress at a glance.</p></div><button className="subtle-link" onClick={() => selectTab('tasks')}>All tasks <ArrowRight size={15} /></button></div><div className="progress-main"><div className="progress-ring" style={{ '--progress': `${progress * 3.6}deg` }}><div><strong>{progress}%</strong><small>complete</small></div></div><div className="progress-legend"><div><span className="legend-dot legend-done" /><span>Completed</span><strong>{completedTasks}</strong></div><div><span className="legend-dot legend-open" /><span>In progress</span><strong>{tasks.filter((t) => String(t.status).toUpperCase() === 'IN_PROGRESS').length}</strong></div><div><span className="legend-dot legend-open" /><span>To do</span><strong>{tasks.filter((t) => String(t.status).toUpperCase() === 'TODO').length}</strong></div></div></div><div className="mini-bars">{TASK_STATUSES.map((status) => { const count = tasks.filter((t) => String(t.status).toUpperCase() === status).length; const width = tasks.length ? count / tasks.length * 100 : 0; return <div key={status} className="mini-bar-row"><span>{labelize(status)}</span><div className="mini-bar-track"><span className={`mini-bar-fill ${statusClass(status)}`} style={{ width: `${width}%` }} /></div><strong>{count}</strong></div>; })}</div></section>
            <section className="surface-card attention-card"><div className="card-heading"><div><span className="card-eyebrow">UP NEXT</span><h2>Needs attention</h2><p>Tasks that could use a nudge.</p></div><span className="attention-count">{attentionTasks.length}</span></div>{attentionTasks.length ? <div className="attention-list">{attentionTasks.slice(0, 4).map((task) => { const project = projectById.get(Number(task.projectId)); const overdue = overdueTasks.some((item) => Number(item.id) === Number(task.id)); return <div className="attention-item" key={task.id}><span className={`attention-mark ${overdue ? 'attention-mark-danger' : ''}`}><Clock3 size={16} /></span><div className="attention-task"><button type="button" className="attention-task-link" onClick={() => setModal({ type: 'task-details', task, project, canManage: canManageProject(project) })}>{task.title}</button><small>{project?.name || `Project #${task.projectId}`}</small></div><span className={`attention-date ${overdue ? 'text-danger' : ''}`}>{overdue ? 'Overdue' : labelize(task.priority)}</span></div>; })}</div> : <EmptyState icon={CheckCircle2} title="You're in good shape" message="No overdue or high-priority open tasks right now." />}</section></div>
          <section className="surface-card recent-tasks-card"><div className="card-heading"><div><span className="card-eyebrow">LATEST WORK</span><h2>Tasks at a glance</h2><p>Update status or open the full task list.</p></div><button className="subtle-link" onClick={() => selectTab('tasks')}>View all tasks <ArrowRight size={15} /></button></div>{filteredTasks.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Task</th><th>Project</th><th>Status</th><th>Priority</th><th>Due date</th><th /></tr></thead><tbody>{[...filteredTasks].sort((a, b) => (a.dueDate || '9999').localeCompare(b.dueDate || '9999')).slice(0, 5).map(taskRow)}</tbody></table></div> : <EmptyState icon={ListTodo} title={query ? 'No matching tasks' : 'No tasks yet'} message={query ? 'Try another search term or clear the search.' : 'Create a task inside one of your projects to get started.'} action={!query && <button className="btn btn-primary btn-small" disabled={!manageableProjects.length} onClick={() => setModal({ type: 'task' })}><Plus size={15} /> Create a task</button>} />}</section>
          <section className="projects-preview-section"><div className="section-heading compact-heading"><div><div className="form-eyebrow">YOUR SPACE</div><h2>Projects</h2></div><button className="subtle-link" onClick={() => selectTab('projects')}>Browse projects <ArrowRight size={15} /></button></div><div className="project-grid">{filteredProjects.slice(0, 3).map((project, index) => <ProjectCard key={project.id} project={project} canManage={canManageProject(project)} taskCount={tasks.filter((task) => Number(task.projectId) === Number(project.id)).length} onOpen={() => { setActiveTab('tasks'); setMobileNavOpen(false); setTaskStatusFilter('ALL'); setTaskPriorityFilter('ALL'); setQuery(project.name); }} onEdit={() => setModal({ type: 'project', project })} onMembers={() => setModal({ type: 'members', project })} onDelete={() => deleteProject(project)} accent={index} />)}{!filteredProjects.length && <div className="project-empty-tile"><div className="empty-icon"><FolderKanban size={22} /></div><strong>{query ? 'No matching projects' : 'Your first project starts here'}</strong><span>{query ? 'Clear the search to see all your projects.' : 'Make a space for work that matters.'}</span>{!query && <button className="btn btn-secondary btn-small" disabled={!company} title={!company ? 'Create or join a company first' : ''} onClick={() => setModal({ type: 'project' })}><Plus size={15} /> Create project</button>}</div>}</div></section>
        </>}

        {activeTab === 'projects' && <>
          <SectionHeading eyebrow="PROJECT DIRECTORY" title="Projects" description="Organize work into focused spaces and keep your team aligned." action={<button className="btn btn-primary" onClick={() => setModal({ type: 'project' })} disabled={!company} title={!company ? 'Create or join a company first' : ''}><Plus size={17} /> New project</button>} />
          <div className="toolbar-row"><div className="result-count"><strong>{filteredProjects.length}</strong> {filteredProjects.length === 1 ? 'project' : 'projects'}{query && <> matching <b>“{query}”</b></>}</div><div className="filter-group"><label className="filter-select"><span>Status</span><select value={projectStatusFilter} onChange={(e) => setProjectStatusFilter(e.target.value)}><option value="ALL">All statuses</option>{PROJECT_STATUSES.map((s) => <option key={s} value={s}>{labelize(s)}</option>)}</select></label>{(query || projectStatusFilter !== 'ALL') && <button className="text-button" onClick={() => { setQuery(''); setProjectStatusFilter('ALL'); }}>Clear filters</button>}</div></div>
          {filteredProjects.length ? <div className="project-grid project-grid-large">{filteredProjects.map((project, index) => <ProjectCard key={project.id} project={project} canManage={canManageProject(project)} taskCount={tasks.filter((task) => Number(task.projectId) === Number(project.id)).length} onOpen={() => { setActiveTab('tasks'); setMobileNavOpen(false); setTaskStatusFilter('ALL'); setTaskPriorityFilter('ALL'); setQuery(project.name); }} onEdit={() => setModal({ type: 'project', project })} onMembers={() => setModal({ type: 'members', project })} onDelete={() => deleteProject(project)} accent={index} />)}</div> : <EmptyState icon={FolderKanban} title={query ? 'No matching projects' : 'Your project space is ready'} message={query ? 'Try a different search or clear your filters.' : 'Create your first project to give the team a home for its work.'} action={!query && <button className="btn btn-primary" disabled={!company} title={!company ? 'Create or join a company first' : ''} onClick={() => setModal({ type: 'project' })}><Plus size={16} /> Create a project</button>} />}
        </>}

        {activeTab === 'tasks' && <>
          <SectionHeading eyebrow="TASK TRACKER" title="Tasks" description="Move work forward, one clear action at a time." action={<button className="btn btn-primary" onClick={() => setModal({ type: 'task' })} disabled={!manageableProjects.length}><Plus size={17} /> New task</button>} />
          {!projects.length && <div className="helper-callout page-callout"><CircleHelp size={17} /> Create a project before adding tasks. Tasks must belong to a project you own or administer.</div>}
          <div className="toolbar-row"><div className="result-count"><strong>{filteredTasks.length}</strong> {filteredTasks.length === 1 ? 'task' : 'tasks'}{query && <> matching <b>“{query}”</b></>}</div><div className="filter-group"><label className="filter-select"><span>Status</span><select value={taskStatusFilter} onChange={(e) => setTaskStatusFilter(e.target.value)}><option value="ALL">All statuses</option>{TASK_STATUSES.map((s) => <option key={s} value={s}>{labelize(s)}</option>)}</select></label><label className="filter-select"><span>Priority</span><select value={taskPriorityFilter} onChange={(e) => setTaskPriorityFilter(e.target.value)}><option value="ALL">All priorities</option>{PRIORITIES.map((p) => <option key={p} value={p}>{labelize(p)}</option>)}</select></label>{(query || taskStatusFilter !== 'ALL' || taskPriorityFilter !== 'ALL') && <button className="text-button" onClick={() => { setQuery(''); setTaskStatusFilter('ALL'); setTaskPriorityFilter('ALL'); }}>Clear filters</button>}</div></div>
          <section className="surface-card tasks-table-card">{filteredTasks.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Task</th><th>Project</th><th>Status</th><th>Priority</th><th>Due date</th><th>Actions</th></tr></thead><tbody>{filteredTasks.map(taskRow)}</tbody></table></div> : <EmptyState icon={ListTodo} title={query || taskStatusFilter !== 'ALL' || taskPriorityFilter !== 'ALL' ? 'No tasks match your filters' : 'Nothing on the task list yet'} message={query || taskStatusFilter !== 'ALL' || taskPriorityFilter !== 'ALL' ? 'Change your search or filters to see more tasks.' : 'Create the first task and start building momentum.'} action={!query && taskStatusFilter === 'ALL' && taskPriorityFilter === 'ALL' && <button className="btn btn-primary" disabled={!manageableProjects.length} onClick={() => setModal({ type: 'task' })}><Plus size={16} /> Create a task</button>} />}</section>
        </>}

        {activeTab === 'my-work' && <>
          <SectionHeading eyebrow="YOUR ASSIGNMENTS" title="My work" description="Tasks assigned to you across the projects you belong to." action={<button className="btn btn-secondary" onClick={() => refreshData(true)} disabled={refreshing}><RefreshCw size={16} /> Refresh</button>} />
          <div className="my-work-summary"><div className="my-work-summary-icon"><ClipboardList size={21} /></div><div><strong>{filteredAssignedTasks.length} {filteredAssignedTasks.length === 1 ? 'assignment' : 'assignments'}</strong><span>Your personal queue from the backend's assigned-tasks endpoint.</span></div><div className="my-work-summary-number">{filteredAssignedTasks.filter((item) => { const task = tasks.find((t) => Number(t.id) === Number(item.taskId)); return task && String(task.status).toUpperCase() !== 'COMPLETED'; }).length}<small>open</small></div></div>
          {filteredAssignedTasks.length ? <div className="assigned-grid">{filteredAssignedTasks.map((assignment) => { const task = tasks.find((item) => Number(item.id) === Number(assignment.taskId)); return <article className="assigned-card" key={assignment.id}><div className="assigned-card-top"><span className="assigned-project-chip"><FolderKanban size={13} />{assignment.projectName || 'Project'}</span>{task?.priority && <PriorityPill value={task.priority} />}</div><h3><button type="button" className="assigned-task-title" onClick={() => task && setModal({ type: 'task-details', task, project: projectById.get(Number(task.projectId)) || { id: task.projectId, name: assignment.projectName || `Project #${task.projectId}` }, canManage: canManageProject(projectById.get(Number(task.projectId))) })} disabled={!task}>{assignment.taskName || task?.title || `Task #${assignment.taskId}`}</button></h3><p>{task?.description || 'No description provided for this task.'}</p><div className="assigned-card-meta"><span><CalendarDays size={14} />{formatDate(task?.dueDate)}</span>{task && <StatusPill value={task.status} />}</div>{task && <div className="assigned-card-actions"><label className="field compact-field"><span>Update status</span><select value={task.status} onChange={(e) => changeTaskStatus(task, e.target.value)}>{TASK_STATUSES.map((s) => <option key={s} value={s}>{labelize(s)}</option>)}</select></label><button className="icon-button" title="View assignees" onClick={() => setModal({ type: 'assignments', task, project: projectById.get(Number(task.projectId)) || { id: task.projectId, name: assignment.projectName }, canManage: canManageProject(projectById.get(Number(task.projectId))) })}><Users size={16} /></button></div>}</article>; })}</div> : <EmptyState icon={CheckCircle2} title={query ? 'No matching assignments' : 'Your queue is clear'} message={query ? 'Try another search term or clear the search.' : 'Tasks assigned to you will appear here. Ask a project owner to assign you to a task.'} />}
        </>}

        {activeTab === 'companies' && session?.isAdmin && <CompanyExplorerPage
          users={users}
          projects={projects}
          tasks={tasks}
          query={query}
          onEditUser={(user) => setModal({ type: 'edit-user', user })}
          onGrantAdmin={grantAdmin}
          onDeleteUser={deleteUser}
        />}

        {activeTab === 'admin' && session?.isAdmin && <>
          <SectionHeading eyebrow="ADMINISTRATION" title="User management" description="Manage workspace accounts and grant administrator access." action={<div className="admin-total"><Users size={17} /><strong>{users.length}</strong><span>registered users</span></div>} />
          <div className="helper-callout page-callout"><ShieldCheck size={18} /> Administrator access is managed through your backend's protected <code>/api/v1/admin/makeAdmin/{'{userId}'}</code> endpoint. Promoting an existing administrator returns a conflict and is shown as an informational message.</div>
          <section className="surface-card users-table-card"><div className="card-heading"><div><span className="card-eyebrow">WORKSPACE DIRECTORY</span><h2>All users</h2><p>Company association and company-level role are shown from each user's current account record.</p></div><button className="icon-button" onClick={() => refreshData(true)} title="Refresh users"><RefreshCw size={16} /></button></div><div className="table-wrap"><table className="data-table users-table"><thead><tr><th>User</th><th>User ID</th><th>Company</th><th>Company role</th><th>Created</th><th>Actions</th></tr></thead><tbody>{users.filter((user) => `${user.name} ${user.email} ${user.companyName || ''} ${user.companyRole || ''}`.toLowerCase().includes(query.toLowerCase())).map((user) => <tr key={user.id}><td><div className="admin-user-cell"><Avatar name={user.name} /><div><strong>{user.name}</strong><small>{user.email}</small></div></div></td><td><span className="member-id">#{user.id}</span></td><td>{getCompanyId(user) !== null ? <span>{user.companyName || `Company #${getCompanyId(user)}`}</span> : <span className="muted-text">Not assigned</span>}</td><td>{user.companyRole ? <span className={`role-chip ${user.companyRole === 'COMPANY_ADMIN' ? 'role-admin' : ''}`}>{user.companyRole === 'COMPANY_ADMIN' ? 'Company admin' : 'Company member'}</span> : '—'}</td><td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</td><td><div className="table-actions"><button className="btn btn-secondary btn-small" onClick={() => setModal({ type: 'edit-user', user })}><Pencil size={14} /> Edit</button><button className="btn btn-secondary btn-small" onClick={() => grantAdmin(user)}><Shield size={14} /> Grant admin</button><button className="icon-button danger-hover" title="Delete user" onClick={() => deleteUser(user)}><Trash2 size={15} /></button></div></td></tr>)}</tbody></table></div>{!users.length && <EmptyState icon={Users} title={query ? "No matching users" : "No users found"} message={query ? "Try another search term or clear the search." : "Users registered through the API will appear here."} />}</section>
        </>}

        {activeTab === 'company' && <CompanyPage
          company={company}
          companyAdmin={companyAdmin}
          users={users}
          session={session}
          lastInvitationCode={lastInvitationCode}
          onSetupComplete={completeCompanySetup}
          onRotateInvitation={rotateCompanyInvitation}
          onTransferAdmin={transferCompanyAdmin}
          onSignOut={logout}
        />}

        {activeTab === 'account' && <>
          <SectionHeading eyebrow="PERSONAL SETTINGS" title="Account settings" description="Manage your profile and sign-in credentials." />
          <div className="account-layout"><aside className="surface-card account-identity"><div className="account-avatar"><Avatar name={session?.displayName || session?.email} size="xl" /></div><h2>{session?.displayName || 'ProjectHub member'}</h2><p>{session?.email}</p><span className={`role-chip ${session?.isAdmin ? 'role-admin' : ''}`}>{session?.isAdmin ? <ShieldCheck size={14} /> : <UserRound size={14} />}{session?.isAdmin ? 'Administrator' : 'Workspace member'}</span><div className="identity-divider" /><div className="identity-meta"><span>Account ID</span><strong>{session?.userId || 'Not discovered'}</strong></div><div className="identity-meta"><span>Company</span><strong>{session?.companyId != null ? (session?.companyName || `Company #${session.companyId}`) : 'Not assigned'}</strong></div><div className="identity-meta"><span>Company role</span><strong>{session?.companyRole ? labelize(session.companyRole) : '—'}</strong></div><div className="identity-meta"><span>Authentication</span><strong>HTTP Basic</strong></div><button className="btn btn-secondary full-width" onClick={logout}><LogOut size={16} /> Sign out</button></aside>
            <div className="account-main"><section className="surface-card account-form-card"><div className="card-heading"><div><h2>Profile information</h2><p>Changes are saved to your ProjectHub account.</p></div><UserRound size={19} className="muted-icon" /></div><form className="stack-form" onSubmit={saveAccount}><div className="account-id-row"><span>Account ID</span><strong>#{session?.userId ?? '—'}</strong><button className="text-button" type="button" onClick={openAccount} disabled={accountLoading}>{accountLoading ? 'Refreshing…' : 'Refresh profile'}</button></div><div className="form-grid-2"><label className="field"><span>Full name</span><input maxLength={100} required value={account.name} onChange={(e) => setAccount((v) => ({ ...v, name: e.target.value }))} /></label><label className="field"><span>Email address</span><input type="email" maxLength={200} required value={account.email} onChange={(e) => setAccount((v) => ({ ...v, email: e.target.value }))} /></label></div>{accountError && <div className="form-error">{accountError}</div>}<div className="account-actions"><button className="btn btn-secondary" type="button" disabled={accountLoading} onClick={openAccount}>{accountLoading ? <span className="spinner spinner-dark" /> : <><RefreshCw size={15} /> Discard changes</>}</button><button className="btn btn-primary" type="submit" disabled={accountLoading}>{accountLoading ? <span className="spinner" /> : 'Save changes'}</button></div></form></section>
              <section className="surface-card account-form-card"><div className="card-heading"><div><h2>Security</h2><p>Update the password used for Basic Auth API requests.</p></div><LockKeyhole size={19} className="muted-icon" /></div><div className="security-row"><div className="security-icon"><LockKeyhole size={18} /></div><div><strong>Password</strong><small>You'll need your current password to change it.</small></div><button className="btn btn-secondary btn-small" disabled={!session?.userId} onClick={() => setModal({ type: 'password', userId: Number(session.userId) })}>Change password</button></div><div className="security-note"><ShieldCheck size={15} /> Your authentication credential is kept in session storage for this tab and cleared when you sign out.</div></section>
            </div></div>
        </>}
      </div>
      <footer className="app-footer"><span>© {new Date().getFullYear()} ProjectHub</span><span><span className="footer-dot" /> Signed in</span><button onClick={() => notify('info', 'ProjectHub sends API requests to the configured Spring Boot backend. Local development uses the Vite proxy; deployments use VITE_API_BASE_URL.')}><CircleHelp size={14} /> Help</button></footer>
    </main>

    {toast && <div key={toast.key} className={`toast toast-${toast.type}`} role="status"><span className="toast-icon">{toast.type === 'success' ? <CheckCircle2 size={18} /> : toast.type === 'info' ? <CircleHelp size={18} /> : <AlertCircle size={18} />}</span><span>{toast.message}</span><button aria-label="Dismiss notification" onClick={() => { if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current); setToast(null); }}><X size={16} /></button></div>}
    {modal?.type === 'project' && <ProjectForm project={modal.project} onClose={() => setModal(null)} onSave={saveProject} />}
    {modal?.type === 'task-details' && <TaskDetailsModal task={modal.task} project={modal.project} canManage={modal.canManage} onClose={() => setModal(null)} onEdit={() => setModal({ type: 'task', task: modal.task })} onAssignees={() => setModal({ type: 'assignments', task: modal.task, project: modal.project || { id: modal.task.projectId, name: `Project #${modal.task.projectId}` }, canManage: modal.canManage })} onStatusChange={changeTaskStatus} />}
    {modal?.type === 'task' && <TaskForm task={modal.task} projects={modal.task ? manageableProjects.filter((p) => Number(p.id) === Number(modal.task.projectId) || canManageProject(p)) : manageableProjects} onClose={() => setModal(null)} onSave={saveTask} />}
    {modal?.type === 'members' && <ProjectMembersModal
      project={modal.project}
      users={users.filter((user) => {
        const responseCompanyId = getProjectCompanyId(modal.project);
        const projectCompanyId = responseCompanyId ?? (session?.isAdmin ? null : getCompanyId(session));
        return projectCompanyId !== null && getCompanyId(user) === projectCompanyId;
      })}
      canSelectUsers={canManageProject(modal.project)}
      onClose={() => setModal(null)}
      notify={notify}
      onChanged={() => refreshData(true)}
    />}
    {modal?.type === 'assignments' && <TaskAssignmentsModal task={modal.task} project={modal.project} canManage={modal.canManage} onClose={() => setModal(null)} notify={notify} onChanged={() => refreshData(true)} />}
    {modal?.type === 'edit-user' && <UserEditModal user={modal.user} onClose={() => setModal(null)} onSave={saveUser} />}
    {modal?.type === 'password' && <PasswordModal userId={modal.userId} onClose={() => setModal(null)} onSuccess={changePasswordSuccess} />}
  </div>;
}

function ProjectCard({ project, canManage, taskCount, onOpen, onEdit, onMembers, onDelete, accent = 0 }) {
  const palette = ['project-accent-blue', 'project-accent-violet', 'project-accent-green', 'project-accent-orange'];
  return <article className={`project-card ${palette[accent % palette.length]}`}><div className="project-card-top"><div className="project-symbol"><FolderKanban size={20} /></div><StatusPill value={project.status} /></div><h3><button type="button" className="project-title-link" onClick={onOpen}>{project.name}</button></h3><p className="project-card-description">{taskCount} {taskCount === 1 ? 'task' : 'tasks'} · {canManage ? 'You can manage this project' : 'You are a project member'}</p><button type="button" className="project-card-open" onClick={onOpen}>View tasks <ArrowRight size={15} /></button><div className="project-card-footer"><div className="project-card-metric"><ListTodo size={15} /><strong>{taskCount}</strong><span>{taskCount === 1 ? 'task' : 'tasks'}</span></div><div className="project-card-actions">{canManage && <><button type="button" aria-label="Manage members" title="Manage members" onClick={onMembers}><Users size={15} /></button><button type="button" aria-label="Edit project" title="Edit project" onClick={onEdit}><Pencil size={14} /></button><button type="button" aria-label="Delete project" title="Delete project" onClick={onDelete}><Trash2 size={14} /></button></>}</div></div></article>;
}
