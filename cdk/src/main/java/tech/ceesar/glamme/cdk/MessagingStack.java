package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

public class MessagingStack extends Stack {
    private final EventBus eventBus;
    private final Queue imageJobsQueue;
    private final Queue commEventsQueue;
    private final Queue socialEventsQueue;
    private final Queue bookingEventsQueue;
    private final Queue paymentEventsQueue;
    private final Queue rideEventsQueue;

    public MessagingStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // EventBridge Bus
        this.eventBus = EventBus.Builder.create(this, "GlammeEventBus")
                .eventBusName("glamme-bus")
                .build();

        // Dead Letter Queues
        DeadLetterQueue imageJobsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "ImageJobsDlq")
                        .queueName("image-jobs-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        DeadLetterQueue commEventsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "CommEventsDlq")
                        .queueName("comm-events-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        DeadLetterQueue socialEventsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "SocialEventsDlq")
                        .queueName("social-events-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        DeadLetterQueue bookingEventsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "BookingEventsDlq")
                        .queueName("booking-events-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        DeadLetterQueue paymentEventsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "PaymentEventsDlq")
                        .queueName("payment-events-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        DeadLetterQueue rideEventsDlq = DeadLetterQueue.builder()
                .queue(Queue.Builder.create(this, "RideEventsDlq")
                        .queueName("ride-events-dlq")
                        .build())
                .maxReceiveCount(5)
                .build();

        // Main Queues
        this.imageJobsQueue = Queue.Builder.create(this, "ImageJobsQueue")
                .queueName("image-jobs")
                .deadLetterQueue(imageJobsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(300))
                .build();

        this.commEventsQueue = Queue.Builder.create(this, "CommEventsQueue")
                .queueName("comm-events")
                .deadLetterQueue(commEventsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(60))
                .build();

        this.socialEventsQueue = Queue.Builder.create(this, "SocialEventsQueue")
                .queueName("social-events")
                .deadLetterQueue(socialEventsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(60))
                .build();

        this.bookingEventsQueue = Queue.Builder.create(this, "BookingEventsQueue")
                .queueName("booking-events")
                .deadLetterQueue(bookingEventsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(60))
                .build();

        this.paymentEventsQueue = Queue.Builder.create(this, "PaymentEventsQueue")
                .queueName("payment-events")
                .deadLetterQueue(paymentEventsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(60))
                .build();

        this.rideEventsQueue = Queue.Builder.create(this, "RideEventsQueue")
                .queueName("ride-events")
                .deadLetterQueue(rideEventsDlq)
                .visibilityTimeout(software.amazon.awscdk.Duration.seconds(60))
                .build();

        // Outputs
        software.amazon.awscdk.CfnOutput.Builder.create(this, "EventBusArn")
                .exportName("EventBusArn")
                .value(eventBus.getEventBusArn())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "ImageJobsQueueUrl")
                .exportName("ImageJobsQueueUrl")
                .value(imageJobsQueue.getQueueUrl())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "CommEventsQueueUrl")
                .exportName("CommEventsQueueUrl")
                .value(commEventsQueue.getQueueUrl())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "SocialEventsQueueUrl")
                .exportName("SocialEventsQueueUrl")
                .value(socialEventsQueue.getQueueUrl())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "BookingEventsQueueUrl")
                .exportName("BookingEventsQueueUrl")
                .value(bookingEventsQueue.getQueueUrl())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "PaymentEventsQueueUrl")
                .exportName("PaymentEventsQueueUrl")
                .value(paymentEventsQueue.getQueueUrl())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "RideEventsQueueUrl")
                .exportName("RideEventsQueueUrl")
                .value(rideEventsQueue.getQueueUrl())
                .build();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Queue getImageJobsQueue() {
        return imageJobsQueue;
    }

    public Queue getCommEventsQueue() {
        return commEventsQueue;
    }

    public Queue getSocialEventsQueue() {
        return socialEventsQueue;
    }

    public Queue getBookingEventsQueue() {
        return bookingEventsQueue;
    }

    public Queue getPaymentEventsQueue() {
        return paymentEventsQueue;
    }

    public Queue getRideEventsQueue() {
        return rideEventsQueue;
    }
}
