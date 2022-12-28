package de.julielab.smithsearch.services;

import com.google.gson.Gson;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.LowerCaseFilter;
import de.julielab.jcore.consumer.es.filter.SnowballFilter;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.smithsearch.data.SearchHit;
import de.julielab.smithsearch.data.SearchRequest;
import de.julielab.smithsearch.data.SearchResponse;
import de.julielab.smithsearch.data.Sorting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class SearchServiceImplIntegrationTest {
    public static final Path MAPPING_PATH = Path.of("src", "main", "resources", "esMappingFile.json");
    public static final String TEST_INDEX = "testindex";
    public static final String TEST_CLUSTER = "testcluster";
    private final static Logger log = LoggerFactory.getLogger(SearchServiceImplIntegrationTest.class);

    @Container
    private static final GenericContainer es = new GenericContainer(
            new ImageFromDockerfile("smithsearch-it", true)
                    .withFileFromClasspath("Dockerfile", "dockercontext/Dockerfile")
                    .withFileFromClasspath("elasticsearch-mapper-preanalyzed-7.17.8.zip", "dockercontext/elasticsearch-mapper-preanalyzed-7.17.8.zip"))
            .withExposedPorts(9200)
            .withEnv("cluster.name", TEST_CLUSTER)
            .withEnv("discovery.type", "single-node");

    @Autowired
    private SearchServiceImpl searchService;


    @DynamicPropertySource
    static void registerEsConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + es.getHost() + ":" + es.getMappedPort(9200));
    }


    @AfterEach
    void tearDown() throws Exception {
        deleteIndex(TEST_INDEX);
    }

    @Test
    public void search() throws Exception {
        putEsMapping(MAPPING_PATH);
        indexTestDocuments(10);
        final SearchRequest searchRequest = new SearchRequest("Aspirin", 2, 5, true);
        searchRequest.setSorting(List.of(new Sorting(SearchServiceImpl.FIELD_DOCID, Sorting.Order.asc)));
        final SearchResponse searchResponse = searchService.search(searchRequest);
        assertEquals(5, searchResponse.getHits().size());
        assertThat(searchResponse.getHits()).extracting(SearchHit::getDocId).containsExactlyInAnyOrder("2", "3", "4", "5", "6");
        assertEquals(10, searchResponse.getNumHits());
        assertEquals("<em>Aspirin</em> wirkt auch in Dokument nr. 2 gut gegen Kopfschmerzen.", searchResponse.getHits().get(0).getText());
    }

    @Test
    public void searchIds() throws Exception {
        putEsMapping(MAPPING_PATH);
        indexTestDocuments(10);
        final SearchRequest searchRequest = new SearchRequest("id2", 7, 2, true);
        searchRequest.setSorting(List.of(new Sorting(SearchServiceImpl.FIELD_DOCID, Sorting.Order.asc)));
        final SearchResponse searchResponse = searchService.search(searchRequest);
        assertEquals(2, searchResponse.getHits().size());
        assertThat(searchResponse.getHits()).extracting(SearchHit::getDocId).containsExactlyInAnyOrder("7", "8");
        assertEquals(10, searchResponse.getNumHits());
        assertEquals("Aspirin wirkt auch in Dokument nr. 7 gut gegen <em>Kopfschmerzen</em>.", searchResponse.getHits().get(0).getText());
    }

    private void deleteIndex(String indexName) throws Exception {
        URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + indexName);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestMethod("DELETE");
        log.debug("Response for index deletion: {}", urlConnection.getResponseMessage());
        if (urlConnection.getErrorStream() != null)
            log.debug("Error messages for index deletion: {}", IOUtils.toString(urlConnection.getErrorStream(), UTF_8));
    }

    private void indexTestDocuments(int numDocs) throws IOException, InterruptedException {
        List<String> bulkCommandLines = new ArrayList<>();
        final Map<String, String> entityIds = Map.of("Aspirin", "id1", "Kopfschmerzen", "id2");
        ObjectMapper om = new ObjectMapper();
        for (int i = 0; i < numDocs; i++) {
            Map<String, Object> doc = Map.of(SearchServiceImpl.FIELD_TEXT, convertToSerializedPreanalyzedFieldValue("Aspirin wirkt auch in Dokument nr. " + i + " gut gegen Kopfschmerzen.", entityIds), SearchServiceImpl.FIELD_DOCID, String.valueOf(i));
            String jsonContents = om.writeValueAsString(doc);
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("_index", TEST_INDEX);
            indexMap.put("_id", doc.get(SearchServiceImpl.FIELD_DOCID));
            Map<String, Object> map = new HashMap<>();
            map.put("index", indexMap);

            bulkCommandLines.add(om.writeValueAsString(map));
            bulkCommandLines.add(jsonContents);
        }
        log.info("Indexing test documents");
        URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/_bulk");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        OutputStream outputStream = urlConnection.getOutputStream();
        IOUtils.writeLines(bulkCommandLines, System.getProperty("line.separator"), outputStream, "UTF-8");
        log.debug("Response for indexing: {}", urlConnection.getResponseMessage());
        if (urlConnection.getErrorStream() != null)
            log.debug("Error messages for indexing: {}", IOUtils.toString(urlConnection.getErrorStream(), UTF_8));

        Thread.sleep(2000);
        URL countUrl = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX + "/_count");
        HttpURLConnection countUrlConnection = (HttpURLConnection) countUrl.openConnection();
        String countResponse = IOUtils.toString(countUrlConnection.getInputStream(), UTF_8);
        log.debug("Response for the count of documents: {}", countResponse);
        assertTrue(countResponse.contains("count\":" + numDocs), "Count returned unexpected response: " + countResponse);
    }

    private void putEsMapping(Path mappingPath) throws Exception {
        // Create the test index
        URL url = new URL("http://localhost:" + es.getMappedPort(9200) + "/" + TEST_INDEX);
        String mapping = IOUtils.toString(mappingPath.toFile().toURI(), UTF_8);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("PUT");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);
        IOUtils.write(mapping, urlConnection.getOutputStream(), UTF_8);
        log.info("Response for index creation: {}", urlConnection.getResponseMessage());

        if (urlConnection.getErrorStream() != null) {
            String error = IOUtils.toString(urlConnection.getErrorStream(), UTF_8);
            log.error("Error when creating index: {}", error);
        }
    }

    /**
     * <p>Converts the given string to a preanalyzed field value in JSON format.</p>
     * <p>The input string is tokenized in a simple way, lower-cased and stemmed.</p>
     * <p>Additional tokens are added when a token of the input is also the key of the given map. Then, the map value is used as the term for a token that is "stacked" onto the original token.</p>
     * <p>For ElasticSearch to accept such a value, the JULIE Lab PreanalyzedFieldMapper must be installed as an
     * ElasticSearch plugin.</p>
     * @param input Some text.
     * @param additionalTokens Map from token strings that should be the result of the tokenization of the input to terms that should be stacked onto the token, e.g. entity IDs.
     * @return The serialized PreanalyzedFieldValue in JSON format.
     */
    private String convertToSerializedPreanalyzedFieldValue(String input, Map<String, String> additionalTokens) {
        final FilterChain tokenFilter = new FilterChain(new LowerCaseFilter(), new SnowballFilter("org.tartarus.snowball.ext.German2Stemmer"));
        // We need Gson here because the PreanalyzedFieldValue (used at the very end of the method) uses Gson
        // annotations for serialization into the correct JSON format. Jackson is currently not supported.
        final Gson gson = new Gson();
        final Pattern tokenizer = Pattern.compile("([^\s\\p{P}]+)|(\\p{P}+)");
        final Matcher tokenMatcher = tokenizer.matcher(input);
        final List<PreanalyzedToken> tokens = new ArrayList<>();
        while (tokenMatcher.find()) {
            for (int i = 0; i < tokenMatcher.groupCount(); i++) {
                int groupNum = i + 1;
                if (tokenMatcher.group(groupNum) != null) {
                    final PreanalyzedToken t = new PreanalyzedToken();
                    final String tokenString = tokenMatcher.group(groupNum);
                    final String filteredToken = tokenFilter.filter(tokenString).get(0);
                    t.start = tokenMatcher.start(groupNum);
                    t.end = tokenMatcher.end(groupNum);
                    t.term = filteredToken;
                    System.out.println(t.term);
                    tokens.add(t);
                    // We use the original token here so it is easier to create the map.
                    final String additionalTerm = additionalTokens.get(tokenString);
                    if (additionalTerm != null) {
                        // 'additional tokens' are tokens that are stacked at the same position as the original token with the same
                        // offsets. In this way, one can search for the additional term and highlight the original term
                        final PreanalyzedToken additionalToken = new PreanalyzedToken();
                        additionalToken.term = additionalTerm;
                        additionalToken.start = t.start;
                        additionalToken.end = t.end;
                        // the positionIncrement is key: normal is 1, 0 zero means that this token and the previous token
                        // are at the same position
                        additionalToken.positionIncrement = 0;
                        tokens.add(additionalToken);
                    }
                }
            }
        }
        return gson.toJson(new PreanalyzedFieldValue(input, tokens));
    }

}
