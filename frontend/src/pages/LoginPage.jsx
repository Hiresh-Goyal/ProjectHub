import { useState } from 'react';
import { ArrowRight, CheckCircle2, Layers3, LockKeyhole, ShieldCheck, UsersRound } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { login, register, authBusy } = useAuth();
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    try {
      if (mode === 'register') {
        if (password.length < 8) {
          setError('Use a password of at least 8 characters.');
          return;
        }
        await register({ name, email, password });
      } else {
        await login(email, password);
      }
    } catch (err) {
      setError(err.message || 'Could not sign in. Please try again.');
    }
  };

  return (
    <main className="auth-screen">
      <div className="auth-showcase">
        <div className="brand brand-light"><span className="brand-mark"><Layers3 size={22} strokeWidth={2.5} /></span><span>project<span>hub</span></span></div>
        <div className="showcase-copy">
          <div className="eyebrow light-eyebrow"><span className="eyebrow-dot" /> YOUR TEAM'S WORKSPACE</div>
          <h1>Make meaningful work<br /><em>move forward.</em></h1>
          <p>Bring projects, people, and priorities into one clear view. Spend less time chasing updates and more time building.</p>
          <div className="showcase-features">
            <div><span className="feature-icon"><CheckCircle2 size={19} /></span><span><strong>Projects that stay on track</strong><small>One home for every milestone.</small></span></div>
            <div><span className="feature-icon"><UsersRound size={19} /></span><span><strong>Teams that stay connected</strong><small>Clear ownership and assignments.</small></span></div>
            <div><span className="feature-icon"><ShieldCheck size={19} /></span><span><strong>Access that makes sense</strong><small>Role-aware controls and visibility.</small></span></div>
          </div>
        </div>
        <div className="showcase-footer"><span>PLAN TOGETHER.</span><span>SHIP WITH CONFIDENCE.</span><span>© PROJECTHUB</span></div>
        <div className="showcase-orb orb-one" /><div className="showcase-orb orb-two" />
      </div>
      <section className="auth-panel">
        <div className="auth-panel-inner">
          <div className="mobile-brand brand"><span className="brand-mark"><Layers3 size={22} /></span><span>project<span>hub</span></span></div>
          <div className="auth-heading">
            <div className="form-eyebrow">{mode === 'login' ? 'WELCOME BACK' : 'GET STARTED'}</div>
            <h2>{mode === 'login' ? 'Sign in to your workspace' : 'Create your account'}</h2>
            <p>{mode === 'login' ? 'Pick up where you and your team left off.' : 'Create an account to start organizing your work.'}</p>
          </div>
          <form className="auth-form" onSubmit={submit}>
            {mode === 'register' && <label className="field"><span>Full name</span><input autoComplete="name" required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Alex Morgan" /></label>}
            <label className="field"><span>Email address</span><input type="email" autoComplete="email" required maxLength={200} value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" /></label>
            <label className="field"><span>Password</span><div className="password-input"><LockKeyhole size={17} /><input type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required minLength={mode === 'register' ? 8 : 1} maxLength={50} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" /></div>{mode === 'register' && <small className="field-hint">Use at least 8 characters.</small>}</label>
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="btn btn-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? <span className="spinner" /> : <>{mode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={17} /></>}</button>
          </form>
          <div className="auth-switch">{mode === 'login' ? <>New to ProjectHub? <button type="button" onClick={() => { setMode('register'); setError(''); }}>Create an account</button></> : <>Already have an account? <button type="button" onClick={() => { setMode('login'); setError(''); }}>Sign in instead</button></>}</div>
          <div className="auth-security"><ShieldCheck size={15} /> Secure sign-in · Your session stays in this browser tab</div>
        </div>
      </section>
    </main>
  );
}
