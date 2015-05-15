/**
 * Copyright 2007-2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package de.tudarmstadt.ukp.dkpro.core.stanfordnlp;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.min;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.Messages;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.SegmenterBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBEscapingProcessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

/**
 * @author Richard Eckart de Castilho
 * @author Oliver Ferschke
 */
public
class StanfordSegmenter
extends SegmenterBase
{
    private static final Map<String, InternalTokenizerFactory> tokenizerFactories;
//    private static final Map<String, TreebankLanguagePack> languagePacks;

    static {
    	tokenizerFactories = new HashMap<String, InternalTokenizerFactory>();
        tokenizerFactories.put("ar", new InternalArabicTokenizerFactory());
    	tokenizerFactories.put("en", new InternalPTBTokenizerFactory());
        tokenizerFactories.put("es", new InternalSpanishTokenizerFactory());
        tokenizerFactories.put("fr", new InternalFrenchTokenizerFactory());
    	// The Negra tokenizer is not really a full tokenizer.
//    	tokenizerFactories.put("de", new InternalNegraPennTokenizerFactory());
    	// Not sure if those really work - don't know how to test
//    	tokenizerFactories.put("zh", new InternalCHTBTokenizerFactory());

//    	languagePacks = new HashMap<String, TreebankLanguagePack>();
//    	languagePacks.put("en", new PennTreebankLanguagePack());
//    	languagePacks.put("zh", new ChineseTreebankLanguagePack());
//    	languagePacks.put("en", new ArabicTreebankLanguagePack());
//    	languagePacks.put("de", new NegraPennLanguagePack());
    }

    public static final String PARAM_LANGUAGE_FALLBACK = "languageFallback";
    @ConfigurationParameter(name = PARAM_LANGUAGE_FALLBACK, mandatory = false)
    private String languageFallback;

    @Override
	protected void process(JCas aJCas, String aText, int aZoneBegin)
		throws AnalysisEngineProcessException
    {
        List<Token> casTokens = null;

        // Use value from language parameter, document language or fallback language - whatever
        // is available
        String language = getLanguage(aJCas);
        
        if (isWriteToken()) {
            casTokens = new ArrayList<Token>();
            final String text = aText;
            final Tokenizer<?> tokenizer = getTokenizer(language, aText);
            int offsetInSentence = 0;

            List<?> tokens = tokenizer.tokenize();
            outer: for (int i = 0; i < tokens.size(); i++) {
                final Object token = tokens.get(i);
                // System.out.println("Token class: "+token.getClass());
                String t = null;
                if (token instanceof String) {
                    t = (String) token;
                }
                if (token instanceof CoreLabel) {
                    CoreLabel l = (CoreLabel) token;
                    t = l.word();
                    int begin = l.get(CharacterOffsetBeginAnnotation.class);
                    int end = l.get(CharacterOffsetEndAnnotation.class);

                    casTokens.add(createToken(aJCas, aZoneBegin + begin, aZoneBegin + end, i));
                    offsetInSentence = end;
                    continue;
                }
                if (token instanceof Word) {
                    Word w = (Word) token;
                    t = w.word();
                }

                if (t == null) {
                    throw new AnalysisEngineProcessException(new IllegalStateException(
                            "Unknown token type: " + token.getClass()));
                }

                // Skip whitespace
                while (isWhitespace(text.charAt(offsetInSentence))) {
                    offsetInSentence++;
                    if (offsetInSentence >= text.length()) {
                        break outer;
                    }
                }

                // Match
                if (text.startsWith(t, offsetInSentence)) {
                    casTokens.add(createToken(aJCas, aZoneBegin + offsetInSentence, aZoneBegin
                            + offsetInSentence + t.length(), i));
                    offsetInSentence = offsetInSentence + t.length();
                }
                else {
//                    System.out.println(aText);
                    throw new AnalysisEngineProcessException(new IllegalStateException(
                            "Text mismatch. Tokenizer: ["
                                    + t
                                    + "] CAS: ["
                                    + text.substring(offsetInSentence,
                                            min(offsetInSentence + t.length(), text.length()))));
                }
            }
        }

        if (isWriteSentence()) {
            if (casTokens == null) {
                casTokens = selectCovered(aJCas, Token.class, aZoneBegin,
                        aZoneBegin + aText.length());
            }
            
    		// Prepare the tokens for processing by WordToSentenceProcessor
    		List<CoreLabel> tokensInDocument = new ArrayList<CoreLabel>();
    		for (Token token : casTokens) {
    			CoreLabel l = new CoreLabel();
    			l.set(CharacterOffsetBeginAnnotation.class, token.getBegin());
    			l.set(CharacterOffsetEndAnnotation.class, token.getEnd());
    			l.setWord(token.getCoveredText());
    			tokensInDocument.add(l);
    		}

    		// The sentence splitter (probably) requires the escaped text, so we prepare it here
    		PTBEscapingProcessor escaper = new PTBEscapingProcessor();
    		escaper.apply(tokensInDocument);
    
    		// Apply the WordToSentenceProcessor to find the sentence boundaries
    		WordToSentenceProcessor<CoreLabel> proc =
    				new WordToSentenceProcessor<CoreLabel>();
    		List<List<CoreLabel>> sentencesInDocument = proc.process(tokensInDocument);
    		for (List<CoreLabel> sentence : sentencesInDocument) {
    			int begin = sentence.get(0).get(CharacterOffsetBeginAnnotation.class);
    			int end = sentence.get(sentence.size()-1).get(CharacterOffsetEndAnnotation.class);
    
    			createSentence(aJCas, begin, end);
    		}
		}
    }

	private
    Tokenizer getTokenizer(
    		final String aLanguage,
    		final String aText) throws AnalysisEngineProcessException
    {
        InternalTokenizerFactory tk = tokenizerFactories.get(aLanguage);
        if (tk == null && languageFallback == null) {
            throw new AnalysisEngineProcessException(Messages.BUNDLE,
                    Messages.ERR_UNSUPPORTED_LANGUAGE, new String[] { aLanguage });
        }
        
        tk = tokenizerFactories.get(languageFallback);
        if (tk == null) {
            throw new AnalysisEngineProcessException(Messages.BUNDLE,
                    Messages.ERR_UNSUPPORTED_LANGUAGE, new String[] { aLanguage });
        }
        
    	return tk.create(aText);
    }

    private static
    interface InternalTokenizerFactory
    {
    	Tokenizer<?> create(String s);
    }

    private static
    class InternalPTBTokenizerFactory
    implements InternalTokenizerFactory
    {
    	@Override
    	public
    	Tokenizer<?> create(
    			final String s)
    	{

//    		TokenizerFactory<Word> f = PTBTokenizer.factory(false, true, false);


//    		TokenizerFactory<CoreLabel> f = PTBTokenizer.factory(new CoreLabelTokenFactory(), "invertible,ptb3Escaping=false");
    		return new PTBTokenizer<CoreLabel>(new StringReader(s),new CoreLabelTokenFactory(),"invertible");

    	}
    }

	// The InternalNegraPennTokenizer is not meant for German text. It
	// is for parsing a particular corpus format.
//    private static
//    class InternalNegraPennTokenizerFactory
//    implements InternalTokenizerFactory
//    {
//    	@Override
//    	public
//    	Tokenizer<?> create(
//    			final String s)
//    	{
//    		return new NegraPennTokenizer(new StringReader(s));
//    	}
//    }

    private static
    class InternalArabicTokenizerFactory
    implements InternalTokenizerFactory
    {
    	@Override
    	public
    	Tokenizer<?> create(
    			final String s)
    	{
    		return ArabicTokenizer.newArabicTokenizer(new StringReader(s), new Properties());
    	}
    }

    private static
    class InternalFrenchTokenizerFactory
    implements InternalTokenizerFactory
    {
        @Override
        public
        Tokenizer<?> create(
                final String s)
        {
            return FrenchTokenizer.factory().getTokenizer(new StringReader(s), "tokenizeNLs=false");
        }
    }

    private static
    class InternalSpanishTokenizerFactory
    implements InternalTokenizerFactory
    {
        @Override
        public
        Tokenizer<?> create(
                final String s)
        {
            return SpanishTokenizer.factory(new CoreLabelTokenFactory(), null).getTokenizer(
                    new StringReader(s));
        }
    }

    // While the stanford parser should come with a proper tokenizer for
    // Chinese (because it can parse chinese text), this does not seem to be
    // the right one or I am using it wrong. The associated test cases do not
    // work.
//    private static
//    class InternalCHTBTokenizerFactory
//    implements InternalTokenizerFactory
//    {
//    	@Override
//    	public
//    	Tokenizer<?> create(
//    			final String s)
//    	{
//    		return new CHTBTokenizer(new StringReader(s));
//    	}
//    }
}
