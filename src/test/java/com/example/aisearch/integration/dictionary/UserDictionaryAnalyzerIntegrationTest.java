package com.example.aisearch.integration.dictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

class UserDictionaryAnalyzerIntegrationTest extends DictionaryIntegrationTestBase {

    @Test
    void with_yalpi_적용시_얄피가_단일_토큰으로_유지된다() throws Exception {
        switchUserDictFixture("user_dict_ko.with-yalpi.txt");
        rollOut();

        List<String> tokens = analyze("얄피 만두");

        assertContainsToken(tokens, "얄피");
        assertContainsToken(tokens, "만두");
        assertNotContainsToken(tokens, "얄");
        assertNotContainsToken(tokens, "피");
    }

    @Test
    void without_yalpi_적용시_얄피가_분리되어_분석된다() throws Exception {
        switchUserDictFixture("user_dict_ko.without-yalpi.txt");
        rollOut();

        List<String> tokens = analyze("얄피 만두");

        assertContainsToken(tokens, "얄");
        assertContainsToken(tokens, "피");
        assertContainsToken(tokens, "만두");
        assertNotContainsToken(tokens, "얄피");
    }
}
