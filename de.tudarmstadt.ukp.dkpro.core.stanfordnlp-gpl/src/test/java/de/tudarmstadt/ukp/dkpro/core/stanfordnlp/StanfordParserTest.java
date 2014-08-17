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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tudarmstadt.ukp.dkpro.core.stanfordnlp;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.PennTree;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.util.TreeUtils;
import de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations;
import de.tudarmstadt.ukp.dkpro.core.testing.TestRunner;
import edu.stanford.nlp.ling.StringLabel;
import edu.stanford.nlp.trees.Tree;

/**
 * @author Oliver Ferschke
 * @author Niklas Jakob
 * @author Richard Eckart de Castilho
 */
public class StanfordParserTest
{
    static final String documentEnglish = "We need a very complicated example sentence, which "
            + "contains as many constituents and dependencies as possible.";

    // TODO Maybe test link to parents (not tested by syntax tree recreation)

    @Test
    public void testGermanPcfg()
        throws Exception
    {
        JCas jcas = runTest("de", "pcfg", "Wir brauchen ein sehr kompliziertes Beispiel, welches "
                + "möglichst viele Konstituenten und Dependenzen beinhaltet.");

        String[] constituentMapped = new String[] { "NP 13,110", "NP 64,99", "ROOT 0,111",
                "S 0,111", "S 46,110", "X 17,35", "X 70,99" };

        String[] constituentOriginal = new String[] { "AP 17,35", "CNP 70,99", "NP 13,110",
                "NP 64,99", "ROOT 0,111", "S 0,111", "S 46,110" };

        String[] synFunc = new String[] { "OA 13,110", "SB 64,99" };

        String[] posOriginal = new String[] { "PPER", "VVFIN", "ART", "ADV", "ADJA", "NN", "$,",
                "PRELS", "ADV", "PIDAT", "NN", "KON", "NN", "VVFIN", "$." };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "ADJ", "NN", "PUNC", "PR",
                "ADV", "PR", "NN", "CONJ", "NN", "V", "PUNC" };

        String[] dependencies = new String[] {/** No dependencies for German */
        };

        String pennTree = "(ROOT (S (PPER-SB Wir) (VVFIN brauchen) (NP-OA (ART ein) (AP "
                + "(ADV sehr) (ADJA kompliziertes)) (NN Beispiel) ($, ,) (S (PRELS-SB welches) "
                + "(ADV möglichst) (NP-SB (PIDAT viele) (CNP (NN Konstituenten) (KON und) "
                + "(NN Dependenzen))) (VVFIN beinhaltet))) ($. .)))";

        String[] posTags = new String[] { "$*LRB*", "$,", "$.", "-", ".$$.", "ADJA", "ADJD", "ADV",
                "APPO", "APPR", "APPRART", "APZR", "ART", "CARD", "FM", "ITJ", "KOKOM", "KON",
                "KOUI", "KOUS", "NE", "NN", "PDAT", "PDS", "PIAT", "PIDAT", "PIS", "PPER",
                "PPOSAT", "PPOSS", "PRELAT", "PRELS", "PRF", "PROAV", "PTKA", "PTKANT", "PTKNEG",
                "PTKVZ", "PTKZU", "PWAT", "PWAV", "PWS", "TRUNC", "VAFIN", "VAIMP", "VAINF",
                "VAPP", "VMFIN", "VMINF", "VMPP", "VVFIN", "VVIMP", "VVINF", "VVIZU", "VVPP", "XY" };

        String[] constituentTags = new String[] { "AA", "AP", "AVP", "CAC", "CAP", "CAVP", "CCP",
                "CH", "CNP", "CO", "CPP", "CS", "CVP", "CVZ", "DL", "ISU", "MPN", "MTA", "NM",
                "NP", "NUR", "PP", "QL", "ROOT", "S", "VP", "VZ" };

        String[] unmappedPos = new String[] { "$*LRB*", "-", ".$$." };

