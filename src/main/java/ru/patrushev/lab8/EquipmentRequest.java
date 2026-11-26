package ru.patrushev.lab8;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EquipmentRequest(String name, String type,
                               @JsonProperty("serial_number") String serialNumber,
                               String status) {
}
