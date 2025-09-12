package com.milesight.beaveriot.canvas.model.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/1 8:55
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanvasExchangePayload implements Serializable {

    private List<String> entityIds;

}
