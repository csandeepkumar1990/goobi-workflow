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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Ruleset;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperComparator;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.ExportFileformat;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataGroupType;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;

public class MetadatenHelper implements Comparator<Object> {
    private static final Logger logger = Logger.getLogger(MetadatenHelper.class);
    public static final int PAGENUMBER_FIRST = 0;
    public static final int PAGENUMBER_LAST = 1;

    private Prefs myPrefs;
    private DigitalDocument mydocument;

    /* =============================================================== */

    public MetadatenHelper(Prefs inPrefs, DigitalDocument inDocument) {
        this.myPrefs = inPrefs;
        this.mydocument = inDocument;
    }

    /* =============================================================== */
    public DocStruct ChangeCurrentDocstructType(DocStruct inOldDocstruct, String inNewType) throws DocStructHasNoTypeException,
    MetadataTypeNotAllowedException, TypeNotAllowedAsChildException, TypeNotAllowedForParentException {
        // inOldDocstruct.getType().getName()
        // + " soll werden zu " + inNewType);
        DocStructType dst = this.myPrefs.getDocStrctTypeByName(inNewType);
        DocStruct newDocstruct = this.mydocument.createDocStruct(dst);
        /*
         * -------------------------------- alle Metadaten hinzufügen --------------------------------
         */
        if (inOldDocstruct.getAllMetadata() != null && inOldDocstruct.getAllMetadata().size() > 0) {
            for (Metadata old : inOldDocstruct.getAllMetadata()) {
                boolean match = false;
                if (old.getValue() != null && !old.getValue().isEmpty()) {
                    if (newDocstruct.getPossibleMetadataTypes() != null && newDocstruct.getPossibleMetadataTypes().size() > 0) {
                        for (MetadataType mt : newDocstruct.getPossibleMetadataTypes()) {
                            if (mt.getName().equals(old.getType().getName())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            try {
                                newDocstruct.addMetadata(old);
                            } catch (Exception e) {
                                Helper.setFehlerMeldung("Metadata " + old.getType().getName() + " is not allowed in new element " + newDocstruct
                                        .getType().getName());
                                return inOldDocstruct;
                            }
                        } else {
                            newDocstruct.addMetadata(old);
                        }

                    } else {
                        Helper.setFehlerMeldung("Metadata " + old.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                        .getName());
                        return inOldDocstruct;
                    }
                }
            }
        }
        /*
         * -------------------------------- alle Personen hinzufügen --------------------------------
         */
        if (inOldDocstruct.getAllPersons() != null && inOldDocstruct.getAllPersons().size() > 0) {
            for (Person old : inOldDocstruct.getAllPersons()) {
                boolean match = false;
                if ((old.getFirstname() != null && !old.getFirstname().isEmpty()) || (old.getLastname() != null && !old.getLastname().isEmpty())) {

                    if (newDocstruct.getPossibleMetadataTypes() != null && newDocstruct.getPossibleMetadataTypes().size() > 0) {
                        for (MetadataType mt : newDocstruct.getPossibleMetadataTypes()) {
                            if (mt.getName().equals(old.getType().getName())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            Helper.setFehlerMeldung("Person " + old.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                            .getName());
                        } else {
                            newDocstruct.addPerson(old);
                        }
                    } else {
                        Helper.setFehlerMeldung("Person " + old.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                        .getName());
                        return inOldDocstruct;
                    }
                }
            }
        }
        if (inOldDocstruct.getAllMetadataGroups() != null && inOldDocstruct.getAllMetadataGroups().size() > 0) {
            for (MetadataGroup mg : inOldDocstruct.getAllMetadataGroups()) {
                boolean match = false;
                if (newDocstruct.getPossibleMetadataGroupTypes() != null && newDocstruct.getPossibleMetadataGroupTypes().size() > 0) {
                    for (MetadataGroupType mgt : newDocstruct.getPossibleMetadataGroupTypes()) {
                        if (mgt.getName().equals(mg.getType().getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        Helper.setFehlerMeldung("Person " + mg.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                        .getName());
                    } else {
                        newDocstruct.addMetadataGroup(mg);
                    }
                } else {
                    Helper.setFehlerMeldung("Person " + mg.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                    .getName());
                    return inOldDocstruct;
                }

            }

        }

        /*
         * -------------------------------- alle Seiten hinzufügen --------------------------------
         */
        if (inOldDocstruct.getAllToReferences() != null) {
            for (Reference p : inOldDocstruct.getAllToReferences()) {
                newDocstruct.addReferenceTo(p.getTarget(), p.getType());
            }
        }

        /*
         * -------------------------------- alle Docstruct-Children hinzufügen --------------------------------
         */
        if (inOldDocstruct.getAllChildren() != null && inOldDocstruct.getAllChildren().size() > 0) {
            for (DocStruct old : inOldDocstruct.getAllChildren()) {
                if (newDocstruct.getType().getAllAllowedDocStructTypes() != null && newDocstruct.getType().getAllAllowedDocStructTypes().size() > 0) {

                    if (!newDocstruct.getType().getAllAllowedDocStructTypes().contains(old.getType().getName())) {
                        Helper.setFehlerMeldung("Child element " + old.getType().getName() + " is not allowed in new element " + newDocstruct
                                .getType().getName());
                        return inOldDocstruct;
                    } else {
                        newDocstruct.addChild(old);
                    }
                } else {
                    Helper.setFehlerMeldung("Child element " + old.getType().getName() + " is not allowed in new element " + newDocstruct.getType()
                    .getName());
                    return inOldDocstruct;
                }
            }
        }
        /*
         * -------------------------------- neues Docstruct zum Parent hinzufügen und an die gleiche Stelle schieben, wie den Vorg?nger
         * --------------------------------
         */
        // int index = 0;
        // for (DocStruct ds : inOldDocstruct.getParent().getAllChildren()) {
        // index++;
        // if (ds.equals(inOldDocstruct)) {
        // break;
        // }
        // }
        newDocstruct.setParent(inOldDocstruct.getParent());

        int index = inOldDocstruct.getParent().getAllChildren().indexOf(inOldDocstruct);
        inOldDocstruct.getParent().getAllChildren().add(index, newDocstruct);
        /*
         * -------------------------------- altes Docstruct vom Parent entfernen und neues als aktuelles nehmen --------------------------------
         */
        inOldDocstruct.getParent().removeChild(inOldDocstruct);
        return newDocstruct;
    }

    /* =============================================================== */

    public void KnotenUp(DocStruct inStruct) throws TypeNotAllowedAsChildException {
        DocStruct parent = inStruct.getParent();
        if (parent == null) {
            return;
        }

        int index = parent.getAllChildren().indexOf(inStruct);
        if (index == 0) {
            return;
        } else {
            parent.getAllChildren().remove(inStruct);
            parent.getAllChildren().add(index - 1, inStruct);

        }

    }

    /* =============================================================== */

    public void KnotenDown(DocStruct inStruct) throws TypeNotAllowedAsChildException {
        DocStruct parent = inStruct.getParent();
        if (parent == null) {
            return;
        }
        int max = parent.getAllChildren().size();
        int index = parent.getAllChildren().indexOf(inStruct);

        if (max - 1 <= index) {
            return;
        } else {
            parent.getAllChildren().remove(inStruct);
            parent.getAllChildren().add(index + 1, inStruct);
        }

    }

    /* =============================================================== */

    /**
     * die MetadatenTypen zurückgeben
     */
    public SelectItem[] getAddableDocStructTypen(DocStruct inStruct, boolean checkTypesFromParent) {
        /*
         * -------------------------------- zuerst mal die addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<String> types;
        SelectItem myTypes[] = new SelectItem[0];

        try {
            if (!checkTypesFromParent) {
                types = inStruct.getType().getAllAllowedDocStructTypes();
            } else {
                types = inStruct.getParent().getType().getAllAllowedDocStructTypes();
            }
        } catch (RuntimeException e) {
            return myTypes;
        }

        if (types == null) {
            return myTypes;
        }

        List<DocStructType> newTypes = new ArrayList<>();
        for (String tempTitel : types) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName(tempTitel);
            if (dst != null) {
                newTypes.add(dst);
            } else {
                Helper.setMeldung(null, "Regelsatz-Fehler: ", " DocstructType " + tempTitel + " nicht definiert");
                logger.error("getAddableDocStructTypen() - Regelsatz-Fehler: DocstructType " + tempTitel + " nicht definiert");
            }
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("DocStructTypen");
        // TODO: Uses generics, if possible
        Collections.sort(newTypes, c);

        /*
         * -------------------------------- nun ein Array mit der richtigen Größe anlegen --------------------------------
         */
        int zaehler = newTypes.size();
        myTypes = new SelectItem[zaehler];

        /*
         * -------------------------------- und anschliessend alle Elemente in das Array packen --------------------------------
         */
        zaehler = 0;
        Iterator<DocStructType> it = newTypes.iterator();
        while (it.hasNext()) {
            DocStructType dst = it.next();
            String label = dst.getNameByLanguage(Helper.getMetadataLanguage());
            if (label == null) {
                label = dst.getName();
            }
            myTypes[zaehler] = new SelectItem(dst, label);
            zaehler++;
        }
        return myTypes;
    }

    /**
     * alle unbenutzen Metadaten des Docstruct löschen, Unterelemente rekursiv aufrufen
     * ================================================================
     */
    public void deleteAllUnusedElements(DocStruct inStruct) {
        inStruct.deleteUnusedPersonsAndMetadata();
        if (inStruct.getAllChildren() != null && inStruct.getAllChildren().size() > 0) {
            for (DocStruct ds : inStruct.getAllChildren()) {
                deleteAllUnusedElements(ds);
            }
        }
    }

    /**
     * die erste Imagenummer zurückgeben ================================================================
     */
    // FIXME: alphanumerisch

    public String getImageNumber(DocStruct inStrukturelement, int inPageNumber) {
        String rueckgabe = "";

        if (inStrukturelement == null) {
            return "";
        }
        List<Reference> listReferenzen = inStrukturelement.getAllReferences("to");
        if (listReferenzen != null && listReferenzen.size() > 0) {
            /*
             * -------------------------------- Referenzen sortieren --------------------------------
             */
            Collections.sort(listReferenzen, new Comparator<Reference>() {
                @Override
                public int compare(final Reference o1, final Reference o2) {
                    final Reference r1 = o1;
                    final Reference r2 = o2;
                    Integer page1 = 0;
                    Integer page2 = 0;
                    final MetadataType mdt = MetadatenHelper.this.myPrefs.getMetadataTypeByName("physPageNumber");
                    List<? extends Metadata> listMetadaten = r1.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        final Metadata meineSeite = listMetadaten.get(0);
                        page1 = Integer.parseInt(meineSeite.getValue());
                    }
                    listMetadaten = r2.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        final Metadata meineSeite = listMetadaten.get(0);
                        page2 = Integer.parseInt(meineSeite.getValue());
                    }
                    return page1.compareTo(page2);
                }
            });

            MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
            List<? extends Metadata> listSeiten = listReferenzen.get(0).getTarget().getAllMetadataByType(mdt);
            if (inPageNumber == PAGENUMBER_LAST) {
                listSeiten = listReferenzen.get(listReferenzen.size() - 1).getTarget().getAllMetadataByType(mdt);
            }
            if (listSeiten != null && listSeiten.size() > 0) {
                Metadata meineSeite = listSeiten.get(0);
                rueckgabe += meineSeite.getValue();
            }
            mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
            listSeiten = listReferenzen.get(0).getTarget().getAllMetadataByType(mdt);
            if (inPageNumber == PAGENUMBER_LAST) {
                listSeiten = listReferenzen.get(listReferenzen.size() - 1).getTarget().getAllMetadataByType(mdt);
            }
            if (listSeiten != null && listSeiten.size() > 0) {
                Metadata meineSeite = listSeiten.get(0);
                rueckgabe += ":" + meineSeite.getValue();
            }
        }
        return rueckgabe;
    }

    /**
     * vom übergebenen DocStruct alle Metadaten ermitteln und um die fehlenden DefaultDisplay-Metadaten ergänzen
     * ================================================================
     */
    @SuppressWarnings("deprecation")
    public List<? extends Metadata> getMetadataInclDefaultDisplay(DocStruct inStruct, String inLanguage, boolean inIsPerson, Process inProzess,
            boolean displayInternalMetadata) {
        List<MetadataType> displayMetadataTypes = inStruct.getDisplayMetadataTypes();
        /* sofern Default-Metadaten vorhanden sind, diese ggf. ergänzen */
        if (displayMetadataTypes != null) {
            for (MetadataType mdt : displayMetadataTypes) {
                // check, if mdt is already in the allMDs Metadata list, if not
                // - add it
                if (inStruct.getAllMetadataByType(mdt) == null || inStruct.getAllMetadataByType(mdt).isEmpty()) {
                    try {
                        if (mdt.getIsPerson()) {
                            Person p = new Person(mdt);
                            p.setRole(mdt.getName());
                            inStruct.addPerson(p);
                        } else {
                            Metadata md = new Metadata(mdt);
                            inStruct.addMetadata(md); // add this new metadata
                            // element
                        }
                    } catch (DocStructHasNoTypeException e) {
                        continue;
                    } catch (MetadataTypeNotAllowedException e) {
                        continue;
                    }
                }
            }
        }

        /*
         * wenn keine Sortierung nach Regelsatz erfolgen soll, hier alphabetisch sortieren
         */
        if (inIsPerson) {
            List<Person> persons = inStruct.getAllPersons();
            if (persons != null && !inProzess.getRegelsatz().isOrderMetadataByRuleset()) {
                Collections.sort(persons, new MetadataComparator(inLanguage));
            }
            return persons;
        } else {
            List<Metadata> metadata = inStruct.getAllMetadata();
            if (metadata != null && !inProzess.getRegelsatz().isOrderMetadataByRuleset()) {
                Collections.sort(metadata, new MetadataComparator(inLanguage));
            }
            if (displayInternalMetadata) {
                return metadata;
            } else {
                return getAllVisibleMetadataHack(inStruct);
            }

        }
    }

    /**
     * vom übergebenen DocStruct alle Metadaten ermitteln und um die fehlenden DefaultDisplay-Metadaten ergänzen
     * ================================================================
     */
    public List<MetadataGroup> getMetadataGroupsInclDefaultDisplay(DocStruct inStruct, String inLanguage, Process inProzess) {
        List<MetadataGroupType> displayMetadataTypes = inStruct.getDefaultDisplayMetadataGroupTypes();
        /* sofern Default-Metadaten vorhanden sind, diese ggf. ergänzen */
        if (displayMetadataTypes != null) {
            for (MetadataGroupType mdt : displayMetadataTypes) {
                // check, if mdt is already in the allMDs Metadata list, if not
                // - add it
                if (!(inStruct.getAllMetadataGroupsByType(mdt) != null && inStruct.getAllMetadataGroupsByType(mdt).size() != 0)) {
                    try {

                        MetadataGroup md = new MetadataGroup(mdt);
                        inStruct.addMetadataGroup(md); // add this new metadata
                        // element

                    } catch (DocStructHasNoTypeException e) {
                        continue;
                    } catch (MetadataTypeNotAllowedException e) {
                        continue;
                    }
                }

            }
        }

        /*
         * wenn keine Sortierung nach Regelsatz erfolgen soll, hier alphabetisch sortieren
         */
        // TODO order alphabetical
        return inStruct.getAllMetadataGroups();
    }

    /** TODO: Replace it, after Maven is kicked :) */
    private List<Metadata> getAllVisibleMetadataHack(DocStruct inStruct) {

        // Start with the list of all metadata.
        List<Metadata> result = new LinkedList<>();

        // Iterate over all metadata.
        if (inStruct.getAllMetadata() != null) {
            for (Metadata md : inStruct.getAllMetadata()) {
                // If the metadata has some value and it does not start with the
                // HIDDEN_METADATA_CHAR, add it to the result list.
                if (!md.getType().getName().startsWith("_")) {
                    result.add(md);
                }
            }
        }
        if (result.isEmpty()) {
            result = null;
        }
        return result;
    }

    /**
     * prüfen, ob es sich hier um eine rdf- oder um eine mets-Datei handelt ================================================================
     */
    public static String getMetaFileType(String file) throws IOException {
        /*
         * --------------------- Typen und Suchbegriffe festlegen -------------------
         */
        HashMap<String, String> types = new HashMap<>();
        types.put("metsmods", "ugh.fileformats.mets.MetsModsImportExport".toLowerCase());
        types.put("Mets", "www.loc.gov/METS/".toLowerCase());
        types.put("Rdf", "<RDF:RDF ".toLowerCase());
        types.put("XStream", "<ugh.dl.DigitalDocument>".toLowerCase());
        types.put("Lido", "lido:lido");

        FileReader input = new FileReader(file);
        BufferedReader bufRead = new BufferedReader(input);
        char[] buffer = new char[200];
        while ((bufRead.read(buffer)) >= 0) {

            String temp = new String(buffer).toLowerCase();
            Iterator<Entry<String, String>> i = types.entrySet().iterator();
            while (i.hasNext()) {
                Entry<String, String> entry = i.next();
                if (temp.contains(entry.getValue())) {
                    bufRead.close();
                    input.close();
                    return entry.getKey();
                }
            }
        }
        bufRead.close();

        return "-";
    }

    /**
     * @param inMdt
     * @return localized Title of metadata type ================================================================
     */
    public String getMetadatatypeLanguage(MetadataType inMdt) {
        String label = inMdt.getLanguage(Helper.getMetadataLanguage());
        if (label == null) {
            label = inMdt.getName();
        }
        return label;
    }

    public String getMetadataGroupTypeLanguage(MetadataGroupType inMdt) {
        String label = inMdt.getLanguage(Helper.getMetadataLanguage());
        if (label == null) {
            label = inMdt.getName();
        }
        return label;
    }

    /**
     * Comparator für die Metadaten ================================================================
     */
    // TODO: Uses generics, if possible
    public static class MetadataComparator implements Comparator<Object> {
        private String language = "de";

        public MetadataComparator(String inLanguage) {
            this.language = inLanguage;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @Override
        public int compare(Object o1, Object o2) {
            Metadata s1 = (Metadata) o1;
            Metadata s2 = (Metadata) o2;
            if (s1 == null) {
                return -1;
            }
            if (s2 == null) {
                return 1;
            }
            String name1 = "", name2 = "";
            try {
                MetadataType mdt1 = s1.getType();
                MetadataType mdt2 = s2.getType();
                name1 = mdt1.getNameByLanguage(this.language);
                name2 = mdt2.getNameByLanguage(this.language);
            } catch (java.lang.NullPointerException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Language " + language + " for metadata " + s1.getType() + " or " + s2.getType() + " is missing in ruleset");
                }
                return 0;
            }
            if (name1 == null || name1.length() == 0) {
                name1 = s1.getType().getName();
                if (name1 == null) {
                    return -1;
                }
            }
            if (name2 == null || name2.length() == 0) {
                name2 = s2.getType().getName();
                if (name2 == null) {
                    return 1;
                }
            }

            return name1.compareToIgnoreCase(name2);
        }
    }

    /**
     * Alle Rollen ermitteln, die für das übergebene Strukturelement erlaubt sind
     * 
     * @param Strukturtyp
     * @param Rollenname der aktuellen Person, damit diese ggf. in die Liste mit übernommen wird ================================================
     *            ================
     */
    public ArrayList<SelectItem> getAddablePersonRoles(DocStruct myDocStruct, String inRoleName) {
        ArrayList<SelectItem> myList = new ArrayList<>();
        /*
         * -------------------------------- zuerst mal alle addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<MetadataType> types = myDocStruct.getPossibleMetadataTypes();
        if (types == null) {
            types = new ArrayList<>();
        }
        if (inRoleName != null && inRoleName.length() > 0) {
            boolean addRole = true;
            for (MetadataType mdt : types) {
                if (mdt.getName().equals(inRoleName)) {
                    addRole = false;
                }
            }

            if (addRole) {
                types.add(this.myPrefs.getMetadataTypeByName(inRoleName));
            }
        }
        /*
         * --------------------- alle Metadatentypen, die keine Person sind, oder mit einem Unterstrich anfangen rausnehmen -------------------
         */
        for (MetadataType mdt : new ArrayList<>(types)) {
            if (!mdt.getIsPerson()) {
                types.remove(mdt);
            }
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("MetadatenTypen");
        Collections.sort(types, c);

        for (MetadataType mdt : types) {
            myList.add(new SelectItem(mdt.getName(), getMetadatatypeLanguage(mdt)));
        }
        return myList;
    }

    @Override
    public int compare(Object o1, Object o2) {
        String imageSorting = ConfigurationHelper.getInstance().getImageSorting();
        String s1 = (String) o1;
        String s2 = (String) o2;
        // comparing only prefixes of files:
        s1 = s1.substring(0, s1.lastIndexOf("."));
        s2 = s2.substring(0, s2.lastIndexOf("."));

        if (imageSorting.equalsIgnoreCase("number")) {
            try {
                Integer i1 = Integer.valueOf(s1);
                Integer i2 = Integer.valueOf(s2);
                return i1.compareTo(i2);
            } catch (NumberFormatException e) {
                return s1.compareToIgnoreCase(s2);
            }
        } else if (imageSorting.equalsIgnoreCase("alphanumeric")) {
            return s1.compareToIgnoreCase(s2);
        } else {
            return s1.compareToIgnoreCase(s2);
        }
    }

    public static Fileformat getFileformatByName(String name, Ruleset ruleset) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addUrls(ClasspathHelper.forPackage("ugh.fileformats"));
        Reflections reflections = new Reflections(builder);

        //        Set<Class<? extends Fileformat>> formatSet = new Reflections("ugh.fileformats.*").getSubTypesOf(Fileformat.class);
        Set<Class<? extends Fileformat>> formatSet = reflections.getSubTypesOf(Fileformat.class);
        for (Class<? extends Fileformat> cl : formatSet) {
            try {
                Fileformat ff = cl.newInstance();
                if (ff.isWritable() && ff.getDisplayName().equals(name)) {
                    ff.setPrefs(ruleset.getPreferences());
                    return ff;
                }
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (PreferencesException e) {
                logger.error(e);
            }

        }
        return null;
    }

    public static ExportFileformat getExportFileformatByName(String name, Ruleset ruleset) {
        Set<Class<? extends ExportFileformat>> formatSet = new Reflections("ugh.fileformats.*").getSubTypesOf(ExportFileformat.class);
        for (Class<? extends ExportFileformat> cl : formatSet) {

            try {
                ExportFileformat ff = cl.newInstance();
                if (ff.isExportable() && ff.getDisplayName().equals(name)) {
                    ff.setPrefs(ruleset.getPreferences());
                    return ff;
                }
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (PreferencesException e) {
                logger.error(e);
            }

        }
        return null;
    }

    public static Map<String, List<String>> getMetadataOfFileformat(Fileformat gdzfile, boolean includeAuthority) {

        Map<String, List<String>> metadataList = new HashMap<>();
        try {
            DocStruct ds = gdzfile.getDigitalDocument().getLogicalDocStruct();
            metadataList.put("DocStruct", Collections.singletonList(ds.getType().getName()));
            if (ds.getAllMetadataGroups() != null) {
                for (MetadataGroup mg : ds.getAllMetadataGroups()) {
                    if (mg.getPersonList() != null) {
                        for (Person p : mg.getPersonList()) {
                            if (includeAuthority) {
                                addAuthorityFromPerson(metadataList, p);
                            }
                            if (StringUtils.isNotBlank(p.getFirstname()) || StringUtils.isNotBlank(p.getLastname())) {
                                if (metadataList.containsKey(p.getType().getName())) {
                                    List<String> oldValue = metadataList.get(p.getType().getName());
                                    oldValue.add(p.getFirstname() + " " + p.getLastname());
                                    metadataList.put(p.getType().getName(), oldValue);
                                } else {
                                    List<String> list = new ArrayList<>();
                                    list.add(p.getFirstname() + " " + p.getLastname());
                                    metadataList.put(p.getType().getName(), list);
                                }
                            }
                        }
                    }
                    if (mg.getMetadataList() != null) {
                        for (Metadata md : mg.getMetadataList()) {
                            if (includeAuthority) {
                                addAuthorityFromMeta(metadataList, md);
                            }
                            if (StringUtils.isNotBlank(md.getValue())) {
                                if (metadataList.containsKey(md.getType().getName())) {
                                    List<String> oldValue = metadataList.get(md.getType().getName());
                                    oldValue.add(md.getValue());
                                    metadataList.put(md.getType().getName(), oldValue);
                                } else {
                                    List<String> list = new ArrayList<>();
                                    list.add(md.getValue());
                                    metadataList.put(md.getType().getName(), list);
                                }
                            }
                        }
                    }
                }
            }
            if (ds.getAllMetadata() != null) {
                for (Metadata md : ds.getAllMetadata()) {
                    if (includeAuthority) {
                        addAuthorityFromMeta(metadataList, md);
                    }
                    if (StringUtils.isNotBlank(md.getValue())) {
                        if (metadataList.containsKey(md.getType().getName())) {
                            List<String> oldValue = metadataList.get(md.getType().getName());
                            oldValue.add(md.getValue());
                            metadataList.put(md.getType().getName(), oldValue);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(md.getValue());
                            metadataList.put(md.getType().getName(), list);
                        }
                    }
                }
            }
            if (ds.getAllPersons() != null) {
                for (Person p : ds.getAllPersons()) {
                    if (includeAuthority) {
                        addAuthorityFromPerson(metadataList, p);
                    }
                    if (StringUtils.isNotBlank(p.getFirstname()) || StringUtils.isNotBlank(p.getLastname())) {
                        if (metadataList.containsKey(p.getType().getName())) {
                            List<String> oldValue = metadataList.get(p.getType().getName());
                            oldValue.add(p.getFirstname() + " " + p.getLastname());
                            metadataList.put(p.getType().getName(), oldValue);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(p.getFirstname() + " " + p.getLastname());
                            metadataList.put(p.getType().getName(), list);
                        }
                    }
                }
            }

            if (ds.getType().isAnchor()) {
                ds = ds.getAllChildren().get(0);
                if (ds.getAllMetadata() != null) {
                    for (Metadata md : ds.getAllMetadata()) {
                        if (includeAuthority) {
                            addAuthorityFromMeta(metadataList, md);
                        }
                        if (StringUtils.isNotBlank(md.getValue())) {
                            if (metadataList.containsKey(md.getType().getName())) {
                                List<String> oldValue = metadataList.get(md.getType().getName());
                                oldValue.add(md.getValue());
                                metadataList.put(md.getType().getName(), oldValue);
                            } else {
                                List<String> list = new ArrayList<>();
                                list.add(md.getValue());
                                metadataList.put(md.getType().getName(), list);
                            }
                        }
                    }
                }
                if (ds.getAllPersons() != null) {
                    for (Person p : ds.getAllPersons()) {
                        if (includeAuthority) {
                            addAuthorityFromPerson(metadataList, p);
                        }
                        if (StringUtils.isNotBlank(p.getFirstname()) || StringUtils.isNotBlank(p.getLastname())) {
                            if (metadataList.containsKey(p.getType().getName())) {
                                List<String> oldValue = metadataList.get(p.getType().getName());
                                oldValue.add(p.getFirstname() + " " + p.getLastname());
                                metadataList.put(p.getType().getName(), oldValue);
                            } else {
                                List<String> list = new ArrayList<>();
                                list.add(p.getFirstname() + " " + p.getLastname());
                                metadataList.put(p.getType().getName(), list);
                            }
                        }
                    }
                }
            }
            ds = gdzfile.getDigitalDocument().getPhysicalDocStruct();
            if (ds.getAllMetadata() != null) {
                for (Metadata md : ds.getAllMetadata()) {
                    if (includeAuthority) {
                        addAuthorityFromMeta(metadataList, md);
                    }
                    if (StringUtils.isNotBlank(md.getValue())) {
                        if (metadataList.containsKey(md.getType().getName())) {
                            List<String> oldValue = metadataList.get(md.getType().getName());
                            oldValue.add(md.getValue());
                            metadataList.put(md.getType().getName(), oldValue);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(md.getValue());
                            metadataList.put(md.getType().getName(), list);
                        }
                    }
                }
            }
            if (ds.getAllPersons() != null) {

                for (Person p : ds.getAllPersons()) {
                    if (includeAuthority) {
                        addAuthorityFromPerson(metadataList, p);
                    }
                    if (StringUtils.isNotBlank(p.getFirstname()) || StringUtils.isNotBlank(p.getLastname())) {
                        if (metadataList.containsKey(p.getType().getName())) {
                            List<String> oldValue = metadataList.get(p.getType().getName());
                            oldValue.add(p.getFirstname() + " " + p.getLastname());
                            metadataList.put(p.getType().getName(), oldValue);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(p.getFirstname() + " " + p.getLastname());
                            metadataList.put(p.getType().getName(), list);
                        }
                    }
                }
            }

        } catch (PreferencesException e) {
            logger.error(e);
        }
        return metadataList;
    }

    private static void addAuthorityFromPerson(Map<String, List<String>> metadataList, Person p) {
        if(StringUtils.isNotBlank(p.getAuthorityID())) {
            String key = p.getType().getName() + "_authority";
            List<String> value = metadataList.get(key);
            if(value == null) {
                value = new ArrayList<>();
                metadataList.put(key, value);
            }
            value.add(p.getAuthorityID());
        }
    }

    private static void addAuthorityFromMeta(Map<String, List<String>> metadataList, Metadata md) {
        if(StringUtils.isNotBlank(md.getAuthorityID())) {
            String key = md.getType().getName() + "_authority";
            List<String> value = metadataList.get(key);
            if(value == null) {
                value = new ArrayList<>();
                metadataList.put(key, value);
            }
            value.add(md.getAuthorityID());
        }
    }
}
