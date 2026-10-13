import { useState } from "react";
import { api, message } from "../api";
import { Download } from "lucide-react";
import { downloadBlob } from "../utils/download.js";
import { FilterDates } from "../components/ui.jsx";
export function Reports({ notify }) {
  const [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [method, setMethod] = useState("");
  async function dl(kind) {
    try {
      const q = new URLSearchParams();
      if (from) q.set("from", from);
      if (to) q.set("to", to);
      if (method && ["payments", "revenue"].includes(kind))
        q.set("method", method);
      const r = await api.get(`/reports/${kind}.xlsx?${q}`, {
        responseType: "blob",
      });
      downloadBlob(r.data, `${kind}.xlsx`);
    } catch (e) {
      notify(message(e));
    }
  }
  const items = [
    ["members", "Members"],
    ["payments", "Payments"],
    ["attendance", "Attendance"],
    ["expiring", "Expiring memberships"],
    ["revenue", "Revenue summary"],
  ];
  return (
    <>
      <FilterDates
        from={from}
        to={to}
        setFrom={setFrom}
        setTo={setTo}
        onSubmit={(e) => e.preventDefault()}
      >
        <select value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="">All payment methods</option>
          {["CASH", "UPI", "CARD", "BANK_TRANSFER", "OTHER"].map((v) => (
            <option key={v}>{v}</option>
          ))}
        </select>
      </FilterDates>
      <div className="report-grid">
        {items.map(([k, l]) => (
          <article key={k}>
            <Download />
            <h2>{l}</h2>
            <p>
              Business-branded Excel workbook with filters, readable headings
              and generated timestamp.
            </p>
            <button className="primary" onClick={() => dl(k)}>
              Download .xlsx
            </button>
          </article>
        ))}
      </div>
    </>
  );
}
