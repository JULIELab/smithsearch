package de.julielab.smithsearch;

import com.google.gson.Gson;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.smithsearch.services.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyTest {
    @Test
    public void test() {
        final Map<String, String> entityIds = Map.of("Aspirin", "id1", "Kopfschmerzen", "id2");
        final String s = convertToSerializedPreanalyzedFieldValue("Aspirin wirkt auch in Dokument nr. " + 1 + " gut gegen Kopfschmerzen.", entityIds);
        System.out.println(s);
    }

    private String convertToSerializedPreanalyzedFieldValue(String input, Map<String, String> additionalTokens) {
        final Gson gson = new Gson();
        final Pattern tokenizer = Pattern.compile("([^\s\\p{P}]+)|(\\p{P}+)");
        final Matcher tokenMatcher = tokenizer.matcher(input);
        final List<PreanalyzedToken> token = new ArrayList<>();
        while (tokenMatcher.find()) {
            for (int i = 0; i < tokenMatcher.groupCount(); i++) {
                int groupNum = i + 1;
                if (tokenMatcher.group(groupNum) != null) {
                    System.out.println(groupNum + ": " + tokenMatcher.group(groupNum) + " " + tokenMatcher.start(groupNum) + "-" + tokenMatcher.end(groupNum));
                    final PreanalyzedToken t = new PreanalyzedToken();
                    t.start = tokenMatcher.start(groupNum);
                    t.end = tokenMatcher.end(groupNum);
                    t.term = tokenMatcher.group(groupNum);
                    token.add(t);
                    final String additionalTerm = additionalTokens.get(t.term);
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
                        token.add(additionalToken);
                    }
                }
            }
        }
        return gson.toJson(new PreanalyzedFieldValue(input, token));
    }
}
