package tech.ceesar.glamme.shopping.client;

import com.easypost.exception.EasyPostException;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.easypost.service.EasyPostClient;
import org.springframework.stereotype.Component;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseRequest;
import tech.ceesar.glamme.shopping.dto.ShippingPurchaseResponse;
import tech.ceesar.glamme.shopping.dto.ShippingRateRequest;
import tech.ceesar.glamme.shopping.dto.ShippingRateResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EasyPostShippingClient implements ShippingClient {
    private final EasyPostClient client;

    public EasyPostShippingClient(EasyPostClient client) {
        this.client = client;
    }

    @Override
    public List<ShippingRateResponse> getRates(ShippingRateRequest req) {
        try {
            Map<String,Object> from = Map.of(
                    "name", req.getFrom().getName(),
                    "street1", req.getFrom().getStreet1(),
                    "city", req.getFrom().getCity(),
                    "state", req.getFrom().getState(),
                    "zip", req.getFrom().getPostalCode(),
                    "country", req.getFrom().getCountry()
            );
            Map<String,Object> to = Map.of(
                    "name", req.getTo().getName(),
                    "street1", req.getTo().getStreet1(),
                    "city", req.getTo().getCity(),
                    "state", req.getTo().getState(),
                    "zip", req.getTo().getPostalCode(),
                    "country", req.getTo().getCountry()
            );
            Map<String,Object> parcel = Map.of(
                    "length", req.getLength(),
                    "width", req.getWidth(),
                    "height", req.getHeight(),
                    "weight", req.getWeight()
            );
            // use the client instance instead of static
            Shipment shipment = client.shipment.create(Map.of(
                    "from_address", from,
                    "to_address", to,
                    "parcel", parcel
            ));
            return shipment.getRates().stream()
                    .map(r -> new ShippingRateResponse(
                            r.getId(),
                            r.getCarrier(),
                            r.getService(),
                            r.getRate(),
                            r.getCurrency(),
                            r.getDeliveryDays().intValue()
                    ))
                    .collect(Collectors.toList());
        } catch (EasyPostException e) {
            throw new RuntimeException("Failed to fetch shipping rates", e);
        }
    }

    @Override
    public ShippingPurchaseResponse purchase(ShippingPurchaseRequest req) {
        try {
            // retrieve & buy via the client
            Shipment shipment = client.shipment.retrieve(req.getShipmentId());
            Shipment bought = client.shipment.buy(shipment.getId(), Map.of(
                    "rate", Map.of("id", req.getRateId())
            ));
            Rate rate = bought.getRates().stream()
                    .filter(r -> r.getId().equals(req.getRateId()))
                    .findFirst().orElseThrow();
            return new ShippingPurchaseResponse(
                    bought.getId(),
                    rate.getRate(),
                    bought.getTrackingCode(),
                    bought.getPostageLabel().getLabelUrl()
            );
        } catch (EasyPostException e) {
            throw new RuntimeException("Failed to purchase shipment", e);
        }
    }
}
