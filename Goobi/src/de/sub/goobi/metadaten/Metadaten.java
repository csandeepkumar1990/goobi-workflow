package de.sub.goobi.metadaten;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - http://www.goobi.org
 *          - http://launchpad.net/goobi-production
 *          - http://gdz.sub.uni-goettingen.de
 *          - http://www.intranda.com
 *          - http://digiverso.com
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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.goobi.api.display.helper.ConfigDisplayRules;
import org.goobi.api.display.helper.NormDatabase;
import org.goobi.beans.Process;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IMetadataPlugin;
import org.goobi.production.plugin.interfaces.IOpacPlugin;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperComparator;
import de.sub.goobi.helper.HttpClientHelper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.S3FileUtils;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.Transliteration;
import de.sub.goobi.helper.TreeNode;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.XmlArtikelZaehlen;
import de.sub.goobi.helper.XmlArtikelZaehlen.CountType;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.InvalidImagesException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibImageException;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetImageDimensionAction;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import lombok.Getter;
import lombok.Setter;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataGroupType;
import ugh.dl.MetadataType;
import ugh.dl.NamePart;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.IncompletePersonObjectException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

/**
 * Die Klasse Schritt ist ein Bean für einen einzelnen Schritt mit dessen Eigenschaften und erlaubt die Bearbeitung der Schrittdetails
 * 
 * @author Steffen Hankiewicz
 * @version 1.00 - 17.01.2005
 */
@ManagedBean(name = "Metadaten")
@SessionScoped
public class Metadaten {
    private static final Logger logger = Logger.getLogger(Metadaten.class);
    MetadatenImagesHelper imagehelper;
    MetadatenHelper metahelper;
    private boolean treeReloaden = false;
    private Fileformat gdzfile;
    @Getter
    private DocStruct myDocStruct;
    private DocStruct tempStrukturelement;
    private List<MetadatumImpl> myMetadaten = new LinkedList<>();
    private List<MetaPerson> myPersonen = new LinkedList<>();

    private List<MetadataGroupImpl> groups = new LinkedList<>();
    private MetadataGroupImpl currentGroup;
    private MetadataGroupImpl selectedGroup;
    private List<MetadataGroupImpl> tempMetadataGroups = new ArrayList<>();
    private String tempGroupType;

    private MetadatumImpl curMetadatum;

    private Metadata currentMetadata;

    private MetaPerson curPerson;

    private Person currentPerson;

    private DigitalDocument mydocument;
    private Process myProzess;
    private Prefs myPrefs;
    private String myBenutzerID;
    private String tempTyp;
    private String tempWert;
    private String tempPersonVorname;
    private String tempPersonNachname;
    private String tempPersonRolle;
    private String currentTifFolder;
    private List<String> allTifFolders;
    /* Variablen für die Zuweisung der Seiten zu Strukturelementen */
    private String alleSeitenAuswahl_ersteSeite;
    private String alleSeitenAuswahl_letzteSeite;
    private String[] alleSeitenAuswahl;
    private String[] structSeitenAuswahl;
    private SelectItem alleSeiten[];
    private MetadatumImpl alleSeitenNeu[];
    private ArrayList<MetadatumImpl> tempMetadatumList = new ArrayList<>();
    private MetadatumImpl selectedMetadatum;
    private String currentRepresentativePage = "";

    private String paginierungWert;
    private int paginierungAbSeiteOderMarkierung;
    private String paginierungArt;
    private int paginierungSeitenProImage = 1; // 1=normale Paginierung, 2=zwei
    // Spalten auf einem Image,
    // 3=nur jede zweite Seite hat
    // Seitennummer
    private boolean fictitious = false;

    private String paginationPrefix;
    private String paginationSuffix;

    private SelectItem structSeiten[];
    private MetadatumImpl structSeitenNeu[];
    private DocStruct logicalTopstruct;
    private DocStruct physicalTopstruct;
    private DocStruct currentTopstruct;

    private boolean modusHinzufuegen = false;
    private boolean modusHinzufuegenPerson = false;
    private boolean modeAddGroup = false;
    private String modusAnsicht = "Metadaten";
    private boolean modusCopyDocstructFromOtherProcess = false;

    private TreeNodeStruct3 treeOfFilteredProcess;
    private TreeNodeStruct3 tree3;
    private String myBild;

    // old image parameter
    private int myBildNummer = 0;
    private int myBildLetztes = 0;
    private int myBildCounter = 0;
    private int myBildGroesse = 30;
    private String bildNummerGeheZu = "";
    private int numberOfNavigation = 0;

    private boolean bildAnzeigen = true;
    private boolean bildZuStrukturelement = false;
    private String addDocStructType1;
    private String addDocStructType2;
    private String zurueck = "Main";
    private MetadatenSperrung sperrung = new MetadatenSperrung();
    private boolean nurLesenModus;
    private String neuesElementWohin = "4";
    private boolean modusStrukturelementVerschieben = false;
    private String additionalOpacPpns;
    private String opacSuchfeld = "12";
    private String opacKatalog;

    private String ajaxSeiteStart = "";
    private String ajaxSeiteEnde = "";
    private String pagesStart = "";
    private String pagesEnd = "";

    private String pagesStartCurrentElement = "";
    private String pagesEndCurrentElement = "";

    private HashMap<String, Boolean> treeProperties;
    private int treeWidth = 180;
    private FileManipulation fileManipulation;

    private double currentImageNo = 0;
    private double totalImageNo = 0;
    private Integer progress;

    private boolean tiffFolderHasChanged = true;
    private List<String> dataList = new ArrayList<>();

    private List<MetadatumImpl> addableMetadata = new LinkedList<>();
    private List<MetaPerson> addablePersondata = new LinkedList<>();

    private int pageNumber = 0;
    // new parameter image parameter for OpenSeadragon
    private int numberOfImagesPerPage = 96;
    private int thumbnailSizeInPixel = 200;
    private String THUMBNAIL_FORMAT = "jpg";
    private String MAINIMAGE_FORMAT = "jpg";
    private int pageNo = 0;
    private int imageIndex = 0;
    private String imageFolderName = "";
    private List<Image> allImages;
    private Image image = null;
    private List<String> imageSizes;
    private int containerWidth = 600;;
    private boolean processHasNewTemporaryMetadataFiles = false;
    private boolean sizeChanged = false;

    private List<String> normdataList = new ArrayList<>();
    private String rowIndex;
    @Getter
    @Setter
    private String groupIndex;
    private String rowType;
    private String gndSearchValue;
    private String geonamesSearchValue;
    private String searchOption;
    private String danteSearchValue;

    private IMetadataPlugin currentPlugin;

    private boolean displayHiddenMetadata = false;

    /**
     * Konstruktor ================================================================
     */
    public Metadaten() {
        this.treeProperties = new HashMap<>();

        LoginBean login = (LoginBean) Helper.getManagedBeanValue("#{LoginForm}");
        if (login != null && login.getMyBenutzer() != null) {
            this.treeProperties.put("showtreelevel", login.getMyBenutzer().isMetsDisplayHierarchy());
            this.treeProperties.put("showtitle", login.getMyBenutzer().isMetsDisplayTitle());
            bildZuStrukturelement = login.getMyBenutzer().isMetsLinkImage();
            this.treeProperties.put("showfirstpagenumber", login.getMyBenutzer().isMetsDisplayPageAssignments());
            this.treeProperties.put("fullexpanded", Boolean.valueOf(true));
            this.treeProperties.put("showpagesasajax", Boolean.valueOf(false));
            this.treeProperties.put("showThumbnails", Boolean.valueOf(false));
        } else {
            this.treeProperties = new HashMap<>();
            this.treeProperties.put("showtreelevel", Boolean.valueOf(false));
            this.treeProperties.put("showtitle", Boolean.valueOf(false));
            this.treeProperties.put("fullexpanded", Boolean.valueOf(true));
            this.treeProperties.put("showfirstpagenumber", Boolean.valueOf(false));
            this.treeProperties.put("showpagesasajax", Boolean.valueOf(false));
            this.treeProperties.put("showThumbnails", Boolean.valueOf(false));
        }
        treeProperties.put("showMetadataPopup", ConfigurationHelper.getInstance().isMetsEditorShowMetadataPopup());
    }

