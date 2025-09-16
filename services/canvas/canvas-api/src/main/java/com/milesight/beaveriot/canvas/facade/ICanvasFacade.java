package com.milesight.beaveriot.canvas.facade;

import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.model.CanvasDTO;

import java.util.List;

/**
 * ICanvasFacade class.
 *
 * @author simon
 * @date 2025/9/10
 */
public interface ICanvasFacade {
    CanvasDTO createCanvas(String name, CanvasAttachType attachType, String attachId);

    void deleteCanvasByAttach(CanvasAttachType attachType, List<String> attachIdList);

    void deleteCanvasByIds(List<Long> canvasIdList);

    List<CanvasDTO> getCanvasByAttach(CanvasAttachType attachType, List<String> attachIdList);
}
