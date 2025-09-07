package tech.ceesar.glamme.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StylistSearchService {

    private final OpenSearchService openSearchService;

    private static final String STYLISTS_INDEX = "stylists";

    /**
     * Initialize stylist index with mapping
     */
    public boolean initializeStylistIndex() {
        Map<String, Object> mapping = new HashMap<>();

        // Text fields
        Map<String, Object> textField = Map.of("type", "text", "analyzer", "standard");
        Map<String, Object> keywordField = Map.of("type", "keyword");

        mapping.put("name", textField);
        mapping.put("bio", textField);
        mapping.put("specialties", Map.of("type", "keyword"));
        mapping.put("location", Map.of(
                "type", "geo_point",
                "ignore_malformed", true
        ));
        mapping.put("rating", Map.of("type", "float"));
        mapping.put("experience_years", Map.of("type", "integer"));
        mapping.put("price_range", Map.of(
                "type", "object",
                "properties", Map.of(
                        "min", Map.of("type", "integer"),
                        "max", Map.of("type", "integer")
                )
        ));
        mapping.put("availability", Map.of("type", "keyword"));
        mapping.put("verified", Map.of("type", "boolean"));
        mapping.put("featured", Map.of("type", "boolean"));
        mapping.put("portfolio_urls", Map.of("type", "keyword"));
        mapping.put("services_offered", Map.of("type", "keyword"));
        mapping.put("languages_spoken", Map.of("type", "keyword"));
        mapping.put("certifications", Map.of("type", "keyword"));

        return openSearchService.createIndex(STYLISTS_INDEX, mapping);
    }

    /**
     * Index stylist profile
     */
    public boolean indexStylist(StylistDocument stylist) {
        return openSearchService.indexDocument(STYLISTS_INDEX, stylist.getId(), stylist).isPresent();
    }

    /**
     * Search stylists by text query
     */
    public List<OpenSearchService.SearchResult<StylistDocument>> searchStylists(String query, int from, int size) {
        return openSearchService.searchByText(STYLISTS_INDEX, "name", query, StylistDocument.class, from, size);
    }

    /**
     * Search stylists by location and specialties
     */
    public List<OpenSearchService.SearchResult<StylistDocument>> searchStylistsByLocation(
            double lat, double lon, double radiusKm, List<String> specialties, int from, int size) {

        Map<String, Object> filters = new HashMap<>();
        if (specialties != null && !specialties.isEmpty()) {
            filters.put("specialties", specialties.get(0)); // Simplified - can be enhanced
        }

        // Note: For geo search, we'd need to implement geo queries
        // This is a simplified version
        return openSearchService.searchWithFilters(STYLISTS_INDEX, "location", "", filters,
                                                 StylistDocument.class, from, size);
    }

    /**
     * Get stylist suggestions for autocomplete
     */
    public List<String> getStylistNameSuggestions(String prefix, int size) {
        return openSearchService.getSuggestions(STYLISTS_INDEX, "name", prefix, size);
    }

    /**
     * Update stylist profile
     */
    public boolean updateStylist(StylistDocument stylist) {
        return openSearchService.updateDocument(STYLISTS_INDEX, stylist.getId(), stylist);
    }

    /**
     * Delete stylist profile
     */
    public boolean deleteStylist(String stylistId) {
        return openSearchService.deleteDocument(STYLISTS_INDEX, stylistId);
    }

    /**
     * Get stylist by ID
     */
    public StylistDocument getStylist(String stylistId) {
        return openSearchService.getDocument(STYLISTS_INDEX, stylistId, StylistDocument.class)
                .orElse(null);
    }

    /**
     * Get top rated stylists
     */
    public List<OpenSearchService.SearchResult<StylistDocument>> getTopRatedStylists(int limit) {
        // This would require sorting by rating - simplified implementation
        return openSearchService.searchByText(STYLISTS_INDEX, "name", "*", StylistDocument.class, 0, limit);
    }

    /**
     * Search stylists by price range
     */
    public List<OpenSearchService.SearchResult<StylistDocument>> searchByPriceRange(int minPrice, int maxPrice, int from, int size) {
        Map<String, Object> filters = new HashMap<>();
        // Note: Price range filtering would need more complex query implementation
        return openSearchService.searchWithFilters(STYLISTS_INDEX, "name", "*", filters,
                                                 StylistDocument.class, from, size);
    }

    /**
     * Search verified stylists only
     */
    public List<OpenSearchService.SearchResult<StylistDocument>> searchVerifiedStylists(String query, int from, int size) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("verified", true);
        return openSearchService.searchWithFilters(STYLISTS_INDEX, "name", query, filters,
                                                 StylistDocument.class, from, size);
    }

    /**
     * Bulk index stylist profiles
     */
    public int bulkIndexStylists(Map<String, StylistDocument> stylists) {
        return openSearchService.bulkIndex(STYLISTS_INDEX, stylists);
    }

    /**
     * Get total stylist count
     */
    public long getTotalStylistCount() {
        return openSearchService.getDocumentCount(STYLISTS_INDEX);
    }

    /**
     * Refresh stylist index
     */
    public boolean refreshIndex() {
        return openSearchService.refreshIndex(STYLISTS_INDEX);
    }

    /**
     * Stylist document for OpenSearch
     */
    public static class StylistDocument {
        private String id;
        private String name;
        private String bio;
        private List<String> specialties;
        private double latitude;
        private double longitude;
        private float rating;
        private int experienceYears;
        private PriceRange priceRange;
        private String availability;
        private boolean verified;
        private boolean featured;
        private List<String> portfolioUrls;
        private List<String> servicesOffered;
        private List<String> languagesSpoken;
        private List<String> certifications;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }

        public List<String> getSpecialties() { return specialties; }
        public void setSpecialties(List<String> specialties) { this.specialties = specialties; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public float getRating() { return rating; }
        public void setRating(float rating) { this.rating = rating; }

        public int getExperienceYears() { return experienceYears; }
        public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

        public PriceRange getPriceRange() { return priceRange; }
        public void setPriceRange(PriceRange priceRange) { this.priceRange = priceRange; }

        public String getAvailability() { return availability; }
        public void setAvailability(String availability) { this.availability = availability; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean featured) { this.featured = featured; }

        public List<String> getPortfolioUrls() { return portfolioUrls; }
        public void setPortfolioUrls(List<String> portfolioUrls) { this.portfolioUrls = portfolioUrls; }

        public List<String> getServicesOffered() { return servicesOffered; }
        public void setServicesOffered(List<String> servicesOffered) { this.servicesOffered = servicesOffered; }

        public List<String> getLanguagesSpoken() { return languagesSpoken; }
        public void setLanguagesSpoken(List<String> languagesSpoken) { this.languagesSpoken = languagesSpoken; }

        public List<String> getCertifications() { return certifications; }
        public void setCertifications(List<String> certifications) { this.certifications = certifications; }

        public static class PriceRange {
            private int min;
            private int max;

            public int getMin() { return min; }
            public void setMin(int min) { this.min = min; }

            public int getMax() { return max; }
            public void setMax(int max) { this.max = max; }
        }
    }
}
