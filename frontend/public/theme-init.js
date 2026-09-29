// Runs before styles and React so the first paint uses the chosen theme.
(() => {
  let preference;
  try { preference = localStorage.getItem('phoenix-theme'); } catch { /* Storage may be unavailable. */ }
  const dark = preference !== 'light';
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
})();
