// Client-side CSV export for report tables.
// columns: [{ label, value(row) }]

export function downloadCsv(filename, columns, rows) {
  const escape = (value) => {
    const text = value == null ? "" : String(value);
    return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  };

  const lines = [
    columns.map((c) => escape(c.label)).join(","),
    ...rows.map((row) => columns.map((c) => escape(c.value(row))).join(",")),
  ];

  // BOM so Excel opens UTF-8 correctly
  const blob = new Blob(["﻿" + lines.join("\r\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
