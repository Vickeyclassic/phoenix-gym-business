import { useState } from "react";
import { api, message } from "../api";
import { Plus, Edit3, RefreshCw, Trash2 } from "lucide-react";
import { membershipLabel, formatIndiaDateTime, validateMember } from "../logic";
import { validateLogoFile } from "../branding";
import { today } from "../utils/dates.js";
import { useLoad } from "../hooks/useLoad.js";
import { pageUrl } from "../utils/navigation.js";
import {
  Field,
  Modal,
  Notice,
  Pagination,
  Panel,
  Select,
  State,
  Table,
} from "../components/ui.jsx";
import { downloadBlob } from "../utils/download.js";
import { LogoCropper } from "./BusinessSettings.jsx";
export const blankMember = {
  memberCode: "",
  name: "",
  phone: "",
  email: "",
  gender: "",
  dateOfBirth: "",
  address: "",
  emergencyContact: "",
  joinDate: today(),
  planId: "",
  membershipStartDate: "",
  expiryDate: "",
  status: "ACTIVE",
  notes: "",
  photoData: "",
};
export function Members({ user, notify }) {
  const settings = useLoad("/settings", {});
  const [archived, setArchived] = useState(false),
    [removing, setRemoving] = useState(null),
    [removalBusy, setRemovalBusy] = useState(false),
    [removalError, setRemovalError] = useState("");
  const [profile, setProfile] = useState(null);
  const [q, setQ] = useState(""),
    [status, setStatus] = useState(""),
    [path, setPath] = useState("/members?size=50"),
    x = useLoad(path, {
      content: [],
    }),
    plans = useLoad("/plans", []),
    [editing, setEditing] = useState(null),
    [renewing, setRenewing] = useState(null);
  function search(e) {
    e.preventDefault();
    setPath(
      `/members?size=50&archived=${archived}&q=${encodeURIComponent(q)}${status ? `&status=${status}` : ""}`,
    );
  }
  async function remove() {
    setRemovalBusy(true);
    setRemovalError("");
    try {
      const { data } = await api.delete("/members/" + removing.id);
      notify(
        data.outcome === "archived"
          ? "Member archived. History preserved."
          : "Member deleted",
      );
      setRemoving(null);
      setPath(pageUrl(path, 0));
      x.load();
    } catch (e) {
      setRemovalError(message(e));
    } finally {
      setRemovalBusy(false);
    }
  }
  async function restore(m) {
    try {
      await api.post("/members/" + m.id + "/restore");
      notify("Member restored");
      setPath(pageUrl(path, 0));
      x.load();
    } catch (e) {
      notify(message(e));
    }
  }
  return (
    <>
      <form className="filters" onSubmit={search}>
        <select
          aria-label="Member directory view"
          value={archived ? "archived" : "current"}
          onChange={(e) => {
            const value = e.target.value === "archived";
            setArchived(value);
            setStatus("");
            setPath(
              `/members?size=50&archived=${value}&q=${encodeURIComponent(q)}`,
            );
          }}
        >
          <option value="current">Current members</option>
          <option value="archived">Archived members</option>
        </select>
        <input
          placeholder="Search member"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select
          aria-label="Member status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">All statuses</option>
          <option>ACTIVE</option>
          <option>EXPIRED</option>
          <option>INACTIVE</option>
        </select>
        <select
          aria-label="Sort members"
          onChange={(e) => {
            const u = new URLSearchParams(path.split("?")[1]);
            u.set("sort", e.target.value);
            u.set("page", "0");
            setPath(`/members?${u}`);
          }}
        >
          <option value="name,asc">Name A–Z</option>
          <option value="name,desc">Name Z–A</option>
          <option value="expiryDate,asc">Expiry date</option>
        </select>
        <button>Search</button>
      </form>
      <State {...x}>
        <Panel
          title={archived ? "Archived members" : "Member directory"}
          action={
            !archived && (
              <button
                className="primary"
                onClick={() => setEditing(blankMember)}
              >
                <Plus /> Add member
              </button>
            )
          }
        >
          <Table
            rows={x.data.content || []}
            cols={[
              ["memberCode", "Member ID"],
              [
                (m) => (
                  <span className="member-identity">
                    <MemberAvatar member={m} />
                    {m.name}
                  </span>
                ),
                "Name",
              ],
              ["phone", "Phone"],
              ["email", "Email"],
              [
                (m) => membershipLabel(m, settings.data.reminderDays ?? 7),
                "Status",
              ],
              ["expiryDate", "Expiry"],
            ]}
            actions={(m) => (
              <>
                <button onClick={() => setProfile(m.id)}>Profile</button>
                {!archived && (
                  <>
                    <button
                      title="Edit member"
                      onClick={() =>
                        setEditing({
                          ...m,
                          planId: m.plan?.id || "",
                        })
                      }
                    >
                      <Edit3 /> Edit
                    </button>
                    <button
                      title="Renew membership"
                      onClick={() => setRenewing(m)}
                    >
                      <RefreshCw /> Renew
                    </button>
                  </>
                )}
                {user.role !== "STAFF" &&
                  (archived ? (
                    <button onClick={() => restore(m)}>Restore</button>
                  ) : (
                    <button
                      className="danger"
                      title="Remove member"
                      aria-label={`Remove ${m.name}`}
                      onClick={() => {
                        setRemovalError("");
                        setRemoving(m);
                      }}
                    >
                      <Trash2 /> Remove
                    </button>
                  ))}
              </>
            )}
          />
          <Pagination data={x.data} onPage={(p) => setPath(pageUrl(path, p))} />
        </Panel>
      </State>
      {removing && (
        <Modal
          title={`Remove ${removing.name}?`}
          onClose={() => !removalBusy && setRemoving(null)}
        >
          <div className="profile-details">
            <p>
              This member will leave the current directory. If they have
              membership, payment or attendance history, they will be archived
              and can be restored later. Members without history will be
              permanently deleted.
            </p>
            {removalError && <Notice error text={removalError} />}
            <div className="form-actions">
              <button
                type="button"
                disabled={removalBusy}
                onClick={() => setRemoving(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="danger-action"
                disabled={removalBusy}
                onClick={remove}
              >
                {removalBusy ? "Removing…" : "Remove member"}
              </button>
            </div>
          </div>
        </Modal>
      )}
      {profile && (
        <MemberProfile
          id={profile}
          onClose={() => setProfile(null)}
          notify={notify}
        />
      )}
      {editing && (
        <MemberForm
          initial={editing}
          plans={plans.data}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            notify("Member saved");
            x.load();
          }}
        />
      )}
      {renewing && (
        <RenewForm
          member={renewing}
          plans={plans.data}
          onClose={() => setRenewing(null)}
          onSaved={() => {
            setRenewing(null);
            notify("Membership renewed and payment recorded");
            x.load();
          }}
        />
      )}
    </>
  );
}
export function MemberAvatar({ member, large = false }) {
  const [failed, setFailed] = useState("");
  const src = member?.photoData;
  return (
    <span className={large ? "member-avatar large" : "member-avatar"}>
      {src && failed !== src ? (
        <img
          src={src}
          alt={member?.name || "Member"}
          onError={() => setFailed(src)}
        />
      ) : (
        <span aria-label={member?.name || "Member"}>
          {(member?.name || "?")
            .trim()
            .split(/\s+/)
            .slice(0, 2)
            .map((word) => word[0])
            .join("")
            .toUpperCase()}
        </span>
      )}
    </span>
  );
}
export function MemberProfile({ id, onClose, notify }) {
  const settings = useLoad("/settings", {});
  const x = useLoad("/members/" + id + "/profile", {});
  async function receipt(p) {
    try {
      const r = await api.get("/payments/" + p.id + "/receipt.pdf", {
        responseType: "blob",
      });
      downloadBlob(r.data, "receipt-" + p.receiptNumber + ".pdf");
    } catch (e) {
      notify(message(e));
    }
  }
  return (
    <Modal title="Member profile" onClose={onClose}>
      <State {...x}>
        <div className="profile-details">
          <MemberAvatar member={x.data.member} large />
          <h2>{x.data.member?.name}</h2>
          <p>
            {x.data.member?.memberCode} ·{" "}
            {x.data.member &&
              membershipLabel(x.data.member, settings.data.reminderDays ?? 7)}
          </p>
          <p>
            Membership: {x.data.member?.membershipStartDate || "—"} to{" "}
            {x.data.member?.expiryDate || "—"}
          </p>
          <p>
            {x.data.member?.phone} {x.data.member?.email}
          </p>
        </div>
        <Panel title="Payment history">
          <Table
            rows={x.data.payments || []}
            cols={[
              ["receiptNumber", "Receipt"],
              [(row) => formatIndiaDateTime(row.paymentDate), "Date (IST)"],
              ["amount", "Amount"],
            ]}
            actions={(p) => <button onClick={() => receipt(p)}>Receipt</button>}
          />
        </Panel>
        <Panel title="Recent attendance">
          <Table
            rows={x.data.recentAttendance || []}
            cols={[
              [
                (row) => formatIndiaDateTime(row.checkedInAt),
                "Check-in (India time)",
              ],
              [
                (row) => formatIndiaDateTime(row.checkedOutAt),
                "Check-out (India time)",
              ],
              ["source", "Source"],
            ]}
          />
        </Panel>
      </State>
    </Modal>
  );
}
export function MemberForm({ initial, plans, onClose, onSaved }) {
  const [cropFile, setCropFile] = useState(null);
  const [f, setF] = useState(initial),
    [busy, setBusy] = useState(false),
    [err, setErr] = useState("");
  async function save(e) {
    e.preventDefault();
    const invalid = validateMember(f);
    if (invalid) return setErr(invalid);
    setBusy(true);
    try {
      const payload = {
        ...f,
        planId: f.planId ? Number(f.planId) : null,
        dateOfBirth: f.dateOfBirth || null,
        membershipStartDate: f.membershipStartDate || null,
        expiryDate: f.expiryDate || null,
      };
      initial.id
        ? await api.put(`/members/${initial.id}`, payload)
        : await api.post("/members", payload);
      onSaved();
    } catch (e) {
      setErr(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title={initial.id ? "Edit member" : "Add member"} onClose={onClose}>
      <form className="form-grid" onSubmit={save}>
        {err && <Notice error text={err} />}
        <div className="wide member-photo-editor">
          <MemberAvatar member={f} large />
          <label className="file-button">
            Choose and crop photo
            <input
              type="file"
              accept="image/png,image/jpeg"
              disabled={busy}
              onChange={(e) => {
                const file = e.target.files?.[0];
                e.target.value = "";
                if (!file) return;
                const error = validateLogoFile(file);
                setErr(error);
                if (!error) setCropFile(file);
              }}
            />
          </label>
          {f.photoData && (
            <button
              type="button"
              disabled={busy}
              onClick={() =>
                setF({
                  ...f,
                  photoData: "",
                })
              }
            >
              Remove photo
            </button>
          )}
          <small>PNG or JPEG, up to 2 MB. Crop before saving.</small>
        </div>
        <Field
          label="Member ID *"
          value={f.memberCode}
          onChange={(v) =>
            setF({
              ...f,
              memberCode: v,
            })
          }
        />
        <Field
          label="Name *"
          value={f.name}
          onChange={(v) =>
            setF({
              ...f,
              name: v,
            })
          }
        />
        <Field
          label="Phone"
          value={f.phone}
          onChange={(v) =>
            setF({
              ...f,
              phone: v,
            })
          }
        />
        <Field
          label="Email"
          type="email"
          value={f.email}
          onChange={(v) =>
            setF({
              ...f,
              email: v,
            })
          }
        />
        <Select
          label="Gender"
          value={f.gender}
          onChange={(v) =>
            setF({
              ...f,
              gender: v,
            })
          }
          options={[
            ["", "Not specified"],
            ["Female", "Female"],
            ["Male", "Male"],
            ["Other", "Other"],
          ]}
        />
        <Field
          label="Date of birth"
          type="date"
          value={f.dateOfBirth}
          onChange={(v) =>
            setF({
              ...f,
              dateOfBirth: v,
            })
          }
        />
        <Field
          label="Join date *"
          type="date"
          value={f.joinDate}
          onChange={(v) =>
            setF({
              ...f,
              joinDate: v,
            })
          }
        />
        <Select
          label="Plan"
          value={f.planId}
          onChange={(v) =>
            setF({
              ...f,
              planId: v,
            })
          }
          options={[["", "No plan"], ...plans.map((p) => [p.id, p.name])]}
        />
        <Select
          label="Status"
          value={f.status}
          onChange={(v) =>
            setF({
              ...f,
              status: v,
            })
          }
          options={["ACTIVE", "EXPIRED", "INACTIVE"].map((v) => [v, v])}
        />
        <Field
          label="Membership start"
          type="date"
          value={f.membershipStartDate}
          onChange={(v) =>
            setF({
              ...f,
              membershipStartDate: v,
            })
          }
        />
        <Field
          label="Membership expiry"
          type="date"
          value={f.expiryDate}
          onChange={(v) =>
            setF({
              ...f,
              expiryDate: v,
            })
          }
        />
        <Field
          label="Emergency contact"
          value={f.emergencyContact}
          onChange={(v) =>
            setF({
              ...f,
              emergencyContact: v,
            })
          }
        />
        <label className="wide">
          Address
          <textarea
            value={f.address || ""}
            onChange={(e) =>
              setF({
                ...f,
                address: e.target.value,
              })
            }
          />
        </label>
        <label className="wide">
          Notes
          <textarea
            value={f.notes || ""}
            onChange={(e) =>
              setF({
                ...f,
                notes: e.target.value,
              })
            }
          />
        </label>
        <div className="form-actions">
          <button type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="primary" disabled={busy}>
            {busy ? "Saving…" : "Save member"}
          </button>
        </div>
      </form>
      {cropFile && (
        <LogoCropper
          file={cropFile}
          subject="profile photo"
          onCancel={() => setCropFile(null)}
          onApply={(_file, dataUrl) => {
            setF({
              ...f,
              photoData: dataUrl,
            });
            setCropFile(null);
          }}
        />
      )}
    </Modal>
  );
}
export function RenewForm({ member, plans, onClose, onSaved }) {
  const [f, setF] = useState({
      memberId: member.id,
      planId: member.plan?.id || "",
      method: "CASH",
      referenceNote: "",
      amount: "",
    }),
    [err, setErr] = useState("");
  async function save(e) {
    e.preventDefault();
    if (!f.planId) return setErr("Choose an active plan.");
    try {
      await api.post("/payments/renew", {
        ...f,
        planId: Number(f.planId),
        amount: f.amount === "" ? null : Number(f.amount),
      });
      onSaved();
    } catch (e) {
      setErr(message(e));
    }
  }
  return (
    <Modal title={`Renew ${member.name}`} onClose={onClose}>
      <form className="form-grid" onSubmit={save}>
        {err && <Notice error text={err} />}
        <Select
          label="Plan"
          value={f.planId}
          onChange={(v) =>
            setF({
              ...f,
              planId: v,
            })
          }
          options={[
            ["", "Choose plan"],
            ...plans
              .filter((p) => p.active)
              .map((p) => [p.id, `${p.name} — ${p.price}`]),
          ]}
        />
        <Select
          label="Method"
          value={f.method}
          onChange={(v) =>
            setF({
              ...f,
              method: v,
            })
          }
          options={["CASH", "UPI", "CARD", "BANK_TRANSFER", "OTHER"].map(
            (v) => [v, v.replace("_", " ")],
          )}
        />
        <Field
          label="Amount override (optional)"
          type="number"
          min="0"
          step="0.01"
          value={f.amount}
          onChange={(v) =>
            setF({
              ...f,
              amount: v,
            })
          }
        />
        <Field
          label="Reference / note"
          value={f.referenceNote}
          onChange={(v) =>
            setF({
              ...f,
              referenceNote: v,
            })
          }
        />
        <div className="form-actions">
          <button type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="primary">Renew and record payment</button>
        </div>
      </form>
    </Modal>
  );
}
