import { h } from "../core/dom.js";
import { monthBounds, todayISO } from "../core/format.js";
import { can, hasRole, isLinked, ROLES } from "../core/permissions.js";
import { takeAction } from "../core/router.js";
import { AttendanceApi } from "../services/api.js";
import { departmentLabel, employeeLabel, lookups } from "../services/lookups.js";
import { asyncSection, skeleton } from "../components/feedback.js";
import { button, iconButton } from "../components/button.js";
import { compactInput, compactSelect } from "../components/fields.js";
import { dataTable } from "../components/table.js";
import { tabbedView } from "../components/tabs.js";
import { barList, card, page, pageHeader } from "../components/page.js";
import { attendanceColumns, byDateDesc } from "../widgets/columns.js";
import { correctAttendanceDialog, deleteAttendance, logAttendanceDialog } from "../widgets/dialogs.js";
import { attendanceTodayCard } from "../widgets/selfService.js";

const statusFilters = [
  { id: "all", label: "All" },
  { id: "PRESENT", label: "Present", predicate: (r) => r.attendanceStatus === "PRESENT" },
  { id: "LATE", label: "Late", predicate: (r) => r.attendanceStatus === "LATE" },
  { id: "HALF_DAY", label: "Half day", predicate: (r) => r.attendanceStatus === "HALF_DAY" },
  { id: "ABSENT", label: "Absent", predicate: (r) => r.attendanceStatus === "ABSENT" },
];

export default function AttendancePage(ctx) {
  const { user } = ctx;
  const action = takeAction(ctx);
  const linked = isLinked(user);
  const canLog = can(user, "logAttendance");
  const isSupervisor = hasRole(user, ROLES.SUPERVISOR);

  const openLog = async () => {
    if (await logAttendanceDialog()) ctx.navigate("/attendance", { tab: "team" });
  };

  // ---- My attendance ----
  const myView = () => {
    let history;
    const summary = asyncSection(
      () => AttendanceApi.mine(),
      (records) => {
        const { start, end } = monthBounds();
        const month = records.filter((r) => r.eventDate >= start && r.eventDate <= end);
        const count = (s) => month.filter((r) => r.attendanceStatus === s).length;
        return barList([
          { label: "Present", value: count("PRESENT") },
          { label: "Late", value: count("LATE") },
          { label: "Half day", value: count("HALF_DAY") },
          { label: "Absent", value: count("ABSENT") },
        ]);
      },
      { placeholder: () => skeleton({ rows: 4 }) }
    );

    history = asyncSection(
      async () => (await AttendanceApi.mine()).sort(byDateDesc("eventDate")),
      (rows) =>
        dataTable({
          title: "History",
          rows,
          searchable: false,
          filters: statusFilters,
          columns: attendanceColumns(),
          empty: { icon: "event_available", title: "No attendance yet", text: "Check in from the card above to start your record." },
        })
    );

    return h(
      "div",
      { class: "stack" },
      h(
        "div",
        { class: "grid grid--2" },
        attendanceTodayCard(ctx, {
          onChange: () => {
            history.reload();
            summary.reload();
          },
        }),
        card({ title: "This month" }, summary)
      ),
      history
    );
  };

  // ---- Team / department ----
  const teamView = () =>
    asyncSection(
      async () => ({
        departments: isSupervisor ? [] : await lookups.departments(),
        me: isSupervisor ? await lookups.myEmployee() : null,
      }),
      ({ departments, me }) => {
        if (isSupervisor && !me?.departmentId) throw Object.assign(new Error("Your employee record isn't assigned to a department."), { status: 403 });

        const deptControl = isSupervisor
          ? null
          : compactSelect({ label: "Department", value: departments[0]?.departmentId, options: departments.map((d) => ({ value: d.departmentId, label: d.name })), onChange: () => records.reload() });
        const dateControl = compactInput({ label: "Date", type: "date", value: todayISO(), max: todayISO(), onChange: () => records.reload() });

        const records = asyncSection(
          async () => {
            const deptId = isSupervisor ? me.departmentId : Number(deptControl.control.value);
            const [list, maps] = await Promise.all([deptId ? AttendanceApi.byDepartment(deptId) : [], lookups.maps()]);
            return { list: list.sort(byDateDesc("eventDate")), maps };
          },
          ({ list, maps }, reload) => {
            const date = dateControl.control.value;
            const rows = date ? list.filter((r) => r.eventDate === date) : list;
            return dataTable({
              rows,
              toolbarExtra: [
                dateControl,
                date && button({ label: "All dates", variant: "text", size: "sm", onClick: () => { dateControl.control.value = ""; records.reload(); } }),
              ],
              searchPlaceholder: "Search people",
              searchText: (r) => employeeLabel(maps.employees, r.employeeId),
              filters: statusFilters,
              columns: attendanceColumns({
                maps,
                withEmployee: true,
                actions: canLog
                  ? (r) => [
                      iconButton({ icon: "edit_calendar", label: "Correct", size: "sm", onClick: async () => (await correctAttendanceDialog(r, employeeLabel(maps.employees, r.employeeId))) && reload() }),
                      can(user, "deleteAttendance") && iconButton({ icon: "delete", label: "Delete", size: "sm", onClick: async () => (await deleteAttendance(r)) && reload() }),
                    ]
                  : null,
              }),
              empty: {
                icon: "event_busy",
                title: date ? "No attendance recorded for this date" : "No attendance records",
                action: canLog ? { label: "Log attendance", icon: "more_time", onClick: openLog } : undefined,
              },
            });
          }
        );

        return h("div", { class: "stack" }, deptControl && h("div", { class: "filter-bar", style: { marginBottom: "0" } }, deptControl), records);
      }
    );

  const tabs = tabbedView({
    active: ctx.query.tab,
    items: [
      { id: "mine", label: "My attendance", icon: "person", visible: linked },
      { id: "team", label: isSupervisor ? "My team" : "By department", icon: "groups", visible: can(user, "viewDepartmentAttendance") },
    ],
    render: (id) => (id === "team" ? teamView() : myView()),
  });

  if (action === "log" && canLog) setTimeout(openLog);

  return page(
    pageHeader({
      title: "Attendance",
      subtitle: "Check-ins, check-outs and corrections",
      actions: canLog ? button({ label: "Log attendance", icon: "more_time", onClick: openLog }) : null,
    }),
    tabs
  );
}
