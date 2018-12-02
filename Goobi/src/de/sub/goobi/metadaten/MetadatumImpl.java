package de.sub.goobi.metadaten;

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
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.goobi.api.display.DisplayCase;
import org.goobi.api.display.Item;
import org.goobi.api.display.enums.DisplayType;
import org.goobi.api.display.helper.NormDatabase;

import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;

import org.goobi.beans.Process;
import org.goobi.production.plugin.interfaces.IMetadataPlugin;

import de.sub.goobi.helper.Helper;

/**
 * Die Klasse Schritt ist ein Bean für einen einzelnen Schritt mit dessen Eigenschaften und erlaubt die Bearbeitung der Schrittdetails
 * 
 * @author Steffen Hankiewicz
 * @version 1.00 - 10.01.2005
 */

public class MetadatumImpl implements Metadatum {
    private Metadata md;
    private int identifier;
    private Prefs myPrefs;
    private Process myProcess;
    private DisplayCase myValues;
    private List<SelectItem> items;
    private List<String> selectedItems;
    private IMetadataPlugin plugin;

    /**
     * Allgemeiner Konstruktor ()
     */
    public MetadatumImpl(Metadata m, int inID, Prefs inPrefs, Process inProcess, Metadaten bean) {
        this.md = m;
        this.identifier = inID;
        this.myPrefs = inPrefs;
        this.myProcess = inProcess;
        myValues = new DisplayCase(this.myProcess, this.md.getType());

        plugin = myValues.getDisplayType().getPlugin();
        if (plugin != null) {
            plugin.setMetadata(md);
            plugin.setBean(bean);
            initializeValues();
        }
        
    }

    private void initializeValues() {
        if (myValues.getDisplayType() == DisplayType.select || myValues.getDisplayType() == DisplayType.select1) {
            List<String> selectedItems = new ArrayList<String>();
            List<SelectItem> items = new ArrayList<SelectItem>();
            for (Item i : this.myValues.getItemList()) {
                items.add(new SelectItem(i.getLabel(), i.getValue()));
                if (i.getIsSelected()) {
                    selectedItems.add(i.getValue());
                }
            }
            plugin.setPossibleItems(items);
            plugin.setDefaultItems(selectedItems);
            if (selectedItems.size() == 1) {
                plugin.setDefaultValue(selectedItems.get(0));
            }
        } else {
            if (myValues.getItemList().size() == 1) {
                Item item = myValues.getItemList().get(0);
                if (item.getIsSelected()) {
                    plugin.setDefaultValue(item.getValue());
                }
            }
        }
    }

    public List<Item> getWert() {
        String value = this.md.getValue();
        if (value != null) {
            for (Item i : myValues.getItemList()) {
                if (i.getValue().equals(value)) {
                    i.setIsSelected(true);
                } else {
                    i.setIsSelected(false);
                }
            }
        }
        return this.myValues.getItemList();
    }

    public void setWert(String inWert) {
        this.md.setValue(inWert.trim());
    }

    public String getTyp() {
        String label = this.md.getType().getLanguage(Helper.getMetadataLanguage());
        if (label == null) {
            label = this.md.getType().getName();
        }
        return label;
    }

    public void setTyp(String inTyp) {
        MetadataType mdt = this.myPrefs.getMetadataTypeByName(inTyp);
        this.md.setType(mdt);
    }

    /*
     * ##################################################### ##################################################### ## ## Getter und Setter ##
     * ##################################################### ####################################################
     */

    public int getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public Metadata getMd() {
        return this.md;
    }

    public void setMd(Metadata md) {
        this.md = md;
    }

    /******************************************************
     * 
     * new functions for use of display configuration whithin xml files
     * 
     *****************************************************/

    public String getOutputType() {
        String type = this.myValues.getDisplayType().name();
        if (type.toLowerCase().startsWith("dante")) {
            return "dante";
        }
        return this.myValues.getDisplayType().name();
    }

