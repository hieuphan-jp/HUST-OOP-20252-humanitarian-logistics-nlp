package com.disaster.analysis.infrastructure.analysis;

import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.contract.analysis.SentimentAnalyzer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Properties;

/**
 * Công cụ phân tích cảm xúc Tiếng Anh bằng AI của Stanford CoreNLP.
 */
public class StanfordSentimentAnalyzer implements SentimentAnalyzer {

    private StanfordCoreNLP pipeline;
    private boolean initialized = false;

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,parse,sentiment");
        props.setProperty("parse.model", "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        props.setProperty("sentiment.model", "edu/stanford/nlp/models/sentiment/sentiment.ser.gz");

        this.pipeline = new StanfordCoreNLP(props);
        this.initialized = true;
    }

    @Override
    public Sentiment analyzeSentiment(String text) {
        if (!initialized || pipeline == null) {
            throw new IllegalStateException("Cần gọi initialize() trước khi phân tích bằng Stanford NLP.");
        }

        if (text == null || text.trim().isEmpty()) {
            return Sentiment.NEUTRAL;
        }

        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences == null || sentences.isEmpty()) {
            return Sentiment.NEUTRAL;
        }

        int totalSentimentScore = 0;
        int sentenceCount = 0;

        for (CoreMap sentence : sentences) {
            Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            if (tree != null) {
                int sentimentScore = RNNCoreAnnotations.getPredictedClass(tree);
                totalSentimentScore += sentimentScore;
                sentenceCount++;
            }
        }

        if (sentenceCount == 0) return Sentiment.NEUTRAL;

        double averageSentiment = (double) totalSentimentScore / sentenceCount;

        // Thang điểm Stanford: 0-1 (Tiêu cực), 2 (Trung lập), 3-4 (Tích cực)
        if (averageSentiment < 1.5) {
            return Sentiment.NEGATIVE;
        } else if (averageSentiment > 2.5) {
            return Sentiment.POSITIVE;
        } else {
            return Sentiment.NEUTRAL;
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}