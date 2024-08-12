package org.omegat.core.data;

import java.util.List;

import org.omegat.tokenizer.ITokenizer;

public interface IProject {

	ITokenizer getSourceTokenizer();

    ProjectProperties getProjectProperties();

	List<SourceTextEntry> getAllEntries();

}
