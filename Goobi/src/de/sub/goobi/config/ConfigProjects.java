package de.sub.goobi.config;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import de.sub.goobi.helper.Helper;

public class ConfigProjects {
    XMLConfiguration config;
    private String projektTitel;
    private static final Logger logger = Logger.getLogger(ConfigProjects.class);

    public ConfigProjects(String projectTitle) throws IOException {
        this(projectTitle, new Helper().getGoobiConfigDirectory() + "goobi_projects.xml");
    }

    public ConfigProjects(String projectTitle, String configPfad) throws IOException {
        if (!Files.exists(Paths.get(configPfad))) {
            throw new IOException("File not found: " + configPfad);
        }
        try {
            this.config = new XMLConfiguration(configPfad);
        } catch (ConfigurationException e) {
            logger.error(e);
            this.config = new XMLConfiguration();
        }
        this.config.setListDelimiter('&');
        this.config.setReloadingStrategy(new FileChangedReloadingStrategy());

        int countProjects = this.config.getMaxIndex("project");
        for (int i = 0; i <= countProjects; i++) {
            String title = this.config.getString("project(" + i + ")[@name]");
            if (title.equals(projectTitle)) {
                this.projektTitel = "project(" + i + ").";
                break;
            }
        }

        try {
            this.config.getBoolean(this.projektTitel + "createNewProcess.opac[@use]");
        } catch (NoSuchElementException e) {
            this.projektTitel = "project(0).";
        }

    }

    /**
     * Ermitteln eines bestimmten Paramters der Konfiguration als String
     * 
     * @return Paramter als String
     */
    public String getParamString(String inParameter) {
        try {
            this.config.setListDelimiter('&');
            String rueckgabe = this.config.getString(this.projektTitel + inParameter);
            return cleanXmlFormatedString(rueckgabe);
        } catch (RuntimeException e) {
            logger.error(e);
            return null;
        }
    }

    private String cleanXmlFormatedString(String inString) {
        if (inString != null) {
            inString = inString.replaceAll("\t", " ");
            inString = inString.replaceAll("\n", " ");
            while (inString.contains("  ")) {
                inString = inString.replaceAll("  ", " ");
            }
        }
        return inString;
    }

    /**
     * Ermitteln eines bestimmten Paramters der Konfiguration mit Angabe eines Default-Wertes
     * 
     * @return Paramter als String
     */
    public String getParamString(String inParameter, String inDefaultIfNull) {
        try {
            this.config.setListDelimiter('&');
            String myParam = this.projektTitel + inParameter;
            String rueckgabe = this.config.getString(myParam, inDefaultIfNull);
            return cleanXmlFormatedString(rueckgabe);
        } catch (RuntimeException e) {
            return inDefaultIfNull;
        }
    }

    /**
     * Ermitteln eines boolean-Paramters der Konfiguration
     * 
     * @return Paramter als String
     */
    public boolean getParamBoolean(String inParameter) {
        try {
            return this.config.getBoolean(this.projektTitel + inParameter);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Ermitteln eines long-Paramters der Konfiguration
     * 
     * @return Paramter als Long
     */
    public long getParamLong(String inParameter) {
        try {
            return this.config.getLong(this.projektTitel + inParameter);
        } catch (RuntimeException e) {
            logger.error(e);
            return 0;
        }
    }

    /**
     * Ermitteln einer Liste von Paramtern der Konfiguration
     * 
     * @return Paramter als List
     */
    @SuppressWarnings("unchecked")
    public List<String> getParamList(String inParameter) {
        try {
            return this.config.getList(this.projektTitel + inParameter);
        } catch (RuntimeException e) {
            logger.error(e);
            return new ArrayList<String>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<HierarchicalConfiguration> getList(String inParameter) {
        try {
            return config.configurationsAt(this.projektTitel + inParameter);
        } catch (RuntimeException e) {
            logger.error(e);
            return new ArrayList<HierarchicalConfiguration>();
        }
    }

}
