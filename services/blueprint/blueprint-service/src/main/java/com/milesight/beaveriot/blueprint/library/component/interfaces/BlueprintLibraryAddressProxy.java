package com.milesight.beaveriot.blueprint.library.component.interfaces;

import java.io.InputStream;

/**
 * author: Luxb
 * create: 2025/10/21 11:17
 **/
public interface BlueprintLibraryAddressProxy {
    String getManifestContent();
    InputStream getDataInputStream();
}
