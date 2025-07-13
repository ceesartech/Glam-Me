package tech.ceesar.glamme.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an entity cannot be found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {

      super(String.format("%s not found with %s = '%s'",
              resourceName, fieldName, fieldValue));
    }
}
