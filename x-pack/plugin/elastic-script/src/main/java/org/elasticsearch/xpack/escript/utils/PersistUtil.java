/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.utils;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.internal.Client;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.ActionListener;

public class PersistUtil {

    /**
     * Indexes a list of documents into the given target index using a bulk request.
     * Each document is pre-processed (flattened) so that any key containing a dot is removed.
     */
    public static void bulkIndexDocuments(Client client, String targetIndex, List<Map<String, Object>> documents, ActionListener<Void> listener) {
        BulkRequest bulkRequest = new BulkRequest();
        for (Map<String, Object> doc : documents) {
            Map<String, Object> flatDoc = flattenDocument(doc);
            // Create an index request with the flat document
            IndexRequest indexRequest = new IndexRequest(targetIndex).source(flatDoc);
            bulkRequest.add(indexRequest);
        }
        client.bulk(bulkRequest, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkResponse) {
                if (bulkResponse.hasFailures()) {
                    listener.onFailure(new RuntimeException("Bulk indexing failures: " + bulkResponse.buildFailureMessage()));
                } else {
                    listener.onResponse(null);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Flattens a document map. In this implementation we remove any keys that contain a dot,
     * assuming that those represent sub-fields (like "continent.keyword") that would conflict
     * with the mapping if indexed as top-level fields.
     */
    public static Map<String, Object> flattenDocument(Map<String, Object> doc) {
        Map<String, Object> flat = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // If the key contains a dot, skip it (or optionally rename it)
            if (key.contains(".")) {
                continue;
            }
            // If the value is a Map and it contains a "keyword" key, extract that value.
            if (value instanceof Map) {
                Map<?, ?> mapValue = (Map<?, ?>) value;
                if (mapValue.size() == 1 && mapValue.containsKey("keyword")) {
                    flat.put(key, mapValue.get("keyword"));
                } else {
                    // Otherwise, flatten recursively
                    flat.put(key, flattenDocument(castToMapOfStringObject(mapValue)));
                }
            } else if (value instanceof List) {
                // If the value is a list, flatten each element if necessary.
                List<Object> flatList = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        Map<?, ?> mapItem = (Map<?, ?>) item;
                        if (mapItem.size() == 1 && mapItem.containsKey("keyword")) {
                            flatList.add(mapItem.get("keyword"));
                        } else {
                            flatList.add(flattenDocument(castToMapOfStringObject(mapItem)));
                        }
                    } else {
                        flatList.add(item);
                    }
                }
                flat.put(key, flatList);
            } else {
                flat.put(key, value);
            }
        }
        return flat;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMapOfStringObject(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
