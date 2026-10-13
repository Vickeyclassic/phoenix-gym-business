import { useState, useRef } from "react";
import { api, message } from "../api";
import { queryString, formatIndiaDateTime } from "../logic";
import { useLoad } from "../hooks/useLoad.js";
import { today } from "../utils/dates.js";
import { MemberAvatar } from "./Members.jsx";
import { Pagination, Panel, State, Table } from "../components/ui.jsx";
import { pageUrl } from "../utils/navigation.js";
export function Attendance({ notify }) {
  const [lookup, setLookup] = useState("");
  const [selected, setSelected] = useState(null);
  const [openState, setOpenState] = useState(null);
  const [openLoading, setOpenLoading] = useState(false);
  const [actionBusy, setActionBusy] = useState(false);
  const lookupPath = "/members?size=20&q=" + encodeURIComponent(lookup);
  const members = useLoad(lookupPath, {
    content: [],
  });
  const selectionRequest = useRef(0);
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [path, setPath] = useState(
    `/attendance?size=50&from=${today()}&to=${today()}`,
  );
  const history = useLoad(path, {
    content: [],
  });
  const matches =
    members.resolvedPath === lookupPath && !members.loading && !members.error
      ? members.data.content || []
      : [];
  async function selectMember(member) {
    const request = ++selectionRequest.current;
    setSelected(member);
    setLookup(`${member.memberCode} — ${member.name}`);
    setOpenState(null);
    setOpenLoading(true);
    try {
      const { data } = await api.get(`/attendance/open/${member.id}`);
      if (request === selectionRequest.current) setOpenState(data.open);
    } catch (error) {
      if (request === selectionRequest.current) notify(message(error), "error");
    } finally {
      if (request === selectionRequest.current) setOpenLoading(false);
    }
  }
  function changeLookup(value) {
    selectionRequest.current++;
    setLookup(value);
    if (selected) {
      setSelected(null);
      setOpenState(null);
    }
  }
  function applyFilters(event) {
    event.preventDefault();
    if (from && to && to < from)
      return notify("To date must be on or after from date", "error");
    setPath(
      `/attendance?${queryString({
        size: 50,
        from,
        to,
        memberId: selected?.id,
      })}`,
    );
  }
  function resetFilters() {
    const date = today();
    selectionRequest.current++;
    setLookup("");
    setSelected(null);
    setOpenState(null);
    setFrom(date);
    setTo(date);
    setPath(`/attendance?size=50&from=${date}&to=${date}`);
  }
  async function action(type) {
    if (!selected) return notify("Select an active member first", "error");
    setActionBusy(true);
    try {
      await api.post(`/attendance/${type}/${selected.id}`);
      setOpenState(type === "check-in");
      notify(
        type === "check-in"
          ? `${selected.name} checked in`
          : `${selected.name} checked out`,
      );
      await history.load();
    } catch (error) {
      notify(message(error), "error");
    } finally {
      setActionBusy(false);
    }
  }
  const canCheckIn =
    Boolean(selected) &&
    selected.status === "ACTIVE" &&
    selected.expiryDate >= today() &&
    openState === false &&
    !openLoading &&
    !actionBusy;
  const canCheckOut =
    Boolean(selected) && openState === true && !openLoading && !actionBusy;
  return (
    <>
      <section className="attendance-controls" aria-label="Attendance controls">
        <div className="member-picker">
          <label htmlFor="attendance-member-search">Find member</label>
          <input
            id="attendance-member-search"
            disabled={actionBusy}
            role="combobox"
            aria-expanded={Boolean(lookup.trim() && !selected)}
            aria-controls="attendance-member-results"
            autoComplete="off"
            placeholder="Type a member name or ID"
            value={lookup}
            onChange={(event) => changeLookup(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && matches[0] && !selected) {
                event.preventDefault();
                selectMember(matches[0]);
              }
            }}
          />
          {lookup.trim() && !selected && (
            <div
              className="member-results"
              id="attendance-member-results"
              role="listbox"
            >
              {members.error ? (
                <span>{members.error}</span>
              ) : members.loading || members.resolvedPath !== lookupPath ? (
                <span>Searching…</span>
              ) : matches.length ? (
                matches.map((member) => (
                  <button
                    type="button"
                    role="option"
                    key={member.id}
                    onClick={() => selectMember(member)}
                  >
                    <span className="member-identity">
                      <MemberAvatar member={member} />
                      <strong>{member.name}</strong>
                    </span>
                    <small>
                      {member.memberCode} · expires{" "}
                      {member.expiryDate || "not set"}
                    </small>
                  </button>
                ))
              ) : (
                <span>No members match this search.</span>
              )}
            </div>
          )}
        </div>
        <div className={`selected-member ${selected ? "is-selected" : ""}`}>
          {selected ? (
            <>
              <MemberAvatar member={selected} />
              <div>
                <small>Selected member</small>
                <strong>{selected.name}</strong>
                <span>
                  {selected.memberCode} ·{" "}
                  {openLoading
                    ? "Checking attendance…"
                    : openState === true
                      ? "Currently checked in"
                      : openState === null
                        ? "Attendance unavailable"
                        : canCheckIn
                          ? "Ready to check in"
                          : "Check-in unavailable"}
                </span>
              </div>
              <button
                type="button"
                disabled={actionBusy}
                onClick={() => {
                  selectionRequest.current++;
                  setLookup("");
                  setSelected(null);
                  setOpenState(null);
                }}
              >
                Clear selection
              </button>
            </>
          ) : (
            <span>Search and select a member to record attendance.</span>
          )}
        </div>
        <div className="attendance-actions">
          <button
            type="button"
            className="primary"
            disabled={!canCheckIn}
            onClick={() => action("check-in")}
          >
            Check In
          </button>
          <button
            type="button"
            className="secondary-action"
            disabled={!canCheckOut}
            onClick={() => action("check-out")}
          >
            Check Out
          </button>
        </div>
      </section>
      <form className="filters attendance-filters" onSubmit={applyFilters}>
        <label>
          From date
          <input
            aria-label="Attendance from date"
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
        </label>
        <label>
          To date
          <input
            aria-label="Attendance to date"
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
        </label>
        <button className="primary">Apply filters</button>
        <button type="button" disabled={actionBusy} onClick={resetFilters}>
          Reset
        </button>
        {selected && (
          <span className="filter-summary">
            Showing {selected.name} when filters are applied
          </span>
        )}
      </form>
      <State {...history}>
        <Panel title="Attendance history">
          <Table
            rows={history.data.content || []}
            cols={[
              [(row) => row.member?.name, "Member"],
              [
                (row) => formatIndiaDateTime(row.checkedInAt),
                "Check-in (India time)",
              ],
              [
                (row) => formatIndiaDateTime(row.checkedOutAt),
                "Check-out (India time)",
              ],
              ["status", "Status"],
              ["source", "Source"],
            ]}
          />
          <Pagination
            data={history.data}
            onPage={(page) => setPath(pageUrl(path, page))}
          />
        </Panel>
      </State>
    </>
  );
}
