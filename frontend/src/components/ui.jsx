import { RefreshCw, X } from "lucide-react";
export const Notice = ({ text, error = false }) => (
  <div className={error ? "alert" : "notice"}>{text}</div>
);
export const State = ({ loading, error, children }) =>
  loading ? (
    <div className="empty">
      <RefreshCw className="spin" /> Loading…
    </div>
  ) : error ? (
    <Notice error text={error} />
  ) : (
    children
  );
export function Field({ label, type = "text", value, onChange, ...rest }) {
  return (
    <label>
      {label}
      <input
        type={type}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value)}
        {...rest}
      />
    </label>
  );
}
export function Select({ label, value, onChange, options }) {
  return (
    <label>
      {label}
      <select value={value ?? ""} onChange={(e) => onChange(e.target.value)}>
        {options.map(([v, l]) => (
          <option key={v} value={v}>
            {l}
          </option>
        ))}
      </select>
    </label>
  );
}
export function Modal({ title, onClose, children }) {
  return (
    <div
      className="modal-bg"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <section className="modal">
        <div className="panel-head">
          <h2>{title}</h2>
          <button className="icon" onClick={onClose}>
            <X />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}
export function Pagination({ data, onPage }) {
  return (
    <div className="pagination">
      <button disabled={!data.number} onClick={() => onPage(data.number - 1)}>
        Previous
      </button>
      <span>
        Page {(data.number || 0) + 1} of {Math.max(1, data.totalPages || 0)} ·{" "}
        {data.totalElements || 0} records
      </span>
      <button
        disabled={data.last !== false}
        onClick={() => onPage((data.number || 0) + 1)}
      >
        Next
      </button>
    </div>
  );
}
export function FilterDates({ from, to, setFrom, setTo, onSubmit, children }) {
  return (
    <form className="filters" onSubmit={onSubmit}>
      <input
        type="date"
        value={from}
        onChange={(e) => setFrom(e.target.value)}
      />
      <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
      {children}
      <button>Apply filters</button>
    </form>
  );
}
export function Panel({ title, action, children }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
export function Table({ rows, cols, actions }) {
  if (!rows.length) return <div className="empty">No records found.</div>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {cols.map(([, h]) => (
              <th key={h}>{h}</th>
            ))}
            {actions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id ?? i}>
              {cols.map(([k]) => (
                <td key={String(k)}>
                  {typeof k === "function"
                    ? k(r)
                    : r[k] === true
                      ? "Yes"
                      : r[k] === false
                        ? "No"
                        : (r[k] ?? "—")}
                </td>
              ))}
              {actions && <td className="actions">{actions(r)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
