package com.example.aisearch.integration.dictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

class StopwordAnalyzerIntegrationTest extends DictionaryIntegrationTestBase {

    @Test
    void with_stopword_적용시_식감이_제거된다() throws Exception {
        switchStopwordFixture("stopword.with-stopword.txt");
        rollOut();

        List<String> tokens = analyze("바삭한 식감 만두");

        assertNotContainsToken(tokens, "식감");
        assertContainsToken(tokens, "만두");
    }

    @Test
    void without_stopword_적용시_식감이_유지된다() throws Exception {
        switchStopwordFixture("stopword.without-stopword.txt");
        rollOut();

        List<String> tokens = analyze("바삭한 식감 만두");

        assertContainsToken(tokens, "식감");
        assertContainsToken(tokens, "만두");
    }
}
