package com.fintrac.dto;

import com.fintrac.model.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {
    private Long id;
    private AlertType type;
    private String title;
    private String message;
    private Boolean isRead;
}
