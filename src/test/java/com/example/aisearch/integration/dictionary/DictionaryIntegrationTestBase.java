package com.example.aisearch.integration.dictionary;

import com.example.aisearch.integration.helper.ElasticsearchIntegrationTestBase;
import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "ai-search.index-name=dictionary-it-products",
        "ai-search.read-alias=dictionary-it-products-read",
        "ai-search.synonyms-set=dictionary-it-synonyms"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DictionaryIntegrationTestBase extends ElasticsearchIntegrationTestBase {

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String USERDICT_DIR = ".local-nas/userdict";
    private static final String MALL_ANALYZER = "ko_mall_analyzer";

    private static final Path ACTIVE_USERDICT_PATH = Path.of(PROJECT_ROOT, USERDICT_DIR, "user_dict_ko.txt");
    private static final Path ACTIVE_STOPWORD_PATH = Path.of(PROJECT_ROOT, USERDICT_DIR, "stopword.txt");

    @Autowired
    protected IndexRolloutService indexRolloutService;

    private byte[] originalUserDict;
    private byte[] originalStopword;

    @BeforeAll
    void setUpDictionaryFixture() throws Exception {
        printIsolationConfig(getClass().getSimpleName());
        deleteAllVersionedIndices();
        originalUserDict = readBytesOrEmpty(ACTIVE_USERDICT_PATH);
        originalStopword = readBytesOrEmpty(ACTIVE_STOPWORD_PATH);
    }

    @AfterAll
    void restoreDictionaryFixture() throws Exception {
        restoreFile(ACTIVE_USERDICT_PATH, originalUserDict);
        restoreFile(ACTIVE_STOPWORD_PATH, originalStopword);
        deleteAllVersionedIndices();
    }

    protected void switchUserDictFixture(String fixtureFileName) throws IOException {
        replaceActiveFile(ACTIVE_USERDICT_PATH, Path.of(PROJECT_ROOT, USERDICT_DIR, fixtureFileName));
    }

    protected void switchStopwordFixture(String fixtureFileName) throws IOException {
        replaceActiveFile(ACTIVE_STOPWORD_PATH, Path.of(PROJECT_ROOT, USERDICT_DIR, fixtureFileName));
    }

    protected void rollOut() {
        indexRolloutService.rollOutFromSourceData();
    }

    protected List<String> analyze(String text) throws IOException {
        return analyzeTokens(MALL_ANALYZER, text);
    }

    protected void assertContainsToken(List<String> tokens, String token) {
        assertTrue(tokens.contains(token),
                "토큰 '" + token + "' 이 포함되어야 합니다. actual=" + tokens);
    }

    protected void assertNotContainsToken(List<String> tokens, String token) {
        assertFalse(tokens.contains(token),
                "토큰 '" + token + "' 이 포함되면 안 됩니다. actual=" + tokens);
    }

    private void replaceActiveFile(Path activePath, Path sourcePath) throws IOException {
        Files.copy(sourcePath, activePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private byte[] readBytesOrEmpty(Path path) throws IOException {
        return Files.exists(path) ? Files.readAllBytes(path) : new byte[0];
    }

    private void restoreFile(Path path, byte[] content) throws IOException {
        if (content.length == 0) {
            Files.deleteIfExists(path);
            return;
        }
        Files.write(path, content);
    }
}
