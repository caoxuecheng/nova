package com.onescorpin.search.transform;

/*-
 * #%L
 * nova-search-elasticsearch
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.onescorpin.search.api.SearchIndex;
import com.onescorpin.search.rest.model.CategoryMetadataSearchResultData;
import com.onescorpin.search.rest.model.NflowMetadataSearchResultData;
import com.onescorpin.search.rest.model.HiveColumn;
import com.onescorpin.search.rest.model.Pair;
import com.onescorpin.search.rest.model.SchemaSearchResultData;
import com.onescorpin.search.rest.model.SearchResult;
import com.onescorpin.search.rest.model.SearchResultData;
import com.onescorpin.search.rest.model.SearchResultType;
import com.onescorpin.search.rest.model.TableSearchResultData;
import com.onescorpin.search.rest.model.UnknownTypeSearchResultData;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform Elasticsearch result to REST model
 */
public class ElasticSearchSearchResultTransform {

    private Long tableDataTypeResultCount = 0L;
    private Long schemaTypeResultCount = 0L;
    private Long nflowsMetadataTypeResultCount = 0L;
    private Long categoriesMetadataTypeResultCount = 0L;
    private Long unknownTypeResultCount = 0L;

    public SearchResult transformResult(String query, int size, int start, SearchResponse searchResponse) {
        final String NOVA_DATA = "nova-data";
        final String NOVA_SCHEMA_METADATA = "nova-schema-metadata";
        final String NOVA_NFLOWS = "nova-nflows";
        final String NOVA_CATEGORIES = "nova-categories";
        final String ELASTIC_SEARCH = "Elasticsearch";

        List<SearchResultData> searchResultData = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            if (searchHit.getIndex().equals(NOVA_DATA)) {
                searchResultData.add(getTableSearchResultData(searchHit));
            } else if (searchHit.getIndex().equals(SearchIndex.DATASOURCES)) {
                searchResultData.add(getSchemaSearchResultData(searchHit));
            } else if (searchHit.getIndex().contains(NOVA_NFLOWS)) {
                // This is dependent upon ModeShape configuration. The prefix will remain the same.
                // Hence only checking part of index name.
                searchResultData.add(getNflowMetadataSearchResultData(searchHit));
            } else if (searchHit.getIndex().contains(NOVA_CATEGORIES)) {
                // This is dependent upon ModeShape configuration. The prefix will remain the same.
                // Hence only checking part of index name.
                searchResultData.add(getCategoryMetadataSearchResultData(searchHit));
            } else {
                searchResultData.add(getUnknownTypeSearchResultData(searchHit));
            }
        }

        SearchResult elasticSearchResult = new SearchResult();
        elasticSearchResult.setQuery(query);
        elasticSearchResult.setTotalHits(searchResponse.getHits().getTotalHits());
        elasticSearchResult.setFrom((long) (start + 1));
        elasticSearchResult.setTo((long) (start + size));

        if (elasticSearchResult.getTotalHits() < (start + size)) {
            elasticSearchResult.setTo(elasticSearchResult.getTotalHits());
        }

        if (elasticSearchResult.getTotalHits() == 0) {
            elasticSearchResult.setFrom(0L);
        }

        elasticSearchResult.setTookInMillis(searchResponse.getTookInMillis());
        elasticSearchResult.setEngine(ELASTIC_SEARCH);
        elasticSearchResult.setSearchResults(searchResultData);

        elasticSearchResult.setSearchResultsSummary(getSearchResultSummary());

