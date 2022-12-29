package de.julielab.smithsearch.index;

import de.julielab.jcore.consumer.es.ArrayFieldValue;
import de.julielab.jcore.consumer.es.FilterRegistry;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Token;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
class SmithSearchFieldGeneratorTest {

    @Test
    void addFields() throws Exception{
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-types", "de.julielab.jcore.types.jcore-semantics-biology-types");
        jCas.setDocumentText("Aspirin hilft auch gegen Schmerzen in den Beinen.");
        tokenize(jCas);
        final Header h = new Header(jCas);
        h.setDocId("1234");
        h.addToIndexes();

        final EntityMention aspirin = new EntityMention(jCas, 0, 8);
        aspirin.setSpecificType("id1");
        aspirin.addToIndexes();

        final EntityMention schmerzen = new EntityMention(jCas, 25, 48);
        schmerzen.setSpecificType("id2");
        schmerzen.addToIndexes();

        final SmithSearchFieldGenerator generator = new SmithSearchFieldGenerator(new FilterRegistry(UimaContextFactory.createUimaContext()));
        final Document doc = generator.addFields(jCas, new Document());

        assertThat(doc).containsKeys("text", "doc_id", "entity_ids");
        assertThat(doc.get("text")).isInstanceOf(PreanalyzedFieldValue.class).extracting("fieldString").isEqualTo("Aspirin hilft auch gegen Schmerzen in den Beinen.");
        assertThat(doc.get("text")).extracting("tokens").isNotNull().isInstanceOf(List.class);
        final List<PreanalyzedToken> tokens = ((PreanalyzedFieldValue) doc.get("text")).tokens;
        assertThat(tokens.get(0)).extracting(t -> t.term).isEqualTo("aspirin");
        assertThat(tokens.get(1)).extracting(t -> t.term).isEqualTo("id1");
        assertThat(tokens.get(1)).extracting(t -> t.start).isEqualTo(0);
        assertThat(tokens.get(1)).extracting(t -> t.end).isEqualTo(8);
        assertThat(tokens.get(1)).extracting(t -> t.positionIncrement).isEqualTo(0);

        assertThat(doc.get("entity_ids")).asInstanceOf(InstanceOfAssertFactories.list(RawToken.class)).flatExtracting("token").containsExactly("id1", "id2");
    }

    private void tokenize(JCas jCas) {
        final Matcher m = Pattern.compile("[a-zA-Z0-9]+").matcher(jCas.getDocumentText());
        while (m.find()) {
            new Token(jCas, m.start(), m.end()).addToIndexes();
        }
    }
}