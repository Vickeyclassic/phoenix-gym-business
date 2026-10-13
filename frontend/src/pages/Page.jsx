import { Dashboard } from "./Dashboard.jsx";
import { Members } from "./Members.jsx";
import { Plans } from "./Plans.jsx";
import { Payments } from "./Payments.jsx";
import { Attendance } from "./Attendance.jsx";
import { Reports } from "./Reports.jsx";
import { Staff } from "./Staff.jsx";
import { Audit } from "./Audit.jsx";
import { BusinessSettings } from "./BusinessSettings.jsx";
import { ChangePassword } from "./ChangePassword.jsx";
export function Page({
  name,
  user,
  notify,
  onPasswordChanged,
  onOwnerChanged,
}) {
  return (
    <main className="content">
      {
        {
          Dashboard: <Dashboard />,
          Members: <Members user={user} notify={notify} />,
          "Membership Plans": <Plans user={user} notify={notify} />,
          Payments: <Payments notify={notify} />,
          Attendance: <Attendance notify={notify} />,
          Reports: <Reports notify={notify} />,
          "Users / Staff": (
            <Staff notify={notify} onOwnerChanged={onOwnerChanged} />
          ),
          "Audit Logs": <Audit />,
          "Business Settings": <BusinessSettings user={user} notify={notify} />,
          "Change Password": <ChangePassword onChanged={onPasswordChanged} />,
        }[name]
      }
    </main>
  );
}