        return elasticSearchResult;
    }

    private TableSearchResultData getTableSearchResultData(SearchHit searchHit) {
        final String NOVA_SCHEMA = "nova_schema";
        final String NOVA_TABLE = "nova_table";
        final String POST_DATE = "post_date";

        Map<String, Object> searchHitSourceMap = searchHit.sourceAsMap();
        TableSearchResultData tableSearchResultData = new TableSearchResultData();
        tableSearchResultData.setSchemaName(searchHitSourceMap.get(NOVA_SCHEMA).toString());
        tableSearchResultData.setTableName(searchHitSourceMap.get(NOVA_TABLE).toString());
        List<Pair> columnNamesAndValues = new ArrayList<>();
        List<Pair> highlightsList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : searchHitSourceMap.entrySet()) {
            String key = entry.getKey();
            if (!((key.equals(POST_DATE)) || (key.equals(NOVA_SCHEMA)) || (key.equals(NOVA_TABLE)))) {
                columnNamesAndValues.add(new Pair(key, entry.getValue()));
            }
        }

        Map<String, HighlightField> highlights = searchHit.getHighlightFields();
        for (Map.Entry<String, HighlightField> entry : highlights.entrySet()) {
            if (entry.getValue().getFragments().length >= 1) {
                highlightsList.add(new Pair(entry.getKey(), entry.getValue().getFragments()[0].toString()));
            }
        }

        tableSearchResultData.setHighlights(highlightsList);
        tableSearchResultData.setColumnNamesAndValues(columnNamesAndValues);
        tableSearchResultData.setRawData(searchHit.getSource());
        tableDataTypeResultCount += 1L;
        return tableSearchResultData;
    }

    private SchemaSearchResultData getSchemaSearchResultData(SearchHit searchHit) {
        final String DATABASE_NAME = "databaseName";
        final String DATABASE_OWNER = "databaseOwner";
        final String TABLE_CREATE_TIME = "tableCreateTime";
        final String TABLE_NAME = "tableName";
        final String TABLE_TYPE = "tableType";
        final String HIVE_COLUMNS = "hiveColumns";
        final String COLUMN_TYPE = "columnType";
        final String COLUMN_NAME = "columnName";
        final String COLUMN_COMMENT = "columnComment";

        final String HIVE_COLUMNS_COLUMN_TYPE = HIVE_COLUMNS + "." + COLUMN_TYPE;
        final String HIVE_COLUMNS_COLUMN_TYPE_NEW_DESCRIPTION = "Column type";
        final String HIVE_COLUMNS_COLUMN_NAME = HIVE_COLUMNS + "." + COLUMN_NAME;
        final String HIVE_COLUMNS_COLUMN_NAME_NEW_DESCRIPTION = "Column name";
        final String HIVE_COLUMNS_COLUMN_COMMENT = HIVE_COLUMNS + "." + COLUMN_COMMENT;
        final String HIVE_COLUMNS_COLUMN_COMMENT_NEW_DESCRIPTION = "Column comment";

        Map<String, Object> searchHitSourceMap = searchHit.sourceAsMap();
        SchemaSearchResultData schemaSearchResultData = new SchemaSearchResultData();
        schemaSearchResultData.setDatabaseName(searchHitSourceMap.getOrDefault(DATABASE_NAME, "").toString());
        schemaSearchResultData.setDatabaseOwner(searchHitSourceMap.getOrDefault(DATABASE_OWNER, "").toString());
        schemaSearchResultData.setTableCreateTime(searchHitSourceMap.getOrDefault(TABLE_CREATE_TIME, "").toString());
        schemaSearchResultData.setTableName(searchHitSourceMap.getOrDefault(TABLE_NAME, "").toString());
        schemaSearchResultData.setTableType(searchHitSourceMap.getOrDefault(TABLE_TYPE, "").toString());

        List<HiveColumn> hiveColumns = new ArrayList<>();
        List<Pair> highlightsList = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> hiveColumnsListOfMaps = (List<Map<String, String>>) searchHitSourceMap.getOrDefault(HIVE_COLUMNS, Collections.emptyList());
        for (Map<String, String> hiveColumnsMap : hiveColumnsListOfMaps) {
            String columnName = "";
            String columnType = "";
            String columnComment = "";

            for (Map.Entry<String, String> hiveColumnEntry : hiveColumnsMap.entrySet()) {
                if (hiveColumnEntry.getKey().equals(COLUMN_TYPE)) {
                    columnType = hiveColumnEntry.getValue();
                } else if (hiveColumnEntry.getKey().equals(COLUMN_NAME)) {
                    columnName = hiveColumnEntry.getValue();
                } else if (hiveColumnEntry.getKey().equals(COLUMN_COMMENT)) {
                    columnComment = hiveColumnEntry.getValue();
                }
            }

            hiveColumns.add(new HiveColumn(columnName, columnType, columnComment));
        }

        Map<String, HighlightField> highlights = searchHit.getHighlightFields();
        for (Map.Entry<String, HighlightField> entry : highlights.entrySet()) {
            String key = entry.getKey();
            switch (key) {
                case HIVE_COLUMNS_COLUMN_TYPE:
                    key = HIVE_COLUMNS_COLUMN_TYPE_NEW_DESCRIPTION;
                    break;
                case HIVE_COLUMNS_COLUMN_NAME:
                    key = HIVE_COLUMNS_COLUMN_NAME_NEW_DESCRIPTION;
                    break;
                case HIVE_COLUMNS_COLUMN_COMMENT:
                    key = HIVE_COLUMNS_COLUMN_COMMENT_NEW_DESCRIPTION;
                    break;
            }

            if (entry.getValue().getFragments().length >= 1) {
                highlightsList.add(new Pair(key, entry.getValue().getFragments()[0].toString()));
            }
        }
        schemaSearchResultData.setHighlights(highlightsList);
        schemaSearchResultData.setHiveColumns(hiveColumns);
        schemaSearchResultData.setRawData(searchHit.getSource());
        schemaTypeResultCount += 1L;
        return schemaSearchResultData;
    }

    private NflowMetadataSearchResultData getNflowMetadataSearchResultData(SearchHit searchHit) {
        //jcr has no notion of null-valued property. So, account for the situation when there is no tag present.
        final String TBA_SYSTEM_NAME = "tba:systemName";
        final String JCR_TITLE = "jcr:title";
        final String JCR_DESCRIPTION = "jcr:description";
        final String TBA_CATEGORY = "tba:category";
        final String TBA_TAGS = "tba:tags";

        final String TBA_SYSTEM_NAME_NEW_DESCRIPTION = "System name (Nova)";
        final String JCR_TITLE_NEW_DESCRIPTION = "Title";
        final String JCR_DESCRIPTION_NEW_DESCRIPTION = "Description";
        final String TBA_CATEGORY_NEW_DESCRIPTION = "Category";
        final String TBA_TAGS_NEW_DESCRIPTION = "Tags";

        final String SPACE_STRING = " ";

        Map<String, Object> searchHitSourceMap = searchHit.sourceAsMap();
        NflowMetadataSearchResultData nflowMetadataSearchResultData = new NflowMetadataSearchResultData();
        List<Pair> highlightsList = new ArrayList<>();

        if (searchHitSourceMap.containsKey(TBA_SYSTEM_NAME)) {
            nflowMetadataSearchResultData.setNflowSystemName(searchHitSourceMap.get(TBA_SYSTEM_NAME).toString());
        }

        if (searchHitSourceMap.containsKey(JCR_TITLE)) {
            nflowMetadataSearchResultData.setNflowTitle(searchHitSourceMap.get(JCR_TITLE).toString());
        }

        if (searchHitSourceMap.containsKey(JCR_DESCRIPTION)) {
            nflowMetadataSearchResultData.setNflowDescription(searchHitSourceMap.get(JCR_DESCRIPTION).toString());
        }

        if (searchHitSourceMap.containsKey(TBA_CATEGORY)) {
            nflowMetadataSearchResultData.setNflowCategoryId(searchHitSourceMap.get(TBA_CATEGORY).toString());
        }

        if (searchHitSourceMap.containsKey(TBA_TAGS)) {
            nflowMetadataSearchResultData.setNflowTags(Arrays.asList(searchHitSourceMap
                                                                       .get(TBA_TAGS)
                                                                       .toString()
                                                                       .split(SPACE_STRING)));
        }

        Map<String, HighlightField> highlights = searchHit.getHighlightFields();
        for (Map.Entry<String, HighlightField> entry : highlights.entrySet()) {
            String key = entry.getKey();
            Boolean includeHighlight = false;

            switch (key) {
                case JCR_TITLE:
                    key = JCR_TITLE_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case JCR_DESCRIPTION:
                    key = JCR_DESCRIPTION_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case TBA_TAGS:
                    key = TBA_TAGS_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case TBA_CATEGORY:
                    key = TBA_CATEGORY_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case TBA_SYSTEM_NAME:
                    key = TBA_SYSTEM_NAME_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
            }

            if (includeHighlight && entry.getValue().getFragments().length >= 1) {
                highlightsList.add(new Pair(key, entry.getValue().getFragments()[0].toString()));
            }
        }

        nflowMetadataSearchResultData.setHighlights(highlightsList);
        nflowMetadataSearchResultData.setRawData(searchHit.getSource());
        nflowsMetadataTypeResultCount += 1L;
        return nflowMetadataSearchResultData;
    }

    private CategoryMetadataSearchResultData getCategoryMetadataSearchResultData(SearchHit searchHit) {
        //jcr has no notion of null-valued property. So, account for the situation when there is no tag present.
        final String TBA_SYSTEM_NAME = "tba:systemName";
        final String JCR_TITLE = "jcr:title";
        final String JCR_DESCRIPTION = "jcr:description";

        final String TBA_SYSTEM_NAME_NEW_DESCRIPTION = "System name (Nova)";
        final String JCR_TITLE_NEW_DESCRIPTION = "Title";
        final String JCR_DESCRIPTION_NEW_DESCRIPTION = "Description";

        Map<String, Object> searchHitSourceMap = searchHit.sourceAsMap();
        CategoryMetadataSearchResultData categoryMetadataSearchResultData = new CategoryMetadataSearchResultData();
        List<Pair> highlightsList = new ArrayList<>();

        if (searchHitSourceMap.containsKey(TBA_SYSTEM_NAME)) {
            categoryMetadataSearchResultData.setCategorySystemName(searchHitSourceMap.get(TBA_SYSTEM_NAME).toString());
        }

        if (searchHitSourceMap.containsKey(JCR_TITLE)) {
            categoryMetadataSearchResultData.setCategoryTitle(searchHitSourceMap.get(JCR_TITLE).toString());
        }

        if (searchHitSourceMap.containsKey(JCR_DESCRIPTION)) {
            categoryMetadataSearchResultData.setCategoryDescription(searchHitSourceMap.get(JCR_DESCRIPTION).toString());
        }

        Map<String, HighlightField> highlights = searchHit.getHighlightFields();
        for (Map.Entry<String, HighlightField> entry : highlights.entrySet()) {
            String key = entry.getKey();
            Boolean includeHighlight = false;

            switch (key) {
                case JCR_TITLE:
                    key = JCR_TITLE_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case JCR_DESCRIPTION:
                    key = JCR_DESCRIPTION_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
                case TBA_SYSTEM_NAME:
                    key = TBA_SYSTEM_NAME_NEW_DESCRIPTION;
                    includeHighlight = true;
                    break;
            }

            if (includeHighlight && entry.getValue().getFragments().length >= 1) {
                highlightsList.add(new Pair(key, entry.getValue().getFragments()[0].toString()));
            }
        }

        categoryMetadataSearchResultData.setHighlights(highlightsList);
        categoryMetadataSearchResultData.setRawData(searchHit.getSource());
        categoriesMetadataTypeResultCount += 1L;
        return categoryMetadataSearchResultData;
    }

    private UnknownTypeSearchResultData getUnknownTypeSearchResultData(SearchHit searchHit) {
        UnknownTypeSearchResultData unknownTypeSearchResultData = new UnknownTypeSearchResultData();
        unknownTypeSearchResultData.setRawData(searchHit.getSource());
        unknownTypeResultCount += 1L;
        return unknownTypeSearchResultData;
    }


    private Map<SearchResultType, Long> getSearchResultSummary() {
        Map<SearchResultType, Long> searchResultSummary = new HashMap<>();

        if (tableDataTypeResultCount > 0) {
            searchResultSummary.put(SearchResultType.NOVA_DATA, tableDataTypeResultCount);
        }

        if (schemaTypeResultCount > 0) {
            searchResultSummary.put(SearchResultType.NOVA_SCHEMA, schemaTypeResultCount);
        }

        if (nflowsMetadataTypeResultCount > 0) {
            searchResultSummary.put(SearchResultType.NOVA_NFLOWS, nflowsMetadataTypeResultCount);
        }

        if (categoriesMetadataTypeResultCount > 0) {
            searchResultSummary.put(SearchResultType.NOVA_CATEGORIES, categoriesMetadataTypeResultCount);
        }

        if (unknownTypeResultCount > 0) {
            searchResultSummary.put(SearchResultType.NOVA_UNKNOWN, unknownTypeResultCount);
        }

        return searchResultSummary;
    }

}
