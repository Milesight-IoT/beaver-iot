package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.ResourceRefDTO;

/**
 * author: Luxb
 * create: 2025/11/6 15:44
 **/
public interface ResourceServiceProvider {
    /**
     * Creates a permanent association between a resource (identified by URL)
     * and a business entity reference.
     * This method links the resource from the given URL and binds it
     * to the specified reference. The resource will be managed by the resource center.
     *
     * @param url the URL pointing to the resource content (e.g., image, file)
     * @param resourceRefDTO the DTO containing the reference ID and type of the business entity
     *                       that the resource should be linked to
     * @throws IllegalArgumentException if the URL is invalid or inaccessible
     * @throws RuntimeException if the linking process fails
     */
    void linkByUrl(String url, ResourceRefDTO resourceRefDTO);
    /**
     * Removes the association between a business entity reference and its linked resource(s).
     * This operation does not necessarily delete the physical resource immediately,
     * but marks the reference as unlinked. Actual cleanup may be deferred based on policy.
     *
     * @param resourceRefDTO the DTO identifying the reference (refId and refType)
     *                       whose associated resource should be unlinked
     * @throws RuntimeException if the unlinking process fails
     */
    void unlinkRef(ResourceRefDTO resourceRefDTO);
    /**
     * Uploads a temporary resource to the resource center and returns its resource URL.
     * The uploaded resource is typically short-lived and may be automatically expired
     * after a certain period. It is not permanently bound to any business reference
     * until explicitly linked via {@link #linkByUrl(String, ResourceRefDTO)}.
     *
     * @param fileName the logical name of the resource (e.g., "avatar.png")
     * @param contentType the MIME type of the resource (e.g., "image/png", "application/pdf")
     * @param data the raw byte array of the resource content
     * @return the temporary URL where the uploaded resource can be accessed
     * @throws IllegalArgumentException if input data is null or invalid
     * @throws RuntimeException if upload fails
     */
    String putTempResource(String fileName, String contentType, byte[] data);
}
