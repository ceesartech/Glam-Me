package tech.ceesar.glamme.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSearchService {

    private final org.opensearch.client.opensearch.OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;

    /**
     * Create index with mapping
     */
    public boolean createIndex(String indexName, Map<String, Object> mapping) {
        try {
            if (indexExists(indexName)) {
                log.info("Index {} already exists", indexName);
                return true;
            }

            Map<String, Property> propertyMapping = convertToPropertyMapping(mapping);
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .mappings(m -> m.properties(propertyMapping))
                    .build();

            var response = openSearchClient.indices().create(createIndexRequest);
            boolean acknowledged = response.acknowledged();

            if (acknowledged) {
                log.info("Created index: {}", indexName);
            }

            return acknowledged;

        } catch (Exception e) {
            log.error("Failed to create index: {}", indexName, e);
            return false;
        }
    }

    /**
     * Delete index
     */
    public boolean deleteIndex(String indexName) {
        try {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder()
                    .index(indexName)
                    .build();

            var response = openSearchClient.indices().delete(deleteIndexRequest);
            boolean acknowledged = response.acknowledged();

            if (acknowledged) {
                log.info("Deleted index: {}", indexName);
            }

            return acknowledged;

        } catch (Exception e) {
            log.error("Failed to delete index: {}", indexName, e);
            return false;
        }
    }

    /**
     * Check if index exists
     */
    public boolean indexExists(String indexName) {
        try {
            ExistsRequest existsRequest = new ExistsRequest.Builder()
                    .index(indexName)
                    .build();

            return openSearchClient.indices().exists(existsRequest).value();

        } catch (Exception e) {
            log.error("Failed to check if index exists: {}", indexName, e);
            return false;
        }
    }

    /**
     * Index a document
     */
    public <T> Optional<String> indexDocument(String indexName, String documentId, T document) {
        try {
            String jsonDocument = objectMapper.writeValueAsString(document);

            IndexRequest<JsonNode> indexRequest = new IndexRequest.Builder<JsonNode>()
                    .index(indexName)
                    .id(documentId)
                    .document(objectMapper.readTree(jsonDocument))
                    .build();

            var response = openSearchClient.index(indexRequest);
            String indexedId = response.id();

            log.debug("Indexed document {} in index {}", indexedId, indexName);
            return Optional.of(indexedId);

        } catch (Exception e) {
            log.error("Failed to index document in index: {}", indexName, e);
            return Optional.empty();
        }
    }

    /**
     * Get document by ID
     */
    public <T> Optional<T> getDocument(String indexName, String documentId, Class<T> documentType) {
        try {
            GetRequest getRequest = new GetRequest.Builder()
                    .index(indexName)
                    .id(documentId)
                    .build();

            var response = openSearchClient.get(getRequest, JsonNode.class);

            if (response.found()) {
                T document = objectMapper.treeToValue(response.source(), documentType);
                return Optional.of(document);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Failed to get document {} from index: {}", documentId, indexName, e);
            return Optional.empty();
        }
    }

    /**
     * Update document
     */
    public <T> boolean updateDocument(String indexName, String documentId, T document) {
        try {
            String jsonDocument = objectMapper.writeValueAsString(document);

            UpdateRequest<JsonNode, JsonNode> updateRequest = new UpdateRequest.Builder<JsonNode, JsonNode>()
                    .index(indexName)
                    .id(documentId)
                    .doc(objectMapper.readTree(jsonDocument))
                    .build();

            var response = openSearchClient.update(updateRequest, JsonNode.class);
            boolean updated = response.result().toString().contains("updated");

            if (updated) {
                log.debug("Updated document {} in index {}", documentId, indexName);
            }

            return updated;

        } catch (Exception e) {
            log.error("Failed to update document {} in index: {}", documentId, indexName, e);
            return false;
        }
    }

    /**
     * Delete document
     */
    public boolean deleteDocument(String indexName, String documentId) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest.Builder()
                    .index(indexName)
                    .id(documentId)
                    .build();

            var response = openSearchClient.delete(deleteRequest);
            boolean deleted = response.result().toString().contains("deleted");

            if (deleted) {
                log.debug("Deleted document {} from index {}", documentId, indexName);
            }

            return deleted;

        } catch (Exception e) {
            log.error("Failed to delete document {} from index: {}", documentId, indexName, e);
            return false;
        }
    }

    /**
     * Search documents with query
     */
    public <T> List<SearchResult<T>> searchDocuments(String indexName, Query query,
                                                    Class<T> documentType, int from, int size) {
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(query)
                    .from(from)
                    .size(size)
                    .sort(s -> s.field(f -> f.field("_score").order(SortOrder.Desc)))
                    .build();

            var response = openSearchClient.search(searchRequest, JsonNode.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        try {
                            T document = objectMapper.treeToValue(hit.source(), documentType);
                            return new SearchResult<>(hit.id(), document, hit.score().floatValue());
                        } catch (Exception e) {
                            log.error("Failed to deserialize search result", e);
                            return null;
                        }
                    })
                    .filter(result -> result != null)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to search documents in index: {}", indexName, e);
            return List.of();
        }
    }

    /**
     * Search with text query
     */
    public <T> List<SearchResult<T>> searchByText(String indexName, String field, String searchText,
                                                 Class<T> documentType, int from, int size) {
        Query query = new Query.Builder()
                .match(m -> m.field(field).query(FieldValue.of(searchText)))
                .build();

        return searchDocuments(indexName, query, documentType, from, size);
    }

    /**
     * Search with multiple fields
     */
    public <T> List<SearchResult<T>> searchMultiMatch(String indexName, List<String> fields, String searchText,
                                                      Class<T> documentType, int from, int size) {
        Query query = new Query.Builder()
                .multiMatch(m -> m.fields(fields).query(searchText))
                .build();

        return searchDocuments(indexName, query, documentType, from, size);
    }

    /**
     * Search with filters
     */
    public <T> List<SearchResult<T>> searchWithFilters(String indexName, String searchField, String searchText,
                                                      Map<String, Object> filters, Class<T> documentType,
                                                      int from, int size) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder()
                .must(m -> m.match(mt -> mt.field(searchField).query(FieldValue.of(searchText))));

        // Add filters
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field(filter.getKey()).value(FieldValue.of(filter.getValue().toString()))));
        }

        Query query = new Query.Builder().bool(boolQueryBuilder.build()).build();

        return searchDocuments(indexName, query, documentType, from, size);
    }

    /**
     * Bulk index documents
     */
    public <T> int bulkIndex(String indexName, Map<String, T> documents) {
        // Note: OpenSearch Java client bulk operations are more complex
        // This is a simplified version
        int successCount = 0;

        for (Map.Entry<String, T> entry : documents.entrySet()) {
            if (indexDocument(indexName, entry.getKey(), entry.getValue()).isPresent()) {
                successCount++;
            }
        }

        log.info("Bulk indexed {} documents in index {}", successCount, indexName);
        return successCount;
    }

    /**
     * Get search suggestions (autocomplete)
     */
    public List<String> getSuggestions(String indexName, String field, String prefix, int size) {
        try {
            Query query = new Query.Builder()
                    .prefix(p -> p.field(field).value(prefix))
                    .build();

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(query)
                    .size(size)
                    .source(s -> s.filter(f -> f.includes(field)))
                    .build();

            var response = openSearchClient.search(searchRequest, JsonNode.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        try {
                            return hit.source().get(field).asText();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(suggestion -> suggestion != null)
                    .distinct()
                    .toList();

        } catch (Exception e) {
            log.error("Failed to get suggestions for field: {}", field, e);
            return List.of();
        }
    }

    /**
     * Get document count
     */
    public long getDocumentCount(String indexName) {
        try {
            CountRequest countRequest = new CountRequest.Builder()
                    .index(indexName)
                    .build();

            var response = openSearchClient.count(countRequest);
            return response.count();

        } catch (Exception e) {
            log.error("Failed to get document count for index: {}", indexName, e);
            return 0;
        }
    }

    /**
     * Refresh index
     */
    public boolean refreshIndex(String indexName) {
        try {
            var response = openSearchClient.indices().refresh(r -> r.index(indexName));
            return response.shards().successful().intValue() > 0;

        } catch (Exception e) {
            log.error("Failed to refresh index: {}", indexName, e);
            return false;
        }
    }

    /**
     * Search result wrapper
     */
    public static class SearchResult<T> {
        private final String id;
        private final T document;
        private final float score;

        public SearchResult(String id, T document, float score) {
            this.id = id;
            this.document = document;
            this.score = score;
        }

        public String getId() { return id; }
        public T getDocument() { return document; }
        public float getScore() { return score; }
    }

    /**
     * Convert Map<String, Object> to Map<String, Property> for OpenSearch mappings
     */
    private Map<String, Property> convertToPropertyMapping(Map<String, Object> mapping) {
        Map<String, Property> propertyMapping = new HashMap<>();

        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldConfig = entry.getValue();

            // Convert field configuration to Property
            // This is a simplified conversion - you may need to handle different field types
            if (fieldConfig instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldMap = (Map<String, Object>) fieldConfig;
                String type = (String) fieldMap.get("type");

                if ("text".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.text(t -> t)));
                } else if ("keyword".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.keyword(k -> k)));
                } else if ("integer".equals(type) || "long".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.long_(l -> l)));
                } else if ("double".equals(type) || "float".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.double_(d -> d)));
                } else if ("boolean".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.boolean_(b -> b)));
                } else if ("date".equals(type)) {
                    propertyMapping.put(fieldName, Property.of(p -> p.date(d -> d)));
                } else {
                    // Default to text for unknown types
                    propertyMapping.put(fieldName, Property.of(p -> p.text(t -> t)));
                }
            }
        }

        return propertyMapping;
    }
}
