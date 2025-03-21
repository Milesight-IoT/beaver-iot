package com.milesight.beaveriot.credentials.model.request;

import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteCredentialsRequest {

    List<Long> ids;

}
