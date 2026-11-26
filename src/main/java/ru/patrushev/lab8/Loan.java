package ru.patrushev.lab8;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Loan(long id, @JsonProperty("equipment_id") long equipmentId,
                   String borrower, @JsonProperty("issue_date") String issueDate,
                   @JsonProperty("return_date") String returnDate,
                   @JsonProperty("equipment_name") String equipmentName) {
}
