"use strict";

const equipmentRows = document.getElementById("equipment-rows");
const loanRows = document.getElementById("loan-rows");
const typeFilter = document.getElementById("type-filter");
const statusFilter = document.getElementById("status-filter");
const equipmentDialog = document.getElementById("equipment-dialog");
const loanDialog = document.getElementById("loan-dialog");
const equipmentForm = document.getElementById("equipment-form");
const loanForm = document.getElementById("loan-form");
const notice = document.getElementById("notice");

const statusNames = {
  available: "Доступно",
  in_use: "В использовании",
  maintenance: "На обслуживании"
};

async function request(url, options = {}) {
  const response = await fetch(url, options);
  if (response.status === 204) return null;

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Не удалось выполнить запрос");
  }
  return data;
}

function showMessage(message, kind = "success") {
  notice.textContent = message;
  notice.className = "notice " + kind;
}

function cell(row, value) {
  const item = document.createElement("td");
  item.textContent = value;
  row.append(item);
  return item;
}

function action(label, callback, kind = "secondary") {
  const button = document.createElement("button");
  button.type = "button";
  button.className = kind;
  button.textContent = label;
  button.addEventListener("click", callback);
  return button;
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("ru-RU");
}

function renderEquipment(items) {
  equipmentRows.replaceChildren();
  document.getElementById("equipment-empty").hidden = items.length !== 0;

  for (const item of items) {
    const row = document.createElement("tr");
    cell(row, item.name);
    cell(row, item.type);
    cell(row, item.serial_number);

    const statusCell = cell(row, "");
    const badge = document.createElement("span");
    badge.className = "badge " + item.status;
    badge.textContent = statusNames[item.status] || item.status;
    statusCell.append(badge);

    const buttons = cell(row, "");
    const group = document.createElement("div");
    group.className = "actions";
    group.append(action("Изменить", () => openEdit(item)));
    if (item.status === "available") {
      group.append(action("Выдать", () => openLoan(item), "primary"));
    }
    group.append(action("Удалить", () => removeEquipment(item), "secondary danger"));
    buttons.append(group);
    equipmentRows.append(row);
  }
}

function renderLoans(items) {
  loanRows.replaceChildren();
  document.getElementById("loans-empty").hidden = items.length !== 0;
  document.getElementById("loan-count").textContent = "Активных выдач: " + items.length;

  for (const item of items) {
    const row = document.createElement("tr");
    cell(row, item.equipment_name);
    cell(row, item.borrower);
    cell(row, formatDate(item.issue_date));
    const buttons = cell(row, "");
    buttons.append(action("Оформить возврат", () => returnEquipment(item.id)));
    loanRows.append(row);
  }
}

function updateTypes(types) {
  const previous = typeFilter.value;
  typeFilter.replaceChildren(new Option("Все типы", ""));
  for (const type of types) {
    typeFilter.append(new Option(type, type));
  }
  typeFilter.value = types.includes(previous) ? previous : "";
}

async function refresh() {
  const params = new URLSearchParams();
  if (statusFilter.value) params.set("status", statusFilter.value);
  if (typeFilter.value) params.set("type", typeFilter.value);

  const [equipment, loans, types] = await Promise.all([
    request("/api/equipment?" + params),
    request("/api/loans?active=true"),
    request("/api/equipment/types")
  ]);
  renderEquipment(equipment);
  renderLoans(loans);
  updateTypes(types);
}

function openEdit(item = null) {
  equipmentForm.reset();
  document.getElementById("equipment-error").hidden = true;
  const editing = item !== null;
  document.getElementById("equipment-dialog-title").textContent =
    editing ? "Изменить оборудование" : "Добавить оборудование";
  document.getElementById("equipment-id").value = editing ? item.id : "";
  document.getElementById("equipment-name").value = editing ? item.name : "";
  document.getElementById("equipment-type").value = editing ? item.type : "";
  document.getElementById("equipment-serial").value = editing ? item.serial_number : "";

  const status = document.getElementById("equipment-status");
  status.querySelector('option[value="in_use"]').hidden = !editing || item.status !== "in_use";
  status.value = editing ? item.status : "available";
  status.disabled = editing && item.status === "in_use";
  equipmentDialog.showModal();
}

function openLoan(item) {
  loanForm.reset();
  document.getElementById("loan-error").hidden = true;
  document.getElementById("loan-equipment-id").value = item.id;
  document.getElementById("loan-equipment-name").textContent = item.name;
  loanDialog.showModal();
}

async function removeEquipment(item) {
  if (!confirm("Удалить «" + item.name + "» и историю его выдач?")) return;
  try {
    await request("/api/equipment/" + item.id, { method: "DELETE" });
    await refresh();
    showMessage("Оборудование удалено");
  } catch (error) {
    showMessage(error.message, "error");
  }
}

async function returnEquipment(id) {
  try {
    await request("/api/loans/" + id + "/return", { method: "PUT" });
    await refresh();
    showMessage("Возврат оформлен");
  } catch (error) {
    showMessage(error.message, "error");
  }
}

equipmentForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const id = document.getElementById("equipment-id").value;
  const data = {
    name: document.getElementById("equipment-name").value.trim(),
    type: document.getElementById("equipment-type").value.trim(),
    serial_number: document.getElementById("equipment-serial").value.trim(),
    status: document.getElementById("equipment-status").value
  };
  try {
    await request(id ? "/api/equipment/" + id : "/api/equipment", {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    equipmentDialog.close();
    await refresh();
    showMessage(id ? "Оборудование обновлено" : "Оборудование добавлено");
  } catch (error) {
    const message = document.getElementById("equipment-error");
    message.textContent = error.message;
    message.hidden = false;
  }
});

loanForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await request("/api/loans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        equipment_id: Number(document.getElementById("loan-equipment-id").value),
        borrower: document.getElementById("borrower").value.trim()
      })
    });
    loanDialog.close();
    await refresh();
    showMessage("Выдача оформлена");
  } catch (error) {
    const message = document.getElementById("loan-error");
    message.textContent = error.message;
    message.hidden = false;
  }
});

document.getElementById("add-equipment").addEventListener("click", () => openEdit());
statusFilter.addEventListener("change", () => refresh().catch((error) => showMessage(error.message, "error")));
typeFilter.addEventListener("change", () => refresh().catch((error) => showMessage(error.message, "error")));
document.querySelectorAll("[data-close]").forEach((button) => {
  button.addEventListener("click", () => document.getElementById(button.dataset.close).close());
});

refresh().catch((error) => showMessage(error.message, "error"));
