package de.sub.goobi.export.download;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;
import org.goobi.beans.Process;
import org.goobi.beans.ProjectFileGroup;
import org.goobi.beans.User;
import org.goobi.managedbeans.LoginBean;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.InvalidImagesException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.metadaten.MetadatenHelper;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.ExportFileformat;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.dl.VirtualFileGroup;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

public class ExportMets {
    protected Helper help = new Helper();
    protected Prefs myPrefs;
    protected List<String> problems = new ArrayList<>();

    protected static final Logger logger = LogManager.getLogger(ExportMets.class);

    /**
     * DMS-Export in das Benutzer-Homeverzeichnis
     * 
     * @param myProzess
     * @throws InterruptedException
     * @throws IOException
     * @throws DAOException
     * @throws SwapException
     * @throws ReadException
     * @throws UghHelperException
     * @throws ExportFileException
     * @throws MetadataTypeNotAllowedException
     * @throws WriteException
     * @throws PreferencesException
     * @throws DocStructHasNoTypeException
     * @throws TypeNotAllowedForParentException
     */
    public boolean startExport(Process myProzess) throws IOException, InterruptedException, DocStructHasNoTypeException, PreferencesException,
            WriteException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException, SwapException, DAOException,
            TypeNotAllowedForParentException {
        LoginBean login = (LoginBean) Helper.getManagedBeanValue("#{LoginForm}");
        String benutzerHome = "";
        if (login != null) {
            benutzerHome = login.getMyBenutzer().getHomeDir();
        } else {
            benutzerHome = myProzess.getProjekt().getDmsImportImagesPath();
        }
        return startExport(myProzess, benutzerHome);
    }

    /**
     * DMS-Export an eine gewünschte Stelle
     * 
     * @param myProzess
     * @param zielVerzeichnis
     * @throws InterruptedException
     * @throws IOException
     * @throws PreferencesException
     * @throws WriteException
     * @throws UghHelperException
     * @throws ExportFileException
     * @throws MetadataTypeNotAllowedException
     * @throws DocStructHasNoTypeException
     * @throws DAOException
     * @throws SwapException
     * @throws ReadException
     * @throws TypeNotAllowedForParentException
     */
    public boolean startExport(Process myProzess, String inZielVerzeichnis) throws IOException, InterruptedException, PreferencesException,
            WriteException, DocStructHasNoTypeException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException,
            SwapException, DAOException, TypeNotAllowedForParentException {

        /*
         * -------------------------------- Read Document --------------------------------
         */
        this.myPrefs = myProzess.getRegelsatz().getPreferences();
        String atsPpnBand = myProzess.getTitel();
        Fileformat gdzfile = myProzess.readMetadataFile();

        String zielVerzeichnis = prepareUserDirectory(inZielVerzeichnis);

        String targetFileName = zielVerzeichnis + atsPpnBand + "_mets.xml";
        return writeMetsFile(myProzess, targetFileName, gdzfile, false);

    }

    /**
     * prepare user directory
     * 
     * @param inTargetFolder the folder to proove and maybe create it
     */
    protected String prepareUserDirectory(String inTargetFolder) {
        String target = inTargetFolder;
        User myBenutzer = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
        if (myBenutzer != null) {
            try {
                FilesystemHelper.createDirectoryForUser(target, myBenutzer.getLogin());
            } catch (Exception e) {
                Helper.setFehlerMeldung("Export canceled, could not create destination directory: " + inTargetFolder, e);
            }
        }
        return target;
    }

    /**
     * write MetsFile to given Path
     * 
     * @param myProzess the Process to use
     * @param targetFileName the filename where the metsfile should be written
     * @param gdzfile the FileFormat-Object to use for Mets-Writing
     * @throws DAOException
     * @throws SwapException
     * @throws InterruptedException
     * @throws IOException
     * @throws TypeNotAllowedForParentException
     */

