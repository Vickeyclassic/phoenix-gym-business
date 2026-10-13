import { useState } from "react";
import { api, message } from "../api";
import { Plus, Edit3 } from "lucide-react";
import { validatePlan } from "../logic";
import { useLoad } from "../hooks/useLoad.js";
import {
  Field,
  Modal,
  Notice,
  Panel,
  State,
  Table,
} from "../components/ui.jsx";
export function Plans({ user, notify }) {
  const x = useLoad("/plans"),
    [editing, setEditing] = useState(null);
  async function toggle(p) {
    try {
      await api.put(`/plans/${p.id}`, {
        name: p.name,
        durationMonths: p.durationMonths,
        price: p.price,
        description: p.description,
        active: !p.active,
      });
      notify(`Plan ${p.active ? "deactivated" : "activated"}`);
      x.load();
    } catch (e) {
      notify(message(e));
    }
  }
  return (
    <State {...x}>
      <Panel
        title="Configurable membership plans"
        action={
          user.role !== "STAFF" && (
            <button
              className="primary"
              onClick={() =>
                setEditing({
                  name: "",
                  durationMonths: 1,
                  price: "",
                  description: "",
                  active: true,
                })
              }
            >
              <Plus /> Add plan
            </button>
          )
        }
      >
        <Table
          rows={x.data || []}
          cols={[
            ["name", "Plan"],
            ["durationMonths", "Months"],
            ["price", "Price"],
            ["description", "Description"],
            ["active", "Active"],
          ]}
          actions={
            user.role !== "STAFF" &&
            ((p) => (
              <>
                <button onClick={() => setEditing(p)}>
                  <Edit3 /> Edit
                </button>
                <button onClick={() => toggle(p)}>
                  {p.active ? "Deactivate" : "Activate"}
                </button>
              </>
            ))
          }
        />
        {editing && (
          <PlanForm
            initial={editing}
            onClose={() => setEditing(null)}
            onSaved={() => {
              setEditing(null);
              notify("Plan saved");
              x.load();
            }}
          />
        )}
      </Panel>
    </State>
  );
}
export function PlanForm({ initial, onClose, onSaved }) {
  const [f, setF] = useState(initial),
    [err, setErr] = useState("");
  async function save(e) {
    e.preventDefault();
    const invalid = validatePlan(f);
    if (invalid) return setErr(invalid);
    try {
      const body = {
        ...f,
        durationMonths: Number(f.durationMonths),
        price: Number(f.price),
      };
      initial.id
        ? await api.put(`/plans/${initial.id}`, body)
        : await api.post("/plans", body);
      onSaved();
    } catch (e) {
      setErr(message(e));
    }
  }
  return (
    <Modal title={initial.id ? "Edit plan" : "Add plan"} onClose={onClose}>
      <form className="form-grid" onSubmit={save}>
        {err && <Notice error text={err} />}
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
          label="Duration months *"
          type="number"
          min="1"
          value={f.durationMonths}
          onChange={(v) =>
            setF({
              ...f,
              durationMonths: v,
            })
          }
        />
        <Field
          label="Price *"
          type="number"
          min="0"
          step="0.01"
          value={f.price}
          onChange={(v) =>
            setF({
              ...f,
              price: v,
            })
          }
        />
        <Field
          label="Description"
          value={f.description}
          onChange={(v) =>
            setF({
              ...f,
              description: v,
            })
          }
        />
        <div className="form-actions">
          <button type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="primary">Save plan</button>
        </div>
      </form>
    </Modal>
  );
}
