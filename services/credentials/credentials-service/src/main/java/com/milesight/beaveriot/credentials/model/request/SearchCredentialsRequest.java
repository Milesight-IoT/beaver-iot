package com.milesight.beaveriot.credentials.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCredentialsRequest extends GenericPageRequest {

    private String credentialsType;

}
