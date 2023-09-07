/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package io.goobi.workflow.harvester.export;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sub.goobi.config.ConfigHarvester;

/**
 * Mapping class for translating DC terminology into UGH.
 * 
 * @author andrey
 */
public class MapperDC {

    /** Loggers for this class. */
    private static final Logger loggerInfo = LoggerFactory.getLogger(MapperDC.class);

    private static Map<String, String> terms = new HashMap<>();
    private static Map<String, String> languageCodes = new HashMap<>();

    /**
     * Returns the UGH translation of <code>agoraTerm</code>; null if no translation exists.
     * 
     * @param sourceTermn The term to translate.
     * @return
     */
    public static String getUGHTerm(String sourceTermn) {
        if (terms.isEmpty()) {
            loadTerms(ConfigHarvester.getInstance().getConfigFolder() + File.separator + "terms_dc.properties");
        }

        if (sourceTermn == null) {
            return null;
        }

        return terms.get(sourceTermn);
    }

    /**
     * Returns the UGH translation of <code>agoraLanguageCode</code>; original value of <code>agoraLanguageCode</code> if no translation exists.
     * 
     * @param agoraTerm The term to translate.
     * @return
     */
    public static String getUGHLanguageCode(String sourceLanguageCode) {
        if (languageCodes.isEmpty()) {
            populateLanguageCodes();

        }

        if (sourceLanguageCode == null) {
            return null;
        }

        String ughCode = languageCodes.get(sourceLanguageCode);
        if (ughCode != null) {
            return ughCode;
        }

        return sourceLanguageCode;
    }

    /**
     * Populates the language code map. Keys are DC terms, values are their UGH translations.
     */
    public static void populateLanguageCodes() {
        languageCodes.put("ger", "de");
    }

    /**
     * Populates the term translation map from the given properties file.
     * 
     * @param fileName The .properties file to load.
     * @return true if successful; false otherwise.
     */
    @SuppressWarnings("rawtypes")
    public static boolean loadTerms(String fileName) {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(fileName);
            Iterator keys = config.getKeys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                terms.put(key, config.getString(key));
                if (terms.get(key).length() == 0) {
                    terms.put(key, null);
                }
            }
            loggerInfo.info("Loaded " + terms.size() + " terms.");
        } catch (ConfigurationException e) {
            loggerInfo.error(e.getMessage());
            return false;
        }

        return true;
    }
}
