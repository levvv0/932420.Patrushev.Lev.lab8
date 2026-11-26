package ru.patrushev.lab8;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoanRequest(@JsonProperty("equipment_id") Long equipmentId, String borrower) {
}
