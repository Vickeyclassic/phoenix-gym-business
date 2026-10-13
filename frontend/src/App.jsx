import { useState, useEffect } from "react";
import { api } from "./api";
import { ThemeToggle } from "./ThemeToggle";
import { BrandingContext } from "./context/Branding.jsx";
import { ChangePassword } from "./pages/ChangePassword.jsx";
import { Shell } from "./components/Shell.jsx";
import { Login } from "./pages/Login.jsx";
export function App() {
  const [branding, setBranding] = useState({
    gymName: "Phoenix Business",
    logoUrl: null,
  });
  useEffect(() => {
    let active = true;
    const refresh = () =>
      api
        .get("/public/branding")
        .then(({ data }) => {
          if (active) setBranding(data);
        })
        .catch(() => {});
    refresh();
    addEventListener("business-branding", refresh);
    return () => {
      active = false;
      removeEventListener("business-branding", refresh);
    };
  }, []);
  useEffect(() => {
    document.title = branding.gymName || "Phoenix Business";
  }, [branding.gymName]);
  const [verified, setVerified] = useState(false);
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(sessionStorage.getItem("phoenixUser"));
    } catch {
      return null;
    }
  });
  useEffect(() => {
    const h = () => {
        setUser(null);
        setVerified(true);
      },
      required = () =>
        setUser((u) =>
          u
            ? {
                ...u,
                forcePasswordChange: true,
              }
            : u,
        );
    addEventListener("phoenix-auth", h);
    addEventListener("phoenix-password-required", required);
    if (sessionStorage.getItem("phoenixToken"))
      api
        .get("/auth/me")
        .then(({ data }) => {
          setUser(data);
          sessionStorage.setItem("phoenixUser", JSON.stringify(data));
        })
        .catch(() => setUser(null))
        .finally(() => setVerified(true));
    else setVerified(true);
    return () => {
      removeEventListener("phoenix-auth", h);
      removeEventListener("phoenix-password-required", required);
    };
  }, []);
  async function logout() {
    try {
      await api.post("/auth/logout");
    } finally {
      sessionStorage.clear();
      setUser(null);
    }
  }
  return (
    <BrandingContext.Provider value={branding}>
      {!verified ? (
        <div className="empty">Checking session…</div>
      ) : user?.forcePasswordChange ? (
        <main className="login">
          <div className="login-theme">
            <ThemeToggle />
          </div>
          <ChangePassword forced onChanged={setUser} onLogout={logout} />
        </main>
      ) : user ? (
        <Shell
          user={user}
          onLogout={() => setUser(null)}
          onPasswordChanged={(u) => {
            setUser(u);
            window.alert("Password changed");
          }}
          onOwnerChanged={(account, usernameChanged) => {
            if (usernameChanged) {
              sessionStorage.clear();
              window.alert(
                "Owner username updated. Sign in again with the new username.",
              );
              setUser(null);
            } else {
              const next = {
                ...user,
                displayName: account.displayName,
              };
              sessionStorage.setItem("phoenixUser", JSON.stringify(next));
              setUser(next);
            }
          }}
        />
      ) : (
        <Login onLogin={setUser} />
      )}
    </BrandingContext.Provider>
  );
}
