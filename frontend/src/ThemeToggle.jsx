import {useEffect, useState} from 'react';
import {Moon, Sun} from 'lucide-react';

export function ThemeToggle() {
  const [theme, setTheme] = useState(() => document.documentElement.dataset.theme || 'light');
  useEffect(() => {
    const system = window.matchMedia('(prefers-color-scheme: dark)');
    const sync = () => {
      let saved;
      try { saved = localStorage.getItem('phoenix-theme'); } catch { /* Use system preference. */ }
      const next = saved === 'light' ? 'light' : 'dark';
      document.documentElement.dataset.theme = next;
      document.documentElement.style.colorScheme = next;
      setTheme(next);
    };
    const storage = event => { if (event.key === 'phoenix-theme' || event.key === null) sync(); };
    system.addEventListener('change', sync);
    window.addEventListener('storage', storage);
    return () => {
      system.removeEventListener('change', sync);
      window.removeEventListener('storage', storage);
    };
  }, []);
  function toggle() {
    const next = theme === 'dark' ? 'light' : 'dark';
    try { localStorage.setItem('phoenix-theme', next); } catch { /* Still switch for this page. */ }
    document.documentElement.dataset.theme = next;
    document.documentElement.style.colorScheme = next;
    setTheme(next);
  }
  return <button type="button" className="theme-toggle" aria-label="Dark mode" aria-pressed={theme === 'dark'} title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`} onClick={toggle}>
    {theme === 'dark' ? <Sun aria-hidden="true"/> : <Moon aria-hidden="true"/>}
    <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>
  </button>;
}
