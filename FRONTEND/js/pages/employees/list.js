import { fmtDate, fullName } from "../../core/format.js";
import { can, hasRole, ROLES } from "../../core/permissions.js";
import { takeAction } from "../../core/router.js";
import { departmentLabel, lookups, positionLabel } from "../../services/lookups.js";
import { asyncSection } from "../../components/feedback.js";
import { button, iconButton } from "../../components/button.js";
import { dataTable } from "../../components/table.js";
import { page, pageHeader, personCell } from "../../components/page.js";
import { statusBadge } from "../../components/status.js";
import { deleteEmployee, employeeDialog } from "../../widgets/dialogs.js";

export default function EmployeesPage(ctx) {
  const { user } = ctx;
  const isHr = can(user, "manageEmployees");
  const isSupervisor = hasRole(user, ROLES.SUPERVISOR);
  const action = takeAction(ctx);

  lookups.invalidate("employees", "team");

  const create = async () => {
    if (await employeeDialog()) content.reload();
  };

  const content = asyncSection(
    async () => {
      const [list, maps] = await Promise.all([isSupervisor ? lookups.team() : lookups.employees(), lookups.maps()]);
      return { list, maps };
    },
    ({ list, maps }, reload) =>
      dataTable({
        rows: list,
        searchPlaceholder: "Search by name, email, NIC or department",
        searchText: (e) => `${fullName(e)} ${e.email} ${e.nic} ${departmentLabel(maps.departments, e.departmentId)} ${positionLabel(maps.positions, e.positionId)}`,
        filters: [
          { id: "all", label: "All" },
          { id: "ACTIVE", label: "Active", predicate: (e) => e.status === "ACTIVE" },
          { id: "PROBATION", label: "Probation", predicate: (e) => e.status === "PROBATION" },
          { id: "ON_LEAVE", label: "On leave", predicate: (e) => e.status === "ON_LEAVE" },
          { id: "INACTIVE", label: "Inactive", predicate: (e) => e.status === "INACTIVE" },
        ],
        onRowClick: (e) => ctx.navigate(`/employees/${e.employeeId}`),
        columns: [
          { key: "name", label: "Name", render: (e) => personCell(fullName(e), e.email), sortValue: fullName },
          { key: "departmentId", label: "Department", render: (e) => departmentLabel(maps.departments, e.departmentId), sortValue: (e) => departmentLabel(maps.departments, e.departmentId) },
          { key: "positionId", label: "Position", render: (e) => positionLabel(maps.positions, e.positionId), sortValue: (e) => positionLabel(maps.positions, e.positionId) },
          { key: "hireDate", label: "Hire date", render: (e) => fmtDate(e.hireDate) },
          { key: "status", label: "Status", render: (e) => statusBadge(e.status) },
          isHr && {
            key: "actions",
            render: (e) => [
              iconButton({ icon: "edit", label: `Edit ${fullName(e)}`, size: "sm", onClick: async () => (await employeeDialog(e)) && reload() }),
              // You can't delete your own record (the API refuses too)
              e.employeeId !== user.employeeId && iconButton({ icon: "delete", label: `Delete ${fullName(e)}`, size: "sm", onClick: async () => (await deleteEmployee(e)) && reload() }),
            ],
          },
        ].filter(Boolean),
        empty: {
          icon: "group",
          title: "No employees yet",
          text: isHr ? "Add your first employee record to get started." : "Employee records will appear here once HR adds them.",
          action: isHr ? { label: "Add employee", icon: "person_add", onClick: create } : undefined,
        },
      })
  );

  if (action === "new" && isHr) setTimeout(create);

  return page(
    pageHeader({
      title: "Directory",
      subtitle: isSupervisor ? "People in your department" : "Everyone with an employee record at LankaTech",
      actions: isHr ? button({ label: "Add employee", icon: "person_add", onClick: create }) : null,
    }),
    content
  );
}
