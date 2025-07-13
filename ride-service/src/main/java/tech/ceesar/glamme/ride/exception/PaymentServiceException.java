package tech.ceesar.glamme.ride.exception;

public class PaymentServiceException extends RuntimeException {
  public PaymentServiceException(Throwable cause) {
    super("Unexpected payment error", cause);
  }
}
