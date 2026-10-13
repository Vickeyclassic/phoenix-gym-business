import { useState } from "react";
import { api, message } from "../api";
import { formatIndiaDateTime } from "../logic";
import { Download } from "lucide-react";
import { useLoad } from "../hooks/useLoad.js";
import { downloadBlob } from "../utils/download.js";
import {
  FilterDates,
  Pagination,
  Panel,
  State,
  Table,
} from "../components/ui.jsx";
import { pageUrl } from "../utils/navigation.js";
export function Payments({ notify }) {
  const [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [method, setMethod] = useState(""),
    [path, setPath] = useState("/payments?size=50"),
    x = useLoad(path, {
      content: [],
    });
  function filter(e) {
    e.preventDefault();
    setPath(
      `/payments?size=50${from ? `&from=${from}` : ""}${to ? `&to=${to}` : ""}${method ? `&method=${method}` : ""}`,
    );
  }
  async function receipt(p) {
    try {
      const r = await api.get(`/payments/${p.id}/receipt.pdf`, {
        responseType: "blob",
      });
      downloadBlob(r.data, `receipt-${p.receiptNumber}.pdf`);
    } catch (e) {
      notify(message(e));
    }
  }
  return (
    <>
      <FilterDates
        from={from}
        to={to}
        setFrom={setFrom}
        setTo={setTo}
        onSubmit={filter}
      >
        <select value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="">All methods</option>
          {["CASH", "UPI", "CARD", "BANK_TRANSFER", "OTHER"].map((v) => (
            <option key={v}>{v}</option>
          ))}
        </select>
      </FilterDates>
      <State {...x}>
        <Panel title="Payment ledger">
          <Table
            rows={x.data.content || []}
            cols={[
              ["receiptNumber", "Receipt"],
              ["planNameSnapshot", "Plan"],
              [
                (row) => formatIndiaDateTime(row.paymentDate),
                "Date (India time)",
              ],
              ["amount", "Amount"],
              ["method", "Method"],
              ["status", "Status"],
            ]}
            actions={(p) => (
              <button onClick={() => receipt(p)}>
                <Download /> Receipt
              </button>
            )}
          />
          <Pagination data={x.data} onPage={(p) => setPath(pageUrl(path, p))} />
        </Panel>
      </State>
    </>
  );
}
