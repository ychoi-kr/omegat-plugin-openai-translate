package org.omegat.core.data;

import org.omegat.tokenizer.ITokenizer;

public interface IProject {

	ITokenizer getSourceTokenizer();

    ProjectProperties getProjectProperties();

}
