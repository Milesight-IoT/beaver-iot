package com.milesight.beaveriot.base.page;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author loong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GenericQueryPageRequest extends GenericPageRequest {

    private String keyword;
}
