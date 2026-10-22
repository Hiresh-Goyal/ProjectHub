import { useState } from 'react';
import { ArrowRight, Building2, CheckCircle2, Layers3, LockKeyhole, LogOut, UsersRound } from 'lucide-react';
import { api } from '../api';

export default function CompanySetupPage({ onComplete, onSignOut, displayName = 'there', embedded = false }) {
  const [mode, setMode] = useState('create');
  const [companyName, setCompanyName] = useState('');
  const [invitationCode, setInvitationCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      if (mode === 'create') {
        const createdCompany = await api.post('/companies', { companyName: companyName.trim() });
        await onComplete({ company: createdCompany, invitationCode: createdCompany?.invitationCode || '', created: true });
      } else {
        await api.post('/companies/join', { invitationCode: invitationCode.trim() });
        await onComplete({ company: null, invitationCode: '', joined: true });
      }
    } catch (err) {
      setError(err.message || 'Could not complete company setup.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className={`company-setup-shell ${embedded ? 'company-setup-embedded' : ''}`}>
      {!embedded && (
        <header className="setup-topbar">
          <div className="brand"><span className="brand-mark"><Layers3 size={21} /></span><span>project<span>hub</span></span></div>
          <button className="btn btn-quiet setup-signout" type="button" onClick={onSignOut}><LogOut size={16} /> Sign out</button>
        </header>
      )}
      <div className="company-setup-layout">
        <section className="company-setup-intro">
          <div className="setup-eyebrow"><span className="eyebrow-dot" /> YOUR WORKSPACE STARTS HERE</div>
          <h1>{embedded ? 'Your company workspace' : `One more step, ${String(displayName).split(' ')[0]}.`}</h1>
          <p>{embedded ? 'Create a company or join an existing team to work with projects and tasks.' : 'Choose how you want to work with your team. Your projects and tasks will stay within the company you join.'}</p>
          <div className="setup-benefit-list">
            <div><span><Building2 size={18} /></span><div><strong>Create a company</strong><small>Set up a workspace and become its company administrator.</small></div></div>
            <div><span><UsersRound size={18} /></span><div><strong>Join your team</strong><small>Use an invitation code shared by a company administrator.</small></div></div>
            <div><span><LockKeyhole size={18} /></span><div><strong>Keep work separated</strong><small>Projects and tasks are scoped to your company by the backend.</small></div></div>
          </div>
        </section>

        <section className="company-setup-card">
          <div className="setup-card-heading">
            <span className="setup-card-icon"><Building2 size={21} /></span>
            <div><div className="card-eyebrow">COMPANY SETUP</div><h2>{mode === 'create' ? 'Create your workspace' : 'Join an existing company'}</h2><p>{mode === 'create' ? 'You can invite teammates after setup.' : 'Enter the invitation code you received.'}</p></div>
          </div>
          <div className="setup-mode-switch" role="tablist" aria-label="Company setup option">
            <button type="button" role="tab" aria-selected={mode === 'create'} className={mode === 'create' ? 'setup-mode-active' : ''} onClick={() => { setMode('create'); setError(''); }}>Create company</button>
            <button type="button" role="tab" aria-selected={mode === 'join'} className={mode === 'join' ? 'setup-mode-active' : ''} onClick={() => { setMode('join'); setError(''); }}>Join company</button>
          </div>
          <form className="stack-form setup-form" onSubmit={submit}>
            {mode === 'create' ? (
              <label className="field"><span>Company name</span><input autoFocus={!embedded} required maxLength={150} value={companyName} onChange={(event) => setCompanyName(event.target.value)} placeholder="e.g. Northstar Studio" /><small className="field-hint">Use the name your team will recognize.</small></label>
            ) : (
              <label className="field"><span>Invitation code</span><input autoFocus required value={invitationCode} onChange={(event) => setInvitationCode(event.target.value)} autoCapitalize="none" autoCorrect="off" spellCheck="false" placeholder="Paste your company invitation code" /><small className="field-hint">Invitation codes are case-sensitive. Paste the full code.</small></label>
            )}
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="btn btn-primary setup-submit" type="submit" disabled={busy || (mode === 'create' ? !companyName.trim() : !invitationCode.trim())}>
              {busy ? <span className="spinner" /> : <>{mode === 'create' ? 'Create company' : 'Join company'} <ArrowRight size={17} /></>}
            </button>
          </form>
          <div className="setup-secure-note"><CheckCircle2 size={15} /> {mode === 'create' ? 'Your invitation code is shown after your company is created.' : 'The backend validates the invitation code before joining you.'}</div>
        </section>
      </div>
      {!embedded && <footer className="setup-footer"><span>© {new Date().getFullYear()} ProjectHub</span><span>Projects. People. Progress.</span></footer>}
    </main>
  );
}
