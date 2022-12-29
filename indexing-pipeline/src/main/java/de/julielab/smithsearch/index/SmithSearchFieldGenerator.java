package de.julielab.smithsearch.index;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.LowerCaseFilter;
import de.julielab.jcore.consumer.es.filter.SnowballFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

public class SmithSearchFieldGenerator extends FieldGenerator {
    public SmithSearchFieldGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
    }

    @Override
    public Document addFields(JCas aJCas, Document doc) throws FieldGenerationException {
        final String docId = JCoReTools.getDocId(aJCas);
        if (docId == null)
            throw new FieldGenerationException(new IllegalArgumentException("CAS does not have a docId."));

        final FeaturePathSets entityIdFeatureSet = new FeaturePathSets(new FeaturePathSet(Token.type, List.of("/:coveredText()")), new FeaturePathSet(EntityMention.type, List.of("/specificType")));
        final List<PreanalyzedToken> preanalyzedTokens;
        final FilterChain filterChain = new FilterChain(new LowerCaseFilter(), new SnowballFilter("org.tartarus.snowball.ext.German2Stemmer"));
        try {
            preanalyzedTokens = getTokensForAnnotationIndexes(entityIdFeatureSet, filterChain, true, PreanalyzedToken.class, null, null, aJCas);
        } catch (CASException e) {
            log.error("Creation of preanalyzed tokens failed.", e);
            throw new FieldGenerationException(e);
        }
        // Get the IDs of all entities in the document for faceting purposes.
        // We must use RawTokens instead of pure string. When it is pure strings, the ArrayFieldValue constructor would handle the list as one opaque value of type Object instead of a list of values.
        final List<RawToken> entityIds = JCasUtil.select(aJCas, EntityMention.class).stream().map(EntityMention::getSpecificType).map(RawToken::new).toList();
        final ArrayFieldValue entityIdFieldValue = new ArrayFieldValue(entityIds);
        final PreanalyzedFieldValue textFieldValue = new PreanalyzedFieldValue(aJCas.getDocumentText(), preanalyzedTokens);

        doc.addField("doc_id", docId);
        doc.addField("text", textFieldValue);
        doc.addField("entity_ids", entityIdFieldValue);

        return doc;
    }
}
