import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api, createBasicAuth, getCompanyId, SESSION_KEY } from '../api';

const AuthContext = createContext(null);

function readSavedSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null');
  } catch {
    return null;
  }
}

function persist(session) {
  try {
    if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    else sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // Authentication state can still live for the current render if storage is unavailable.
  }
}

async function discoverAccount(email, authHeader) {
  // The current backend exposes /users/me, so we no longer guess the signed-in user's ID.
  const user = await api.get('/users/me', { authHeader });
  let isAdmin = false;

  // The backend's global ROLE_ADMIN is separate from companyRole. The protected admin
  // endpoint is the authoritative global-role probe; 403 is expected for non-admins.
  try {
    await api.get('/admin', { authHeader });
    isAdmin = true;
  } catch (error) {
    if (error.status !== 403) throw error;
  }

  return {
    email: user.email || email.trim().toLowerCase(),
    authHeader,
    userId: Number(user.id),
    displayName: user.name || user.email?.split('@')[0] || email.split('@')[0],
    companyId: getCompanyId(user),
    companyName: user.companyName ?? null,
    companyRole: user.companyRole ?? null,
    isAdmin,
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSavedSession);
  const [authBusy, setAuthBusy] = useState(false);
  const restoreChecked = useRef(false);

  // Refresh identity and role when restoring an existing browser-tab session. This also makes
  // older saved sessions compatible with the newer /users/me endpoint.
  useEffect(() => {
    if (restoreChecked.current) return;
    restoreChecked.current = true;
    const saved = readSavedSession();
    if (!saved?.authHeader || !saved?.email) return;

    let active = true;
    discoverAccount(saved.email, saved.authHeader)
      .then((updated) => {
        if (!active) return;
        persist(updated);
        setSession(updated);
      })
      .catch((error) => {
        if (!active) return;
        if (error.status === 401) {
          persist(null);
          setSession(null);
        }
      });

    return () => { active = false; };
  }, []);

  const login = useCallback(async (email, password) => {
    setAuthBusy(true);
    try {
      const normalizedEmail = email.trim().toLowerCase();
      const authHeader = createBasicAuth(normalizedEmail, password);
      const discovered = await discoverAccount(normalizedEmail, authHeader);
      persist(discovered);
      setSession(discovered);
      return discovered;
    } finally {
      setAuthBusy(false);
    }
  }, []);

  const register = useCallback(async ({ name, email, password }) => {
    setAuthBusy(true);
    try {
      const normalizedEmail = email.trim().toLowerCase();
      await api.post('/users', { name: name.trim(), email: normalizedEmail, password }, { authHeader: null });
      const authHeader = createBasicAuth(normalizedEmail, password);
      const discovered = await discoverAccount(normalizedEmail, authHeader);
      persist(discovered);
      setSession(discovered);
      return discovered;
    } finally {
      setAuthBusy(false);
    }
  }, []);

  const logout = useCallback(() => {
    persist(null);
    setSession(null);
  }, []);

  const updateSession = useCallback((updates) => {
    setSession((current) => {
      if (!current) return current;
      const next = { ...current, ...updates };
      persist(next);
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ session, login, register, logout, authBusy, updateSession }),
    [session, login, register, logout, authBusy, updateSession],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
