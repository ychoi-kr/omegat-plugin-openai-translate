package org.omegat.core.glossaries;

import java.util.List;

import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.util.Language;

public interface IGlossary {

	List<GlossaryEntry> search(Language sLang, Language tLang, String join);

}
