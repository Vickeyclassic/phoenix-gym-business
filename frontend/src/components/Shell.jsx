import {
  LayoutDashboard,
  Users,
  ClipboardList,
  WalletCards,
  CalendarCheck,
  Download,
  UserRoundCog,
  Settings,
  X,
  LogOut,
  Menu,
  CheckCircle,
} from "lucide-react";
import { useState } from "react";
import { canAccessNavigation } from "../logic";
import { api } from "../api";
import { ThemeToggle } from "../ThemeToggle";
import { Brand, BusinessIdentity } from "../context/Branding.jsx";
import { Page } from "../pages/Page.jsx";
export const nav = [
  ["Dashboard", LayoutDashboard],
  ["Members", Users],
  ["Membership Plans", ClipboardList],
  ["Payments", WalletCards],
  ["Attendance", CalendarCheck],
  ["Reports", Download, "ADMIN"],
  ["Users / Staff", UserRoundCog, "OWNER"],
  ["Audit Logs", ClipboardList, "ADMIN"],
  ["Business Settings", Settings],
  ["Change Password", Settings],
];
export function Shell({ user, onLogout, onPasswordChanged, onOwnerChanged }) {
  const [page, setPage] = useState("Dashboard"),
    [open, setOpen] = useState(false),
    [toast, setToast] = useState(null);
  const allowed = ([, , r]) => canAccessNavigation(user.role, r);
  function notify(text, type = "success") {
    setToast({
      text,
      type,
    });
    setTimeout(() => setToast(null), 4000);
  }
  async function out() {
    try {
      await api.post("/auth/logout");
    } finally {
      sessionStorage.clear();
      onLogout();
    }
  }
  return (
    <div className="shell">
      <aside className={open ? "open" : ""}>
        <div className="brand">
          <Brand />
          <button
            className="mobile-close"
            aria-label="Close navigation"
            onClick={() => setOpen(false)}
          >
            <X />
          </button>
        </div>
        <nav>
          {nav.filter(allowed).map(([n, I]) => (
            <button
              key={n}
              className={page === n ? "active" : ""}
              onClick={() => {
                setPage(n);
                setOpen(false);
              }}
            >
              <I />
              {n}
            </button>
          ))}
        </nav>
        <div className="account">
          <span>
            {user.displayName}
            <small>{user.role}</small>
          </span>
          <button title="Log out" onClick={out}>
            <LogOut />
          </button>
        </div>
      </aside>
      <section className="workspace">
        <header>
          <button
            className="mobile-menu"
            aria-label="Open navigation"
            onClick={() => setOpen(true)}
          >
            <Menu />
          </button>
          <div>
            <h1>{page}</h1>
            <BusinessIdentity />
          </div>
          <ThemeToggle />
        </header>
        <Page
          name={page}
          user={user}
          notify={notify}
          onPasswordChanged={onPasswordChanged}
          onOwnerChanged={onOwnerChanged}
        />
      </section>
      {toast && (
        <div className={`toast ${toast.type}`} role="status">
          <CheckCircle />
          {toast.text}
        </div>
      )}
    </div>
  );
}
