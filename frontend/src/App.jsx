import { useAuth } from './auth/AuthContext';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

export default function App() {
  const { session } = useAuth();
  return session?.authHeader ? <DashboardPage /> : <LoginPage />;
}