    /**
     * die Anzeige der Details ändern (z.B. nur die Metadaten anzeigen, oder nur die Paginierungssequenzen)
     * 
     * @return Navigationsanweisung "null" als String (also gleiche Seite reloaden)
     */
    public String AnsichtAendern() {
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Hinzufuegen() {
        this.modusHinzufuegen = true;
        getMetadatum().setValue("");
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String AddGroup() {
        this.modeAddGroup = true;

        // reset selected groups
        selectedGroup = null;
        getSelectedGroup();

        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String HinzufuegenPerson() {
        this.modusHinzufuegenPerson = true;
        this.tempPersonNachname = "";
        this.tempPersonVorname = "";
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Abbrechen() {
        this.modusHinzufuegen = false;
        this.modusHinzufuegenPerson = false;
        this.modeAddGroup = false;
        getMetadatum().setValue("");
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Reload() {

        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        } else {
            try {
                processHasNewTemporaryMetadataFiles = false;
                this.myProzess.writeMetadataFile(this.gdzfile);

            } catch (Exception e) {
                Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e);
                logger.error(e);
            }
            MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
            return "";
        }
    }

    public String toggleImageView() {
        if (treeProperties.get("showThumbnails")) {
            pageNo = ((imageIndex) / numberOfImagesPerPage);
        }
        return "";
    }

    //
    public String automaticSave() {

        try {
            processHasNewTemporaryMetadataFiles = true;
            this.myProzess.saveTemporaryMetsFile(this.gdzfile);

        } catch (Exception e) {
            Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e);
            logger.error(e);
        }
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        return "";

    }

    public String CopyGroup() {
        MetadataGroup newMetadataGroup;
        try {
            newMetadataGroup = new MetadataGroup(this.currentGroup.getMetadataGroup().getType());
            for (MetadatumImpl metadataImpl : currentGroup.getMetadataList()) {

                List<Metadata> metadataList = newMetadataGroup.getMetadataList();
                for (Metadata metadata : metadataList) {
                    if (metadata.getType().getName().equals(metadataImpl.getMd().getType().getName())) {
                        metadata.setValue(metadataImpl.getMd().getValue());
                    }
                }
            }

            this.myDocStruct.addMetadataGroup(newMetadataGroup);
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Fehler beim Kopieren von Metadaten (MetadataTypeNotAllowedException): " + e);
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Kopieren() {
        Metadata md;
        try {
            md = new Metadata(this.curMetadatum.getMd().getType());

            md.setValue(this.curMetadatum.getMd().getValue());

            if (curMetadatum.getMd().getAuthorityID() != null && curMetadatum.getMd().getAuthorityURI() != null && curMetadatum.getMd()
                    .getAuthorityValue() != null) {
                md.setAutorityFile(curMetadatum.getMd().getAuthorityID(), curMetadatum.getMd().getAuthorityURI(), curMetadatum.getMd()
                        .getAuthorityValue());
            }

            this.myDocStruct.addMetadata(md);
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Fehler beim Kopieren von Metadaten (MetadataTypeNotAllowedException): " + e.getMessage());
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Copy() {
        Metadata md;
        try {
            md = new Metadata(currentMetadata.getType());

            md.setValue(currentMetadata.getValue());

            if (currentMetadata.getAuthorityID() != null && currentMetadata.getAuthorityURI() != null && currentMetadata
                    .getAuthorityValue() != null) {
                md.setAutorityFile(currentMetadata.getAuthorityID(), currentMetadata.getAuthorityURI(), currentMetadata.getAuthorityValue());
            }

            this.myDocStruct.addMetadata(md);
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Fehler beim Kopieren von Metadaten (MetadataTypeNotAllowedException): " + e.getMessage());
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        currentMetadata = null;
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String KopierenPerson() {
        Person per;
        try {
            per = new Person(this.myPrefs.getMetadataTypeByName(this.curPerson.getP().getRole()));
            per.setFirstname(this.curPerson.getP().getFirstname());
            per.setLastname(this.curPerson.getP().getLastname());
            per.setRole(this.curPerson.getP().getRole());

            if (curPerson.getAdditionalNameParts() != null && !curPerson.getAdditionalNameParts().isEmpty()) {
                for (NamePart np : curPerson.getAdditionalNameParts()) {
                    NamePart newNamePart = new NamePart(np.getType(), np.getValue());
                    per.addNamePart(newNamePart);
                }
            }
            if (curPerson.getP().getAuthorityID() != null && curPerson.getP().getAuthorityURI() != null && curPerson.getP()
                    .getAuthorityValue() != null) {
                per.setAutorityFile(curPerson.getP().getAuthorityID(), curPerson.getP().getAuthorityURI(), curPerson.getP().getAuthorityValue());
            }

            this.myDocStruct.addPerson(per);
        } catch (IncompletePersonObjectException e) {
            logger.error("Fehler beim Kopieren von Personen (IncompletePersonObjectException): " + e.getMessage());
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Fehler beim Kopieren von Personen (MetadataTypeNotAllowedException): " + e.getMessage());
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String CopyPerson() {
        Person per;
        try {
            per = new Person(this.myPrefs.getMetadataTypeByName(currentPerson.getRole()));
            per.setFirstname(currentPerson.getFirstname());
            per.setLastname(currentPerson.getLastname());
            per.setRole(currentPerson.getRole());

            if (currentPerson.getAdditionalNameParts() != null && !currentPerson.getAdditionalNameParts().isEmpty()) {
                for (NamePart np : curPerson.getAdditionalNameParts()) {
                    NamePart newNamePart = new NamePart(np.getType(), np.getValue());
                    per.addNamePart(newNamePart);
                }
            }
            if (currentPerson.getAuthorityID() != null && currentPerson.getAuthorityURI() != null && currentPerson.getAuthorityValue() != null) {
                per.setAutorityFile(currentPerson.getAuthorityID(), currentPerson.getAuthorityURI(), currentPerson.getAuthorityValue());
            }

            this.myDocStruct.addPerson(per);
        } catch (IncompletePersonObjectException e) {
            logger.error("Fehler beim Kopieren von Personen (IncompletePersonObjectException): " + e.getMessage());
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Fehler beim Kopieren von Personen (MetadataTypeNotAllowedException): " + e.getMessage());
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String ChangeCurrentDocstructType() {

        if (this.myDocStruct != null && this.tempWert != null) {
            try {
                DocStruct rueckgabe = this.metahelper.ChangeCurrentDocstructType(this.myDocStruct, this.tempWert);
                MetadatenalsBeanSpeichern(rueckgabe);
                MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
            } catch (DocStructHasNoTypeException | MetadataTypeNotAllowedException | TypeNotAllowedAsChildException
                    | TypeNotAllowedForParentException e) {
                Helper.setFehlerMeldung("Error while changing DocStructTypes: ", e.getMessage());
                logger.error("Error while changing DocStructTypes: " + e);
            }
        }
        return "metseditor";
    }

    public String Speichern() {
        try {
            Metadata md = new Metadata(this.myPrefs.getMetadataTypeByName(this.tempTyp));
            md.setValue(this.selectedMetadatum.getValue());

            if (StringUtils.isNotBlank(selectedMetadatum.getNormdataValue())) {
                md.setAutorityFile(selectedMetadatum.getMd().getAuthorityID(), selectedMetadatum.getMd().getAuthorityURI(), selectedMetadatum.getMd()
                        .getAuthorityValue());
            }

            this.myDocStruct.addMetadata(md);
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Error while adding metadata (MetadataTypeNotAllowedException): " + e.getMessage());
        }

        /*
         * wenn TitleDocMain, dann gleich Sortiertitel mit gleichem Inhalt anlegen
         */
        if (this.tempTyp.equals("TitleDocMain") && this.myPrefs.getMetadataTypeByName("TitleDocMainShort") != null) {
            try {
                Metadata md2 = new Metadata(this.myPrefs.getMetadataTypeByName("TitleDocMainShort"));
                md2.setValue(this.selectedMetadatum.getValue());
                this.myDocStruct.addMetadata(md2);
            } catch (MetadataTypeNotAllowedException e) {
                logger.error("Error while adding title (MetadataTypeNotAllowedException): " + e.getMessage());
            }
        }

        this.modusHinzufuegen = false;
        this.selectedMetadatum.setValue("");
        this.tempWert = "";
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        return "";
    }

    public String saveGroup() {
        try {
            MetadataGroup md = new MetadataGroup(this.myPrefs.getMetadataGroupTypeByName(this.tempGroupType));

            for (MetadatumImpl mdi : selectedGroup.getMetadataList()) {
                List<Metadata> metadataList = md.getMetadataList();
                for (Metadata metadata : metadataList) {
                    if (metadata.getType().getName().equals(mdi.getMd().getType().getName())) {
                        metadata.setValue(mdi.getMd().getValue());
                    }
                }
            }

            this.myDocStruct.addMetadataGroup(md);
        } catch (MetadataTypeNotAllowedException e) {
            logger.error("Error while adding metadata (MetadataTypeNotAllowedException): " + e.getMessage());
        }

        this.modeAddGroup = false;

        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        return "";
    }

    public String loadRightFrame() {
        this.modusHinzufuegen = false;
        this.modusHinzufuegenPerson = false;
        if (neuesElementWohin.equals("3") || neuesElementWohin.equals("4")) {
            if (!docStructIsAllowed(getAddableDocStructTypenAlsKind(), getAddDocStructType2())) {
                setAddDocStructType2("");
            }

        } else {

            if (!docStructIsAllowed(getAddableDocStructTypenAlsNachbar(), getAddDocStructType1())) {
                setAddDocStructType1("");
            }
        }
        if (!tempMetadatumList.isEmpty()) {
            tempTyp = tempMetadatumList.get(0).getMd().getType().getName();
            selectedMetadatum = tempMetadatumList.get(0);
        }

        return "metseditor";
    }

    private boolean docStructIsAllowed(SelectItem[] itemList, String docstruct) {
        if (itemList != null && itemList.length > 0) {
            for (SelectItem item : itemList) {
                DocStructType type = (DocStructType) item.getValue();
                if (type.getName().equals(docstruct)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String SpeichernPerson() {
        try {
            Person per = new Person(this.myPrefs.getMetadataTypeByName(this.tempPersonRolle));
            per.setFirstname(this.tempPersonVorname);
            per.setLastname(this.tempPersonNachname);
            per.setRole(this.tempPersonRolle);

            this.myDocStruct.addPerson(per);
        } catch (IncompletePersonObjectException e) {
            Helper.setFehlerMeldung("Incomplete data for person", "");

            return "";
        } catch (MetadataTypeNotAllowedException e) {
            Helper.setFehlerMeldung("Person is for this structure not allowed", "");
            return "";
        }
        this.modusHinzufuegenPerson = false;
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String deleteGroup() {
        this.myDocStruct.removeMetadataGroup(this.currentGroup.getMetadataGroup());
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String Loeschen() {
        this.myDocStruct.removeMetadata(this.curMetadatum.getMd());
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String delete() {
        this.myDocStruct.removeMetadata(currentMetadata);
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String deletePerson() {
        this.myDocStruct.removePerson(currentPerson);
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String LoeschenPerson() {
        this.myDocStruct.removePerson(this.curPerson.getP());
        MetadatenalsBeanSpeichern(this.myDocStruct);
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    /**
     * die noch erlaubten Rollen zurückgeben ================================================================
     */
    public ArrayList<SelectItem> getAddableRollen() {
        return this.metahelper.getAddablePersonRoles(this.myDocStruct, "");
    }

    public int getSizeOfRoles() {
        try {
            return getAddableRollen().size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public void setSizeOfRoles(int i) {
        // do nothing, needed for jsp only
    }

    public int getSizeOfMetadata() {
        try {
            return getAddableMetadataTypes().size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public void setSizeOfMetadata(int i) {
        // do nothing, needed for jsp only
    }

    public int getSizeOfMetadataGroups() {
        try {
            return getAddableMetadataGroupTypes().size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public void setSizeOfMetadataGroups(int i) {
        // do nothing, needed for jsp only
    }

    /**
     * die noch erlaubten Metadaten zurückgeben ================================================================
     */
    public ArrayList<SelectItem> getAddableMetadataTypes() {
        ArrayList<SelectItem> myList = new ArrayList<>();
        /*
         * -------------------------------- zuerst mal alle addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<MetadataType> types = this.myDocStruct.getAddableMetadataTypes();
        if (types == null) {
            return myList;
        }

        /*
         * --------------------- alle Metadatentypen, die keine Person sind, oder mit einem Unterstrich anfangen rausnehmen -------------------
         */
        for (MetadataType mdt : new ArrayList<>(types)) {
            if (mdt.getIsPerson()) {
                types.remove(mdt);
            }
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("MetadatenTypen");
        Collections.sort(types, c);

        int counter = types.size();

        for (MetadataType mdt : types) {
            myList.add(new SelectItem(mdt.getName(), this.metahelper.getMetadatatypeLanguage(mdt)));
            try {
                Metadata md = new Metadata(mdt);
                MetadatumImpl mdum = new MetadatumImpl(md, counter, this.myPrefs, this.myProzess, this);
                counter++;
                this.tempMetadatumList.add(mdum);

            } catch (MetadataTypeNotAllowedException e) {
                logger.error("Fehler beim sortieren der Metadaten: " + e.getMessage());
            }
        }
        if (StringUtils.isBlank(tempTyp) && !tempMetadatumList.isEmpty()) {
            tempTyp = tempMetadatumList.get(0).getMd().getType().getName();
            selectedMetadatum = tempMetadatumList.get(0);
        }
        return myList;
    }

    public ArrayList<MetadatumImpl> getTempMetadatumList() {
        return this.tempMetadatumList;
    }

    public void setTempMetadatumList(ArrayList<MetadatumImpl> tempMetadatumList) {
        this.tempMetadatumList = tempMetadatumList;
    }

    public List<SelectItem> getAddableMetadataGroupTypes() {
        List<SelectItem> myList = new ArrayList<>();
        /*
         * -------------------------------- zuerst mal alle addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<MetadataGroupType> types = this.myDocStruct.getAddableMetadataGroupTypes();
        if (types == null) {
            return myList;
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("MetadatenGroupTypes");
        Collections.sort(types, c);

        for (MetadataGroupType mdt : types) {
            myList.add(new SelectItem(mdt.getName(), this.metahelper.getMetadataGroupTypeLanguage(mdt)));
            try {
                MetadataGroup md = new MetadataGroup(mdt);
                MetadataGroupImpl mdum = new MetadataGroupImpl(myPrefs, myProzess, md, this);
                this.tempMetadataGroups.add(mdum);

            } catch (MetadataTypeNotAllowedException e) {
                logger.error("Fehler beim sortieren der Metadaten: " + e.getMessage());
            }
        }
        return myList;
    }

    public List<MetadataGroupImpl> getTempMetadataGroupList() {
        return this.tempMetadataGroups;
    }

    public void setTempMetadataGroupList(List<MetadataGroupImpl> tempMetadatumList) {
        this.tempMetadataGroups = tempMetadatumList;
    }

    /**
     * die MetadatenTypen zurückgeben ================================================================
     */
    public SelectItem[] getMetadatenTypen() {
        /*
         * -------------------------------- zuerst mal die addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<MetadataType> types = this.myDocStruct.getAddableMetadataTypes();

        if (types == null) {
            return new SelectItem[0];
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("MetadatenTypen");
        Collections.sort(types, c);

        /*
         * -------------------------------- nun ein Array mit der richtigen Größe anlegen --------------------------------
         */
        int zaehler = types.size();
        SelectItem myTypen[] = new SelectItem[zaehler];

        /*
         * -------------------------------- und anschliessend alle Elemente in das Array packen --------------------------------
         */
        zaehler = 0;
        for (MetadataType mdt : types) {

            myTypen[zaehler] = new SelectItem(mdt.getName(), this.metahelper.getMetadatatypeLanguage(mdt));
            zaehler++;

        }

        /*
         * -------------------------------- alle Typen, die einen Unterstrich haben nochmal rausschmeissen --------------------------------
         */
        SelectItem myTypenOhneUnterstrich[] = new SelectItem[zaehler];
        for (int i = 0; i < zaehler; i++) {
            myTypenOhneUnterstrich[i] = myTypen[i];
        }
        return myTypenOhneUnterstrich;
    }

    public SelectItem[] getMetadataGroupTypes() {
        /*
         * -------------------------------- zuerst mal die addierbaren Metadatentypen ermitteln --------------------------------
         */
        List<MetadataGroupType> types = this.myDocStruct.getAddableMetadataGroupTypes();

        if (types == null) {
            return new SelectItem[0];
        }

        /*
         * -------------------------------- die Metadatentypen sortieren --------------------------------
         */
        HelperComparator c = new HelperComparator();
        c.setSortierart("MetadatenGroupTypes");
        Collections.sort(types, c);

        /*
         * -------------------------------- nun ein Array mit der richtigen Größe anlegen --------------------------------
         */
        int zaehler = types.size();
        SelectItem myTypen[] = new SelectItem[zaehler];

        /*
         * -------------------------------- und anschliessend alle Elemente in das Array packen --------------------------------
         */
        zaehler = 0;
        for (MetadataGroupType mdt : types) {

            myTypen[zaehler] = new SelectItem(mdt.getName(), this.metahelper.getMetadataGroupTypeLanguage(mdt));
            zaehler++;

        }

        /*
         * -------------------------------- alle Typen, die einen Unterstrich haben nochmal rausschmeissen --------------------------------
         */
        SelectItem myTypenOhneUnterstrich[] = new SelectItem[zaehler];
        for (int i = 0; i < zaehler; i++) {
            myTypenOhneUnterstrich[i] = myTypen[i];
        }
        return myTypenOhneUnterstrich;
    }

    /*
     * ##################################################### ##################################################### ## ## Metadaten lesen und schreiben
     * ## ##################################################### ####################################################
     */

    /**
     * Metadaten Einlesen
     * 
     */

    public String XMLlesen() {
        // myBild="";
        // this.myBildNummer = 1;

        String result = "";

        result = readXmlAndBuildTree();

        return result;
    }

    private String readXmlAndBuildTree() {

        /*
         * re-reading the config for display rules
         */
        ConfigDisplayRules.getInstance().refresh();

        try {
            Integer id = new Integer(Helper.getRequestParameter("ProzesseID"));
            this.myProzess = ProcessManager.getProcessById(id);
        } catch (NumberFormatException e1) {
            Helper.setFehlerMeldung("error while loading process data" + e1.getMessage());
            return Helper.getRequestParameter("zurueck");
            // } catch (DAOException e1) {
            // Helper.setFehlerMeldung("error while loading process data" + e1.getMessage());
            // return Helper.getRequestParameter("zurueck");
        }
        processHasNewTemporaryMetadataFiles = false;
        this.myBenutzerID = Helper.getRequestParameter("BenutzerID");
        this.alleSeitenAuswahl_ersteSeite = "";
        this.alleSeitenAuswahl_letzteSeite = "";
        this.zurueck = Helper.getRequestParameter("zurueck");
        this.nurLesenModus = Helper.getRequestParameter("nurLesen").equals("true") ? true : false;
        this.neuesElementWohin = "4";

        this.tree3 = null;
        image = null;
        treeProperties.put("showThumbnails", false);
        if (Helper.getRequestParameter("discardChanges").equals("true")) {
            myProzess.removeTemporaryMetadataFiles();
        } else if (Helper.getRequestParameter("overwriteChanges").equals("true")) {
            myProzess.overwriteMetadata();
            myProzess.removeTemporaryMetadataFiles();
        }

        try {
            String returnvalue = XMLlesenStart();
            if (returnvalue == null) {
                return "";
            }
        } catch (SwapException e) {
            Helper.setFehlerMeldung(e);
            return Helper.getRequestParameter("zurueck");
        } catch (ReadException e) {
            Helper.setFehlerMeldung(e.getMessage());
            return Helper.getRequestParameter("zurueck");
        } catch (PreferencesException e) {
            Helper.setFehlerMeldung("Error while loading metadata", e);
            return Helper.getRequestParameter("zurueck");
        } catch (WriteException e) {
            Helper.setFehlerMeldung("Error while loading metadata", e);
            return Helper.getRequestParameter("zurueck");
        } catch (IOException e) {
            Helper.setFehlerMeldung("Error while loading metadata", e);
            return Helper.getRequestParameter("zurueck");
        } catch (InterruptedException e) {
            Helper.setFehlerMeldung("Error while loading metadata", e);
            return Helper.getRequestParameter("zurueck");
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error while loading metadata", e);
            return Helper.getRequestParameter("zurueck");
        }
        getAddDocStructType2();
        createAddableData();

        TreeExpand();
        this.sperrung.setLocked(this.myProzess.getId().intValue(), this.myBenutzerID);
        return "metseditor";
    }

    /**
     * Metadaten Einlesen
     * 
     * @throws ReadException
     * @throws InterruptedException
     * @throws IOException
     * @throws PreferencesException ============================================================ == ==
     * @throws DAOException
     * @throws SwapException
     * @throws WriteException
     */

    public String XMLlesenStart() throws ReadException, IOException, InterruptedException, PreferencesException, SwapException, DAOException,
    WriteException {
        currentRepresentativePage = "";
        this.myPrefs = this.myProzess.getRegelsatz().getPreferences();
        this.modusHinzufuegen = false;
        this.modusHinzufuegenPerson = false;
        this.modusStrukturelementVerschieben = false;
        this.modusCopyDocstructFromOtherProcess = false;

        this.currentTifFolder = null;
        readAllTifFolders();

        /*
         * -------------------------------- Dokument einlesen --------------------------------
         */
        this.gdzfile = this.myProzess.readMetadataFile();
        if (gdzfile == null) {
            return null;
        }
        this.mydocument = this.gdzfile.getDigitalDocument();

        this.mydocument.addAllContentFiles();
        this.metahelper = new MetadatenHelper(this.myPrefs, this.mydocument);
        this.imagehelper = new MetadatenImagesHelper(this.myPrefs, this.mydocument);

        /*
         * -------------------------------- Das Hauptelement ermitteln --------------------------------
         */

        this.logicalTopstruct = this.mydocument.getLogicalDocStruct();

        // this exception needs some serious feedback because data is corrupted
        if (this.logicalTopstruct == null) {
            throw new ReadException(Helper.getTranslation("metaDataError"));
        }
        checkImageNames();
        retrieveAllImages();
        // check filenames, correct them

        // initialize image list
        numberOfImagesPerPage = ConfigurationHelper.getInstance().getMetsEditorNumberOfImagesPerPage();
        thumbnailSizeInPixel = ConfigurationHelper.getInstance().getMetsEditorThumbnailSize();
        imageSizes = ConfigurationHelper.getInstance().getMetsEditorImageSizes();

        pageNo = 0;
        imageIndex = 0;
        loadCurrentImages(true);

        if (this.mydocument.getPhysicalDocStruct().getAllMetadata() != null && this.mydocument.getPhysicalDocStruct().getAllMetadata().size() > 0) {
            for (Metadata md : this.mydocument.getPhysicalDocStruct().getAllMetadata()) {
                if (md.getType().getName().equals("_representative")) {
                    try {
                        Integer value = new Integer(md.getValue());
                        currentRepresentativePage = String.valueOf(value - 1);
                    } catch (Exception e) {

                    }
                }
            }
        }

        createDefaultValues(this.logicalTopstruct);
        MetadatenalsBeanSpeichern(this.logicalTopstruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.logicalTopstruct, false);
        physicalTopstruct = mydocument.getPhysicalDocStruct();
        currentTopstruct = logicalTopstruct;
        if (this.nurLesenModus) {
            // inserted to make Paginierung the starting view
            this.modusAnsicht = "Metadaten";
        }
        return "metseditor";
    }

    private void loadCurrentImages(boolean jumpToFirstPage) {
        allImages = new ArrayList<>();
        try {
            List<String> imageNames = imagehelper.getImageFiles(myProzess, currentTifFolder);
            if (imageNames != null && !imageNames.isEmpty()) {
                imageFolderName = myProzess.getImagesDirectory() + currentTifFolder + File.separator;
                imageFolderName = imageFolderName.replaceAll("\\\\", "/");
                int order = 1;
                for (String imagename : imageNames) {
                    Image image = new Image(myProzess, imageFolderName, imagename, order++, thumbnailSizeInPixel);
                    allImages.add(image);
                }
                if (jumpToFirstPage) {
                    setImageIndex(0);
                }
            }
        } catch (InvalidImagesException | SwapException | DAOException | IOException | InterruptedException e1) {
            logger.error(e1);
        }
    }

    private void createDefaultValues(DocStruct element) {
        if (ConfigurationHelper.getInstance().isMetsEditorEnableDefaultInitialisation()) {
            MetadatenalsBeanSpeichern(element);
            if (element.getAllChildren() != null && element.getAllChildren().size() > 0) {
                for (DocStruct ds : element.getAllChildren()) {
                    createDefaultValues(ds);
                }
            }
        }
    }

    public boolean isCheckForRepresentative() {
        MetadataType mdt = myPrefs.getMetadataTypeByName("_representative");
        if (mdt != null) {
            return true;
        }
        return false;
    }

    /**
     * Metadaten Schreiben
     * 
     * @throws InterruptedException
     * @throws IOException ============================================================ == ==
     * @throws DAOException
     * @throws SwapException
     * @throws WriteException
     * @throws PreferencesException
     */
    public String XMLschreiben() {

        XmlArtikelZaehlen zaehlen = new XmlArtikelZaehlen();

        this.myProzess.setSortHelperDocstructs(zaehlen.getNumberOfUghElements(this.logicalTopstruct, CountType.DOCSTRUCT));
        this.myProzess.setSortHelperMetadata(zaehlen.getNumberOfUghElements(this.logicalTopstruct, CountType.METADATA));
        try {
            this.myProzess.setSortHelperImages(StorageProvider.getInstance().getNumberOfFiles(Paths.get(this.myProzess.getImagesOrigDirectory(
                    true))));
            ProcessManager.saveProcess(this.myProzess);
        } catch (DAOException e) {
            Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e);
            logger.error(e);
        } catch (Exception e) {
            Helper.setFehlerMeldung("error while counting current images", e);
            logger.error(e);
        }
        /* xml-Datei speichern */
        // MetadatenDebuggen(gdzfile.getDigitalDocument().getLogicalDocStruct());
        /*
         * --------------------- vor dem Speichern alle ungenutzen Docstructs rauswerfen -------------------
         */
        this.metahelper.deleteAllUnusedElements(this.mydocument.getLogicalDocStruct());

        if (currentRepresentativePage != null && currentRepresentativePage.length() > 0) {
            boolean match = false;
            if (this.mydocument.getPhysicalDocStruct() != null && this.mydocument.getPhysicalDocStruct().getAllMetadata() != null && this.mydocument
                    .getPhysicalDocStruct().getAllMetadata().size() > 0) {
                for (Metadata md : this.mydocument.getPhysicalDocStruct().getAllMetadata()) {
                    if (md.getType().getName().equals("_representative")) {
                        Integer value = new Integer(currentRepresentativePage);
                        md.setValue(String.valueOf(value + 1));
                        match = true;
                    }
                }
            }
            if (!match) {
                MetadataType mdt = myPrefs.getMetadataTypeByName("_representative");
                try {
                    Metadata md = new Metadata(mdt);
                    Integer value = new Integer(currentRepresentativePage);
                    md.setValue(String.valueOf(value + 1));
                    this.mydocument.getPhysicalDocStruct().addMetadata(md);
                } catch (MetadataTypeNotAllowedException e) {

                }

            }
        }

        try {
            // if (!new MetadatenVerifizierungWithoutHibernate().validateIdentifier(gdzfile.getDigitalDocument().getLogicalDocStruct())) {
            // return false;
            // }
            this.myProzess.writeMetadataFile(this.gdzfile);
        } catch (Exception e) {
            Helper.setFehlerMeldung("fehlerNichtSpeicherbar", e);
            logger.error(e);
            return "Metadaten";
        }
        myProzess.removeTemporaryMetadataFiles();

        SperrungAufheben();
        return this.zurueck;
    }

    /**
     * vom aktuellen Strukturelement alle Metadaten einlesen
     * 
     * @param inStrukturelement ============================================================== ==
     */

    private void MetadatenalsBeanSpeichern(DocStruct inStrukturelement) {
        this.myDocStruct = inStrukturelement;
        LinkedList<MetadatumImpl> lsMeta = new LinkedList<>();
        LinkedList<MetaPerson> lsPers = new LinkedList<>();
        List<MetadataGroupImpl> metaGroups = new LinkedList<>();
        /*
         * -------------------------------- alle Metadaten und die DefaultDisplay-Werte anzeigen --------------------------------
         */
        List<? extends Metadata> myTempMetadata = this.metahelper.getMetadataInclDefaultDisplay(inStrukturelement, Helper.getMetadataLanguage(),
                false, this.myProzess, displayHiddenMetadata);
        if (myTempMetadata != null) {
            for (Metadata metadata : myTempMetadata) {
                MetadatumImpl meta = new MetadatumImpl(metadata, 0, this.myPrefs, this.myProzess, this);
                meta.getSelectedItem();
                lsMeta.add(meta);
            }
        }

        /*
         * -------------------------------- alle Personen und die DefaultDisplay-Werte ermitteln --------------------------------
         */
        myTempMetadata = this.metahelper.getMetadataInclDefaultDisplay(inStrukturelement, Helper.getMetadataLanguage(), true, this.myProzess,
                displayHiddenMetadata);
        if (myTempMetadata != null) {
            for (Metadata metadata : myTempMetadata) {
                lsPers.add(new MetaPerson((Person) metadata, 0, this.myPrefs, inStrukturelement, myProzess, this));
            }
        }

        List<MetadataGroup> groups = this.metahelper.getMetadataGroupsInclDefaultDisplay(inStrukturelement, Helper.getMetadataLanguage(),
                this.myProzess);
        if (groups != null) {
            for (MetadataGroup mg : groups) {
                metaGroups.add(new MetadataGroupImpl(myPrefs, myProzess, mg, this));
            }
        }

        this.myMetadaten = lsMeta;
        this.myPersonen = lsPers;
        this.groups = metaGroups;

        /*
         * -------------------------------- die zugehörigen Seiten ermitteln --------------------------------
         */
        StructSeitenErmitteln(this.myDocStruct);
    }

    /*
     * ##################################################### ##################################################### ## ## Treeview ##
     * ##################################################### ####################################################
     */

    private String MetadatenalsTree3Einlesen1(TreeNodeStruct3 inTree, DocStruct inLogicalTopStruct, boolean expandAll) {
        this.tree3 = buildTree(inTree, inLogicalTopStruct, expandAll);

        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "metseditor";
    }

    @SuppressWarnings("rawtypes")
    private TreeNodeStruct3 buildTree(TreeNodeStruct3 inTree, DocStruct inLogicalTopStruct, boolean expandAll) {
        HashMap map;
        TreeNodeStruct3 knoten;
        List<DocStruct> status = new ArrayList<>();

        /*
         * -------------------------------- den Ausklapp-Zustand aller Knoten erfassen --------------------------------
         */
        if (inTree != null) {
            for (Iterator iter = inTree.getChildrenAsList().iterator(); iter.hasNext();) {
                map = (HashMap) iter.next();
                knoten = (TreeNodeStruct3) map.get("node");
                if (knoten.isExpanded()) {
                    status.add(knoten.getStruct());
                }
            }
        }

        if (inLogicalTopStruct == null) {
            return inTree;
        }
        /*
         * -------------------------------- Die Struktur als Tree3 aufbereiten --------------------------------
         */
        String label = inLogicalTopStruct.getType().getNameByLanguage(Helper.getMetadataLanguage());
        if (label == null) {
            label = inLogicalTopStruct.getType().getName();
        }

        inTree = new TreeNodeStruct3(label, inLogicalTopStruct);
        MetadatenalsTree3Einlesen2(inLogicalTopStruct, inTree);

        /*
         * -------------------------------- den Ausklappzustand nach dem neu-Einlesen wieder herstellen --------------------------------
         */
        for (Iterator iter = inTree.getChildrenAsListAlle().iterator(); iter.hasNext();) {
            map = (HashMap) iter.next();
            knoten = (TreeNodeStruct3) map.get("node");
            // Ausklappstatus wiederherstellen
            if (status.contains(knoten.getStruct()) || expandAll) {
                knoten.setExpanded(true);
            }
            // Selection wiederherstellen
            if (this.myDocStruct == knoten.getStruct()) {
                knoten.setSelected(true);
            }
        }
        return inTree;
    }

    /**
     * Metadaten in Tree3 ausgeben
     * 
     * @param inStrukturelement ============================================================== ==
     */
    private void MetadatenalsTree3Einlesen2(DocStruct inStrukturelement, TreeNodeStruct3 OberKnoten) {
        OberKnoten.setMainTitle(MetadatenErmitteln(inStrukturelement, "TitleDocMain"));
        OberKnoten.setZblNummer(MetadatenErmitteln(inStrukturelement, "ZBLIdentifier"));
        OberKnoten.setZblSeiten(MetadatenErmitteln(inStrukturelement, "ZBLPageNumber"));
        OberKnoten.setPpnDigital(MetadatenErmitteln(inStrukturelement, "IdentifierDigital"));
        OberKnoten.setDateIssued(MetadatenErmitteln(inStrukturelement, "DateIssued"));
        OberKnoten.setPartNumber(MetadatenErmitteln(inStrukturelement, "PartNumber"));
        OberKnoten.setFirstImage(this.metahelper.getImageNumber(inStrukturelement, MetadatenHelper.PAGENUMBER_FIRST));
        OberKnoten.setLastImage(this.metahelper.getImageNumber(inStrukturelement, MetadatenHelper.PAGENUMBER_LAST));
        // wenn es ein Heft ist, die Issue-Number mit anzeigen
        if (inStrukturelement.getType().getName().equals("PeriodicalIssue")) {
            OberKnoten.setDescription(OberKnoten.getDescription() + " " + MetadatenErmitteln(inStrukturelement, "CurrentNo"));
        }

        // wenn es ein Periodical oder PeriodicalVolume ist, dann ausklappen
        if (inStrukturelement.getType().getName().equals("Periodical") || inStrukturelement.getType().getName().equals("PeriodicalVolume")) {
            OberKnoten.setExpanded(true);
        }

        /*
         * -------------------------------- vom aktuellen Strukturelement alle Kinder in den Tree packen --------------------------------
         */
        List<DocStruct> meineListe = inStrukturelement.getAllChildren();
        if (meineListe != null) {
            /* es gibt Kinder-Strukturelemente */
            for (DocStruct kind : meineListe) {
                String label = kind.getType().getNameByLanguage(Helper.getMetadataLanguage());
                if (label == null) {
                    label = kind.getType().getName();
                }
                TreeNodeStruct3 tns = new TreeNodeStruct3(label, kind);
                OberKnoten.addChild(tns);
                MetadatenalsTree3Einlesen2(kind, tns);
            }
        }
    }

    /**
     * Metadaten gezielt zurückgeben
     * 
     * @param inStrukturelement ============================================================== ==
     */
    private String MetadatenErmitteln(DocStruct inStrukturelement, String inTyp) {
        String rueckgabe = "";
        List<Metadata> allMDs = inStrukturelement.getAllMetadata();
        if (allMDs != null) {
            for (Metadata md : allMDs) {
                if (md.getType().getName().equals(inTyp)) {
                    rueckgabe += (md.getValue() == null ? "" : md.getValue()) + " ";
                }
            }
        }
        return rueckgabe.trim();
    }

    @SuppressWarnings("rawtypes")
    public void setMyStrukturelement(DocStruct inStruct) {
        rowIndex = null;
        this.modusHinzufuegen = false;
        this.modusHinzufuegenPerson = false;
        MetadatenalsBeanSpeichern(inStruct);

        /*
         * -------------------------------- die Selektion kenntlich machen --------------------------------
         */
        for (Iterator iter = this.tree3.getChildrenAsListAlle().iterator(); iter.hasNext();) {

            HashMap map = (HashMap) iter.next();
            TreeNodeStruct3 knoten = (TreeNodeStruct3) map.get("node");
            // Selection wiederherstellen
            if (this.myDocStruct == knoten.getStruct()) {
                knoten.setSelected(true);
            } else {
                knoten.setSelected(false);
            }
        }

        SperrungAktualisieren();
    }

    /**
     * Knoten nach oben schieben ================================================================
     */
    public String KnotenUp() {
        try {
            this.metahelper.KnotenUp(this.myDocStruct);
        } catch (TypeNotAllowedAsChildException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fehler beim Verschieben des Knotens: " + e.getMessage());
            }
        }
        return MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    /**
     * Knoten nach unten schieben ================================================================
     */
    public String KnotenDown() {
        try {
            this.metahelper.KnotenDown(this.myDocStruct);
        } catch (TypeNotAllowedAsChildException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fehler beim Verschieben des Knotens: " + e.getMessage());
            }
        }
        return MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    /**
     * Knoten zu einer anderen Stelle
     * 
     * @throws TypeNotAllowedAsChildException ============================================================ == ==
     */
    public String KnotenVerschieben() throws TypeNotAllowedAsChildException {
        this.myDocStruct.getParent().removeChild(this.myDocStruct);
        this.tempStrukturelement.addChild(this.myDocStruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        this.neuesElementWohin = "1";
        return "metseditor";
    }

    /**
     * Knoten nach oben schieben
     * 
     * @throws IOException ============================================================ == ==
     */
    public String KnotenDelete() throws IOException {
        if (this.myDocStruct != null && this.myDocStruct.getParent() != null) {
            DocStruct tempParent = this.myDocStruct.getParent().getPreviousChild(this.myDocStruct);
            if (tempParent == null) {
                tempParent = this.myDocStruct;
            }

            this.myDocStruct.getParent().removeChild(this.myDocStruct);
            this.myDocStruct = tempParent;

        }
        // den Tree neu einlesen
        return MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    public String duplicateNode() {
        if (myDocStruct != null && myDocStruct.getParent() != null) {
            List<DocStruct> siblings = myDocStruct.getParent().getAllChildren();
            int index = siblings.indexOf(myDocStruct);
            try {
                DocStruct ds = mydocument.createDocStruct(myDocStruct.getType());
                ds.setParent(myDocStruct.getParent());
                List<Reference> references = myDocStruct.getAllToReferences();
                if (!references.isEmpty()) {
                    Reference last = references.get(references.size() - 1);
                    Integer pageNo = Integer.parseInt(last.getTarget().getAllMetadataByType(myPrefs.getMetadataTypeByName("physPageNumber")).get(0)
                            .getValue());
                    if (alleSeitenNeu.length > pageNo) {
                        ds.addReferenceTo(this.alleSeitenNeu[pageNo].getMd().getDocStruct(), "logical_physical");
                    }

                }
                siblings.add(index + 1, ds);
                myDocStruct = ds;
            } catch (TypeNotAllowedForParentException e) {
                logger.error(e);
            }
        }

        return MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    /**
     * Knoten hinzufügen
     * 
     * @throws TypeNotAllowedForParentException
     * @throws IOException
     * @throws TypeNotAllowedForParentException
     * @throws TypeNotAllowedAsChildException
     * @throws TypeNotAllowedAsChildException ============================================================ == ==
     */
    public String KnotenAdd() throws TypeNotAllowedForParentException, TypeNotAllowedAsChildException {

        /*
         * -------------------------------- prüfen, wohin das Strukturelement gepackt werden soll, anschliessend entscheiden, welches Strukturelement
         * gewählt wird und abschliessend richtig einfügen --------------------------------
         */

        DocStruct ds = null;
        /*
         * -------------------------------- vor das aktuelle Element --------------------------------
         */
        if (this.neuesElementWohin.equals("1")) {
            if (getAddDocStructType1() == null || getAddDocStructType1().equals("")) {
                return "metseditor";
            }
            DocStructType dst = this.myPrefs.getDocStrctTypeByName(getAddDocStructType1());
            ds = this.mydocument.createDocStruct(dst);
            if (this.myDocStruct == null) {
                return "metseditor";
            }
            DocStruct parent = this.myDocStruct.getParent();
            if (parent == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("das gewählte Element kann den Vater nicht ermitteln");
                }
                return "metseditor";
            }
            List<DocStruct> alleDS = new ArrayList<>();

            /* alle Elemente des Parents durchlaufen */
            for (Iterator<DocStruct> iter = parent.getAllChildren().iterator(); iter.hasNext();) {
                DocStruct tempDS = iter.next();

                /* wenn das aktuelle Element das gesuchte ist */
                if (tempDS == this.myDocStruct) {
                    alleDS.add(ds);
                }
                alleDS.add(tempDS);
            }

            /* anschliessend alle Childs entfernen */
            for (Iterator<DocStruct> iter = alleDS.iterator(); iter.hasNext();) {
                parent.removeChild(iter.next());
            }

            /* anschliessend die neue Childliste anlegen */
            for (Iterator<DocStruct> iter = alleDS.iterator(); iter.hasNext();) {
                parent.addChild(iter.next());
            }
        }

        /*
         * -------------------------------- hinter das aktuelle Element --------------------------------
         */
        if (this.neuesElementWohin.equals("2")) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName(getAddDocStructType1());
            ds = this.mydocument.createDocStruct(dst);
            DocStruct parent = this.myDocStruct.getParent();
            if (parent == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("das gewählte Element kann den Vater nicht ermitteln");
                }
                return "metseditor";
            }
            List<DocStruct> alleDS = new ArrayList<>();

            /* alle Elemente des Parents durchlaufen */
            for (Iterator<DocStruct> iter = parent.getAllChildren().iterator(); iter.hasNext();) {
                DocStruct tempDS = iter.next();
                alleDS.add(tempDS);
                /* wenn das aktuelle Element das gesuchte ist */
                if (tempDS == this.myDocStruct) {
                    alleDS.add(ds);
                }
            }

            /* anschliessend alle Childs entfernen */
            for (Iterator<DocStruct> iter = alleDS.iterator(); iter.hasNext();) {
                parent.removeChild(iter.next());
            }

            /* anschliessend die neue Childliste anlegen */
            for (Iterator<DocStruct> iter = alleDS.iterator(); iter.hasNext();) {
                parent.addChild(iter.next());
            }
        }

        /*
         * -------------------------------- als erstes Child --------------------------------
         */
        if (this.neuesElementWohin.equals("3")) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName(getAddDocStructType2());
            ds = this.mydocument.createDocStruct(dst);
            DocStruct parent = this.myDocStruct;
            if (parent == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("das gewählte Element kann den Vater nicht ermitteln");
                }
                return "metseditor";
            }
            List<DocStruct> alleDS = new ArrayList<>();
            alleDS.add(ds);

            if (parent.getAllChildren() != null && parent.getAllChildren().size() != 0) {
                alleDS.addAll(parent.getAllChildren());
                parent.getAllChildren().retainAll(new ArrayList<DocStruct>());
            }

            /* anschliessend die neue Childliste anlegen */
            for (Iterator<DocStruct> iter = alleDS.iterator(); iter.hasNext();) {
                parent.addChild(iter.next());
            }
        }

        /*
         * -------------------------------- als letztes Child --------------------------------
         */
        if (this.neuesElementWohin.equals("4")) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName(getAddDocStructType2());
            ds = this.mydocument.createDocStruct(dst);
            this.myDocStruct.addChild(ds);
        }

        if (!addableMetadata.isEmpty()) {
            for (MetadatumImpl mdi : addableMetadata) {
                try {
                    Metadata md = new Metadata(mdi.getMd().getType());
                    md.setValue(mdi.getValue());
                    md.setAuthorityID(mdi.getMd().getAuthorityID());
                    md.setAuthorityURI(mdi.getMd().getAuthorityURI());
                    md.setAuthorityValue(mdi.getMd().getAuthorityValue());
                    ds.addMetadata(md);

                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    logger.error(e);
                }
            }
        }
        if (!addablePersondata.isEmpty()) {
            for (MetaPerson mdp : addablePersondata) {
                try {
                    Person p = mdp.getP();
                    Person md = new Person(p.getType());
                    md.setFirstname(p.getFirstname());
                    md.setLastname(p.getLastname());
                    md.setAuthorityID(p.getAuthorityID());
                    md.setAuthorityURI(p.getAuthorityURI());
                    md.setAuthorityValue(p.getAuthorityValue());
                    ds.addPerson(p);
                } catch (MetadataTypeNotAllowedException | IncompletePersonObjectException e) {
                    logger.error(e);
                }
            }
        }

        if (!this.pagesStart.equals("") && !this.pagesEnd.equals("")) {
            DocStruct temp = this.myDocStruct;
            this.myDocStruct = ds;
            this.ajaxSeiteStart = this.pagesStart;
            this.ajaxSeiteEnde = this.pagesEnd;

            AjaxSeitenStartUndEndeSetzen();
            this.myDocStruct = temp;
        }
        oldDocstructName = "";
        createAddableData();
        return MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    /**
     * mögliche Docstructs als Kind zurückgeben ================================================================
     */
    public SelectItem[] getAddableDocStructTypenAlsKind() {
        SelectItem[] itemList = this.metahelper.getAddableDocStructTypen(this.myDocStruct, false);
        return itemList;
    }

    /**
     * mögliche Docstructs als Nachbar zurückgeben ================================================================
     */
    public SelectItem[] getAddableDocStructTypenAlsNachbar() {
        SelectItem[] itemList = this.metahelper.getAddableDocStructTypen(this.myDocStruct, true);
        return itemList;
    }

    private String getSelectedStructType(SelectItem[] itemList, String docTypeName) {
        if (itemList != null && itemList.length > 0) {
            for (SelectItem item : itemList) {
                if (item.getValue().toString().equals(docTypeName)) {
                    return docTypeName;
                }
            }
            return itemList[0].getValue().toString();
        } else {
            return "";
        }
    }

    /*
     * ##################################################### ##################################################### ## ## Strukturdaten: Seiten ##
     * ##################################################### ####################################################
     */

    private void checkImageNames() {
        try {
            imagehelper.checkImageNames(this.myProzess, currentTifFolder);
        } catch (TypeNotAllowedForParentException | SwapException | DAOException | IOException | InterruptedException e) {
            logger.error(e);
        }
    }

    /**
     * Markus baut eine Seitenstruktur aus den vorhandenen Images ================================================================
     * 
     * @throws DAOException
     * @throws SwapException
     */
    public String createPagination() throws TypeNotAllowedForParentException, IOException, InterruptedException, SwapException, DAOException {
        this.imagehelper.createPagination(this.myProzess, this.currentTifFolder);
        retrieveAllImages();

        // added new
        if (ConfigurationHelper.getInstance().isMetsEditorEnableImageAssignment()) {
            DocStruct log = this.mydocument.getLogicalDocStruct();
            if (log.getType().isAnchor()) {
                if (log.getAllChildren() != null && log.getAllChildren().size() > 0) {
                    log = log.getAllChildren().get(0);
                } else {
                    return "";
                }
            }

            if (log.getAllChildren() != null) {
                for (Iterator<DocStruct> iter = log.getAllChildren().iterator(); iter.hasNext();) {
                    DocStruct child = iter.next();
                    List<Reference> childRefs = child.getAllReferences("to");
                    for (Reference toAdd : childRefs) {
                        boolean match = false;
                        for (Reference ref : log.getAllReferences("to")) {
                            if (ref.getTarget().equals(toAdd.getTarget())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            log.getAllReferences("to").add(toAdd);
                        }
                    }
                }
            }
        }
        return "";
    }

    public String reloadPagination() throws TypeNotAllowedForParentException, SwapException, DAOException, IOException, InterruptedException {

        DocStruct physical = mydocument.getPhysicalDocStruct();
        if (physical != null && physical.getAllChildren() != null) {
            List<DocStruct> pages = physical.getAllChildren();

            for (DocStruct page : pages) {

                mydocument.getFileSet().removeFile(page.getAllContentFiles().get(0));

                List<Reference> refs = new ArrayList<>(page.getAllFromReferences());
                for (ugh.dl.Reference ref : refs) {
                    ref.getSource().removeReferenceTo(page);
                }
            }
        }
        while (physical.getAllChildren() != null && !physical.getAllChildren().isEmpty()) {
            physical.removeChild(physical.getAllChildren().get(0));
        }

        //                BildErmitteln(0);
        createPagination();
        loadCurrentImages(true);
        return "";
        //        return createPagination();
    }

    /**
     * alle Seiten ermitteln ================================================================
     */
    public void retrieveAllImages() {
        DigitalDocument mydocument = null;
        try {
            mydocument = this.gdzfile.getDigitalDocument();
        } catch (PreferencesException e) {
            Helper.setMeldung(null, "Can not get DigitalDocument: ", e.getMessage());
        }

        List<DocStruct> meineListe = mydocument.getPhysicalDocStruct().getAllChildren();
        if (meineListe == null) {
            this.alleSeiten = null;
            return;
        }
        int zaehler = meineListe.size();
        this.alleSeiten = new SelectItem[zaehler];
        this.alleSeitenNeu = new MetadatumImpl[zaehler];
        zaehler = 0;
        MetadataType mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
        if (meineListe != null && meineListe.size() > 0) {
            for (DocStruct mySeitenDocStruct : meineListe) {
                List<? extends Metadata> mySeitenDocStructMetadaten = mySeitenDocStruct.getAllMetadataByType(mdt);
                for (Metadata meineSeite : mySeitenDocStructMetadaten) {
                    this.alleSeitenNeu[zaehler] = new MetadatumImpl(meineSeite, zaehler, this.myPrefs, this.myProzess, this);
                    this.alleSeiten[zaehler] = new SelectItem(String.valueOf(zaehler), MetadatenErmitteln(meineSeite.getDocStruct(), "physPageNumber")
                            .trim() + ": " + meineSeite.getValue());
                }
                zaehler++;
            }
        }
    }

    /**
     * alle Seiten des aktuellen Strukturelements ermitteln ================================================================
     */
    private void StructSeitenErmitteln(DocStruct inStrukturelement) {
        if (inStrukturelement == null) {
            return;
        }
        List<Reference> listReferenzen = inStrukturelement.getAllReferences("to");
        int zaehler = 0;
        int imageNr = 0;
        if (listReferenzen != null) {
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

                    MetadataType mdt = Metadaten.this.myPrefs.getMetadataTypeByName("physPageNumber");
                    List<? extends Metadata> listMetadaten = r1.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        Metadata meineSeite = listMetadaten.get(0);
                        page1 = Integer.parseInt(meineSeite.getValue());
                    }
                    listMetadaten = r2.getTarget().getAllMetadataByType(mdt);
                    if (listMetadaten != null && listMetadaten.size() > 0) {
                        Metadata meineSeite = listMetadaten.get(0);
                        page2 = Integer.parseInt(meineSeite.getValue());
                    }
                    return page1.compareTo(page2);
                }
            });

            /* die Größe der Arrays festlegen */
            this.structSeiten = new SelectItem[listReferenzen.size()];
            this.structSeitenNeu = new MetadatumImpl[listReferenzen.size()];

            /* alle Referenzen durchlaufen und deren Metadaten ermitteln */
            for (Reference ref : listReferenzen) {
                DocStruct target = ref.getTarget();
                StructSeitenErmitteln2(target, zaehler);
                if (imageNr == 0) {
                    imageNr = StructSeitenErmitteln3(target);
                }
                zaehler++;
            }

        }

        /*
         * Wenn eine Verkn�pfung zwischen Strukturelement und Bildern sein soll, das richtige Bild anzeigen
         */
        if (this.bildZuStrukturelement) {

            if (!allImages.isEmpty()) {
                setImageIndex(imageNr - 1);
                if (imageNr % numberOfImagesPerPage == 0) {
                    pageNo = (imageNr-1) / numberOfImagesPerPage;
                } else {
                    pageNo = imageNr / numberOfImagesPerPage;
                }
                getPaginatorList();
            }

        }
    }

    /**
     * alle Seiten des aktuellen Strukturelements ermitteln 2 ================================================================
     */
    private void StructSeitenErmitteln2(DocStruct inStrukturelement, int inZaehler) {
        MetadataType mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
        List<? extends Metadata> listMetadaten = inStrukturelement.getAllMetadataByType(mdt);
        if (listMetadaten == null || listMetadaten.size() == 0) {
            return;
        }
        for (Metadata meineSeite : listMetadaten) {
            this.structSeitenNeu[inZaehler] = new MetadatumImpl(meineSeite, inZaehler, this.myPrefs, this.myProzess, this);
            this.structSeiten[inZaehler] = new SelectItem(String.valueOf(inZaehler), MetadatenErmitteln(meineSeite.getDocStruct(), "physPageNumber")
                    .trim() + ": " + meineSeite.getValue());
        }
    }

    /**
     * noch für Testzweck zum direkten öffnen der richtigen Startseite 3 ================================================================
     */
    private int StructSeitenErmitteln3(DocStruct inStrukturelement) {
        MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
        List<? extends Metadata> listMetadaten = inStrukturelement.getAllMetadataByType(mdt);
        if (listMetadaten == null || listMetadaten.size() == 0) {
            return 0;
        }
        int rueckgabe = 0;
        for (Metadata meineSeite : listMetadaten) {
            rueckgabe = Integer.parseInt(meineSeite.getValue());
        }
        return rueckgabe;
    }

    /**
     * die Paginierung ändern
     */

    public String Paginierung() {

        int[] pageSelection = new int[alleSeitenAuswahl.length];
        for (int i = 0; i < alleSeitenAuswahl.length; i++) {
            pageSelection[i] = Integer.parseInt(alleSeitenAuswahl[i]);
        }

        Paginator.Mode mode;
        switch (paginierungSeitenProImage) {
            case 2:
                mode = Paginator.Mode.COLUMNS;
                break;
            case 3:
                mode = Paginator.Mode.FOLIATION;
                break;
            case 4:
                mode = Paginator.Mode.RECTOVERSO;
                break;
            case 5:
                mode = Paginator.Mode.RECTOVERSO_FOLIATION;
                break;
            case 6:
                mode = Paginator.Mode.DOUBLE_PAGES;
                break;
            default:
                mode = Paginator.Mode.PAGES;
        }

        Paginator.Type type;
        switch (Integer.parseInt(paginierungArt)) {
            case 1:
                type = Paginator.Type.ARABIC;
                break;
            case 2:
                type = Paginator.Type.ROMAN;
                break;
            case 6:
                type = Paginator.Type.FREETEXT;
                break;
            default:
                type = Paginator.Type.UNCOUNTED;
                fictitious = false;
                paginationPrefix = "";
                paginationSuffix = "";
                break;
        }

        Paginator.Scope scope;
        switch (paginierungAbSeiteOderMarkierung) {
            case 1:
                scope = Paginator.Scope.FROMFIRST;
                break;
            default:
                scope = Paginator.Scope.SELECTED;
                break;

        }

        try {
            Paginator p = new Paginator().setPageSelection(pageSelection).setPagesToPaginate(alleSeitenNeu).setPaginationScope(scope)
                    .setPaginationType(type).setPaginationMode(mode).setFictitious(fictitious).setPaginationStartValue(paginierungWert);
            p.setPrefix(paginationPrefix);
            p.setSuffix(paginationSuffix);
            p.run();
        } catch (IllegalArgumentException iae) {
            Helper.setFehlerMeldung("fehlerBeimEinlesen", iae.getMessage());
        }

        /*
         * -------------------------------- zum Schluss nochmal alle Seiten neu einlesen --------------------------------
         */
        alleSeitenAuswahl = null;
        retrieveAllImages();
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }

        return null;
    }

    /**
     * alle Knoten des Baums expanden oder collapsen ================================================================
     */
    public String TreeExpand() {
        this.tree3.expandNodes(this.treeProperties.get("fullexpanded"));
        return "metseditor";
    }

    /*
     * ##################################################### ##################################################### ## ## Bilder-Anzeige ##
     * ##################################################### ####################################################
     */

    public int getNumberOfNavigation() {
        return numberOfNavigation;
    }

    public void setNumberOfNavigation(int numberOfNavigation) {
        this.numberOfNavigation = numberOfNavigation;
    }

    public String BildBlaettern() {
        BildErmitteln(numberOfNavigation);
        return "";
    }

    public String BildGeheZu() {
        int eingabe;
        try {
            eingabe = Integer.parseInt(this.bildNummerGeheZu);
        } catch (Exception e) {
            eingabe = this.myBildNummer;
        }
        setImageIndex(eingabe - 1);

        return "";
    }

    public String loadImageInThumbnailList() {
        int imageNumber = 0;
        try {
            imageNumber = Integer.parseInt(this.bildNummerGeheZu);
        } catch (Exception e) {
            imageNumber = this.myBildNummer;
        }
        setImageIndex(imageNumber - 1);

        //  get correct paginated page
        if (imageNumber % numberOfImagesPerPage == 0) {
            pageNo = (imageNumber-1) / numberOfImagesPerPage;
        } else {
            pageNo = imageNumber / numberOfImagesPerPage;
        }

        getPaginatorList();
        return "";
    }

    public List<String> getAllTifFolders() {
        return this.allTifFolders;
    }

    public void readAllTifFolders() throws IOException, InterruptedException, SwapException, DAOException {
        this.allTifFolders = new ArrayList<>();
        Path dir = Paths.get(this.myProzess.getImagesDirectory());

        List<String> verzeichnisse = StorageProvider.getInstance().listDirNames(dir.toString());
        for (int i = 0; i < verzeichnisse.size(); i++) {
            this.allTifFolders.add(verzeichnisse.get(i));
        }

        if (!ConfigurationHelper.getInstance().getMetsEditorDefaultSuffix().equals("")) {
            String suffix = ConfigurationHelper.getInstance().getMetsEditorDefaultSuffix();
            for (String directory : this.allTifFolders) {
                if (directory.endsWith(suffix) && !directory.startsWith(ConfigurationHelper.getInstance().getMasterDirectoryPrefix())) {
                    this.currentTifFolder = directory;
                    break;
                }
            }
        }
        if (StringUtils.isBlank(currentTifFolder) && !allTifFolders.isEmpty()) {
            this.currentTifFolder = Paths.get(this.myProzess.getImagesTifDirectory(true)).getFileName().toString();
            if (!this.allTifFolders.contains(this.currentTifFolder)) {
                this.currentTifFolder = allTifFolders.get(0);
            }
        }
    }

    public void BildErmitteln(int welches) {
        /*
         * wenn die Bilder nicht angezeigt werden, brauchen wir auch das Bild nicht neu umrechnen
         */
        if (!this.bildAnzeigen) {
            return;
        }

        // try {
        if (dataList == null || dataList.isEmpty() || tiffFolderHasChanged) {
            tiffFolderHasChanged = false;

            if (this.currentTifFolder != null) {
                try {
                    // dataList = this.imagehelper.getImageFiles(mydocument.getPhysicalDocStruct());
                    dataList = this.imagehelper.getImageFiles(this.myProzess, this.currentTifFolder);
                    if (dataList == null) {
                        myBild = null;
                        myBildNummer = -1;
                        return;
                    }
                    //
                } catch (InvalidImagesException e1) {
                    logger.error("Images could not be read", e1);
                    Helper.setFehlerMeldung("images could not be read", e1);
                }
            } else {
                try {
                    createPagination();
                    dataList = this.imagehelper.getImageFiles(mydocument.getPhysicalDocStruct());
                } catch (TypeNotAllowedForParentException e) {
                    logger.error(e);
                } catch (SwapException e) {
                    logger.error(e);
                } catch (DAOException e) {
                    logger.error(e);
                } catch (IOException e) {
                    logger.error(e);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }

        if (dataList != null && dataList.size() > 0) {
            this.myBildLetztes = dataList.size();
            for (int i = 0; i < dataList.size(); i++) {
                if (this.myBild == null) {
                    this.myBild = dataList.get(0);
                }
                String index = dataList.get(i).substring(0, dataList.get(i).lastIndexOf("."));
                String myPicture = this.myBild.substring(0, this.myBild.lastIndexOf("."));
                /* wenn das aktuelle Bild gefunden ist, das neue ermitteln */
                if (index.equals(myPicture)) {
                    int pos = i + welches;
                    /* aber keine Indexes ausserhalb des Array erlauben */
                    if (pos < 0) {
                        pos = 0;
                    }
                    if (pos > dataList.size() - 1) {
                        pos = dataList.size() - 1;
                    }
                    /* das aktuelle tif erfassen */
                    if (dataList.size() > pos) {
                        this.myBild = dataList.get(pos);
                    } else {
                        this.myBild = dataList.get(dataList.size() - 1);
                    }
                    /* die korrekte Seitenzahl anzeigen */
                    this.myBildNummer = pos + 1;
                    /* Pages-Verzeichnis ermitteln */
                    String myPfad = ConfigurationHelper.getTempImagesPathAsCompleteDirectory();
                    /*
                     * den Counter für die Bild-ID auf einen neuen Wert setzen, damit nichts gecacht wird
                     */
                    this.myBildCounter++;

                    /* Session ermitteln */
                    FacesContext context = FacesContextHelper.getCurrentFacesContext();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
                    String mySession = session.getId() + "_" + this.myBildCounter + ".png";

                    /* das neue Bild zuweisen */
                    try {
                        String tiffconverterpfad = this.myProzess.getImagesDirectory() + this.currentTifFolder + FileSystems.getDefault()
                        .getSeparator() + this.myBild;
                        if (!StorageProvider.getInstance().isFileExists(Paths.get(tiffconverterpfad))) {
                            tiffconverterpfad = this.myProzess.getImagesTifDirectory(true) + this.myBild;
                            Helper.setFehlerMeldung("formularOrdner:TifFolders", "", "image " + this.myBild + " does not exist in folder "
                                    + this.currentTifFolder + ", using image from " + Paths.get(this.myProzess.getImagesTifDirectory(true))
                                    .getFileName().toString());
                        }
                        this.imagehelper.scaleFile(tiffconverterpfad, myPfad + mySession, this.myBildGroesse, 0);
                    } catch (Exception e) {
                        Helper.setFehlerMeldung("could not find image folder", e);
                        logger.error(e);
                    }
                    break;
                }
            }
        }
        BildPruefen();
    }

    private void BildPruefen() {
        /* wenn bisher noch kein Bild da ist, das erste nehmen */
        boolean exists = false;
        try {
            if (this.currentTifFolder != null && this.myBild != null) {
                exists = StorageProvider.getInstance().isFileExists(Paths.get(this.myProzess.getImagesDirectory() + this.currentTifFolder
                        + FileSystems.getDefault().getSeparator() + this.myBild));
            }
        } catch (Exception e) {
            this.myBildNummer = -1;
            logger.error(e);
        }
        /* wenn das Bild nicht existiert, den Status ändern */
        if (!exists) {
            this.myBildNummer = -1;
        }
    }

    private boolean SperrungAktualisieren() {
        /*
         * wenn die Sperrung noch aktiv ist und auch für den aktuellen Nutzer gilt, Sperrung aktualisieren
         */
        if (MetadatenSperrung.isLocked(this.myProzess.getId().intValue()) && this.sperrung.getLockBenutzer(this.myProzess.getId().intValue()).equals(
                this.myBenutzerID)) {
            this.sperrung.setLocked(this.myProzess.getId().intValue(), this.myBenutzerID);
            return true;
        } else {
            return false;
        }
    }

    private void SperrungAufheben() {
        if (MetadatenSperrung.isLocked(this.myProzess.getId().intValue()) && this.sperrung.getLockBenutzer(this.myProzess.getId().intValue()).equals(
                this.myBenutzerID)) {
            this.sperrung.setFree(this.myProzess.getId().intValue());
        }
    }

    /*
     * ##################################################### ##################################################### ## ## Navigationsanweisungen ##
     * ##################################################### ####################################################
     */

    /**
     * zurück zur Startseite, Metadaten vorher freigeben
     */
    public String goMain() {
        SperrungAufheben();
        return "index";
    }

    /**
     * zurück gehen
     */
    public String goZurueck() {
        SperrungAufheben();
        return this.zurueck;
    }

    public String discard() {
        myProzess.removeTemporaryMetadataFiles();
        SperrungAufheben();
        return this.zurueck;
    }

    public boolean isCheckForNewerTemporaryMetadataFiles() {
        return processHasNewTemporaryMetadataFiles;
    }

    /*
     * ##################################################### ##################################################### ## ## Transliteration bestimmter
     * Felder ## ##################################################### ####################################################
     */

    public String Transliterieren() {
        Metadata md = this.curMetadatum.getMd();

        /*
         * -------------------------------- wenn es ein russischer Titel ist, dessen Transliterierungen anzeigen --------------------------------
         */
        if (md.getType().getName().equals("RUSMainTitle")) {
            Transliteration trans = new Transliteration();

            try {
                MetadataType mdt = this.myPrefs.getMetadataTypeByName("MainTitleTransliterated");
                Metadata mdDin = new Metadata(mdt);
                Metadata mdIso = new Metadata(mdt);
                mdDin.setValue(trans.transliterate_din(md.getValue()));
                mdIso.setValue(trans.transliterate_iso(md.getValue()));

                this.myDocStruct.addMetadata(mdDin);
                this.myDocStruct.addMetadata(mdIso);
            } catch (MetadataTypeNotAllowedException e) {
                logger.error("Fehler beim Hinzufügen der Transliterationen (MetadataTypeNotAllowedException): " + e.getMessage());
            }
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);

        /* zum Schluss die Sperrung aktualisieren */
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    public String TransliterierenPerson() {
        Person md = this.curPerson.getP();

        /*
         * -------------------------------- wenn es ein russischer Autor ist, dessen Transliterierungen anlegen --------------------------------
         */
        if (md.getRole().equals("Author")) {
            Transliteration trans = new Transliteration();
            try {
                MetadataType mdtDin = this.myPrefs.getMetadataTypeByName("AuthorTransliteratedDIN");
                MetadataType mdtIso = this.myPrefs.getMetadataTypeByName("AuthorTransliteratedISO");
                Person mdDin = new Person(mdtDin);
                Person mdIso = new Person(mdtIso);

                mdDin.setFirstname(trans.transliterate_din(md.getFirstname()));
                mdDin.setLastname(trans.transliterate_din(md.getLastname()));
                mdIso.setFirstname(trans.transliterate_iso(md.getFirstname()));
                mdIso.setLastname(trans.transliterate_iso(md.getLastname()));
                mdDin.setRole("AuthorTransliteratedDIN");
                mdIso.setRole("AuthorTransliteratedISO");

                this.myDocStruct.addPerson(mdDin);
                this.myDocStruct.addPerson(mdIso);
            } catch (MetadataTypeNotAllowedException e) {
                logger.error("Fehler beim Hinzufügen der Transliterationen (MetadataTypeNotAllowedException): " + e.getMessage());
            }
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);

        /* zum Schluss die Sperrung aktualisieren */
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return "";
    }

    /*
     * ##################################################### ##################################################### ## ## aus einer Liste von PPNs
     * Strukturelemente aus dem Opac ## holen und dem aktuellen Strukturelement unterordnen ## #####################################################
     * ####################################################
     */

    /**
     * mehrere PPNs aus dem Opac abfragen und dem aktuellen Strukturelement unterordnen
     * ================================================================
     */
    public String AddAdditionalOpacPpns() {
        StringTokenizer tokenizer = new StringTokenizer(this.additionalOpacPpns, "\n");
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            try {
                ConfigOpacCatalogue coc = ConfigOpac.getInstance().getCatalogueByName(getOpacKatalog());
                IOpacPlugin iopac = (IOpacPlugin) PluginLoader.getPluginByTitle(PluginType.Opac, coc.getOpacType());

                Fileformat addrdf = iopac.search(this.opacSuchfeld, tok, coc, this.myPrefs);
                if (addrdf != null) {
                    this.myDocStruct.addChild(addrdf.getDigitalDocument().getLogicalDocStruct());
                    MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
                } else {
                    Helper.setMeldung(null, "Opac abgefragt: ", "kein Ergebnis");
                }
            } catch (Exception e) {
                logger.error("Error while importing from catalogue: " + e.getMessage());
            }
        }
        return "Metadaten3links";
    }

    /**
     * eine PPN aus dem Opac abfragen und dessen Metadaten dem aktuellen Strukturelement zuweisen
     * ================================================================
     */
    public String AddMetadaFromOpacPpn() {
        StringTokenizer tokenizer = new StringTokenizer(this.additionalOpacPpns, "\n");
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            try {
                ConfigOpacCatalogue coc = ConfigOpac.getInstance().getCatalogueByName(getOpacKatalog());
                IOpacPlugin iopac = (IOpacPlugin) PluginLoader.getPluginByTitle(PluginType.Opac, coc.getOpacType());
                Fileformat addrdf = iopac.search(this.opacSuchfeld, tok, coc, this.myPrefs);
                if (addrdf != null) {

                    // remove empty default elements
                    List<Metadata> metadataList = this.myDocStruct.getAllMetadata();
                    if (metadataList != null) {
                        List<Metadata> metadataListClone = new ArrayList<>(metadataList);
                        for (Metadata md : metadataListClone) {
                            if (md.getValue() == null || md.getValue().isEmpty()) {
                                this.myDocStruct.removeMetadata(md);
                            }
                        }
                    }
                    List<Person> personList = myDocStruct.getAllPersons();
                    if (personList != null) {
                        List<Person> personListClone = new ArrayList<>(personList);
                        for (Person p : personListClone) {
                            if (p.getFirstname().isEmpty() && p.getLastname().isEmpty()) {
                                myDocStruct.removePerson(p);
                            }
                        }
                    }

                    /* die Liste aller erlaubten Metadatenelemente erstellen */
                    List<String> erlaubte = new ArrayList<>();
                    for (Iterator<MetadataType> it = this.myDocStruct.getAddableMetadataTypes().iterator(); it.hasNext();) {
                        MetadataType mt = it.next();
                        erlaubte.add(mt.getName());
                    }

                    /*
                     * wenn der Metadatentyp in der Liste der erlaubten Typen, dann hinzufügen
                     */
                    for (Iterator<Metadata> it = addrdf.getDigitalDocument().getLogicalDocStruct().getAllMetadata().iterator(); it.hasNext();) {
                        Metadata m = it.next();
                        if (erlaubte.contains(m.getType().getName())) {
                            this.myDocStruct.addMetadata(m);
                        }
                    }

                    for (Iterator<Person> it = addrdf.getDigitalDocument().getLogicalDocStruct().getAllPersons().iterator(); it.hasNext();) {
                        Person m = it.next();
                        if (erlaubte.contains(m.getType().getName())) {
                            this.myDocStruct.addPerson(m);
                        }
                    }

                    for (Iterator<MetadataGroup> it = addrdf.getDigitalDocument().getLogicalDocStruct().getAllMetadataGroups().iterator(); it
                            .hasNext();) {
                        MetadataGroup m = it.next();

                        if (myDocStruct.getAddableMetadataGroupTypes().contains(m.getType())) {
                            myDocStruct.addMetadataGroup(m);
                        }

                    }

                    MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
                } else {
                    Helper.setMeldung(null, "Opac abgefragt: ", "kein Ergebnis");
                }
            } catch (Exception e) {
                logger.error("Error while importing from catalogue: " + e.getMessage());
            }
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
        this.modusAnsicht = "Metadaten";
        return "";
    }

    /*
     * ##################################################### ##################################################### ## ## Metadatenvalidierung ##
     * ##################################################### ####################################################
     */

    public void Validate() {
        MetadatenVerifizierung mv = new MetadatenVerifizierung();
        boolean valid = mv.validate(this.gdzfile, this.myPrefs, this.myProzess);
        if (valid) {
            Helper.setMeldung("ValidationSuccessful");
        }
        MetadatenalsBeanSpeichern(this.myDocStruct);
    }

    /*
     * ##################################################### ##################################################### ## ## Auswahl der Seiten über Ajax
     * ## ##################################################### ####################################################
     */

    public String getAjaxSeiteStart() {
        return this.ajaxSeiteStart;
    }

    public void setAjaxSeiteStart(String ajaxSeiteStart) {
        this.ajaxSeiteStart = ajaxSeiteStart;
    }

    public String getAjaxSeiteEnde() {
        return this.ajaxSeiteEnde;
    }

    public void setAjaxSeiteEnde(String ajaxSeiteEnde) {
        this.ajaxSeiteEnde = ajaxSeiteEnde;
    }

    public String getPagesEnd() {
        return this.pagesEnd;
    }

    public String getPagesStart() {
        return this.pagesStart;
    }

    public void setPagesEnd(String pagesEnd) {
        this.pagesEnd = pagesEnd;
    }

    public void setPagesStart(String pagesStart) {
        this.pagesStart = pagesStart;
    }

    public void CurrentStartpage() {
        for (int i = 0; i < this.alleSeiten.length; i++) {
            SelectItem si = this.alleSeiten[i];
            if (si.getValue().equals(String.valueOf(this.pageNumber))) {
                this.pagesStart = si.getLabel();
            }
        }
    }

    public void CurrentEndpage() {
        for (int i = 0; i < this.alleSeiten.length; i++) {
            SelectItem si = this.alleSeiten[i];
            if (si.getValue().equals(String.valueOf(this.pageNumber))) {
                this.pagesEnd = si.getLabel();
            }
        }
    }

    public void startpage() {
        for (int i = 0; i < this.alleSeiten.length; i++) {
            SelectItem si = this.alleSeiten[i];
            if (si.getValue().equals(String.valueOf(this.pageNumber))) {
                this.pagesStartCurrentElement = si.getLabel();
            }
        }
    }

    public void endpage() {
        for (int i = 0; i < this.alleSeiten.length; i++) {
            SelectItem si = this.alleSeiten[i];
            if (si.getValue().equals(String.valueOf(this.pageNumber))) {
                this.pagesEndCurrentElement = si.getLabel();
            }
        }
    }

    public void setPages() {
        this.ajaxSeiteStart = this.pagesStartCurrentElement;
        this.ajaxSeiteEnde = this.pagesEndCurrentElement;

        AjaxSeitenStartUndEndeSetzen();
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
    }

    public String getPagesEndCurrentElement() {
        return pagesEndCurrentElement;
    }

    public String getPagesStartCurrentElement() {
        return pagesStartCurrentElement;
    }

    public void setPagesEndCurrentElement(String pagesEndCurrentElement) {
        this.pagesEndCurrentElement = pagesEndCurrentElement;
    }

    public void setPagesStartCurrentElement(String pagesStartCurrentElement) {
        this.pagesStartCurrentElement = pagesStartCurrentElement;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber - 1;

    }

    public List<String> getAjaxAlleSeiten(String prefix) {
        if (logger.isDebugEnabled()) {
            logger.debug("Ajax-Liste abgefragt");
        }
        List<String> li = new ArrayList<>();
        if (this.alleSeiten != null && this.alleSeiten.length > 0) {
            for (int i = 0; i < this.alleSeiten.length; i++) {
                SelectItem si = this.alleSeiten[i];
                if (si.getLabel().contains(prefix)) {
                    li.add(si.getLabel());
                }
            }
        }
        return li;
    }

    /**
     * die Seiten über die Ajax-Felder festlegen ================================================================
     */
    public void AjaxSeitenStartUndEndeSetzen() {
        boolean startseiteOk = false;
        boolean endseiteOk = false;

        /*
         * alle Seiten durchlaufen und prüfen, ob die eingestellte Seite überhaupt existiert
         */
        for (int i = 0; i < this.alleSeiten.length; i++) {
            SelectItem si = this.alleSeiten[i];
            if (si.getLabel().equals(this.ajaxSeiteStart)) {
                startseiteOk = true;
                this.alleSeitenAuswahl_ersteSeite = (String) si.getValue();
            }
            if (si.getLabel().equals(this.ajaxSeiteEnde)) {
                endseiteOk = true;
                this.alleSeitenAuswahl_letzteSeite = (String) si.getValue();
            }
        }

        /* wenn die Seiten ok sind */
        if (startseiteOk && endseiteOk) {
            SeitenStartUndEndeSetzen();
        } else {
            Helper.setFehlerMeldung("Selected image(s) unavailable");
        }
    }

    /**
     * die erste und die letzte Seite festlegen und alle dazwischen zuweisen ================================================================
     */
    public String SeitenStartUndEndeSetzen() {
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        int anzahlAuswahl = Integer.parseInt(this.alleSeitenAuswahl_letzteSeite) - Integer.parseInt(this.alleSeitenAuswahl_ersteSeite) + 1;
        if (anzahlAuswahl > 0) {
            /* alle bisher zugewiesenen Seiten entfernen */
            this.myDocStruct.getAllToReferences().clear();
            int zaehler = 0;
            while (zaehler < anzahlAuswahl) {
                this.myDocStruct.addReferenceTo(this.alleSeitenNeu[Integer.parseInt(this.alleSeitenAuswahl_ersteSeite) + zaehler].getMd()
                        .getDocStruct(), "logical_physical");
                zaehler++;
            }
        } else {
            Helper.setFehlerMeldung("mets_paginationAssignmentError");
        }
        StructSeitenErmitteln(this.myDocStruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        return null;
    }

    /**
     * die erste und die letzte Seite festlegen und alle dazwischen zuweisen ================================================================
     */

    public String SeitenVonChildrenUebernehmen() {
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }

        /* alle Kinder des aktuellen DocStructs durchlaufen */
        this.myDocStruct.getAllReferences("to").removeAll(this.myDocStruct.getAllReferences("to"));
        if (this.myDocStruct.getAllChildren() != null) {
            for (Iterator<DocStruct> iter = this.myDocStruct.getAllChildren().iterator(); iter.hasNext();) {
                DocStruct child = iter.next();
                List<Reference> childRefs = child.getAllReferences("to");
                for (Reference toAdd : childRefs) {
                    boolean match = false;
                    for (Reference ref : this.myDocStruct.getAllReferences("to")) {
                        if (ref.getTarget().equals(toAdd.getTarget())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        this.myDocStruct.getAllReferences("to").add(toAdd);
                    }

                }
            }
        }
        StructSeitenErmitteln(this.myDocStruct);
        return null;
    }

    /**
     * die erste und die letzte Seite festlegen und alle dazwischen zuweisen ================================================================
     */
    public String BildErsteSeiteAnzeigen() {
        this.bildAnzeigen = true;
        if (this.treeProperties.get("showpagesasajax")) {
            for (int i = 0; i < this.alleSeiten.length; i++) {
                SelectItem si = this.alleSeiten[i];
                if (si.getLabel().equals(this.ajaxSeiteStart)) {
                    this.alleSeitenAuswahl_ersteSeite = (String) si.getValue();
                    break;
                }
            }
        }
        try {
            int pageNumber = Integer.parseInt(this.alleSeitenAuswahl_ersteSeite) - this.myBildNummer + 1;
            setImageIndex(pageNumber - 1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * die erste und die letzte Seite festlegen und alle dazwischen zuweisen ================================================================
     */
    public String BildLetzteSeiteAnzeigen() {
        this.bildAnzeigen = true;
        if (this.treeProperties.get("showpagesasajax")) {
            for (int i = 0; i < this.alleSeiten.length; i++) {
                SelectItem si = this.alleSeiten[i];
                if (si.getLabel().equals(this.ajaxSeiteEnde)) {
                    this.alleSeitenAuswahl_letzteSeite = (String) si.getValue();
                    break;
                }
            }
        }
        try {
            int pageNumber = Integer.parseInt(this.alleSeitenAuswahl_letzteSeite) - this.myBildNummer + 1;
            setImageIndex(pageNumber - 1);
        } catch (Exception e) {

        }
        return "";
    }

    /**
     * ausgewählte Seiten dem aktuellen Strukturelement hinzufügen ================================================================
     */
    public String SeitenHinzu() {
        /* alle markierten Seiten durchlaufen */
        for (int i = 0; i < this.alleSeitenAuswahl.length; i++) {
            int aktuelleID = Integer.parseInt(this.alleSeitenAuswahl[i]);

            boolean schonEnthalten = false;

            /*
             * wenn schon References vorhanden, prüfen, ob schon enthalten, erst dann zuweisen
             */
            if (this.myDocStruct.getAllToReferences("logical_physical") != null) {
                for (Iterator<Reference> iter = this.myDocStruct.getAllToReferences("logical_physical").iterator(); iter.hasNext();) {
                    Reference obj = iter.next();
                    if (obj.getTarget() == this.alleSeitenNeu[aktuelleID].getMd().getDocStruct()) {
                        schonEnthalten = true;
                        break;
                    }
                }
            }

            if (!schonEnthalten) {
                this.myDocStruct.addReferenceTo(this.alleSeitenNeu[aktuelleID].getMd().getDocStruct(), "logical_physical");
            }
        }
        StructSeitenErmitteln(this.myDocStruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        this.alleSeitenAuswahl = null;
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return null;
    }

    /**
     * ausgewählte Seiten aus dem Strukturelement entfernen ================================================================
     */
    public String SeitenWeg() {
        for (int i = 0; i < this.structSeitenAuswahl.length; i++) {
            int aktuelleID = Integer.parseInt(this.structSeitenAuswahl[i]);
            this.myDocStruct.removeReferenceTo(this.structSeitenNeu[aktuelleID].getMd().getDocStruct());
        }
        StructSeitenErmitteln(this.myDocStruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        this.structSeitenAuswahl = null;
        if (!SperrungAktualisieren()) {
            return "metseditor_timeout";
        }
        return null;
    }

    /*
     * ##################################################### ##################################################### ## ## OCR ##
     * ##################################################### ####################################################
     */

    public boolean isImageHasOcr() {
        try {
            return FilesystemHelper.isOcrFileExists(myProzess, image.getTooltip().substring(0, image.getTooltip().lastIndexOf(".")));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isShowOcrButton() {
        if (ConfigurationHelper.getInstance().isMetsEditorUseExternalOCR()) {
            return ConfigurationHelper.getInstance().isMetsEditorShowOCRButton();
        } else {
            return isImageHasOcr();
        }
    }

    public String getOcrResult() {
        String ocrResult = "";
        if (ConfigurationHelper.getInstance().isMetsEditorUseExternalOCR()) {
            String myOcrUrl = "";

            myOcrUrl = getOcrBasisUrl(image.getOrder());

            CloseableHttpClient client = null;
            HttpGet method = new HttpGet(myOcrUrl);
            InputStream stream = null;
            try {
                client = HttpClientBuilder.create().build();

                stream = client.execute(method, HttpClientHelper.streamResponseHandler);
                if (stream != null) {
                    ocrResult = IOUtils.toString(stream, "UTF-8");
                }
                // this.ocrResult = method.getResponseBodyAsString();

            } catch (IOException e) {
                ocrResult = "Fatal transport error: " + e.getMessage();
            } finally {
                method.releaseConnection();
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        client = null;
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        stream = null;
                    }
                }
            }
        } else {
            String ocrFileNew = image.getTooltip().substring(0, image.getTooltip().lastIndexOf("."));
            ocrResult = FilesystemHelper.getOcrFileContent(myProzess, ocrFileNew);

            //        		String ocrFile = "";
            //			try {
            //				Path textFolder = Paths.get(myProzess.getTxtDirectory());
            //				Path altoFolder = Paths.get(myProzess.getAltoDirectory());
            //				if (Files.exists(textFolder)) {
            //	            		ocrFile = myProzess.getTxtDirectory() + image.getTooltip().substring(0, image.getTooltip().lastIndexOf(".")) + ".txt";
            //	            } else if (Files.exists(altoFolder)) {
            //	            		ocrFile = myProzess.getAltoDirectory() + image.getTooltip().substring(0, image.getTooltip().lastIndexOf(".")) + ".xml";
            //	            }
            //				ocrResult = FilesystemHelper.getOcrFileContent(myProzess, ocrFile);
            //	        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            //				logger.error("Error while reading the OCR result", e);
            //				ocrResult = "";
            //			}
        }
        return ocrResult;
    }

    public String getOcrAddress() {
        int startseite = -1;
        int endseite = -1;
        if (this.structSeiten != null) {
            for (int i = 0; i < this.structSeiten.length; i++) {
                SelectItem si = this.structSeiten[i];
                int temp = Integer.parseInt(si.getLabel().substring(0, si.getLabel().indexOf(":")));
                if (startseite == -1 || startseite > temp) {
                    startseite = temp;
                }
                if (endseite == -1 || endseite < temp) {
                    endseite = temp;
                }
            }
        }
        return getOcrBasisUrl(startseite, endseite);
    }

    private String getOcrBasisUrl(int... seiten) {
        String url = ConfigurationHelper.getInstance().getOcrUrl();
        VariableReplacer replacer = new VariableReplacer(this.mydocument, this.myPrefs, this.myProzess, null);
        url = replacer.replace(url);
        url += "/&imgrange=" + seiten[0];
        if (seiten.length > 1) {
            url += "-" + seiten[1];
        }
        return url;
    }

    /*
     * ##################################################### ##################################################### ## ## Getter und Setter ##
     * ##################################################### ####################################################
     */

    public int getBildNummer() {
        return this.myBildNummer;
    }

    public void setBildNummer(int inBild) {
    }

    public int getBildLetztes() {
        return this.myBildLetztes;
    }

    public int getBildGroesse() {
        return this.myBildGroesse;
    }

    public void setBildGroesse(int myBildGroesse) {
        this.myBildGroesse = myBildGroesse;
    }

    public String getTempTyp() {
        if (this.selectedMetadatum == null) {
            getAddableMetadataTypes();
            this.selectedMetadatum = this.tempMetadatumList.get(0);
        }
        return this.selectedMetadatum.getMd().getType().getName();
    }

    public MetadatumImpl getSelectedMetadatum() {
        return this.selectedMetadatum;
    }

    public void setSelectedMetadatum(MetadatumImpl newMeta) {
        this.selectedMetadatum = newMeta;
    }

    public void setTempTyp(String tempTyp) {
        MetadataType mdt = this.myPrefs.getMetadataTypeByName(tempTyp);
        try {
            Metadata md = new Metadata(mdt);
            this.selectedMetadatum = new MetadatumImpl(md, this.myMetadaten.size() + 1, this.myPrefs, this.myProzess, this);
            currentPlugin = selectedMetadatum.getPlugin();
        } catch (MetadataTypeNotAllowedException e) {
            logger.error(e.getMessage());
        }
        this.tempTyp = tempTyp;
    }

    public MetadatumImpl getMetadatum() {

        if (this.selectedMetadatum == null) {
            getAddableMetadataTypes();
            this.selectedMetadatum = this.tempMetadatumList.get(0);
        }
        return this.selectedMetadatum;
    }

    public void setMetadatum(MetadatumImpl meta) {
        this.selectedMetadatum = meta;
    }

    public String getTempMetadataGroupType() {
        if (this.selectedGroup == null) {
            getAddableMetadataGroupTypes();
            this.selectedGroup = this.tempMetadataGroups.get(0);
            this.tempGroupType = selectedGroup.getMetadataGroup().getType().getName();
        }
        return this.selectedGroup.getMetadataGroup().getType().getName();
    }

    public void setTempMetadataGroupType(String tempTyp) {
        if (!selectedGroup.getMetadataGroup().getType().getName().equals(tempTyp)) {
            MetadataGroupType mdt = this.myPrefs.getMetadataGroupTypeByName(tempTyp);
            try {
                MetadataGroup md = new MetadataGroup(mdt);
                this.selectedGroup = new MetadataGroupImpl(myPrefs, myProzess, md, this);
            } catch (MetadataTypeNotAllowedException e) {
                logger.error(e.getMessage());
            }
            this.tempGroupType = tempTyp;
        }
    }

    public MetadataGroupImpl getSelectedGroup() {

        if (this.selectedGroup == null) {
            getAddableMetadataGroupTypes();
            if (tempMetadataGroups != null && !tempMetadataGroups.isEmpty()) {
                this.selectedGroup = this.tempMetadataGroups.get(0);
            }
        }
        return this.selectedGroup;
    }

    public void setSelectedGroup(MetadataGroupImpl meta) {
        this.selectedGroup = meta;
    }

    public String getOutputType() {
        return this.selectedMetadatum.getOutputType();
    }

    public String getTempWert() {
        return this.tempWert;
    }

    public void setTempWert(String tempWert) {
        this.tempWert = tempWert;
    }

    public boolean isModusHinzufuegen() {
        return this.modusHinzufuegen;
    }

    public void setModusHinzufuegen(boolean modusHinzufuegen) {
        this.modusHinzufuegen = modusHinzufuegen;
    }

    public boolean isModusHinzufuegenPerson() {
        return this.modusHinzufuegenPerson;
    }

    public void setModusHinzufuegenPerson(boolean modusHinzufuegenPerson) {
        this.modusHinzufuegenPerson = modusHinzufuegenPerson;
    }

    public String getTempPersonNachname() {
        return this.tempPersonNachname;
    }

    public void setTempPersonNachname(String tempPersonNachname) {
        this.tempPersonNachname = tempPersonNachname;
    }

    public String getTempPersonRolle() {
        return this.tempPersonRolle;
    }

    public void setTempPersonRolle(String tempPersonRolle) {
        this.tempPersonRolle = tempPersonRolle;
    }

    public String getTempPersonVorname() {
        return this.tempPersonVorname;
    }

    public void setTempPersonVorname(String tempPersonVorname) {
        this.tempPersonVorname = tempPersonVorname;
    }

    public String[] getAlleSeitenAuswahl() {
        return this.alleSeitenAuswahl;
    }

    public void setAlleSeitenAuswahl(String[] alleSeitenAuswahl) {
        this.alleSeitenAuswahl = alleSeitenAuswahl;
    }

    public SelectItem[] getAlleSeiten() {
        return this.alleSeiten;
    }

    public SelectItem[] getStructSeiten() {
        if (this.structSeiten.length > 0 && this.structSeiten[0] == null) {
            return new SelectItem[0];
        } else {
            return this.structSeiten;
        }
    }

    public String[] getStructSeitenAuswahl() {
        return this.structSeitenAuswahl;
    }

    public void setStructSeitenAuswahl(String[] structSeitenAuswahl) {
        this.structSeitenAuswahl = structSeitenAuswahl;
    }

    public Process getMyProzess() {
        return this.myProzess;
    }

    public String getModusAnsicht() {
        return this.modusAnsicht;
    }

    public void setModusAnsicht(String modusAnsicht) {
        this.modusAnsicht = modusAnsicht;
    }

    public String getPaginierungWert() {
        return this.paginierungWert;
    }

    public void setPaginierungWert(String paginierungWert) {
        this.paginierungWert = paginierungWert;
    }

    public int getPaginierungAbSeiteOderMarkierung() {
        return this.paginierungAbSeiteOderMarkierung;
    }

    public void setPaginierungAbSeiteOderMarkierung(int paginierungAbSeiteOderMarkierung) {
        this.paginierungAbSeiteOderMarkierung = paginierungAbSeiteOderMarkierung;
    }

    public String getPaginierungArt() {
        return this.paginierungArt;
    }

    public void setPaginierungArt(String paginierungArt) {
        this.paginierungArt = paginierungArt;
    }

    public boolean isBildAnzeigen() {
        return this.bildAnzeigen;
    }

    public void BildAnzeigen() {
        this.bildAnzeigen = !this.bildAnzeigen;
    }

    public String getAddDocStructType1() {
        if (addDocStructType1 == null || addDocStructType1.isEmpty()) {
            SelectItem[] list = getAddableDocStructTypenAlsNachbar();
            if (list.length > 0) {
                addDocStructType1 = ((DocStructType) list[0].getValue()).getName();
                createAddableData();
            }
        }
        return this.addDocStructType1;
    }

    public void setAddDocStructType1(String addDocStructType1) {
        this.addDocStructType1 = getSelectedStructType(getAddableDocStructTypenAlsNachbar(), addDocStructType1);

        createAddableData();
    }

    public String getAddDocStructType2() {
        if (addDocStructType2 == null || addDocStructType2.isEmpty()) {
            SelectItem[] list = getAddableDocStructTypenAlsKind();
            if (list.length > 0) {
                addDocStructType2 = ((DocStructType) list[0].getValue()).getName();
                createAddableData();
            }
        }

        return this.addDocStructType2;
    }

    public void setAddDocStructType2(String addDocStructType2) {
        this.addDocStructType2 = getSelectedStructType(getAddableDocStructTypenAlsKind(), addDocStructType2);
        createAddableData();
    }

    public String getBildNummerGeheZu() {
        return "";
    }

    public void setBildNummerGeheZu(String bildNummerGeheZu) {
        this.bildNummerGeheZu = bildNummerGeheZu;
    }

    public void setBildNummerGeheZuCompleteString(String bildNummerGeheZu) {
        try {
            this.bildNummerGeheZu = bildNummerGeheZu.substring(0, bildNummerGeheZu.indexOf(":"));
        } catch (Exception e) {
            this.bildNummerGeheZu = bildNummerGeheZu;
        }
    }

    public String getBildNummerGeheZuCompleteString() {
        return "";
    }

    public boolean isNurLesenModus() {
        return this.nurLesenModus;
    }

    public void setNurLesenModus(boolean nurLesenModus) {
        this.nurLesenModus = nurLesenModus;
    }

    public boolean isBildZuStrukturelement() {
        return this.bildZuStrukturelement;
    }

    public void setBildZuStrukturelement(boolean bildZuStrukturelement) {
        this.bildZuStrukturelement = bildZuStrukturelement;
    }

    public String getNeuesElementWohin() {
        if (this.neuesElementWohin == null || this.neuesElementWohin == "") {
            this.neuesElementWohin = "4";
        }
        return this.neuesElementWohin;
    }

    public void setNeuesElementWohin(String inNeuesElementWohin) {
        if (inNeuesElementWohin == null || inNeuesElementWohin.equals("")) {
            this.neuesElementWohin = "1";
        } else {
            if (!inNeuesElementWohin.equals(neuesElementWohin)) {
                if ((neuesElementWohin.equals("1") || neuesElementWohin.equals("2")) && (inNeuesElementWohin.equals("3") || inNeuesElementWohin
                        .equals("4"))) {
                    this.neuesElementWohin = inNeuesElementWohin;
                    getAddDocStructType2();
                    createAddableData();
                } else if ((neuesElementWohin.equals("3") || neuesElementWohin.equals("4")) && (inNeuesElementWohin.equals("1") || inNeuesElementWohin
                        .equals("2"))) {
                    this.neuesElementWohin = inNeuesElementWohin;
                    getAddDocStructType1();
                    createAddableData();

                } else {
                    this.neuesElementWohin = inNeuesElementWohin;
                }
            }
        }
    }

    public String getAlleSeitenAuswahl_ersteSeite() {
        return this.alleSeitenAuswahl_ersteSeite;
    }

    public void setAlleSeitenAuswahl_ersteSeite(String alleSeitenAuswahl_ersteSeite) {
        this.alleSeitenAuswahl_ersteSeite = alleSeitenAuswahl_ersteSeite;
    }

    public String getAlleSeitenAuswahl_letzteSeite() {
        return this.alleSeitenAuswahl_letzteSeite;
    }

    public void setAlleSeitenAuswahl_letzteSeite(String alleSeitenAuswahl_letzteSeite) {
        this.alleSeitenAuswahl_letzteSeite = alleSeitenAuswahl_letzteSeite;
    }

    public List<TreeNode> getStrukturBaum3() {
        if (this.tree3 != null) {
            return this.tree3.getChildrenAsList();
        } else {
            return new ArrayList<>();
        }
    }

    public List<TreeNode> getStrukturBaum3Alle() {
        if (this.tree3 != null) {
            return this.tree3.getChildrenAsListAlle();
        } else {
            return new ArrayList<>();
        }
    }

    public boolean isModusStrukturelementVerschieben() {
        return this.modusStrukturelementVerschieben;
    }

    public void setModusStrukturelementVerschieben(boolean modusStrukturelementVerschieben) {
        this.modusStrukturelementVerschieben = modusStrukturelementVerschieben;

        // wenn der Verschiebevorgang gestartet werden soll, dann in allen
        // DocStructs prüfen
        // ob das aktuelle Strukturelement dort eingefügt werden darf
        if (this.modusStrukturelementVerschieben) {
            List<DocStruct> list = new ArrayList<>();
            list.add(myDocStruct);
            TreeDurchlaufen(this.tree3, list);
        }
    }

    @SuppressWarnings("rawtypes")
    private void TreeDurchlaufen(TreeNodeStruct3 inTreeStruct, List<DocStruct> inDocStructList) {
        for (DocStruct docStruct : inDocStructList) {

            DocStruct temp = inTreeStruct.getStruct();
            if (inTreeStruct.getStruct() == docStruct) {
                inTreeStruct.setSelected(true);
            } else {
                inTreeStruct.setSelected(false);
            }

            // alle erlaubten Typen durchlaufen
            // for (Iterator<String> iter = temp.getType().getAllAllowedDocStructTypes().iterator(); iter.hasNext();) {
            // String dst = iter.next();
            // if (docStruct.getType().getName().equals(dst)) {
            // inTreeStruct.setEinfuegenErlaubt(false);
            // break;
            // }
            // }
            if (!temp.getType().getAllAllowedDocStructTypes().contains(docStruct.getType().getName())) {
                inTreeStruct.setEinfuegenErlaubt(false);
            }
            for (Iterator iter = inTreeStruct.getChildren().iterator(); iter.hasNext();) {
                TreeNodeStruct3 kind = (TreeNodeStruct3) iter.next();
                List<DocStruct> list = new ArrayList<>();
                list.add(docStruct);
                TreeDurchlaufen(kind, list);
            }
        }
    }

    public void setTempStrukturelement(DocStruct tempStrukturelement) {
        this.tempStrukturelement = tempStrukturelement;
    }

    public List<MetadatumImpl> getMyMetadaten() {
        return this.myMetadaten;
    }

    public void setMyMetadaten(List<MetadatumImpl> myMetadaten) {
        this.myMetadaten = myMetadaten;
    }

    public MetadatumImpl getMetadata() {
        return myMetadaten.get(0);
    }

    public List<MetaPerson> getMyPersonen() {
        return this.myPersonen;
    }

    public void setMyPersonen(List<MetaPerson> myPersonen) {
        this.myPersonen = myPersonen;
    }

    public MetadatumImpl getCurMetadatum() {
        return this.curMetadatum;
    }

    public String search() {
        if (currentPlugin != null) {

            if (currentPlugin.getTitle().toLowerCase().startsWith("dante")) {
                currentPlugin.setSearchValue(danteSearchValue);
            } else if (StringUtils.isNotBlank(gndSearchValue)) {
                currentPlugin.setSearchOption(searchOption);
                currentPlugin.setSearchValue(gndSearchValue);
            } else {
                currentPlugin.setSearchValue(geonamesSearchValue);
            }
            currentPlugin.search();
            gndSearchValue = "";
            geonamesSearchValue = "";
            danteSearchValue = "";
        }
        return "";
    }

    public void setCurMetadatum(MetadatumImpl curMetadatum) {
        this.curMetadatum = curMetadatum;
    }

    public Prefs getMyPrefs() {
        return this.myPrefs;
    }

    public MetaPerson getCurPerson() {
        return this.curPerson;
    }

    public void setCurPerson(MetaPerson curPerson) {
        this.curPerson = curPerson;
    }

    public String getAdditionalOpacPpns() {
        return this.additionalOpacPpns;
    }

    public void setAdditionalOpacPpns(String additionalOpacPpns) {
        this.additionalOpacPpns = additionalOpacPpns;
    }

    public boolean isTreeReloaden() {
        return this.treeReloaden;
    }

    public void setTreeReloaden(boolean treeReloaden) {
        this.treeReloaden = treeReloaden;
    }

    public HashMap<String, Boolean> getTreeProperties() {
        return this.treeProperties;
    }

    public void setTreeProperties(HashMap<String, Boolean> treeProperties) {
        this.treeProperties = treeProperties;
    }

    public String getOpacKatalog() {
        if (StringUtils.isBlank(opacKatalog)) {
            opacKatalog = getAllOpacCatalogues().get(0);
        }
        return this.opacKatalog;
    }

    public void setOpacKatalog(String opacKatalog) {
        this.opacKatalog = opacKatalog;
    }

    public Map<String, String> getAllSearchFields() {
        return ConfigOpac.getInstance().getCatalogueByName(getOpacKatalog()).getSearchFields();
    }

    public List<String> getAllOpacCatalogues() {
        return ConfigOpac.getInstance().getAllCatalogueTitles();
    }

    public String getOpacSuchfeld() {
        return this.opacSuchfeld;
    }

    public void setOpacSuchfeld(String opacSuchfeld) {
        this.opacSuchfeld = opacSuchfeld;
    }

    public int getPaginierungSeitenProImage() {
        return this.paginierungSeitenProImage;
    }

    public void setPaginierungSeitenProImage(int paginierungSeitenProImage) {
        this.paginierungSeitenProImage = paginierungSeitenProImage;
    }

    public String getCurrentTifFolder() {
        return this.currentTifFolder;
    }

    public void setCurrentTifFolder(String currentTifFolder) {
        if (!this.currentTifFolder.equals(currentTifFolder)) {
            this.currentTifFolder = currentTifFolder;
        }
    }

    public List<String> autocomplete(String suggest) {
        String pref = suggest;
        List<String> result = new ArrayList<>();
        List<String> alle = new ArrayList<>();
        for (SelectItem si : this.alleSeiten) {
            alle.add(si.getLabel());
        }

        Iterator<String> iterator = alle.iterator();
        while (iterator.hasNext()) {
            String elem = iterator.next();
            if (elem != null && elem.contains(pref) || "".equals(pref)) {
                result.add(elem);
            }
        }
        return result;
    }

    public boolean getIsNotRootElement() {
        if (this.myDocStruct != null) {
            if (this.myDocStruct.getParent() == null) {
                return false;
            }
        }
        return true;
    }

    public boolean getFictitious() {
        return fictitious;
    }

    public void setFictitious(boolean fictitious) {
        this.fictitious = fictitious;
    }

    public String getCurrentRepresentativePage() {
        return currentRepresentativePage;
    }

    public void setCurrentRepresentativePage(String currentRepresentativePage) {
        this.currentRepresentativePage = currentRepresentativePage;
    }

    public void moveSeltectedPagesUp(int positions) {
        List<Integer> selectedPages = new ArrayList<>();
        List<DocStruct> allPages = mydocument.getPhysicalDocStruct().getAllChildren();
        List<String> pageNoList = Arrays.asList(alleSeitenAuswahl);
        for (String order : pageNoList) {
            int currentPhysicalPageNo = Integer.parseInt(order);
            if (currentPhysicalPageNo == 0) {
                break;
            }
            selectedPages.add(currentPhysicalPageNo);
        }

        if (selectedPages.isEmpty()) {
            return;
        }
        List<String> newSelectionList = new ArrayList<>();
        for (Integer pageIndex : selectedPages) {
            if (pageIndex - positions < 0) {
                positions = pageIndex;
            }
            DocStruct image = allPages.get(pageIndex);
            allPages.remove(image);
            allPages.add(pageIndex - positions, image);
            newSelectionList.add(String.valueOf(pageIndex - positions));
        }
        setPhysicalOrder(allPages);

        alleSeitenAuswahl = newSelectionList.toArray(new String[newSelectionList.size()]);
        retrieveAllImages();

        loadCurrentImages(false);

    }

    private void setPhysicalOrder(List<DocStruct> pages) {
        int physicalCounter = 1;
        for (DocStruct image : pages) {
            Metadata physicalImageNo = image.getAllMetadataByType(myPrefs.getMetadataTypeByName("physPageNumber")).get(0);
            physicalImageNo.setValue("" + physicalCounter++);
        }
    }

    public void moveSelectedPages(String inDirection, int inTimes) {
        long times = inTimes;
        if (times < 1) {
            times = 1;
        }
        if (inDirection.equals("up")) {
            moveSeltectedPagesUp(inTimes);
        } else {
            moveSeltectedPagesDown(inTimes);
        }
    }

    public void moveSeltectedPagesDown(int positions) {
        List<Integer> selectedPages = new ArrayList<>();
        List<DocStruct> allPages = mydocument.getPhysicalDocStruct().getAllChildren();
        List<String> pagesList = Arrays.asList(alleSeitenAuswahl);
        Collections.reverse(pagesList);
        for (String order : pagesList) {
            int currentPhysicalPageNo = Integer.parseInt(order);
            if (currentPhysicalPageNo + 1 == alleSeiten.length) {
                break;
            }
            selectedPages.add(currentPhysicalPageNo);
        }

        if (selectedPages.isEmpty()) {
            return;
        }
        List<String> newSelectionList = new ArrayList<>();
        for (Integer pageIndex : selectedPages) {
            if (pageIndex + positions > allPages.size()) {
                positions = allPages.size() - pageIndex - 1;
            }
            DocStruct image = allPages.get(pageIndex);
            allPages.remove(image);
            allPages.add(pageIndex + positions, image);
            //            DocStruct firstpage = allPages.get(pageIndex + 1);
            //            DocStruct secondpage = allPages.get(pageIndex);
            //            switchFileNames(firstpage, secondpage);
            newSelectionList.add(String.valueOf(pageIndex + positions));
        }
        setPhysicalOrder(allPages);
        alleSeitenAuswahl = newSelectionList.toArray(new String[newSelectionList.size()]);
        retrieveAllImages();

        loadCurrentImages(false);

    }

    public void deleteSeltectedPages() {
        List<Integer> selectedPages = new ArrayList<>();
        List<DocStruct> allPages = mydocument.getPhysicalDocStruct().getAllChildren();
        List<String> pagesList = Arrays.asList(alleSeitenAuswahl);
        Collections.reverse(pagesList);
        for (String order : pagesList) {
            int currentPhysicalPageNo = Integer.parseInt(order);
            selectedPages.add(currentPhysicalPageNo);
        }

        if (selectedPages.isEmpty()) {
            return;
        }

        for (Integer pageIndex : selectedPages) {

            DocStruct pageToRemove = allPages.get(pageIndex);
            String imagename = pageToRemove.getImageName();

            removeImage(imagename);
            // try {
            mydocument.getFileSet().removeFile(pageToRemove.getAllContentFiles().get(0));
            // pageToRemove.removeContentFile(pageToRemove.getAllContentFiles().get(0));
            // } catch (ContentFileNotLinkedException e) {
            // myLogger.error(e);
            // }

            mydocument.getPhysicalDocStruct().removeChild(pageToRemove);
            List<Reference> refs = new ArrayList<>(pageToRemove.getAllFromReferences());
            for (ugh.dl.Reference ref : refs) {
                ref.getSource().removeReferenceTo(pageToRemove);
            }

        }

        alleSeitenAuswahl = null;
        if (mydocument.getPhysicalDocStruct().getAllChildren() != null) {
            myBildLetztes = mydocument.getPhysicalDocStruct().getAllChildren().size();
        } else {
            myBildLetztes = 0;
        }

        loadCurrentImages(true);
        allPages = mydocument.getPhysicalDocStruct().getAllChildren();

        int currentPhysicalOrder = 1;
        if (allPages != null) {
            MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
            for (DocStruct page : allPages) {
                List<? extends Metadata> pageNoMetadata = page.getAllMetadataByType(mdt);
                if (pageNoMetadata == null || pageNoMetadata.size() == 0) {
                    currentPhysicalOrder++;
                    break;
                }
                for (Metadata pageNo : pageNoMetadata) {
                    pageNo.setValue(String.valueOf(currentPhysicalOrder));
                }
                currentPhysicalOrder++;
            }
        }
        retrieveAllImages();
        // current image was deleted, load first image
        if (selectedPages.contains(myBildNummer - 1)) {

            BildErsteSeiteAnzeigen();
        } else {

            setImageIndex(myBildNummer - 1);

        }

        // save current state
        Reload();

    }

    public void reOrderPagination() {
        String imageDirectory = "";
        try {
            imageDirectory = myProzess.getImagesDirectory();
        } catch (SwapException e) {
            logger.error(e);
        } catch (DAOException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (InterruptedException e) {
            logger.error(e);

        }
        if (imageDirectory.equals("")) {
            Helper.setFehlerMeldung("ErrorMetsEditorImageRenaming");
            return;
        }

        List<String> oldfilenames = new ArrayList<>();
        for (DocStruct page : mydocument.getPhysicalDocStruct().getAllChildren()) {
            oldfilenames.add(page.getImageName());
        }
        progress = 0;
        totalImageNo = oldfilenames.size() * 2;
        currentImageNo = 0;
        for (String imagename : oldfilenames) {
            String filenamePrefix = imagename.substring(0, imagename.lastIndexOf("."));
            currentImageNo++;
            for (String folder : allTifFolders) {
                // check if folder is empty, otherwise get extension for folder
                Path currentImageFolder = Paths.get(imageDirectory + folder);
                List<String> files = StorageProvider.getInstance().list(currentImageFolder.toString(), NIOFileUtils.DATA_FILTER);
                if (files != null && !files.isEmpty()) {
                    String fileExtension = Metadaten.getFileExtension(imagename);
                    Path filename = Paths.get(currentImageFolder.toString(), filenamePrefix + fileExtension);
                    Path newFileName = Paths.get(currentImageFolder.toString(), filenamePrefix + fileExtension + "_bak");
                    try {
                        StorageProvider.getInstance().move(filename, newFileName);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            }

            try {
                Path ocr = Paths.get(myProzess.getOcrDirectory());
                if (StorageProvider.getInstance().isFileExists(ocr)) {
                    List<Path> allOcrFolder = StorageProvider.getInstance().listFiles(ocr.toString());
                    for (Path folder : allOcrFolder) {

                        List<String> files = StorageProvider.getInstance().list(folder.toString());

                        if (files != null && !files.isEmpty()) {
                            String fileExtension = Metadaten.getFileExtension(imagename.replace("_bak", ""));
                            Path filename = Paths.get(folder.toString(), filenamePrefix + fileExtension);
                            Path newFileName = Paths.get(folder.toString(), filenamePrefix + fileExtension + "_bak");
                            StorageProvider.getInstance().move(filename, newFileName);
                        }
                    }
                }
            } catch (SwapException e) {
                logger.error(e);
            } catch (DAOException e) {
                logger.error(e);
            } catch (IOException e) {
                logger.error(e);
            } catch (InterruptedException e) {
                logger.error(e);
            }

        }
        System.gc();
        int counter = 1;
        for (String imagename : oldfilenames) {
            currentImageNo++;
            String newfilenamePrefix = generateFileName(counter);
            String oldFilenamePrefix = imagename.substring(0, imagename.lastIndexOf("."));
            for (String folder : allTifFolders) {
                Path currentImageFolder = Paths.get(imageDirectory + folder);
                List<String> files = StorageProvider.getInstance().list(currentImageFolder.toString(), NIOFileUtils.fileFilter);
                if (files != null && !files.isEmpty()) {
                    String fileExtension = Metadaten.getFileExtension(imagename.replace("_bak", ""));
                    Path tempFileName = Paths.get(currentImageFolder.toString(), oldFilenamePrefix + fileExtension + "_bak");
                    Path sortedName = Paths.get(imageDirectory + folder, newfilenamePrefix + fileExtension.toLowerCase());
                    try {
                        StorageProvider.getInstance().move(tempFileName, sortedName);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    mydocument.getPhysicalDocStruct().getAllChildren().get(counter - 1).setImageName(sortedName.getFileName().toString());
                }
            }
            try {

                Path ocr = Paths.get(myProzess.getOcrDirectory());
                if (StorageProvider.getInstance().isFileExists(ocr)) {
                    List<Path> allOcrFolder = StorageProvider.getInstance().listFiles(ocr.toString());
                    for (Path folder : allOcrFolder) {

                        List<String> files = StorageProvider.getInstance().list(folder.toString());
                        if (files != null && !files.isEmpty()) {
                            String fileExtension = Metadaten.getFileExtension(imagename.replace("_bak", ""));
                            Path tempFileName = Paths.get(folder.toString(), oldFilenamePrefix + fileExtension + "_bak");
                            Path sortedName = Paths.get(folder.toString(), newfilenamePrefix + fileExtension.toLowerCase());
                            StorageProvider.getInstance().move(tempFileName, sortedName);
                        }
                    }
                }

            } catch (SwapException e) {
                logger.error(e);
            } catch (DAOException e) {
                logger.error(e);
            } catch (IOException e) {
                logger.error(e);
            } catch (InterruptedException e) {
                logger.error(e);
            }
            counter++;
        }

        retrieveAllImages();
        progress = null;
        totalImageNo = 0;
        currentImageNo = 0;

        // load new file names
        changeFolder();

        // save current state
        Reload();

        Helper.setMeldung("finishedFileRenaming");
    }

    private void removeImage(String fileToDelete) {
        try {
            // check what happens with .tar.gz
            String fileToDeletePrefix = fileToDelete.substring(0, fileToDelete.lastIndexOf("."));
            for (String folder : allTifFolders) {
                Path imageFolder = Paths.get(myProzess.getImagesDirectory() + folder);
                List<Path> filesInFolder = StorageProvider.getInstance().listFiles(imageFolder.toString());
                for (Path currentFile : filesInFolder) {
                    String filename = currentFile.getFileName().toString();
                    String filenamePrefix = filename.replace(getFileExtension(filename), "");
                    if (filenamePrefix.equals(fileToDeletePrefix)) {
                        StorageProvider.getInstance().deleteFile(currentFile);
                    }
                }
            }

            Path ocr = Paths.get(myProzess.getOcrDirectory());
            if (StorageProvider.getInstance().isFileExists(ocr)) {
                List<Path> folder = StorageProvider.getInstance().listFiles(ocr.toString());
                for (Path dir : folder) {
                    if (StorageProvider.getInstance().isDirectory(dir) && !StorageProvider.getInstance().list(dir.toString()).isEmpty()) {
                        List<Path> filesInFolder = StorageProvider.getInstance().listFiles(dir.toString());
                        for (Path currentFile : filesInFolder) {
                            String filename = currentFile.getFileName().toString();
                            String filenamePrefix = filename.substring(0, filename.lastIndexOf("."));
                            if (filenamePrefix.equals(fileToDeletePrefix)) {
                                StorageProvider.getInstance().deleteFile(currentFile);
                            }
                        }
                    }
                }
            }
        } catch (SwapException e) {
            logger.error(e);
        } catch (DAOException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (InterruptedException e) {
            logger.error(e);
        }

    }

    private static String generateFileName(int counter) {
        String filename = "";
        if (counter >= 10000000) {
            filename = "" + counter;
        } else if (counter >= 1000000) {
            filename = "0" + counter;
        } else if (counter >= 100000) {
            filename = "00" + counter;
        } else if (counter >= 10000) {
            filename = "000" + counter;
        } else if (counter >= 1000) {
            filename = "0000" + counter;
        } else if (counter >= 100) {
            filename = "00000" + counter;
        } else if (counter >= 10) {
            filename = "000000" + counter;
        } else {
            filename = "0000000" + counter;
        }
        return filename;
    }

    public FileManipulation getFileManipulation() {
        if (fileManipulation == null) {
            fileManipulation = new FileManipulation(this);
        }
        return fileManipulation;
    }

    public void setFileManipulation(FileManipulation fileManipulation) {
        this.fileManipulation = fileManipulation;
    }

    public DigitalDocument getDocument() {
        return mydocument;
    }

    public void setDocument(DigitalDocument document) {
        this.mydocument = document;
    }

    public static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String afterLastSlash = filename.substring(filename.lastIndexOf('/') + 1);
        // int afterLastBackslash = afterLastSlash.lastIndexOf('\\') + 1;
        int dotIndex = afterLastSlash.lastIndexOf('.');
        return (dotIndex == -1) ? "" : afterLastSlash.substring(dotIndex);
    }

    public List<MetadataGroupImpl> getGroups() {
        return groups;
    }

    public void setGroups(List<MetadataGroupImpl> groups) {
        this.groups = groups;
    }

    public MetadataGroupImpl getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(MetadataGroupImpl currentGroup) {
        this.currentGroup = currentGroup;
    }

    public boolean isModeAddGroup() {
        return modeAddGroup;
    }

    public void setModeAddGroup(boolean modeAddGroup) {
        this.modeAddGroup = modeAddGroup;
    }

    public boolean isModusCopyDocstructFromOtherProcess() {
        return modusCopyDocstructFromOtherProcess;
    }

    private boolean displayInsertion = false;

    public void setModusCopyDocstructFromOtherProcess(boolean modusCopyDocstructFromOtherProcess) {
        if (modusCopyDocstructFromOtherProcess) {
            treeOfFilteredProcess = null;
            setDisplayInsertion(false);
            filteredProcess = null;
        }
        this.modusCopyDocstructFromOtherProcess = modusCopyDocstructFromOtherProcess;
    }

    public Boolean getDisplayFileManipulation() {
        return ConfigurationHelper.getInstance().isMetsEditorDisplayFileManipulation();
    }

    private String filterProcessTitle = "";
    private Process filteredProcess = null;

    public String getFilterProcessTitle() {
        return filterProcessTitle;
    }

    public void setFilterProcessTitle(String filterProcessTitle) {
        this.filterProcessTitle = filterProcessTitle;
    }

    public Process getFilteredProcess() {
        return filteredProcess;
    }

    public void filterMyProcess() {
        filteredProcess = ProcessManager.getProcessByTitle(filterProcessTitle);
        if (filteredProcess == null) {
            Helper.setFehlerMeldung("kein Vorgang gefunden");
        } else {
            if (!filteredProcess.getRegelsatz().getId().equals(myProzess.getRegelsatz().getId())) {
                Helper.setFehlerMeldung("unterschiedlicher Regelsatz");
            } else {
                Helper.setMeldung("Vorgang gefunden und gleicher Regelsatz: " + filteredProcess.getId());
                loadTreeFromFilteredProcess();

            }
        }
    }

    private void loadTreeFromFilteredProcess() {
        if (this.modusCopyDocstructFromOtherProcess) {
            try {
                treeOfFilteredProcess = buildTree(treeOfFilteredProcess, filteredProcess.readMetadataFile().getDigitalDocument()
                        .getLogicalDocStruct(), false);

            } catch (PreferencesException e) {
                logger.error("Error loading the tree for filtered processes (PreferencesException): ", e);

            } catch (ReadException e) {
                logger.error("Error loading the tree for filtered processes (ReadException): ", e);

            } catch (SwapException e) {
                logger.error("Error loading the tree for filtered processes (SwapException): ", e);

            } catch (DAOException e) {
                logger.error("Error loading the tree for filtered processes (DAOException): ", e);

            } catch (WriteException e) {
                logger.error("Error loading the tree for filtered processes (WriteException): ", e);

            } catch (IOException e) {
                logger.error("Error loading the tree for filtered processes (IOException): ", e);

            } catch (InterruptedException e) {
                logger.error("Error loading the tree for filtered processes (InterruptedException): ", e);

            }
            activateAllTreeElements(treeOfFilteredProcess);
        }

    }

    @SuppressWarnings("rawtypes")
    private void activateAllTreeElements(TreeNodeStruct3 inTreeStruct) {
        inTreeStruct.setEinfuegenErlaubt(true);
        for (Iterator iter = inTreeStruct.getChildren().iterator(); iter.hasNext();) {
            TreeNodeStruct3 kind = (TreeNodeStruct3) iter.next();
            activateAllTreeElements(kind);
        }
    }

    public List<TreeNode> getStruktureTreeAsTableForFilteredProcess() {
        if (this.treeOfFilteredProcess != null) {
            return this.treeOfFilteredProcess.getChildrenAsListAlle();
        } else {
            return new ArrayList<>();
        }
    }

    public boolean getIsProcessLoaded() {
        return treeOfFilteredProcess != null;
    }

    public void rememberFilteredProcessStruct() {
        activateAllTreeElements(tree3);
        List<DocStruct> selectdElements = getSelectedElements(treeOfFilteredProcess);
        TreeDurchlaufen(this.tree3, selectdElements);

    }

    private List<DocStruct> getSelectedElements(TreeNodeStruct3 currentNode) {
        List<DocStruct> selectdElements = new ArrayList<>();
        for (TreeNode node : currentNode.getChildren()) {
            TreeNodeStruct3 tns = (TreeNodeStruct3) node;
            if (node.isSelected()) {
                selectdElements.add(tns.getStruct().copy(true, true));
            }
            if (tns.getChildren() != null && !tns.getChildren().isEmpty()) {
                selectdElements.addAll(getSelectedElements(tns));
            }
        }
        return selectdElements;
    }

    public void importFilteredProcessStruct() throws TypeNotAllowedAsChildException {

        List<DocStruct> selectdElements = getSelectedElements(treeOfFilteredProcess);

        for (DocStruct docStructFromFilteredProcess : selectdElements) {
            List<Reference> refs = docStructFromFilteredProcess.getAllToReferences();
            List<Reference> clone = new ArrayList<>(refs);
            for (Reference ref : clone) {
                docStructFromFilteredProcess.removeReferenceTo(ref.getTarget());
            }

            this.tempStrukturelement.addChild(docStructFromFilteredProcess);
        }
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, false);
        this.neuesElementWohin = "1";
    }

    public boolean isDisplayInsertion() {
        return displayInsertion;
    }

    public void setDisplayInsertion(boolean displayInsertion) {
        this.displayInsertion = displayInsertion;
    }

    public void updateAllSubNodes() {
        activateAllTreeElements(treeOfFilteredProcess);
        updateAllSubNodes(treeOfFilteredProcess);
    }

    private void updateAllSubNodes(TreeNodeStruct3 papi) {
        for (TreeNode node : papi.getChildren()) {
            TreeNodeStruct3 tns = (TreeNodeStruct3) node;
            if (papi.isSelected()) {
                tns.setEinfuegenErlaubt(false);
                disableSubelements(tns);
            } else {
                updateAllSubNodes(tns);
            }
        }

    }

    private void disableSubelements(TreeNodeStruct3 element) {
        for (TreeNode node : element.getChildren()) {
            TreeNodeStruct3 tns = (TreeNodeStruct3) node;
            tns.setEinfuegenErlaubt(false);
            tns.setSelected(false);

            disableSubelements(tns);
        }
    }

    public int getTreeWidth() {
        return treeWidth;
    }

    public void setTreeWidth(int treeWidth) {
        this.treeWidth = treeWidth;
    }

    // progress bar

    public Integer getProgress() {
        if (progress == null) {
            progress = 0;
        } else if (totalImageNo == 0) {
            progress = 100;
        } else {
            progress = (int) (currentImageNo / totalImageNo * 100);

            if (progress > 100) {
                progress = 100;
            }
        }

        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public void onComplete() {
        progress = null;
    }

    public boolean isShowProgressBar() {
        if (progress == null || progress == 100 || progress == 0) {
            return false;
        }
        return true;
    }

    // needed for junit test
    public void setMyProzess(Process myProzess) {
        this.myProzess = myProzess;
    }

    public void setMyBenutzerID(String id) {
        myBenutzerID = id;
    }

    private String oldDocstructName = "";

    private void createAddableData() {

        String docstructName = "";
        int selection = new Integer(neuesElementWohin).intValue();
        if (selection < 3) {
            docstructName = getAddDocStructType1();
        } else {
            docstructName = getAddDocStructType2();
        }
        if (docstructName != null && (oldDocstructName.isEmpty() || !oldDocstructName.equals(docstructName))) {
            oldDocstructName = docstructName;

            addableMetadata = new LinkedList<>();
            if (docstructName != null) {

                DocStructType dst = this.myPrefs.getDocStrctTypeByName(docstructName);
                try {
                    DocStruct ds = this.mydocument.createDocStruct(dst);

                    List<? extends Metadata> myTempMetadata = this.metahelper.getMetadataInclDefaultDisplay(ds, Helper.getMetadataLanguage(), false,
                            this.myProzess, displayHiddenMetadata);
                    if (myTempMetadata != null) {
                        for (Metadata metadata : myTempMetadata) {
                            MetadatumImpl meta = new MetadatumImpl(metadata, 0, this.myPrefs, this.myProzess, this);
                            // meta.getSelectedItem();
                            addableMetadata.add(meta);
                        }
                    }
                } catch (TypeNotAllowedForParentException e) {
                    logger.error(e);
                }
            }
            addablePersondata = new LinkedList<>();
            if (docstructName != null) {

                DocStructType dst = this.myPrefs.getDocStrctTypeByName(docstructName);
                try {
                    DocStruct ds = this.mydocument.createDocStruct(dst);

                    List<? extends Metadata> myTempMetadata = this.metahelper.getMetadataInclDefaultDisplay(ds, Helper.getMetadataLanguage(), true,
                            this.myProzess, displayHiddenMetadata);
                    if (myTempMetadata != null) {
                        for (Metadata metadata : myTempMetadata) {
                            MetaPerson meta = new MetaPerson((Person) metadata, 0, this.myPrefs, ds, myProzess, this);

                            addablePersondata.add(meta);
                        }
                    }
                } catch (TypeNotAllowedForParentException e) {
                    logger.error(e);
                }
            }
        }
    }

    public List<MetadatumImpl> getAddableMetadata() {

        return addableMetadata;
    }

    public List<MetaPerson> getAddablePersondata() {

        return addablePersondata;
    }

    public void changeTopstruct() {
        if (currentTopstruct.getType().getName().equals(logicalTopstruct.getType().getName())) {
            currentTopstruct = physicalTopstruct;
            modusAnsicht = "Metadaten";
        } else {
            currentTopstruct = logicalTopstruct;
        }
        MetadatenalsBeanSpeichern(this.currentTopstruct);
        MetadatenalsTree3Einlesen1(this.tree3, this.currentTopstruct, true);
    }

    public boolean isPhysicalTopstruct() {
        return currentTopstruct.getType().getName().equals(physicalTopstruct.getType().getName());
    }

    public List<Image> getPaginatorList() {
        List<Image> subList = new ArrayList<>();
        if (allImages.size() > (pageNo * numberOfImagesPerPage) + numberOfImagesPerPage) {
            subList = allImages.subList(pageNo * numberOfImagesPerPage, (pageNo * numberOfImagesPerPage) + numberOfImagesPerPage);
        } else {
            subList = allImages.subList(pageNo * numberOfImagesPerPage, allImages.size());
        }
        for (Image currentImage : subList) {
            if (sizeChanged) {
                currentImage.createThumbnailUrls(thumbnailSizeInPixel);
            }
        }
        return subList;
    }

    private String getContextPath() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
        String baseUrl = session.getServletContext().getContextPath();
        return baseUrl;
    }

    private Dimension getActualImageSize(Image image) {
        Dimension dim;
        try {
            ConfigurationHelper conf = ConfigurationHelper.getInstance();
            String imPath = imageFolderName + image.getImageName();
            String imageURIStr = conf.useS3() ? "s3://" + conf.getS3Bucket() + "/" + S3FileUtils.string2Key(imPath) : "file://" + imPath;
            String dimString = new GetImageDimensionAction().getDimensions(imageURIStr);
            int width = Integer.parseInt(dimString.replaceAll("::.*", ""));
            int height = Integer.parseInt(dimString.replaceAll(".*::", ""));
            dim = new Dimension(width, height);
        } catch (NullPointerException | NumberFormatException | ContentLibImageException | URISyntaxException | IOException e) {
            logger.error("Could not retrieve actual image size", e);
            dim = new Dimension(0, 0);
        }
        return dim;
    }

    private String createImageUrl(Image currentImage, Integer size, String format, String baseUrl) {
        ConfigurationHelper conf = ConfigurationHelper.getInstance();
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("/cs").append("?action=").append("image").append("&format=").append(format).append("&sourcepath=");
        if (conf.useS3()) {
            url.append("s3://").append(conf.getS3Bucket()).append("/").append(S3FileUtils.string2Key(imageFolderName + currentImage.getImageName()));
        } else {
            url.append("file://").append(imageFolderName + currentImage.getImageName());
        }
        url.append("&width=").append(size).append("&height=").append(size);
        return url.toString().replaceAll("\\\\", "/");
    }

    public List<Image> getAllImages() {
        return allImages;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        if (this.imageIndex < 0) {
            this.imageIndex = 0;
        }
        if (this.imageIndex >= getSizeOfImageList()) {
            this.imageIndex = getSizeOfImageList() - 1;
        }
        if (!allImages.isEmpty() && allImages.size() >= imageIndex) {
            setImage(allImages.get(this.imageIndex));
        }
    }

    public void checkSelectedThumbnail(int imageIndex) {
        alleSeitenAuswahl = new String[1];
        alleSeitenAuswahl[0] = String.valueOf(imageIndex);
    }

    public String getImageUrl() {
        if (image == null) {
            return null;
        } else {
            FacesContext context = FacesContextHelper.getCurrentFacesContext();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            String currentImageURL = session.getServletContext().getContextPath() + ConfigurationHelper.getTempImagesPath() + session.getId() + "_"
                    + image.getImageName() + "_large_" + ".jpg";
            return currentImageURL.replaceAll("\\\\", "/");
        }
    }

    public Image getImage() {
        return image;
    }

    public int getImageWidth() {
        if (image == null) {
            logger.error("Must set image before querying image size");
            return 0;
        } else {
            return image.getSize().width;
        }
    }

    public int getImageHeight() {
        if (image == null) {
            logger.error("Must set image before querying image size");
            return 0;
        } else {
            return image.getSize().height;
        }
    }

    public void setImage(final Image image) {
        this.image = image;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public String cmdMoveFirst() {
        if (this.pageNo != 0) {
            this.pageNo = 0;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMovePrevious() {
        if (!isFirstPage()) {
            this.pageNo--;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveNext() {
        if (!isLastPage()) {
            this.pageNo++;
            getPaginatorList();
        }
        return "";
    }

    public String cmdMoveLast() {
        if (this.pageNo != getLastPageNumber()) {
            this.pageNo = getLastPageNumber();
            getPaginatorList();
        }
        return "";
    }

    public void setTxtMoveTo(int neueSeite) {
        if ((this.pageNo != neueSeite - 1) && neueSeite > 0 && neueSeite <= getLastPageNumber() + 1) {
            this.pageNo = neueSeite - 1;
            getPaginatorList();
        }
    }

    public int getTxtMoveTo() {
        return this.pageNo + 1;
    }

    public int getLastPageNumber() {
        int ret = new Double(Math.floor(this.allImages.size() / numberOfImagesPerPage)).intValue();
        if (this.allImages.size() % numberOfImagesPerPage == 0) {
            ret--;
        }
        return ret;
    }

    public boolean isFirstPage() {
        return this.pageNo == 0;
    }

    public boolean isLastPage() {
        return this.pageNo >= getLastPageNumber();
    }

    public boolean hasNextPage() {
        return this.allImages.size() > numberOfImagesPerPage;
    }

    public boolean hasPreviousPage() {
        return this.pageNo > 0;
    }

    public Long getPageNumberCurrent() {
        return Long.valueOf(this.pageNo + 1);
    }

    public Long getPageNumberLast() {
        return Long.valueOf(getLastPageNumber() + 1);
    }

    public int getSizeOfImageList() {
        return allImages.size();
    }

    public int getThumbnailSize() {
        return thumbnailSizeInPixel;
    }

    public void setThumbnailSize(int value) {
        if (value == 0) {
            return;
        }
        if (thumbnailSizeInPixel != value) {
            sizeChanged = true;
            thumbnailSizeInPixel = value;
            getPaginatorList();
            sizeChanged = false;
        }
    }

    public int getContainerWidth() {
        return containerWidth;
    }

    public void setContainerWidth(int containerWidth) {
        if (containerWidth > 100) {
            this.containerWidth = containerWidth;
        }
    }

    public void increaseContainerWidth() {
        containerWidth = containerWidth + 100;
    }

    public void reduceContainerWidth() {
        if (containerWidth > 200) {
            containerWidth = containerWidth - 100;
        }
    }

    public void changeFolder() {
        allImages = new ArrayList<>();
        try {
            List<String> imageNames = imagehelper.getImageFiles(myProzess, currentTifFolder);
            if (imageNames != null && !imageNames.isEmpty()) {
                imageFolderName = myProzess.getImagesDirectory() + currentTifFolder + File.separator;
                int order = 1;
                for (String imagename : imageNames) {
                    Image currentImage = new Image(myProzess, imageFolderName, imagename, order, thumbnailSizeInPixel);
                    allImages.add(currentImage);
                }
                setImageIndex(imageIndex);
            }
        } catch (InvalidImagesException | SwapException | DAOException | IOException | InterruptedException e1) {
            logger.error(e1);
        }

    }

    public int getNumberOfImagesPerPage() {
        return numberOfImagesPerPage;
    }

    public void setNumberOfImagesPerPage(int numberOfImagesPerPage) {
        if (numberOfImagesPerPage == 0) {
            return;
        }
        if (this.numberOfImagesPerPage != numberOfImagesPerPage) {
            this.numberOfImagesPerPage = numberOfImagesPerPage;
            pageNo = 0;
            getPaginatorList();
        }
    }

    public void setCurrentMetadata(Metadata currentMetadata) {
        this.currentMetadata = currentMetadata;
    }

    public Metadata getCurrentMetadata() {
        return currentMetadata;
    }

    public void setCurrentPerson(Person currentPerson) {
        this.currentPerson = currentPerson;
    }

    public Person getCurrentPerson() {
        return currentPerson;
    }

    public List<String> getPossibleDatabases() {
        if (normdataList.isEmpty()) {
            List<NormDatabase> databaseList = NormDatabase.getAllDatabases();
            for (NormDatabase norm : databaseList) {
                normdataList.add(norm.getAbbreviation());
            }
        }
        return normdataList;
    }

    public List<String> getPossibleNamePartTypes() {
        List<String> possibleNamePartTypes = new ArrayList<>();
        possibleNamePartTypes.add("date");
        possibleNamePartTypes.add("termsOfAddress");
        return possibleNamePartTypes;
    }

    public String getPaginationPrefix() {
        return paginationPrefix;
    }

    public String getPaginationSuffix() {
        return paginationSuffix;
    }

    public void setPaginationPrefix(String paginationPrefix) {
        this.paginationPrefix = paginationPrefix;
    }

    public void setPaginationSuffix(String paginationSuffix) {
        this.paginationSuffix = paginationSuffix;
    }

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

    public String getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(String rowIndex) {
        if (this.rowIndex == null || !this.rowIndex.equals(rowIndex)) {
            this.rowIndex = rowIndex;
            loadCurrentPlugin();
        }
    }

    public void loadCurrentPlugin() {
        logger.debug(rowIndex);
        logger.debug(rowType);
        if (rowIndex != null && !rowIndex.isEmpty()) {
            if (rowType.equals("metadata")) {
                currentPlugin = myMetadaten.get(Integer.parseInt(rowIndex)).getPlugin();
            } else if (rowType.equals("person")) {
                currentPlugin = myPersonen.get(Integer.parseInt(rowIndex)).getPlugin();
            } else if (rowType.equals("addablePerson")) {
                currentPlugin = addablePersondata.get(Integer.parseInt(rowIndex)).getPlugin();
            } else if (rowType.equals("addableMetadata")) {
                currentPlugin = addableMetadata.get(Integer.parseInt(rowIndex)).getPlugin();
            } else if (rowType.equals("group-metadata") && !StringUtils.isBlank(groupIndex)) {
                currentPlugin = groups.get(Integer.parseInt(rowIndex)).getMetadataList().get(Integer.parseInt(groupIndex)).getPlugin();
            } else if (rowType.equals("group-person") && !StringUtils.isBlank(groupIndex)) {
                currentPlugin = groups.get(Integer.parseInt(rowIndex)).getPersonList().get(Integer.parseInt(groupIndex)).getPlugin();
            }

        }
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption;
    }

    public String getGndSearchValue() {
        return gndSearchValue;
    }

    public void setGndSearchValue(String searchValue) {
        this.gndSearchValue = searchValue;
    }

    public String getGeonamesSearchValue() {
        return geonamesSearchValue;
    }

    public void setGeonamesSearchValue(String geonamesSearchValue) {
        this.geonamesSearchValue = geonamesSearchValue;
    }

    public IMetadataPlugin getCurrentPlugin() {
        return currentPlugin;
    }

    public void setCurrentPlugin(IMetadataPlugin currentPlugin) {
        this.currentPlugin = currentPlugin;
    }

    public String getDanteSearchValue() {
        return danteSearchValue;
    }

    public void setDanteSearchValue(String danteSearchValue) {
        this.danteSearchValue = danteSearchValue;
    }

    public boolean isDisplayHiddenMetadata() {
        return displayHiddenMetadata;
    }

    public void setDisplayHiddenMetadata(boolean displayHiddenMetadata) {
        this.displayHiddenMetadata = displayHiddenMetadata;
    }

    public void reloadMetadataList() {
        MetadatenalsBeanSpeichern(currentTopstruct);
    }
}
