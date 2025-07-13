package tech.ceesar.glamme.matching.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.matching.dto.OfferingResponse;
import tech.ceesar.glamme.matching.dto.PairDto;
import tech.ceesar.glamme.matching.dto.StableMatchRequest;
import tech.ceesar.glamme.matching.service.MatchingService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

//    /**
//     * Recommend offerings for a given hairstyle:
//     * - styleName: e.g. "bob"
//     * - latitude, longitude: customer's location
//     * - limit (optional, default=10)
//     */
//    @GetMapping("/recommend")
//    public ResponseEntity<List<OfferingResponse>> recommend(
//            @RequestParam String styleName,
//            @RequestParam double latitude,
//            @RequestParam double longitude,
//            @RequestParam(defaultValue = "10") int limit
//    ) {
//        List<OfferingResponse> list = matchingService.recommendOfferings(
//                styleName, latitude, longitude, limit);
//        return ResponseEntity.ok(list);
//    }

    /**
     * Recommend offerings, with optional price filtering and pagination.
     *
     * @param styleName The hairstyle to match on.
     * @param latitude  Customer latitude.
     * @param longitude Customer longitude.
     * @param page      Zero-based page index (default 0).
     * @param size      Page size (default 10).
     * @param minCost   Optional minimum total cost filter.
     * @param maxCost   Optional maximum total cost filter.
     */
    @GetMapping("/recommend")
    public PagedResponse<OfferingResponse> recommend(
            @RequestParam String styleName,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Double minCost,
            @RequestParam(required = false) Double maxCost
    ) {
        // 1) Fetch *all* matching offerings (unbounded limit)
        List<OfferingResponse> all = matchingService.recommendOfferings(
                styleName, latitude, longitude, Integer.MAX_VALUE
        );

        // 2) Apply cost filtering
        Stream<OfferingResponse> stream = all.stream();
        if (minCost != null) {
            stream = stream.filter(o -> o.getTotalCost() >= minCost);
        }
        if (maxCost != null) {
            stream = stream.filter(o -> o.getTotalCost() <= maxCost);
        }
        List<OfferingResponse> filtered = stream.collect(Collectors.toList());

        // 3) Compute pagination indices
        int total = filtered.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<OfferingResponse> content = (fromIndex >= total)
                ? Collections.emptyList()
                : filtered.subList(fromIndex, toIndex);

        int totalPages = size > 0
                ? (int) Math.ceil((double) total / size)
                : 1;
        boolean last = page >= (totalPages - 1);

        // 4) Return paged response
        return new PagedResponse<>(
                content,
                page,
                size,
                total,
                totalPages,
                last
        );
    }

    /**
     * Stable matching for multiple customers & stylists.
     */
    @PostMapping("/stable")
    public ResponseEntity<List<PairDto>> stableMatch(
            @RequestBody StableMatchRequest matchRequest
    ) {
        return ResponseEntity.ok(matchingService.stableMatch(matchRequest));
    }
}
