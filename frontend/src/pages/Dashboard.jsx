import { useLoad } from "../hooks/useLoad.js";
import { Panel, State, Table } from "../components/ui.jsx";
import { today } from "../utils/dates.js";
export function Dashboard() {
  const x = useLoad("/dashboard", {});
  return (
    <State {...x}>
      <div className="dashboard-context">
        Membership status as of{" "}
        <strong>{x.data.businessDate || today()}</strong> (
        {x.data.businessTimeZone || "Asia/Kolkata"}). Expiring soon includes
        active memberships expiring in the next{" "}
        <strong>{x.data.expiringSoonDays ?? 7} days</strong>, starting tomorrow.
      </div>
      <div className="metrics">
        {[
          ["Total members", x.data.totalMembers],
          ["Active members", x.data.activeMembers],
          ["Expired members", x.data.expiredMembers],
          ["Expiring soon", x.data.expiringSoon],
          ["Today's attendance", x.data.todayAttendance],
          ["Today's revenue", x.data.todayRevenue],
          ["Month revenue", x.data.monthRevenue],
          ["New this month", x.data.newMembersThisMonth],
          ["Renewals this month", x.data.renewalsThisMonth],
        ].map(([a, b]) => (
          <article key={a}>
            <small>{a}</small>
            <strong>{b ?? 0}</strong>
          </article>
        ))}
      </div>
      <div className="charts">
        <Chart title="Monthly revenue" data={x.data.monthlyRevenue} />
        <Chart title="Attendance trend" data={x.data.attendanceTrend} />
        <Chart
          title="Membership distribution"
          data={x.data.membershipDistribution}
        />
      </div>
      <Panel title="Recent payments">
        <Table
          rows={x.data.recentPayments || []}
          cols={[
            ["receiptNumber", "Receipt"],
            ["planNameSnapshot", "Plan"],
            ["amount", "Amount"],
            ["method", "Method"],
          ]}
        />
      </Panel>
    </State>
  );
}
export function Chart({ title, data = [] }) {
  const max = Math.max(1, ...data.map((x) => Number(x.value)));
  return (
    <section className="chart">
      <h2>{title}</h2>
      {!data.length ? (
        <div className="mini-empty">No data yet</div>
      ) : (
        <div className="bars">
          {data.map((x, i) => (
            <div className="bar-col" key={x.label + i}>
              <span>{x.value}</span>
              <i
                style={{
                  height: `${Math.max(4, (Number(x.value) / max) * 100)}%`,
                }}
              />
              <small>{x.label}</small>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
