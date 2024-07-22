package org.omegat.core.machinetranslators;

import org.omegat.gui.exttrans.IMachineTranslation;
import org.omegat.util.Language;

public abstract class BaseCachedTranslate extends BaseTranslate implements IMachineTranslation{

	protected String getPreferenceName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

    public final String getCachedTranslation(Language sLang, Language tLang, String text) {
        return null;
    }
    
	protected String translate(Language sLang, Language tLang, String text) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    protected String getFromCache(Language sLang, Language tLang, String text) {
        return null;
    }

    protected String putToCache(Language sLang, Language tLang, String text, String result) {
        return null;
    }

    protected void clearCache() {
    }
}
