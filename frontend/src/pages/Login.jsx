import { useState } from "react";
import { api, message } from "../api";
import { ThemeToggle } from "../ThemeToggle";
import { Brand } from "../context/Branding.jsx";
import { Field, Notice } from "../components/ui.jsx";
export function Login({ onLogin }) {
  const [f, setF] = useState({
      username: "",
      password: "",
    }),
    [busy, setBusy] = useState(false),
    [err, setErr] = useState("");
  async function go(e) {
    e.preventDefault();
    if (!f.username || !f.password)
      return setErr("Username and password are required.");
    setBusy(true);
    setErr("");
    try {
      const { data } = await api.post("/auth/login", f);
      sessionStorage.setItem("phoenixToken", data.accessToken);
      sessionStorage.setItem("phoenixUser", JSON.stringify(data.user));
      onLogin(data.user);
    } catch (x) {
      setErr(message(x));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login">
      <div className="login-theme">
        <ThemeToggle />
      </div>
      <form className="login-card" onSubmit={go}>
        <div className="brand">
          <Brand />
        </div>
        <h1>Welcome back</h1>
        <p>Sign in to manage gym operations.</p>
        {err && <Notice error text={err} />}
        <Field
          label="Username"
          value={f.username}
          autoFocus
          onChange={(v) =>
            setF({
              ...f,
              username: v,
            })
          }
        />
        <Field
          label="Password"
          type="password"
          value={f.password}
          onChange={(v) =>
            setF({
              ...f,
              password: v,
            })
          }
        />
        <button className="primary" disabled={busy}>
          {busy ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </main>
  );
}
