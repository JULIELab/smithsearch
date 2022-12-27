package de.julielab.smithsearch.services;

import de.julielab.smithsearch.data.SearchHit;
import de.julielab.smithsearch.data.SearchRequest;
import de.julielab.smithsearch.data.SearchResponse;
import de.julielab.smithsearch.data.Sorting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@Testcontainers
public class SearchServiceImplIntegrationTest {
    public static final Path MAPPING_PATH = Path.of("src", "main", "resources", "esMappingFile.json");
    public static final String TEST_INDEX = "gepi_testindex";
    public static final String TEST_CLUSTER = "gepi_testcluster";
    private final static Logger log = LoggerFactory.getLogger(SearchServiceImplIntegrationTest.class);
    @Container
    private static final GenericContainer es = new GenericContainer(
            new ImageFromDockerfile("smithsearch-it", true)
                    .withFileFromClasspath("Dockerfile", "dockercontext/Dockerfile"))
            .withExposedPorts(9200)
            .withEnv("cluster.name", TEST_CLUSTER)
            .withEnv("discovery.type", "single-node");

    @Autowired
    private SearchServiceImpl searchService;


    @DynamicPropertySource
    static void registerEsConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://"+es.getHost() + ":" + es.getMappedPort(9200));
    }

    @AfterEach
    void tearDown() {
        es.stop();
    }

    @Test
    public void search() throws Exception {
        putEsMapping(MAPPING_PATH);
        indexTestDocuments(10);
        final SearchRequest searchRequest = new SearchRequest("aspirin", 2, 5);
        searchRequest.setSorting(List.of(new Sorting(SearchServiceImpl.FIELD_DOCID, Sorting.Order.asc)));
        final SearchResponse searchResponse = searchService.search(searchRequest);
        assertEquals(5, searchResponse.getHits().size());
        assertThat(searchResponse.getHits()).extracting(SearchHit::getDocId).containsExactlyInAnyOrder("2", "3", "4", "5", "6");
        assertEquals(10, searchResponse.getNumHits());
    }

    private void indexTestDocuments(int numDocs) throws IOException, InterruptedException {
        List<String> bulkCommandLines = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();
        for (int i = 0; i < numDocs; i++) {
            Map<String, String> doc = Map.of(SearchServiceImpl.FIELD_TEXT, "Aspirin wirkt auch in Dokument nr. " + i + " gut gegen Kopfschmerzen.", SearchServiceImpl.FIELD_DOCID, String.valueOf(i));
            String jsonContents = om.writeValueAsString(doc);
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("_index", TEST_INDEX);
            indexMap.put("_id", doc.get(SearchServiceImpl.FIELD_DOCID));
            Map<String, Object> map = new HashMap<>();
            map.put("index", indexMap);

            bulkCommandLines.add(om.writeValueAsString(map));
            bulkCommandLines.add(jsonContents);
        }
        log.debug("Indexing test documents");
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
            assertTrue(countResponse.contains("count\":10"));
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
}
