package tech.ceesar.glamme.image.exception;

/**
 * Thrown when any step of image upload or AI processing fails.
 */
public class ImageProcessingException extends RuntimeException {
    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