    public List<SelectItem> getItems() {
        this.items = new ArrayList<SelectItem>();
        this.selectedItems = new ArrayList<String>();
        for (Item i : this.myValues.getItemList()) {
            this.items.add(new SelectItem(i.getLabel()));
            if (i.getIsSelected()) {
                this.selectedItems.add(i.getLabel());
            }
        }
        return this.items;
    }

    public void setItems(List<SelectItem> items) {
        for (Item i : this.myValues.getItemList()) {
            i.setIsSelected(false);
        }
        String val = "";
        for (SelectItem sel : items) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getLabel().equals(sel.getValue())) {
                    i.setIsSelected(true);
                    val += i.getValue();
                }
            }
        }
        setWert(val);
    }

    public List<String> getSelectedItems() {
        this.selectedItems = new ArrayList<String>();
        String values = this.md.getValue();
        if (values != null && values.length() != 0) {
            while (values != null && values.length() != 0) {
                int semicolon = values.indexOf(";");
                if (semicolon != -1) {
                    String value = values.substring(0, semicolon);
                    for (Item i : this.myValues.getItemList()) {
                        if (i.getValue().equals(value)) {
                            this.selectedItems.add(i.getLabel());
                        }
                    }
                    int length = values.length();
                    values = values.substring(semicolon + 1, length);
                } else {
                    for (Item i : this.myValues.getItemList()) {
                        if (i.getValue().equals(values)) {
                            this.selectedItems.add(i.getLabel());
                        }
                    }
                    values = "";
                }
            }
        } else {
            for (Item i : this.myValues.getItemList()) {
                if (i.getIsSelected()) {
                    values = values + ";" + i.getValue();
                    this.selectedItems.add(i.getLabel());
                }
            }
            if (values != null) {
                setWert(values);
            }
        }
        return this.selectedItems;
    }

    public void setSelectedItems(List<String> selectedItems) {

        String val = "";
        for (String sel : selectedItems) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getLabel().equals(sel)) {
                    val += i.getValue() + ";";
                }
            }
        }

        setWert(val);
    }

    public String getSelectedItem() {
        String value = this.md.getValue();
        if (value != null && value.length() != 0) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getValue().equals(value)) {
                    return i.getLabel();
                }
            }
        } else {
            for (Item i : this.myValues.getItemList()) {
                if (i.getIsSelected()) {
                    value = i.getValue();
                    setWert(value);
                    return i.getLabel();
                }
            }
        }
        return "";
    }

    public void setSelectedItem(String selectedItem) {

        for (Item i : this.myValues.getItemList()) {
            if (i.getLabel().equals(selectedItem)) {
                setWert(i.getValue());
            }
        }
    }

    public void setValue(String value) {
        setWert(value);
    }

    public String getValue() {
        return this.md.getValue();
    }

    public List<String> getPossibleDatabases() {
        List<NormDatabase> databaseList = NormDatabase.getAllDatabases();
        List<String> abbrev = new ArrayList<String>();
        for (NormDatabase norm : databaseList) {
            abbrev.add(norm.getAbbreviation());
        }
        return abbrev;
    }

    public String getNormdataValue() {
        return md.getAuthorityValue();
    }

    public void setNormdataValue(String normdata) {
        md.setAuthorityValue(normdata);
    }

    public void setNormDatabase(String abbrev) {
        NormDatabase database = NormDatabase.getByAbbreviation(abbrev);
        md.setAuthorityID(database.getAbbreviation());
        md.setAuthorityURI(database.getPath());
    }

    public String getNormDatabase() {
        if (md.getAuthorityURI() != null && md.getAuthorityID() != null) {
            NormDatabase ndb = NormDatabase.getByAbbreviation(md.getAuthorityID());
            return ndb.getAbbreviation();
        } else {
            return null;
        }
    }

    public boolean isNormdata() {
        return md.getType().isAllowNormdata();
    }

    public IMetadataPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(IMetadataPlugin plugin) {
        this.plugin = plugin;
    }
}
