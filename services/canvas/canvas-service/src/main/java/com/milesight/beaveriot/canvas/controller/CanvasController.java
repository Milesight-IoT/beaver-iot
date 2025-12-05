package com.milesight.beaveriot.canvas.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.canvas.enums.CanvasOp;
import com.milesight.beaveriot.canvas.model.request.CanvasUpdateRequest;
import com.milesight.beaveriot.canvas.model.response.CanvasResponse;
import com.milesight.beaveriot.canvas.service.CanvasService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * CanvasController class.
 *
 * @author simon
 * @date 2025/9/9
 */
@RestController
@RequestMapping("/canvas")
public class CanvasController {
    @Autowired
    CanvasService canvasService;

    @GetMapping("/{canvasId}")
    public ResponseBody<CanvasResponse> getCanvas(@PathVariable("canvasId") Long canvasId) {
        canvasService.checkCanvasPermission(canvasId, CanvasOp.READ);
        return ResponseBuilder.success(canvasService.getCanvasData(canvasId));
    }

    @PutMapping("/{canvasId}")
    public ResponseBody<Void> updateCanvas(@PathVariable("canvasId") Long canvasId, @RequestBody @Valid CanvasUpdateRequest request) {
        canvasService.checkCanvasPermission(canvasId, CanvasOp.UPDATE);
        canvasService.updateCanvas(canvasId, request);
        return ResponseBuilder.success();
    }
}
