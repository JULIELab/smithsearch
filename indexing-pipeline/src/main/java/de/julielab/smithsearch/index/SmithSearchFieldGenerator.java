package de.julielab.smithsearch.index;

import de.julielab.jcore.consumer.es.*;
import de.julielab.jcore.consumer.es.filter.FilterChain;
import de.julielab.jcore.consumer.es.filter.LowerCaseFilter;
import de.julielab.jcore.consumer.es.filter.SnowballFilter;
import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedToken;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.cas.CASException;
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

        final FeaturePathSets entityIdFeatureSet = new FeaturePathSets(new FeaturePathSet(EntityMention.type, List.of("/specificType")));
        final List<PreanalyzedToken> preanalyzedTokens;
        final FilterChain filterChain = new FilterChain(new LowerCaseFilter(), new SnowballFilter("org.tartarus.snowball.ext.German2Stemmer"));
        try {
            preanalyzedTokens = getTokensForAnnotationIndexes(entityIdFeatureSet, filterChain, true, PreanalyzedToken.class, null, null, aJCas);
        } catch (CASException e) {
            log.error("Creation of preanalyzed tokens failed.", e);
            throw new FieldGenerationException(e);
        }
        final PreanalyzedFieldValue fieldValue = new PreanalyzedFieldValue(aJCas.getDocumentText(), preanalyzedTokens);
        doc.addField("doc_id", docId);
        doc.addField("text", fieldValue);
        return doc;
    }
}
