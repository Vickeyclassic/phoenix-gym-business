import { useState } from "react";
import { validatePasswordChange } from "../password";
import { api, message } from "../api";
import { Brand } from "../context/Branding.jsx";
import { Field, Notice } from "../components/ui.jsx";
export function ChangePassword({ onChanged, onLogout, forced = false }) {
  const [current, setCurrent] = useState(""),
    [next, setNext] = useState(""),
    [confirmation, setConfirmation] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function save(e) {
    e.preventDefault();
    const invalid = validatePasswordChange(current, next, confirmation);
    if (invalid) return setError(invalid);
    setBusy(true);
    setError("");
    try {
      await api.post("/auth/change-password", {
        currentPassword: current,
        newPassword: next,
      });
      const { data } = await api.get("/auth/me");
      sessionStorage.setItem("phoenixUser", JSON.stringify(data));
      setCurrent("");
      setNext("");
      setConfirmation("");
      onChanged(data);
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form className="login-card password-card" onSubmit={save}>
      <div className="brand">
        <Brand />
      </div>
      <h1>Change password</h1>
      <p>
        {forced
          ? "Replace your temporary password to continue."
          : "Choose a new password for your account."}
      </p>
      {error && <Notice error text={error} />}
      <Field
        label="Current password"
        type="password"
        autoComplete="current-password"
        value={current}
        onChange={setCurrent}
      />
      <Field
        label="New password"
        type="password"
        autoComplete="new-password"
        value={next}
        onChange={setNext}
      />
      <Field
        label="Confirm new password"
        type="password"
        autoComplete="new-password"
        value={confirmation}
        onChange={setConfirmation}
      />
      <button className="primary" disabled={busy}>
        {busy ? "Saving…" : "Change password"}
      </button>
      {forced && (
        <button type="button" onClick={onLogout}>
          Log out
        </button>
      )}
    </form>
  );
}