    protected boolean writeMetsFile(Process myProzess, String targetFileName, Fileformat gdzfile, boolean writeLocalFilegroup)
            throws PreferencesException, WriteException, IOException, InterruptedException, SwapException, DAOException,
            TypeNotAllowedForParentException {

        ExportFileformat mm = MetadatenHelper.getExportFileformatByName(myProzess.getProjekt().getFileFormatDmsExport(), myProzess.getRegelsatz());
        mm.setWriteLocal(writeLocalFilegroup);
        String imageFolderPath = myProzess.getImagesTifDirectory(true);
        Path imageFolder = Paths.get(imageFolderPath);
        /*
         * before creating mets file, change relative path to absolute -
         */
        DigitalDocument dd = gdzfile.getDigitalDocument();

        MetadatenImagesHelper mih = new MetadatenImagesHelper(this.myPrefs, dd);

        if (dd.getFileSet() == null || dd.getFileSet().getAllFiles().isEmpty()) {
            Helper.setMeldung(myProzess.getTitel() + ": digital document does not contain images; temporarily adding them for mets file creation");
            mih.createPagination(myProzess, null);
        } else {
            mih.checkImageNames(myProzess, imageFolder.getFileName().toString());
        }

        /*
         * get the topstruct element of the digital document depending on anchor property
         */
        DocStruct topElement = dd.getLogicalDocStruct();
        if (topElement.getType().isAnchor()) {
            if (topElement.getAllChildren() == null || topElement.getAllChildren().size() == 0) {
                throw new PreferencesException(
                        myProzess.getTitel() + ": the topstruct element is marked as anchor, but does not have any children for physical docstrucs");
            } else {
                topElement = topElement.getAllChildren().get(0);
            }
        }

        /*
         * -------------------------------- if the top element does not have any image related, set them all --------------------------------
         */

        if (ConfigurationHelper.getInstance().isExportValidateImages()) {

            if (topElement.getAllToReferences("logical_physical") == null || topElement.getAllToReferences("logical_physical").size() == 0) {
                if (dd.getPhysicalDocStruct() != null && dd.getPhysicalDocStruct().getAllChildren() != null) {
                    Helper.setMeldung(myProzess.getTitel()
                            + ": topstruct element does not have any referenced images yet; temporarily adding them for mets file creation");
                    for (DocStruct mySeitenDocStruct : dd.getPhysicalDocStruct().getAllChildren()) {
                        topElement.addReferenceTo(mySeitenDocStruct, "logical_physical");
                    }
                } else {
                    Helper.setFehlerMeldung(myProzess.getTitel() + ": could not find any referenced images, export aborted");
                    dd = null;
                    return false;
                }
            }

            for (ContentFile cf : dd.getFileSet().getAllFiles()) {
                String location = cf.getLocation();
                // If the file's location string shoes no sign of any protocol,
                // use the file protocol.
                if (!location.contains("://")) {
                    if (!location.matches("^[A-Z]:.*") && !location.matches("^\\/.*")) {
                        //is a relative path
                        Path f = Paths.get(imageFolder.toString(), location);
                        location = f.toString();
                    }
                    location = "file://" + location;
                }
                cf.setLocation(location);
            }
        }
        mm.setDigitalDocument(dd);

        /*
         * -------------------------------- wenn Filegroups definiert wurden, werden diese jetzt in die Metsstruktur übernommen
         * --------------------------------
         */
        // Replace all pathes with the given VariableReplacer, also the file
        // group pathes!
        VariableReplacer vp = new VariableReplacer(mm.getDigitalDocument(), this.myPrefs, myProzess, null);
        List<ProjectFileGroup> myFilegroups = myProzess.getProjekt().getFilegroups();

        if (myFilegroups != null && myFilegroups.size() > 0) {
            for (ProjectFileGroup pfg : myFilegroups) {
                // check if source files exists
                if (pfg.getFolder() != null && pfg.getFolder().length() > 0) {
                    String foldername = myProzess.getMethodFromName(pfg.getFolder());
                    if (foldername != null) {
                        Path folder = Paths.get(myProzess.getMethodFromName(pfg.getFolder()));
                        if (folder != null && StorageProvider.getInstance().isFileExists(folder)
                                && !StorageProvider.getInstance().list(folder.toString()).isEmpty()) {
                            VirtualFileGroup v = new VirtualFileGroup();
                            v.setName(pfg.getName());
                            v.setPathToFiles(vp.replace(pfg.getPath()));
                            v.setMimetype(pfg.getMimetype());
                            v.setFileSuffix(pfg.getSuffix());
                            mm.getDigitalDocument().getFileSet().addVirtualFileGroup(v);
                        }
                    }
                } else {

                    VirtualFileGroup v = new VirtualFileGroup();
                    v.setName(pfg.getName());
                    v.setPathToFiles(vp.replace(pfg.getPath()));
                    v.setMimetype(pfg.getMimetype());
                    v.setFileSuffix(pfg.getSuffix());
                    mm.getDigitalDocument().getFileSet().addVirtualFileGroup(v);
                }
            }
        }

        // Replace rights and digiprov entries.
        mm.setRightsOwner(vp.replace(myProzess.getProjekt().getMetsRightsOwner()));
        mm.setRightsOwnerLogo(vp.replace(myProzess.getProjekt().getMetsRightsOwnerLogo()));
        mm.setRightsOwnerSiteURL(vp.replace(myProzess.getProjekt().getMetsRightsOwnerSite()));
        mm.setRightsOwnerContact(vp.replace(myProzess.getProjekt().getMetsRightsOwnerMail()));
        mm.setDigiprovPresentation(vp.replace(myProzess.getProjekt().getMetsDigiprovPresentation()));
        mm.setDigiprovReference(vp.replace(myProzess.getProjekt().getMetsDigiprovReference()));
        mm.setDigiprovPresentationAnchor(vp.replace(myProzess.getProjekt().getMetsDigiprovPresentationAnchor()));
        mm.setDigiprovReferenceAnchor(vp.replace(myProzess.getProjekt().getMetsDigiprovReferenceAnchor()));

        mm.setMetsRightsLicense(vp.replace(myProzess.getProjekt().getMetsRightsLicense()));
        mm.setMetsRightsSponsor(vp.replace(myProzess.getProjekt().getMetsRightsSponsor()));
        mm.setMetsRightsSponsorLogo(vp.replace(myProzess.getProjekt().getMetsRightsSponsorLogo()));
        mm.setMetsRightsSponsorSiteURL(vp.replace(myProzess.getProjekt().getMetsRightsSponsorSiteURL()));

        mm.setPurlUrl(vp.replace(myProzess.getProjekt().getMetsPurl()));
        mm.setContentIDs(vp.replace(myProzess.getProjekt().getMetsContentIDs()));

        String pointer = myProzess.getProjekt().getMetsPointerPath();
        pointer = vp.replace(pointer);
        mm.setMptrUrl(pointer);

        String anchor = myProzess.getProjekt().getMetsPointerPathAnchor();
        pointer = vp.replace(anchor);
        mm.setMptrAnchorUrl(pointer);

        mm.setGoobiID(String.valueOf(myProzess.getId()));

        // if (!ConfigMain.getParameter("ImagePrefix", "\\d{8}").equals("\\d{8}")) {
        List<String> images = new ArrayList<String>();
        if (ConfigurationHelper.getInstance().isExportValidateImages()) {
            try {
                // TODO andere Dateigruppen nicht mit image Namen ersetzen
                images = new MetadatenImagesHelper(this.myPrefs, dd).getDataFiles(myProzess, imageFolderPath);

                int sizeOfPagination = dd.getPhysicalDocStruct().getAllChildren().size();
                if (images != null) {
                    int sizeOfImages = images.size();
                    if (sizeOfPagination == sizeOfImages) {
                        dd.overrideContentFiles(images);
                    } else {
                        String[] param = { String.valueOf(sizeOfPagination), String.valueOf(sizeOfImages) };
                        Helper.setFehlerMeldung(Helper.getTranslation("imagePaginationError", param));
                        return false;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                logger.error(e);
                return false;
            } catch (InvalidImagesException e) {
                logger.error(e);
                return false;
            }
        } else {
            // create pagination out of virtual file names
            dd.addAllContentFiles();

        }
        if (ConfigurationHelper.getInstance().isExportInTemporaryFile()) {
            Path tempFile = StorageProvider.getInstance().createTemporaryFile(myProzess.getTitel(), ".xml");
            String filename = tempFile.toString();
            mm.write(filename);
            StorageProvider.getInstance().copyFile(tempFile, Paths.get(targetFileName));

            Path anchorFile = Paths.get(filename.replace(".xml", "_anchor.xml"));
            if (StorageProvider.getInstance().isFileExists(anchorFile)) {
                StorageProvider.getInstance().copyFile(anchorFile, Paths.get(targetFileName.replace(".xml", "_anchor.xml")));
                StorageProvider.getInstance().deleteDir(anchorFile);
            }
            StorageProvider.getInstance().deleteDir(tempFile);
        } else {
            mm.write(targetFileName);
        }
        Helper.setMeldung(null, myProzess.getTitel() + ": ", "ExportFinished");
        return true;
    }

    public List<String> getProblems() {
        return problems;
    }
}
