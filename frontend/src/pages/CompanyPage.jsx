import { useState } from 'react';
import { ArrowRightLeft, Building2, CalendarDays, Check, Copy, KeyRound, RefreshCw, ShieldCheck, UsersRound } from 'lucide-react';
import CompanySetupPage from './CompanySetupPage';
import { getCompanyId } from '../api';

function formatCreatedAt(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function CompanyPage({ company, companyAdmin, users = [], session, lastInvitationCode, onSetupComplete, onRotateInvitation, onTransferAdmin, onSignOut }) {
  const [targetUserId, setTargetUserId] = useState('');
  const [transferBusy, setTransferBusy] = useState(false);
  const [copyState, setCopyState] = useState(false);
  const [rotateError, setRotateError] = useState('');
  const [transferError, setTransferError] = useState('');
  const [rotateBusy, setRotateBusy] = useState(false);

  if (!company) {
    return <CompanySetupPage embedded displayName={session?.displayName} onComplete={onSetupComplete} onSignOut={onSignOut} />;
  }

  const currentCompanyId = getCompanyId(session) ?? (company?.id == null ? null : Number(company.id));
  // The backend permits transfer to an unassigned user or to a member of this company,
  // but rejects users belonging to a different company.
  const possibleAdmins = users.filter((user) => {
    if (Number(user.id) === Number(session?.userId)) return false;
    const targetCompanyId = getCompanyId(user);
    return targetCompanyId === null || (currentCompanyId !== null && targetCompanyId === currentCompanyId);
  });

  const copyCode = async () => {
    if (!lastInvitationCode) return;
    try {
      await navigator.clipboard.writeText(lastInvitationCode);
      setCopyState(true);
      window.setTimeout(() => setCopyState(false), 1800);
    } catch {
      setRotateError('Copy is unavailable in this browser. Select the invitation code and copy it manually.');
    }
  };

  const rotateCode = async () => {
    setRotateError('');
    setRotateBusy(true);
    try { await onRotateInvitation(); }
    catch (err) { setRotateError(err.message || 'Could not rotate the invitation code.'); }
    finally { setRotateBusy(false); }
  };

  const transfer = async (event) => {
    event.preventDefault();
    const target = possibleAdmins.find((user) => Number(user.id) === Number(targetUserId));
    if (!target) { setTransferError('Choose a user first.'); return; }
    if (!window.confirm(`Transfer company administrator privileges to ${target.name} (${target.email})? You will become a company member.`)) return;
    setTransferBusy(true);
    setTransferError('');
    try {
      await onTransferAdmin(Number(target.id));
      setTargetUserId('');
    } catch (err) { setTransferError(err.message || 'Could not transfer company administration.'); }
    finally { setTransferBusy(false); }
  };

  return (
    <div className="company-page">
      <div className="section-heading">
        <div><div className="form-eyebrow">WORKSPACE SETTINGS</div><h1>Company</h1><p>Company details, invitations, and administrator controls.</p></div>
        <span className={`role-chip ${companyAdmin ? 'role-admin' : ''}`}>{companyAdmin ? <ShieldCheck size={14} /> : <UsersRound size={14} />}{companyAdmin ? 'Company administrator' : 'Company member'}</span>
      </div>

      <section className="company-hero-card">
        <div className="company-hero-mark"><Building2 size={26} /></div>
        <div className="company-hero-main"><span className="card-eyebrow">CURRENT COMPANY</span><h2>{company.companyName || 'Company workspace'}</h2><p>Company ID #{company.id}</p></div>
        <div className="company-hero-meta"><span>Administrator</span><strong>{company.companyOwnerName || 'Not available'}</strong></div>
      </section>

      <div className="company-info-grid">
        <section className="surface-card company-info-card"><div className="company-info-icon"><CalendarDays size={19} /></div><div><span className="card-eyebrow">CREATED</span><h3>{formatCreatedAt(company.createdAt)}</h3><p>The date this company was registered.</p></div></section>
        <section className="surface-card company-info-card"><div className="company-info-icon"><UsersRound size={19} /></div><div><span className="card-eyebrow">MEMBERSHIP</span><h3>{companyAdmin ? 'You administer this company' : 'You are part of this company'}</h3><p>{companyAdmin ? 'You can rotate the invitation code and transfer administration.' : 'Contact the company administrator if you need a new invitation code.'}</p></div></section>
      </div>

      {companyAdmin ? (
        <div className="company-admin-grid">
          <section className="surface-card company-action-card">
            <div className="card-heading"><div><span className="card-eyebrow">TEAM ACCESS</span><h2>Invitation code</h2><p>Generate a replacement code to invalidate the previous one.</p></div><div className="company-action-symbol"><KeyRound size={20} /></div></div>
            {lastInvitationCode ? (
              <div className="invitation-code-box"><code>{lastInvitationCode}</code><button type="button" className="btn btn-secondary btn-small" onClick={copyCode}>{copyState ? <Check size={15} /> : <Copy size={15} />}{copyState ? 'Copied' : 'Copy code'}</button></div>
            ) : (
              <div className="invite-hidden-note"><KeyRound size={17} /><span>The current invitation code is not retrievable. Rotate it to generate a new code you can share.</span></div>
            )}
            {rotateError && <div className="form-error" role="alert">{rotateError}</div>}
            <button type="button" className="btn btn-primary" onClick={rotateCode} disabled={rotateBusy}>{rotateBusy ? <span className="spinner" /> : <><RefreshCw size={16} /> Rotate invitation code</>}</button>
          </section>

          <section className="surface-card company-action-card">
            <div className="card-heading"><div><span className="card-eyebrow">ADMINISTRATOR</span><h2>Transfer administration</h2><p>Transfer this company's admin role to another eligible account.</p></div><div className="company-action-symbol"><ArrowRightLeft size={20} /></div></div>
            <form className="stack-form" onSubmit={transfer}>
              <label className="field"><span>New company administrator</span><select required value={targetUserId} onChange={(event) => setTargetUserId(event.target.value)}><option value="">Select a user…</option>{possibleAdmins.map((user) => <option key={user.id} value={user.id}>{user.name} · {user.email} (#{user.id}) — {getCompanyId(user) === null ? 'No company' : 'Same company'}</option>)}</select></label>
              <small className="field-hint">You can transfer administration to an unassigned user or someone already in this company. Users belonging to another company are excluded.</small>
              {transferError && <div className="form-error" role="alert">{transferError}</div>}
              <button type="submit" className="btn btn-secondary" disabled={transferBusy || !targetUserId}>{transferBusy ? <span className="spinner spinner-dark" /> : <><ArrowRightLeft size={16} /> Transfer admin role</>}</button>
            </form>
          </section>
        </div>
      ) : (
        <div className="helper-callout page-callout"><ShieldCheck size={18} /><span>Company administration is managed by <strong>{company.companyOwnerName || 'your company administrator'}</strong>. Invitation-code rotation and administrator transfer are available to the company administrator only.</span></div>
      )}
    </div>
  );
}
