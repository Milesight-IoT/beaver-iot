package com.milesight.beaveriot.canvas.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.canvas.model.dto.CanvasDTO;
import com.milesight.beaveriot.canvas.model.request.CanvasUpdateRequest;
import com.milesight.beaveriot.canvas.model.response.CanvasResponse;
import com.milesight.beaveriot.canvas.po.CanvasPO;
import com.milesight.beaveriot.canvas.repository.CanvasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CanvasFacadeImpl class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Component
public class CanvasFacadeImpl implements ICanvasFacade {
    @Autowired
    CanvasService canvasService;

    @Autowired
    CanvasRepository canvasRepository;

    @Override
    public CanvasResponse getCanvasData(Long canvasId) {
        return canvasService.getCanvasData(canvasId);
    }

    @Override
    public CanvasDTO createCanvas(String name, CanvasAttachType attachType, String attachId) {
        Long canvasId = SnowflakeUtil.nextId();
        CanvasPO canvasPO = new CanvasPO();
        canvasPO.setId(canvasId);
        canvasPO.setName(name);
        canvasPO.setAttachType(attachType);
        canvasPO.setAttachId(attachId);
        canvasRepository.save(canvasPO);
        return covertPOToDTO(canvasPO);
    }

    @Override
    public void updateCanvas(Long canvasId, CanvasUpdateRequest canvasUpdateRequest) {
        canvasService.updateCanvas(canvasId, canvasUpdateRequest);
    }

    private CanvasDTO covertPOToDTO(CanvasPO canvasPO) {
        CanvasDTO canvasDTO = new CanvasDTO();
        canvasDTO.setId(canvasPO.getId());
        canvasDTO.setName(canvasPO.getName());
        canvasDTO.setAttachId(canvasPO.getAttachId());
        canvasDTO.setAttachType(canvasPO.getAttachType());
        return canvasDTO;
    }

    @Override
    public void deleteCanvasByAttach(CanvasAttachType attachType, List<String> attachIdList) {
        // get all related canvas
        List<CanvasPO> canvasPOList = canvasRepository.findAll(f -> f
                .in(CanvasPO.Fields.attachId, attachIdList.toArray())
                .eq(CanvasPO.Fields.attachType, attachType)
        );

        List<Long> canvasIdList = canvasPOList.stream().map(CanvasPO::getId).toList();
        canvasService.doDeleteCanvasByIdList(canvasIdList);
    }

    @Override
    public List<CanvasDTO> getCanvasByAttach(CanvasAttachType attachType, List<String> attachIdList) {
        return canvasRepository.findAll(f -> f
                .in(CanvasPO.Fields.attachId, attachIdList.toArray())
                .eq(CanvasPO.Fields.attachType, attachType)
        ).stream().map(this::covertPOToDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteCanvasByIds(List<Long> canvasIdList) {
        canvasService.doDeleteCanvasByIdList(canvasIdList);
    }
}
