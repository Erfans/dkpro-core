/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.core.io.xces;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.io.xces.models.XcesBody;
import org.dkpro.core.io.xces.models.XcesPara;
import org.dkpro.core.io.xces.models.XcesSentence;
import org.dkpro.core.io.xces.models.XcesToken;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import javanet.staxutils.IndentingXMLEventWriter;

@TypeCapability(
        inputs = {            
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
            "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph" })
public class XcesXmlWriter
    extends JCasFileWriter_ImplBase
{
    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".xml")
    private String filenameSuffix;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            XMLEventWriter xmlEventWriter = new IndentingXMLEventWriter(
                    xmlOutputFactory.createXMLEventWriter(docOS));
            JAXBContext context = JAXBContext.newInstance(XcesBody.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            XMLEventFactory xmlef = XMLEventFactory.newInstance();
            xmlEventWriter.add(xmlef.createStartDocument());
            // Begin cesDoc
            xmlEventWriter.add(xmlef.createStartElement("", "", "cesDoc"));
            // Begin and End cesHeader
            xmlEventWriter.add(xmlef.createStartElement("", "", "cesHeader"));
            xmlEventWriter.add(xmlef.createEndElement("", "", "cesHeader"));

            // Begin text and body
            xmlEventWriter.add(xmlef.createStartElement("", "", "text"));
            // xmlEventWriter.add(xmlef.createStartElement("", "", "body"));

            // Begin body of all the paragraphs            
            Collection<Paragraph> parasInCas = JCasUtil.select(aJCas, Paragraph.class);
            XcesBody xb = convertToXcesPara(parasInCas);
            marshaller.marshal(new JAXBElement<XcesBody>(new QName("body"), XcesBody.class, xb),
                    xmlEventWriter);
            // End body of all the paragraphs
            // xmlEventWriter.add(xmlef.createEndElement("", "", "body"));
            xmlEventWriter.add(xmlef.createEndElement("", "", "text"));
            xmlEventWriter.add(xmlef.createEndElement("", "", "cesDoc"));
            xmlEventWriter.add(xmlef.createEndDocument());
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(docOS);
        }

    }

    private XcesBody convertToXcesPara(Collection<Paragraph> paras)
    {
        int paraNo = 1, sentNo = 1, tokenNo = 1;
        XcesBody xb = new XcesBody();
        List<XcesPara> lp = new ArrayList<XcesPara>();
        for (Paragraph p : paras) {
            XcesPara para = new XcesPara();
            List<XcesSentence> xcesSents = new ArrayList<XcesSentence>();
            para.id = "p" + Integer.toString(paraNo);
            for (Sentence s : JCasUtil.selectCovered(Sentence.class, p)) {
                XcesSentence xcesSent = new XcesSentence();
                List<XcesToken> sentTokens = new ArrayList<XcesToken>();
                xcesSent.id = "s" + Integer.toString(sentNo);
                for (Token t : JCasUtil.selectCovered(Token.class, s)) {

                    XcesToken tok = new XcesToken();
                    tok.id = "t" + Integer.toString(tokenNo);
                    tok.word = t.getCoveredText();
                    if (t.getPos() != null) {
                        tok.tag = t.getPos().getPosValue();
                    }
                    if (t.getLemma() != null) {
                        tok.lemma = t.getLemma().getValue();
                    }
                    sentTokens.add(tok);
                    tokenNo++;
                }
                xcesSent.xcesTokens = sentTokens;
                xcesSents.add(xcesSent);
                sentNo++;
            }
            para.s = xcesSents;
            lp.add(para);
            paraNo++;
        }
        xb.p = lp;
        return xb;
    }

}
