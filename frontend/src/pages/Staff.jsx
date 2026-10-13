import { useState } from "react";
import { api, message } from "../api";
import { Plus, Edit3 } from "lucide-react";
import { formatIndiaDateTime } from "../logic";
import { useLoad } from "../hooks/useLoad.js";
import {
  Field,
  Modal,
  Notice,
  Panel,
  Select,
  State,
  Table,
} from "../components/ui.jsx";
export function Staff({ notify, onOwnerChanged }) {
  const x = useLoad("/users"),
    [editing, setEditing] = useState(null);
  async function toggle(u) {
    if (u.role === "OWNER") return;
    try {
      await api.put(`/users/${u.id}`, {
        active: !u.active,
      });
      notify(`User ${u.active ? "deactivated" : "activated"}`);
      x.load();
    } catch (e) {
      notify(message(e), "error");
    }
  }
  async function reset(u) {
    const password = prompt(
      `Enter a temporary strong password for ${u.username}`,
    );
    if (!password) return;
    try {
      await api.post(`/users/${u.id}/reset-password`, {
        password,
      });
      notify("Password reset; change required at next login");
      x.load();
    } catch (e) {
      notify(message(e), "error");
    }
  }
  function saved(account, usernameChanged) {
    setEditing(null);
    if (account.role === "OWNER") onOwnerChanged(account, usernameChanged);
    else {
      notify("Staff account saved");
      x.load();
    }
  }
  return (
    <State {...x}>
      <Panel
        title="Staff accounts"
        action={
          <button
            className="primary"
            onClick={() =>
              setEditing({
                username: "",
                displayName: "",
                role: "STAFF",
                password: "",
                active: true,
              })
            }
          >
            <Plus /> Add staff
          </button>
        }
      >
        <Table
          rows={x.data || []}
          cols={[
            ["username", "Username"],
            ["displayName", "Name"],
            ["role", "Role"],
            ["active", "Active"],
            [
              (u) => formatIndiaDateTime(u.lastLoginAt),
              "Last login (India time)",
            ],
            [(u) => formatIndiaDateTime(u.createdAt), "Created (India time)"],
          ]}
          actions={(u) => (
            <>
              <button onClick={() => setEditing(u)}>
                <Edit3 /> Edit
              </button>
              {u.role !== "OWNER" && (
                <>
                  <button onClick={() => toggle(u)}>
                    {u.active ? "Deactivate" : "Activate"}
                  </button>
                  <button onClick={() => reset(u)}>Reset password</button>
                </>
              )}
            </>
          )}
        />
        {editing && (
          <StaffForm
            initial={editing}
            onClose={() => setEditing(null)}
            onSaved={saved}
          />
        )}
      </Panel>
    </State>
  );
}
export function StaffForm({ initial, onClose, onSaved }) {
  const [f, setF] = useState(initial),
    [err, setErr] = useState(""),
    [busy, setBusy] = useState(false),
    isOwner = initial.role === "OWNER";
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const payload = initial.id
        ? isOwner
          ? {
              username: f.username,
              displayName: f.displayName,
            }
          : {
              displayName: f.displayName,
              role: f.role,
              active: f.active,
            }
        : f;
      const { data } = initial.id
        ? await api.put(`/users/${initial.id}`, payload)
        : await api.post("/users", payload);
      onSaved(data, isOwner && initial.username !== data.username);
    } catch (e) {
      setErr(message(e));
      setBusy(false);
    }
  }
  return (
    <Modal
      title={
        isOwner ? "Edit primary owner" : initial.id ? "Edit staff" : "Add staff"
      }
      onClose={onClose}
    >
      <form className="form-grid" onSubmit={save}>
        {err && <Notice error text={err} />}
        <Field
          label="Username *"
          value={f.username}
          disabled={!!initial.id && !isOwner}
          onChange={(v) =>
            setF({
              ...f,
              username: v,
            })
          }
        />
        <Field
          label="Display name *"
          value={f.displayName}
          onChange={(v) =>
            setF({
              ...f,
              displayName: v,
            })
          }
        />
        <Select
          label="Role"
          value={f.role}
          disabled={isOwner}
          onChange={(v) =>
            setF({
              ...f,
              role: v,
            })
          }
          options={(isOwner ? ["OWNER"] : ["ADMIN", "STAFF"]).map((v) => [
            v,
            v,
          ])}
        />
        {isOwner && (
          <p className="form-note">
            Changing the username signs you out so the existing session cannot
            continue under an obsolete identity.
          </p>
        )}
        {!initial.id && (
          <Field
            label="Temporary password *"
            type="password"
            value={f.password}
            onChange={(v) =>
              setF({
                ...f,
                password: v,
              })
            }
          />
        )}
        <div className="form-actions">
          <button type="button" disabled={busy} onClick={onClose}>
            Cancel
          </button>
          <button className="primary" disabled={busy}>
            {busy ? "Saving…" : "Save account"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
