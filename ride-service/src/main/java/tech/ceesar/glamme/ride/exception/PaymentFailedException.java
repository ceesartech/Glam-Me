package tech.ceesar.glamme.ride.exception;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super("Charge failed: " + message);
    }
}