        String[] unmappedConst = new String[] { "NUR" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertSyntacticFunction(synFunc, select(jcas, Constituent.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "stts", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "stts", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "negra", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "negra", unmappedConst, jcas);
    }

    @Test
    public void testGermanFactored()
        throws Exception
    {
        JCas jcas = runTest("de", "factored",
                "Wir brauchen ein sehr kompliziertes Beispiel, welches "
                        + "möglichst viele Konstituenten und Dependenzen beinhaltet.");

        String[] constituentMapped = new String[] { "NP 13,110", "NP 54,99", "ROOT 0,111",
                "S 0,111", "S 46,110", "X 17,35", "X 54,69", "X 70,99" };

        String[] constituentOriginal = new String[] { "AP 17,35", "AP 54,69", "CNP 70,99",
                "NP 13,110", "NP 54,99", "ROOT 0,111", "S 0,111", "S 46,110" };

        String[] posOriginal = new String[] { "PPER", "VVFIN", "ART", "ADV", "ADJA", "NN", "$,",
                "PRELS", "ADV", "PIDAT", "NN", "KON", "NN", "VVFIN", "$." };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "ADJ", "NN", "PUNC", "PR",
                "ADV", "PR", "NN", "CONJ", "NN", "V", "PUNC" };

        String[] dependencies = new String[] {/** No dependencies for German */
        };

        String pennTree = "(ROOT (S (PPER-SB Wir) (VVFIN brauchen) (NP-OA (ART ein) (AP "
                + "(ADV sehr) (ADJA kompliziertes)) (NN Beispiel) ($, ,) (S (PRELS-SB welches) "
                + "(NP-DA (AP (ADV möglichst) (PIDAT viele)) (CNP (NN Konstituenten) (KON und) "
                + "(NN Dependenzen))) (VVFIN beinhaltet))) ($. .)))";

        String[] posTags = new String[] { "$*LRB*", "$,", "$.", "-", ".$$.", "ADJA", "ADJD", "ADV",
                "APPO", "APPR", "APPRART", "APZR", "ART", "CARD", "FM", "ITJ", "KOKOM", "KON",
                "KOUI", "KOUS", "NE", "NN", "PDAT", "PDS", "PIAT", "PIDAT", "PIS", "PPER",
                "PPOSAT", "PPOSS", "PRELAT", "PRELS", "PRF", "PROAV", "PTKA", "PTKANT", "PTKNEG",
                "PTKVZ", "PTKZU", "PWAT", "PWAV", "PWS", "TRUNC", "VAFIN", "VAIMP", "VAINF",
                "VAPP", "VMFIN", "VMINF", "VMPP", "VVFIN", "VVIMP", "VVINF", "VVIZU", "VVPP", "XY" };

        String[] constituentTags = new String[] { "AA", "AP", "AVP", "CAC", "CAP", "CAVP", "CCP",
                "CH", "CNP", "CO", "CPP", "CS", "CVP", "CVZ", "DL", "ISU", "MPN", "MTA", "NM",
                "NP", "NUR", "PP", "QL", "ROOT", "S", "VP", "VZ" };

        String[] unmappedPos = new String[] { "$*LRB*", "-", ".$$." };

        String[] unmappedConst = new String[] { "NUR" };

        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "stts", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "stts", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "negra", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "negra", unmappedConst, jcas);
    }

    @Test
    public void testEnglishPcfg()
        throws Exception
    {
        JCas jcas = runTest("en", "pcfg", documentEnglish);

        String[] constituentMapped = new String[] { "ROOT 0,110", "S 0,110", "NP 0,2", "VP 3,109",
                "NP 8,109", "NP 8,43", "ADJP 10,26", "SBAR 45,109", "WHNP 45,50", "VP 51,109",
                "S 51,109", "PP 60,97", "NP 63,97", "PP 98,109", "ADJP 101,109" };

        String[] constituentOriginal = new String[] { "ROOT 0,110", "S 0,110", "NP 0,2",
                "VP 3,109", "NP 8,109", "NP 8,43", "ADJP 10,26", "SBAR 45,109", "WHNP 45,50",
                "VP 51,109", "S 51,109", "PP 60,97", "NP 63,97", "PP 98,109", "ADJP 101,109" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 45, 50]NSUBJ(nsubj) D[45,50](which) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 63, 67]AMOD(amod) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]PREP(prep_as) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[51,59](contains)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "V", "NN", "NN", "PUNC",
                "ART", "V", "PP", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "VBN", "NN", "NN", ",",
                "WDT", "VBZ", "IN", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(VBN complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (PP (IN as) (NP (JJ many) (NNS constituents) (CC and) "
                + "(NNS dependencies))) (PP (IN as) (ADJP (JJ possible)))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }

    @Test
    public void testEnglishPcfgCollapsed()
        throws Exception
    {
        JCas jcas = runTest("en", "pcfg", documentEnglish, 
                StanfordParser.PARAM_MODE, StanfordParser.DependenciesMode.COLLAPSED_WITH_EXTRA);

        String[] constituentMapped = new String[] { "ROOT 0,110", "S 0,110", "NP 0,2", "VP 3,109",
                "NP 8,109", "NP 8,43", "ADJP 10,26", "SBAR 45,109", "WHNP 45,50", "VP 51,109",
                "S 51,109", "PP 60,97", "NP 63,97", "PP 98,109", "ADJP 101,109" };

        String[] constituentOriginal = new String[] { "ROOT 0,110", "S 0,110", "NP 0,2",
                "VP 3,109", "NP 8,109", "NP 8,43", "ADJP 10,26", "SBAR 45,109", "WHNP 45,50",
                "VP 51,109", "S 51,109", "PP 60,97", "NP 63,97", "PP 98,109", "ADJP 101,109" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 35, 43]NSUBJ(nsubj) D[35,43](sentence) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 63, 67]AMOD(amod) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]PREP(prep_as) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[51,59](contains)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "V", "NN", "NN", "PUNC",
                "ART", "V", "PP", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "VBN", "NN", "NN", ",",
                "WDT", "VBZ", "IN", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(VBN complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (PP (IN as) (NP (JJ many) (NNS constituents) (CC and) "
                + "(NNS dependencies))) (PP (IN as) (ADJP (JJ possible)))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }

    @Test
    public void testEnglishFactored()
        throws Exception
    {
        JCas jcas = runTest("en", "factored", documentEnglish);

        String[] constituentMapped = new String[] { "ADJP 10,26", "ADJP 101,109", "ADJP 60,67",
                "NP 0,2", "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "ROOT 0,110", "S 0,110",
                "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] constituentOriginal = new String[] { "ADJP 10,26", "ADJP 101,109", "ADJP 60,67",
                "NP 0,2", "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "ROOT 0,110", "S 0,110",
                "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 45, 50]NSUBJ(nsubj) D[45,50](which) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 60, 62]ADVMOD(advmod) D[60,62](as) G[63,67](many)",
                "[ 63, 67]AMOD(amod) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]DOBJ(dobj) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[51,59](contains)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "V", "NN", "NN", "PUNC",
                "ART", "V", "ADV", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "VBN", "NN", "NN", ",",
                "WDT", "VBZ", "RB", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(VBN complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (NP (ADJP (RB as) (JJ many)) (NNS constituents) (CC and) "
                + "(NNS dependencies)) (PP (IN as) (ADJP (JJ possible)))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }

    @Test
    public void testEnglishRnn()
        throws Exception
    {
        JCas jcas = runTest("en", "rnn", documentEnglish);

        String[] constituentMapped = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "QP 60,67", "ROOT 0,110",
                "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] constituentOriginal = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "QP 60,67", "ROOT 0,110",
                "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 45, 50]NSUBJ(nsubj) D[45,50](which) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 60, 62]QUANTMOD(quantmod) D[60,62](as) G[63,67](many)",
                "[ 63, 67]NUM(num) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]DOBJ(dobj) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[51,59](contains)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "V", "NN", "NN", "PUNC",
                "ART", "V", "ADV", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "VBN", "NN", "NN", ",",
                "WDT", "VBZ", "RB", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(VBN complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (NP (QP (RB as) (JJ many)) (NNS constituents) (CC and) "
                + "(NNS dependencies)) (PP (IN as) (ADJP (JJ possible)))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }

    @Test
    public void testEnglishShiftReduce()
        throws Exception
    {
        JCas jcas = runTestWithPosTagger("en", "sr", documentEnglish);

        String[] constituentMapped = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 63,109", "NP 63,97", "NP 8,109", "NP 8,43", "PP 60,109", "PP 98,109",
                "ROOT 0,110", "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109",
                "WHNP 45,50" };

        String[] constituentOriginal = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 63,109", "NP 63,97", "NP 8,109", "NP 8,43", "PP 60,109", "PP 98,109",
                "ROOT 0,110", "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109",
                "WHNP 45,50" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 45, 50]NSUBJ(nsubj) D[45,50](which) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 63, 67]AMOD(amod) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]PREP(prep_as) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[68,80](constituents)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "ADJ", "NN", "NN", "PUNC",
                "ART", "V", "PP", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "JJ", "NN", "NN", ",",
                "WDT", "VBZ", "IN", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(JJ complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (PP (IN as) (NP (NP (JJ many) (NNS constituents) "
                + "(CC and) (NNS dependencies)) (PP (IN as) (ADJP (JJ possible)))))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }
    @Test
    public void testEnglishWsjRnn()
        throws Exception
    {
        JCas jcas = runTest("en", "wsj-rnn", documentEnglish);

        String[] constituentMapped = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "QP 60,67", "ROOT 0,110",
                "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] constituentOriginal = new String[] { "ADJP 10,26", "ADJP 101,109", "NP 0,2",
                "NP 60,97", "NP 8,109", "NP 8,43", "PP 98,109", "QP 60,67", "ROOT 0,110",
                "S 0,110", "S 51,109", "SBAR 45,109", "VP 3,109", "VP 51,109", "WHNP 45,50" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](We) G[3,7](need)",
                "[  8,  9]DET(det) D[8,9](a) G[35,43](sentence)",
                "[ 10, 14]ADVMOD(advmod) D[10,14](very) G[15,26](complicated)",
                "[ 15, 26]AMOD(amod) D[15,26](complicated) G[35,43](sentence)",
                "[ 27, 34]NN(nn) D[27,34](example) G[35,43](sentence)",
                "[ 35, 43]DOBJ(dobj) D[35,43](sentence) G[3,7](need)",
                "[ 45, 50]NSUBJ(nsubj) D[45,50](which) G[51,59](contains)",
                "[ 51, 59]RCMOD(rcmod) D[51,59](contains) G[35,43](sentence)",
                "[ 60, 62]QUANTMOD(quantmod) D[60,62](as) G[63,67](many)",
                "[ 63, 67]NUM(num) D[63,67](many) G[68,80](constituents)",
                "[ 68, 80]DOBJ(dobj) D[68,80](constituents) G[51,59](contains)",
                "[ 85, 97]CONJ(conj_and) D[85,97](dependencies) G[68,80](constituents)",
                "[101,109]PREP(prep_as) D[101,109](possible) G[51,59](contains)" };

        String[] posMapped = new String[] { "PR", "V", "ART", "ADV", "V", "NN", "NN", "PUNC",
                "ART", "V", "ADV", "ADJ", "NN", "CONJ", "NN", "PP", "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "PRP", "VBP", "DT", "RB", "VBN", "NN", "NN", ",",
                "WDT", "VBZ", "RB", "JJ", "NNS", "CC", "NNS", "IN", "JJ", "." };

        String pennTree = "(ROOT (S (NP (PRP We)) (VP (VBP need) (NP (NP (DT a) (ADJP (RB very) "
                + "(VBN complicated)) (NN example) (NN sentence)) (, ,) (SBAR (WHNP (WDT which)) "
                + "(S (VP (VBZ contains) (NP (QP (RB as) (JJ many)) (NNS constituents) (CC and) "
                + "(NNS dependencies)) (PP (IN as) (ADJP (JJ possible)))))))) (. .)))";

        String[] posTags = new String[] { "#", "$", "''", ",", "-LRB-", "-RRB-", ".", ".$$.", ":",
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
                "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
                "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "``" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST",
                "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "ROOT", "RRC", "S", "SBAR", "SBARQ",
                "SINV", "SQ", "UCP", "VP", "WHADJP", "WHADVP", "WHNP", "WHPP", "X" };

        String[] depTags = new String[] { "acomp", "advcl", "advmod", "agent", "amod", "appos",
                "arg", "aux", "auxpass", "cc", "ccomp", "comp", "conj", "cop", "csubj",
                "csubjpass", "dep", "det", "discourse", "dobj", "expl", "goeswith", "gov", "iobj",
                "mark", "mod", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass", "num",
                "number", "obj", "parataxis", "pcomp", "pobj", "poss", "possessive", "preconj",
                "pred", "predet", "prep", "prt", "punct", "quantmod", "rcmod", "ref", "rel",
                "sdep", "subj", "tmod", "vmod", "xcomp", "xsubj" };

        String[] unmappedPos = new String[] { "#", "$", "''", "-LRB-", "-RRB-", ".$$.", "``" };

        String[] unmappedConst = new String[] {};

        String[] unmappedDep = new String[] { "gov" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertTagset(POS.class, "ptb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ptb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ptb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ptb", unmappedConst, jcas);
        AssertAnnotations.assertTagset(Dependency.class, "stanford331", depTags, jcas);
        AssertAnnotations.assertTagsetMapping(Dependency.class, "stanford331", unmappedDep, jcas);
    }

    /**
     * This test uses simple double quotes.
     */
    @Test
    public void testEnglishFactoredDirectSpeech()
        throws Exception
    {
        JCas jcas = runTest("en", "factored",
                "\"It's cold outside,\" he said, \"and it's starting to rain.\"");

        String[] posOriginal = new String[] { "``", "PRP", "VBZ", "JJ", "JJ", ",", "''", "PRP",
                "VBD", ",", "``", "CC", "PRP", "VBZ", "VBG", "TO", "NN", ".", "''" };

        String pennTree = "(ROOT (S (`` \") (S (NP (PRP It)) (VP (VBZ 's) (ADJP (JJ cold)) (S "
                + "(ADJP (JJ outside))))) (PRN (, ,) ('' \") (S (NP (PRP he)) (VP (VBD said))) (, "
                + ",) (`` \")) (CC and) (S (NP (PRP it)) (VP (VBZ 's) (VP (VBG starting) (PP "
                + "(TO to) (NP (NN rain)))))) (. .) ('' \")))";

        AssertAnnotations.assertPOS(null, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
    }

    /**
     * This test uses UTF-8 quotes as they can be found in the British National Corpus.
     */
    @Test
    public void testEnglishFactoredDirectSpeech2()
        throws Exception
    {
        // JCas jcas = runTest("en", "factored",
        // "‘Prices are used as a barrier so that the sort of " +
        // "people we don't want go over the road ,’ he said .");
        JCas jcas = runTest("en", "factored", new String[] { "‘", "It", "'s", "cold", "outside",
                ",", "’", "he", "said", ",", "‘", "and", "it", "'s", "starting", "to", "rain", ".",
                "’" });

        String[] posOriginal = new String[] { "``", "PRP", "VBZ", "JJ", "JJ", ",", "''", "PRP",
                "VBD", ",", "``", "CC", "PRP", "VBZ", "VBG", "TO", "NN", ".", "''" };

        String pennTree = "(ROOT (S (`` ‘) (S (NP (PRP It)) (VP (VBZ 's) (ADJP (JJ cold)) (S "
                + "(ADJP (JJ outside))))) (PRN (, ,) ('' ’) (S (NP (PRP he)) (VP (VBD said))) "
                + "(, ,) (`` ‘)) (CC and) (S (NP (PRP it)) (VP (VBZ 's) (VP (VBG starting) (PP "
                + "(TO to) (NP (NN rain)))))) (. .) ('' ’)))";

        AssertAnnotations.assertPOS(null, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
    }

    /**
     * Tests the parser reading pre-existing POS tags
     */
    @Test
    public void testExistingPos()
        throws Exception
    {
        AnalysisEngineDescription engine = createEngineDescription(
                createEngineDescription(StanfordPosTagger.class),
                createEngineDescription(StanfordParser.class,
                        StanfordParser.PARAM_READ_POS, true,
                        StanfordParser.PARAM_WRITE_POS, false,
                        StanfordParser.PARAM_WRITE_PENN_TREE, true));
        
        JCas jcas = TestRunner.runTest(engine, "en", "This is a test .");
        
        String[] posOriginal = new String[] { "DT", "VBZ", "DT", "NN", "." };

        String pennTree = "(ROOT (S (NP (DT This)) (VP (VBZ is) (NP (DT a) (NN test))) (. .)))";

        AssertAnnotations.assertPOS(null, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
    }

    @Test
    public void testFrenchFactored()
        throws Exception
    {
        Assume.assumeTrue(Runtime.getRuntime().maxMemory() > 1000000000);

        JCas jcas = runTest("fr", "factored", "Nous avons besoin d'une phrase par exemple très "
                + "compliqué, qui contient des constituants que de nombreuses dépendances et que "
                + "possible.");

        String[] constituentMapped = new String[] { "NP 11,47", "NP 20,47", "NP 59,62", "NP 72,88",
                "NP 93,118", "PP 18,47", "ROOT 0,135", "X 0,135", "X 0,57", "X 119,134",
                "X 122,134", "X 126,134", "X 31,42", "X 31,47", "X 59,88", "X 63,71", "X 89,134" };

        String[] constituentOriginal = new String[] { "AP 126,134", "AdP 31,47", "COORD 119,134",
                "MWADV 31,42", "NP 11,47", "NP 20,47", "NP 59,62", "NP 72,88", "NP 93,118",
                "PP 18,47", "ROOT 0,135", "SENT 0,135", "Srel 59,88", "Ssub 122,134",
                "Ssub 89,134", "VN 0,57", "VN 63,71" };

        String[] dependencies = new String[] {/** No dependencies for French */
        };

        String[] posMapped = new String[] { "PR", "V", "NN", "PP", "ART", "NN", "PP", "N", "ADV",
                "V", "PUNC", "PR", "V", "ART", "NN", "CONJ", "ART", "ADJ", "NN", "CONJ", "CONJ",
                "ADJ", "PUNC" };

        String[] posOriginal = new String[] { "CLS", "V", "NC", "P", "DET", "NC", "P", "N", "ADV",
                "VPP", "PUNC", "PROREL", "V", "DET", "NC", "CS", "DET", "ADJ", "NC", "CC", "CS",
                "ADJ", "PUNC" };

        String pennTree = "(ROOT (SENT (VN (CLS Nous) (V avons) (NP (NC besoin) (PP (P d') (NP "
                + "(DET une) (NC phrase) (AdP (MWADV (P par) (N exemple)) (ADV très))))) "
                + "(VPP compliqué)) (PUNC ,) (Srel (NP (PROREL qui)) (VN (V contient)) (NP "
                + "(DET des) (NC constituants))) (Ssub (CS que) (NP (DET de) (ADJ nombreuses) "
                + "(NC dépendances)) (COORD (CC et) (Ssub (CS que) (AP (ADJ possible))))) "
                + "(PUNC .)))";

        String[] posTags = new String[] { ".$$.", "A", "ADJ", "ADJWH", "ADV", "ADVWH", "C", "CC",
                "CL", "CLO", "CLR", "CLS", "CS", "DET", "DETWH", "ET", "I", "N", "NC", "NPP", "P",
                "PREF", "PRO", "PROREL", "PROWH", "PUNC", "V", "VIMP", "VINF", "VPP", "VPR", "VS" };

        String[] constituentTags = new String[] { "AP", "AdP", "COORD", "MWA", "MWADV", "MWC",
                "MWCL", "MWD", "MWET", "MWI", "MWN", "MWP", "MWPRO", "MWV", "NP", "PP", "ROOT",
                "SENT", "Sint", "Srel", "Ssub", "VN", "VPinf", "VPpart" };

        // NO DEP TAGS String[] depTags = new String[] {};

        String[] unmappedPos = new String[] { ".$$." };

        String[] unmappedConst = new String[] { "MWA", "MWADV", "MWC", "MWCL", "MWD", "MWET",
                "MWI", "MWN", "MWP", "MWPRO", "MWV" };

        // NO DEP TAGS String[] unmappedDep = new String[] {};

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));

        AssertAnnotations.assertTagset(POS.class, "corenlp34", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "corenlp34", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ftb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ftb", unmappedConst, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagset(Dependency.class, null, depTags, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagsetMapping(Dependency.class, null, unmappedDep, jcas);
    }

    @Test
    public void testFrench2()
        throws Exception
    {
        Assume.assumeTrue(Runtime.getRuntime().maxMemory() > 1000000000);

        JCas jcas = runTest("fr", null, "La traduction d'un texte du français vers l'anglais.");

        String[] constituentMapped = new String[] { "NP 0,51", "NP 16,36", "NP 42,51", "PP 14,36",
                "PP 25,36", "PP 37,51", "ROOT 0,52", "X 0,52", "X 28,36" };

        String[] constituentOriginal = new String[] { "AP 28,36", "NP 0,51", "NP 16,36",
                "NP 42,51", "PP 14,36", "PP 25,36", "PP 37,51", "ROOT 0,52", "SENT 0,52" };

        String[] posMapped = new String[] { "ART", "NN", "PP", "ART", "NN", "PP", "ADJ", "PP",
                "ART", "NN", "PUNC" };

        String[] posOriginal = new String[] { "DET", "NC", "P", "DET", "NC", "P", "ADJ", "P",
                "DET", "NC", "PUNC" };

        String pennTree = "(ROOT (SENT (NP (DET La) (NC traduction) (PP (P d') (NP (DET un) "
                + "(NC texte) (PP (P du) (AP (ADJ français))))) (PP (P vers) (NP (DET l') "
                + "(NC anglais)))) (PUNC .)))";

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
    }

    @Test
    public void testChineseFactored()
        throws Exception
    {
        JCas jcas = runTest("zh", "factored", "我们需要一个非常复杂的句子例如其中包含许多成分和尽可能的依赖。");

        String[] constituentMapped = new String[] { "ADJP 6,10", "ADJP 8,10", "ADVP 13,15",
                "ADVP 24,27", "ADVP 6,8", "NP 0,2", "NP 11,13", "NP 15,17", "NP 19,23", "NP 21,23",
                "NP 4,13", "QP 19,21", "QP 4,6", "ROOT 0,31", "VP 17,23", "VP 17,30", "VP 2,13",
                "VP 24,30", "VP 28,30", "X 0,13", "X 0,31", "X 13,30", "X 24,28", "X 6,11" };

        String[] constituentOriginal = new String[] { "ADJP 6,10", "ADJP 8,10", "ADVP 13,15",
                "ADVP 24,27", "ADVP 6,8", "DNP 6,11", "DVP 24,28", "IP 0,13", "IP 0,31",
                "IP 13,30", "NP 0,2", "NP 11,13", "NP 15,17", "NP 19,23", "NP 21,23", "NP 4,13",
                "QP 19,21", "QP 4,6", "ROOT 0,31", "VP 17,23", "VP 17,30", "VP 2,13", "VP 24,30",
                "VP 28,30" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](我们) G[2,4](需要)",
                "[  4,  6]Dependency(nummod) D[4,6](一个) G[11,13](句子)",
                "[  6,  8]ADVMOD(advmod) D[6,8](非常) G[8,10](复杂)",
                "[  8, 10]Dependency(assmod) D[8,10](复杂) G[11,13](句子)",
                "[ 10, 11]Dependency(assm) D[10,11](的) G[8,10](复杂)",
                "[ 11, 13]DOBJ(dobj) D[11,13](句子) G[2,4](需要)",
                "[ 13, 15]ADVMOD(advmod) D[13,15](例如) G[17,19](包含)",
                "[ 15, 17]NSUBJ(nsubj) D[15,17](其中) G[17,19](包含)",
                "[ 17, 19]CONJ(conj) D[17,19](包含) G[2,4](需要)",
                "[ 19, 21]Dependency(nummod) D[19,21](许多) G[21,23](成分)",
                "[ 21, 23]DOBJ(dobj) D[21,23](成分) G[17,19](包含)",
                "[ 23, 24]CC(cc) D[23,24](和) G[17,19](包含)",
                "[ 24, 27]Dependency(dvpmod) D[24,27](尽可能) G[28,30](依赖)",
                "[ 27, 28]Dependency(dvpm) D[27,28](的) G[24,27](尽可能)",
                "[ 28, 30]CONJ(conj) D[28,30](依赖) G[17,19](包含)" };

        String[] posMapped = new String[] { "PR", "V", "CARD", "ADJ", "ADJ", "PRT", "NN", "ADJ",
                "NN", "V", "CARD", "NN", "CONJ", "ADJ", "PRT", "V", "PUNC" };

        String[] posOriginal = new String[] { "PN", "VV", "CD", "AD", "JJ", "DEG", "NN", "AD",
                "NN", "VV", "CD", "NN", "CC", "AD", "DEV", "VV", "PU" };

        String pennTree = "(ROOT (IP (IP (NP (PN 我们)) (VP (VV 需要) (NP (QP (CD 一个)) (DNP "
                + "(ADJP (ADVP (AD 非常)) (ADJP (JJ 复杂))) (DEG 的)) (NP (NN 句子))))) (IP (ADVP "
                + "(AD 例如)) (NP (NN 其中)) (VP (VP (VV 包含) (NP (QP (CD 许多)) (NP (NN 成分)))) "
                + "(CC 和) (VP (DVP (ADVP (AD 尽可能)) (DEV 的)) (VP (VV 依赖))))) (PU 。)))";

        String[] posTags = new String[] { ".$$.", "AD", "AS", "BA", "CC", "CD", "CS", "DEC", "DEG",
                "DER", "DEV", "DT", "ETC", "FRAG", "FW", "IJ", "JJ", "LB", "LC", "M", "MSP", "NN",
                "NR", "NT", "OD", "ON", "P", "PN", "PU", "SB", "SP", "URL", "VA", "VC", "VE", "VV",
                "X" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CLP", "CP", "DFL", "DNP", "DP",
                "DVP", "FLR", "INC", "INTJ", "IP", "LCP", "LST", "NP", "PP", "PRN", "QP", "ROOT",
                "UCP", "VCD", "VCP", "VNV", "VP", "VPT", "VRD", "VSB", "WHPP" };

        // NO DEP TAGS String[] depTags = new String[] {};

        String[] unmappedPos = new String[] { ".$$.", "FRAG", "URL" };

        String[] unmappedConst = new String[] { "DFL", "FLR", "INC", "WHPP" };

        // NO DEP TAGS String[] unmappedDep = new String[] {};

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "ctb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ctb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ctb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ctb", unmappedConst, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagset(Dependency.class, null, depTags, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagsetMapping(Dependency.class, null, unmappedDep, jcas);
    }

    @Test
    public void testChineseXinhuaFactored()
        throws Exception
    {
        JCas jcas = runTest("zh", "xinhua-factored", "我们需要一个非常复杂的句子例如其中包含许多成分和尽可能的依赖。");

        String[] constituentMapped = new String[] { "ADVP 13,15", "ADVP 24,27", "ADVP 6,8",
                "NP 0,2", "NP 11,13", "NP 15,17", "NP 19,23", "NP 21,23", "NP 28,30", "NP 4,30",
                "NP 6,13", "QP 19,21", "QP 4,6", "ROOT 0,31", "VP 17,23", "VP 17,27", "VP 2,30",
                "VP 24,27", "VP 6,10", "VP 8,10", "X 0,31", "X 13,27", "X 6,10", "X 6,11",
                "X 6,27", "X 6,28" };

        String[] constituentOriginal = new String[] { "ADVP 13,15", "ADVP 24,27", "ADVP 6,8",
                "CP 6,11", "CP 6,28", "IP 0,31", "IP 13,27", "IP 6,10", "IP 6,27", "NP 0,2",
                "NP 11,13", "NP 15,17", "NP 19,23", "NP 21,23", "NP 28,30", "NP 4,30", "NP 6,13",
                "QP 19,21", "QP 4,6", "ROOT 0,31", "VP 17,23", "VP 17,27", "VP 2,30", "VP 24,27",
                "VP 6,10", "VP 8,10" };

        String[] dependencies = new String[] {
                "[  0,  2]NSUBJ(nsubj) D[0,2](我们) G[2,4](需要)",
                "[  4,  6]Dependency(nummod) D[4,6](一个) G[28,30](依赖)",
                "[  6,  8]ADVMOD(advmod) D[6,8](非常) G[8,10](复杂)",
                "[  8, 10]RCMOD(rcmod) D[8,10](复杂) G[11,13](句子)",
                "[ 10, 11]Dependency(cpm) D[10,11](的) G[8,10](复杂)",
                "[ 11, 13]NSUBJ(nsubj) D[11,13](句子) G[17,19](包含)",
                "[ 13, 15]ADVMOD(advmod) D[13,15](例如) G[17,19](包含)",
                "[ 15, 17]NSUBJ(nsubj) D[15,17](其中) G[17,19](包含)",
                "[ 17, 19]RCMOD(rcmod) D[17,19](包含) G[28,30](依赖)",
                "[ 19, 21]Dependency(nummod) D[19,21](许多) G[21,23](成分)",
                "[ 21, 23]DOBJ(dobj) D[21,23](成分) G[17,19](包含)",
                "[ 23, 24]CC(cc) D[23,24](和) G[17,19](包含)",
                "[ 24, 27]CONJ(conj) D[24,27](尽可能) G[17,19](包含)",
                "[ 27, 28]Dependency(cpm) D[27,28](的) G[17,19](包含)",
                "[ 28, 30]DOBJ(dobj) D[28,30](依赖) G[2,4](需要)" };

        String[] posMapped = new String[] { "PR", "V", "CARD", "ADJ", "V", "PRT", "NN", "ADJ",
                "NN", "V", "CARD", "NN", "CONJ", "ADJ", "PRT", "NN", "PUNC" };

        String[] posOriginal = new String[] { "PN", "VV", "CD", "AD", "VA", "DEC", "NN", "AD",
                "NN", "VV", "CD", "NN", "CC", "AD", "DEC", "NN", "PU" };

        String pennTree = "(ROOT (IP (NP (PN 我们)) (VP (VV 需要) (NP (QP (CD 一个)) (CP (IP (NP "
                + "(CP (IP (VP (ADVP (AD 非常)) (VP (VA 复杂)))) (DEC 的)) (NP (NN 句子))) (IP "
                + "(ADVP (AD 例如)) (NP (NN 其中)) (VP (VP (VV 包含) (NP (QP (CD 许多)) (NP "
                + "(NN 成分)))) (CC 和) (VP (ADVP (AD 尽可能)))))) (DEC 的)) (NP (NN 依赖)))) "
                + "(PU 。)))";

        String[] posTags = new String[] { ".$$.", "AD", "AS", "BA", "CC", "CD", "CS", "DEC", "DEG",
                "DER", "DEV", "DT", "ETC", "FW", "JJ", "LB", "LC", "M", "MSP", "NN", "NR", "NT",
                "OD", "P", "PN", "PU", "SB", "SP", "VA", "VC", "VE", "VV" };

        String[] constituentTags = new String[] { "ADJP", "ADVP", "CLP", "CP", "DNP", "DP", "DVP",
                "FRAG", "IP", "LCP", "LST", "NP", "PP", "PRN", "QP", "ROOT", "UCP", "VCD", "VCP",
                "VNV", "VP", "VPT", "VRD", "VSB" };

        // NO DEP TAGS String[] depTags = new String[] {};

        String[] unmappedPos = new String[] { ".$$." };

        String[] unmappedConst = new String[] { };

        // NO DEP TAGS String[] unmappedDep = new String[] {};

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertPennTree(pennTree, selectSingle(jcas, PennTree.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
        AssertAnnotations.assertTagset(POS.class, "ctb", posTags, jcas);
        AssertAnnotations.assertTagsetMapping(POS.class, "ctb", unmappedPos, jcas);
        AssertAnnotations.assertTagset(Constituent.class, "ctb", constituentTags, jcas);
        AssertAnnotations.assertTagsetMapping(Constituent.class, "ctb", unmappedConst, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagset(Dependency.class, null, depTags, jcas);
        // NO DEP TAGS AssertAnnotations.assertTagsetMapping(Dependency.class, null, unmappedDep, jcas);
    }

    @Ignore("Currently fails an assertion in StanfordAnnotator:188 - need to investigate")
    @Test
    public void testArabicFactored()
        throws Exception
    {
        JCas jcas = runTest("ar", "factored",
                "نحن بحاجة إلى مثال على جملة معقدة جدا، والتي تحتوي على مكونات مثل العديد من والتبعيات وقت ممكن.");

        String[] constituentMapped = new String[] { "NP 0,1", "ROOT 0,1" };

        String[] constituentOriginal = new String[] { "NP 0,1", "ROOT 0,1" };

        String[] dependencies = new String[] {};

        String[] posMapped = new String[] { "POS", "POS" };

        String[] posOriginal = new String[] { "NN", "NN" };

        AssertAnnotations.assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        AssertAnnotations.assertConstituents(constituentMapped, constituentOriginal,
                select(jcas, Constituent.class));
        AssertAnnotations.assertDependencies(dependencies, select(jcas, Dependency.class));
    }

    /**
     * This tests whether a complete syntax tree can be recreated from the annotations without any
     * loss. Consequently, all links to children should be correct. (This makes no assertions about
     * the parent-links, because they are not used for the recreation)
     */
    @Test
    public void testEnglishSyntaxTreeReconstruction()
        throws Exception
    {
        JCas jcas = runTest("en", "factored", documentEnglish);

        String pennOriginal = "";
        String pennFromRecreatedTree = "";

        // As we only have one input sentence, each loop only runs once!

        for (PennTree curPenn : select(jcas, PennTree.class)) {
            // get original penn representation of syntax tree
            pennOriginal = curPenn.getPennTree();
        }

        for (ROOT curRoot : select(jcas, ROOT.class)) {
            // recreate syntax tree
            Tree recreation = TreeUtils.createStanfordTree(curRoot);

            // make a tree with simple string-labels
            recreation = recreation.deepCopy(recreation.treeFactory(), StringLabel.factory());

            pennFromRecreatedTree = recreation.pennString();
        }

        assertTrue("The recreated syntax-tree did not match the input syntax-tree.",
                pennOriginal.equals(pennFromRecreatedTree));
    }

    @Test
    public void testModelSharing()
        throws Exception
    {
        final List<LoggingEvent> records = new ArrayList<LoggingEvent>();

        // Tell the logger to log everything
        Logger rootLogger = org.apache.log4j.LogManager.getRootLogger();
        final org.apache.log4j.Level oldLevel = rootLogger.getLevel();
        rootLogger.setLevel(org.apache.log4j.Level.ALL);
        Appender appender = (Appender) rootLogger.getAllAppenders().nextElement();
        // Capture output, log only what would have passed the original logging level
        appender.addFilter(new org.apache.log4j.spi.Filter()
        {
            @Override
            public int decide(LoggingEvent event)
            {
                records.add(event);
                return event.getLevel().toInt() >= oldLevel.toInt() ? org.apache.log4j.spi.Filter.NEUTRAL
                        : org.apache.log4j.spi.Filter.DENY;
            }
        });

        try {
            AnalysisEngineDescription pipeline = createEngineDescription(
                    createEngineDescription(StanfordParser.class,
                            StanfordParser.PARAM_SHARED_MODEL, true,
                            StanfordParser.PARAM_WRITE_CONSTITUENT, true,
                            StanfordParser.PARAM_WRITE_DEPENDENCY, false),
                    createEngineDescription(StanfordParser.class,
                            StanfordParser.PARAM_SHARED_MODEL, true,
                            StanfordParser.PARAM_WRITE_CONSTITUENT, false,
                            StanfordParser.PARAM_WRITE_DEPENDENCY, true));
            
            TestRunner.runTest(pipeline, "en", "This is a test .");
            
            boolean found = false;
            for (LoggingEvent e : records) {
                if (String.valueOf(e.getMessage()).contains("Used resource from cache")) {
                    found = true;
                }
            }
            
            assertTrue("No log message about using the cached resource was found!", found);
        }
        finally {
            if (oldLevel != null) {
                rootLogger.setLevel(oldLevel);
                appender.clearFilters();
            }
        }
    }

    private JCas runTestWithPosTagger(String aLanguage, String aVariant, String aText,
            Object... aExtraParams)
        throws Exception
    {
        AggregateBuilder aggregate = new AggregateBuilder();
        
        aggregate.add(getSegmenter(aLanguage));
        
        aggregate.add(createEngineDescription(StanfordPosTagger.class));
                
        Object[] params = new Object[] {
                StanfordParser.PARAM_VARIANT, aVariant,
                StanfordParser.PARAM_PRINT_TAGSET, true,
                StanfordParser.PARAM_WRITE_CONSTITUENT, true,
                StanfordParser.PARAM_WRITE_DEPENDENCY, true,
                StanfordParser.PARAM_WRITE_PENN_TREE, true,
                StanfordParser.PARAM_READ_POS, true,
                StanfordParser.PARAM_WRITE_POS, false};
        params = ArrayUtils.addAll(params, aExtraParams);
        aggregate.add(createEngineDescription(StanfordParser.class, params));

        AnalysisEngine engine = aggregate.createAggregate();

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage(aLanguage);
        jcas.setDocumentText(aText);
        engine.process(jcas);

        return jcas;    
    }

    private JCas runTest(String aLanguage, String aVariant, String aText, Object... aExtraParams)
        throws Exception
    {
        AggregateBuilder aggregate = new AggregateBuilder();
        
        aggregate.add(getSegmenter(aLanguage));
                
        Object[] params = new Object[] {
                StanfordParser.PARAM_VARIANT, aVariant,
                StanfordParser.PARAM_PRINT_TAGSET, true,
                StanfordParser.PARAM_WRITE_CONSTITUENT, true,
                StanfordParser.PARAM_WRITE_DEPENDENCY, true,
                StanfordParser.PARAM_WRITE_PENN_TREE, true,
                StanfordParser.PARAM_WRITE_POS, true};
        params = ArrayUtils.addAll(params, aExtraParams);
        aggregate.add(createEngineDescription(StanfordParser.class, params));

        AnalysisEngine engine = aggregate.createAggregate();

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage(aLanguage);
        jcas.setDocumentText(aText);
        engine.process(jcas);

        return jcas;
    }
    
    private AnalysisEngineDescription getSegmenter(String aLanguage)
        throws ResourceInitializationException
    {
        if ("zh".equals(aLanguage) || "de".equals(aLanguage)) {
            return createEngineDescription(LanguageToolSegmenter.class);
        }
        else {
            return createEngineDescription(StanfordSegmenter.class);
        }
    }

    private JCas runTest(String aLanguage, String aVariant, String[] aTokens)
        throws Exception
    {
        // setup English
        AnalysisEngineDescription parser = createEngineDescription(StanfordParser.class,
                StanfordParser.PARAM_VARIANT, aVariant,
                StanfordParser.PARAM_PRINT_TAGSET, true,
                StanfordParser.PARAM_WRITE_CONSTITUENT, true,
                StanfordParser.PARAM_WRITE_DEPENDENCY, true,
                StanfordParser.PARAM_WRITE_PENN_TREE, true,
                StanfordParser.PARAM_WRITE_POS, true,
                StanfordParser.PARAM_WRITE_PENN_TREE, true,
                StanfordParser.PARAM_QUOTE_BEGIN, new String[] { "‘" },
                StanfordParser.PARAM_QUOTE_END, new String[] { "’" });

        AnalysisEngine engine = createEngine(parser);
        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage(aLanguage);

        JCasBuilder builder = new JCasBuilder(jcas);
        for (String t : aTokens) {
            builder.add(t, Token.class);
            builder.add(" ");
        }
        builder.add(0, Sentence.class);
        builder.close();

        engine.process(jcas);

        return jcas;
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void printSeparator()
    {
        System.out.println("\n=== " + name.getMethodName() + " =====================");
    }
    
    @Before
    public void setupLogging()
    {
        System.setProperty("org.apache.uima.logger.class", "org.apache.uima.util.impl.Log4jLogger_impl");
    }
}
