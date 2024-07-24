package org.omegat.tokenizer;

import org.omegat.util.Token;

public interface ITokenizer {
    enum StemmingMode {
        NONE, MATCHING, GLOSSARY
    }

	Token[] tokenizeWords(String text, StemmingMode none);

}
