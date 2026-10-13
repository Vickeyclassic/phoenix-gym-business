import { useState } from "react";
import { formatIndiaDateTime } from "../logic";
import { useLoad } from "../hooks/useLoad.js";
import { Pagination, Panel, State, Table } from "../components/ui.jsx";
import { pageUrl } from "../utils/navigation.js";
export function Audit() {
  const [user, setUser] = useState(""),
    [action, setAction] = useState(""),
    [entity, setEntity] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [path, setPath] = useState("/audit-logs?size=50"),
    x = useLoad(path, {
      content: [],
    });
  function filter(e) {
    e.preventDefault();
    const q = new URLSearchParams({
      size: "50",
    });
    if (user) q.set("user", user);
    if (action) q.set("action", action);
    if (entity) q.set("entityType", entity);
    if (from) q.set("from", from);
    if (to) q.set("to", to);
    setPath(`/audit-logs?${q}`);
  }
  return (
    <>
      <form className="filters" onSubmit={filter}>
        <input
          placeholder="Acting user"
          value={user}
          onChange={(e) => setUser(e.target.value)}
        />
        <input
          placeholder="Exact action"
          value={action}
          onChange={(e) => setAction(e.target.value)}
        />
        <input
          placeholder="Entity type"
          value={entity}
          onChange={(e) => setEntity(e.target.value)}
        />
        <input
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
        />
        <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        <button>Filter</button>
      </form>
      <State {...x}>
        <Panel title="Security and business activity">
          <Table
            rows={x.data.content || []}
            cols={[
              [
                (row) => formatIndiaDateTime(row.createdAt),
                "Time (India time)",
              ],
              ["actorUsername", "User"],
              ["action", "Action"],
              ["entityType", "Entity"],
              ["summary", "Summary"],
            ]}
          />
          <Pagination data={x.data} onPage={(p) => setPath(pageUrl(path, p))} />
        </Panel>
      </State>
    </>
  );
}
