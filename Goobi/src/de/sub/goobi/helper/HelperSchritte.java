package de.sub.goobi.helper;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- http://www.goobi.org
 *     		- http://launchpad.net/goobi-production
 * 		    - http://gdz.sub.uni-goettingen.de
 * 			- http://www.intranda.com
 * 			- http://digiverso.com
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
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.managedbeans.LoginBean;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.jobs.HistoryAnalyserJob;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IExportPlugin;
import org.goobi.production.plugin.interfaces.IValidatorPlugin;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.persistence.managers.HistoryManager;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.UGHException;

public class HelperSchritte {
    private static final Logger logger = Logger.getLogger(HelperSchritte.class);
    public final static String DIRECTORY_PREFIX = "orig_";
    private static final Namespace goobiNamespace = Namespace.getNamespace("goobi", "http://meta.goobi.org/v1.5.1/");
    private static final Namespace mets = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    private static final Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    /**
     * Schritt abschliessen und dabei parallele Schritte berücksichtigen ================================================================
     */

    public void CloseStepObjectAutomatic(Step currentStep) {
        closeStepObject(currentStep, currentStep.getProcessId());
    }

    private void closeStepObject(Step currentStep, int processId) {
        currentStep.setBearbeitungsstatusEnum(StepStatus.DONE);
        Date myDate = new Date();

        currentStep.setBearbeitungszeitpunkt(myDate);
        try {
            LoginBean lf = (LoginBean) Helper.getManagedBeanValue("#{LoginForm}");
            if (lf != null) {
                User ben = lf.getMyBenutzer();
                if (ben != null) {
                    currentStep.setBearbeitungsbenutzer(ben);
                }
            }
        } catch (Exception e) {

        }
        currentStep.setBearbeitungsende(myDate);
        try {
            StepManager.saveStep(currentStep);
            Helper.addMessageToProcessLog(currentStep.getProcessId(), LogType.DEBUG, "Step '" + currentStep.getTitel() + "' closed.");
        } catch (DAOException e) {
            logger.error("An exception occurred while closing the step '" + currentStep.getTitel() + "' of process with ID " + processId, e);
        }

        if (currentStep.isUpdateMetadataIndex()) {
            try {
                String metdatdaPath = currentStep.getProzess().getMetadataFilePath();
                String anchorPath = metdatdaPath.replace("meta.xml", "meta_anchor.xml");
                Path metadataFile = Paths.get(metdatdaPath);
                Path anchorFile = Paths.get(anchorPath);
                Map<String, List<String>> pairs = new HashMap<>();

                extractMetadata(metadataFile, pairs);

                if (StorageProvider.getInstance().isFileExists(anchorFile)) {
                    extractMetadata(anchorFile, pairs);
                }

                MetadataManager.updateMetadata(currentStep.getProzess().getId(), pairs);

                // now add all authority fields to the metadata pairs
                extractAuthorityMetadata(metadataFile, pairs);
                if (StorageProvider.getInstance().isFileExists(anchorFile)) {
                    extractAuthorityMetadata(anchorFile, pairs);
                }
                MetadataManager.updateJSONMetadata(processId, pairs);

                HistoryAnalyserJob.updateHistory(currentStep.getProzess());

                if (!currentStep.getProzess().isMediaFolderExists() && StorageProvider.getInstance().isFileExists(Paths.get(currentStep.getProzess()
                        .getImagesDirectory()))) {
                    currentStep.getProzess().setMediaFolderExists(true);
                    ProcessManager.saveProcessInformation(currentStep.getProzess());
                }

            } catch (SwapException | DAOException | IOException | InterruptedException e1) {
                logger.error("An exception occurred while updating the metadata file process with ID " + processId, e1);
            }
        }

        List<Step> automatischeSchritte = new ArrayList<>();
        List<Step> stepsToFinish = new ArrayList<>();
        HistoryManager.addHistory(myDate, new Integer(currentStep.getReihenfolge()).doubleValue(), currentStep.getTitel(), HistoryEventType.stepDone
                .getValue(), processId);

        /* prüfen, ob es Schritte gibt, die parallel stattfinden aber noch nicht abgeschlossen sind */
        List<Step> steps = StepManager.getStepsForProcess(processId);
        List<Step> allehoeherenSchritte = new ArrayList<>();
        int offeneSchritteGleicherReihenfolge = 0;
        for (Step so : steps) {
            if (so.getReihenfolge() == currentStep.getReihenfolge() && !(so.getBearbeitungsstatusEnum().equals(StepStatus.DONE) || so
                    .getBearbeitungsstatusEnum().equals(StepStatus.DEACTIVATED)) && so.getId() != currentStep.getId()) {
                offeneSchritteGleicherReihenfolge++;
            } else if (so.getReihenfolge() > currentStep.getReihenfolge()) {
                allehoeherenSchritte.add(so);
            }
        }

        /* wenn keine offenen parallelschritte vorhanden sind, die nächsten Schritte aktivieren */
        if (offeneSchritteGleicherReihenfolge == 0) {
            int reihenfolge = 0;
            boolean matched = false;
            for (Step myStep : allehoeherenSchritte) {
                if (reihenfolge < myStep.getReihenfolge() && !matched) {
                    reihenfolge = myStep.getReihenfolge();
                }

                if (reihenfolge == myStep.getReihenfolge() && !(myStep.getBearbeitungsstatusEnum().equals(StepStatus.DONE) || myStep
                        .getBearbeitungsstatusEnum().equals(StepStatus.DEACTIVATED))) {
                    /*
                     * open step, if it is locked, otherwise stop
                     */

                    if (myStep.getBearbeitungsstatusEnum().equals(StepStatus.LOCKED)) {
                        myStep.setBearbeitungsstatusEnum(StepStatus.OPEN);
                        myStep.setBearbeitungszeitpunkt(myDate);
                        myStep.setEditTypeEnum(StepEditType.AUTOMATIC);
                        HistoryManager.addHistory(myDate, new Integer(myStep.getReihenfolge()).doubleValue(), myStep.getTitel(),
                                HistoryEventType.stepOpen.getValue(), processId);
                        /* wenn es ein automatischer Schritt mit Script ist */
                        if (myStep.isTypAutomatisch()) {
                            automatischeSchritte.add(myStep);
                        } else if (myStep.isTypBeimAnnehmenAbschliessen()) {
                            stepsToFinish.add(myStep);
                        }
                        try {
                            StepManager.saveStep(myStep);
                            Helper.addMessageToProcessLog(currentStep.getProcessId(), LogType.DEBUG, "Step '" + myStep.getTitel() + "' opened.");
                        } catch (DAOException e) {
                            logger.error("An exception occurred while saving a step for process with ID " + myStep.getProcessId(), e);
                        }
                    }
                    matched = true;

                } else {
                    if (matched) {
                        break;
                    }
                }
            }
        }
        Process po = ProcessManager.getProcessById(processId);

        try {
            int numberOfFiles = StorageProvider.getInstance().getNumberOfFiles(Paths.get(po.getImagesOrigDirectory(true)));
            if (numberOfFiles > 0 && po.getSortHelperImages() != numberOfFiles) {
                ProcessManager.updateImages(numberOfFiles, processId);
            }

        } catch (Exception e) {
            logger.error("An exception occurred while closing a step for process with ID " + po.getId(), e);
        }

        updateProcessStatus(processId);
        for (Step automaticStep : automatischeSchritte) {
            automaticStep.setBearbeitungsbeginn(new Date());
            automaticStep.setBearbeitungsbenutzer(null);
            automaticStep.setBearbeitungsstatusEnum(StepStatus.INWORK);
            automaticStep.setEditTypeEnum(StepEditType.AUTOMATIC);
            HistoryManager.addHistory(automaticStep.getBearbeitungsbeginn(), automaticStep.getReihenfolge().doubleValue(), automaticStep.getTitel(),
                    HistoryEventType.stepInWork.getValue(), automaticStep.getProzess().getId());
            try {
                StepManager.saveStep(automaticStep);
                Helper.addMessageToProcessLog(currentStep.getProcessId(), LogType.DEBUG, "Step '" + automaticStep.getTitel()
                        + "' started to work automatically.");
            } catch (DAOException e) {
                logger.error("An exception occurred while saving an automatic step for process with ID " + automaticStep.getProcessId(), e);
            }
            // save
            if (logger.isDebugEnabled()) {
                logger.debug("Starting scripts for step with stepId " + automaticStep.getId() + " and processId " + automaticStep.getProcessId());
            }
            ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(automaticStep);
            myThread.start();
        }
        for (Step finish : stepsToFinish) {
            CloseStepObjectAutomatic(finish);
        }

    }

    public void updateProcessStatus(int processId) {

        int offen = 0;
        int inBearbeitung = 0;
        int abgeschlossen = 0;
        List<Step> stepsForProcess = StepManager.getStepsForProcess(processId);
        for (Step step : stepsForProcess) {
            if (step.getBearbeitungsstatusEnum().equals(StepStatus.DONE) || step.getBearbeitungsstatusEnum().equals(StepStatus.DEACTIVATED)) {
                abgeschlossen++;
            } else if (step.getBearbeitungsstatusEnum().equals(StepStatus.LOCKED)) {
                offen++;
            } else {
                inBearbeitung++;
            }
        }
        double offen2 = 0;
        double inBearbeitung2 = 0;
        double abgeschlossen2 = 0;

        if ((offen + inBearbeitung + abgeschlossen) == 0) {
            offen = 1;
        }

        offen2 = (offen * 100) / (double) (offen + inBearbeitung + abgeschlossen);
        inBearbeitung2 = (inBearbeitung * 100) / (double) (offen + inBearbeitung + abgeschlossen);
        abgeschlossen2 = 100 - offen2 - inBearbeitung2;
        // (abgeschlossen * 100) / (offen + inBearbeitung + abgeschlossen);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#000");
        String value = df.format(abgeschlossen2) + df.format(inBearbeitung2) + df.format(offen2);

        ProcessManager.updateProcessStatus(value, processId);
    }

    public int executeAllScriptsForStep(Step step, boolean automatic) {
        List<String> scriptpaths = step.getAllScriptPaths();
        int count = 1;
        int size = scriptpaths.size();
        int returnParameter = 0;
        outerloop: for (String script : scriptpaths) {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting script " + script + " for process with ID " + step.getProcessId());
            }

            if (script != null && !script.equals(" ") && script.length() != 0) {
                if (automatic && (count == size)) {
                    returnParameter = executeScriptForStepObject(step, script, true);
                } else {
                    returnParameter = executeScriptForStepObject(step, script, false);
                }
            }

            if (automatic) {
                switch (returnParameter) {
                    // return code 99 means wait for finishing
                    case 99:

                        break;
                    // return code 98: re-open task
                    case 98:
                        reOpenStep(step);
                        break;
                    // return code 0: script returned without error
                    case 0:
                        break;
                    // everything else: error
                    default:
                        errorStep(step);
                        break outerloop;

                }
            }
            //            if (returnParameter != 0 && automatic && returnParameter != 99) {
            //                errorStep(step);
            //                break;
            //            }
            count++;
        }
        return returnParameter;
    }

    public void runHttpStep(Step step) {
        if (!step.isTypAutomatisch()) {
            return;
        }
        DigitalDocument dd = null;
        Process po = step.getProzess();
        Prefs prefs = null;
        try {
            prefs = po.getRegelsatz().getPreferences();
            Fileformat ff = po.readMetadataFile();
            if (ff == null) {
                logger.error("Metadata file is not readable for process with ID " + step.getProcessId());
                LogEntry le = new LogEntry();
                le.setProcessId(step.getProzess().getId());
                le.setContent("Metadata file is not readable");
                le.setType(LogType.ERROR);
                le.setUserName("http step");
                ProcessManager.saveLogEntry(le);
                errorStep(step);
                return;
            }
            dd = ff.getDigitalDocument();
        } catch (Exception e2) {
            logger.error("An exception occurred while reading the metadata file for process with ID " + step.getProcessId(), e2);
            LogEntry le = new LogEntry();
            le.setProcessId(step.getProzess().getId());
            le.setContent("error reading metadata file");
            le.setType(LogType.ERROR);
            le.setUserName("http step");
            ProcessManager.saveLogEntry(le);
            errorStep(step);
            return;
        }
        VariableReplacer replacer = new VariableReplacer(dd, prefs, step.getProzess(), step);
        String bodyStr = replacer.replace(step.getHttpJsonBody());
        String url = replacer.replace(step.getHttpUrl());
        // START dirty hack to allow testing with certs with wrong hostnames, this should be removed when we have correct hostnames/certificates
        SSLConnectionSocketFactory scsf = null;
        try {
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            LogEntry le = new LogEntry();
            le.setCreationDate(new Date());
            le.setProcessId(step.getProzess().getId());
            le.setContent("error executing http request: " + e1.getMessage());
            le.setType(LogType.ERROR);
            le.setUserName("http step");
            ProcessManager.saveLogEntry(le);
            errorStep(step);
            logger.error(e1);
            return;
        }
        // END dirty hack
        HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(scsf).build();
        Executor executor = Executor.newInstance(httpclient);
        try {
            HttpResponse resp = null;
            switch (step.getHttpMethod()) {
                case "POST":
                    resp = executor.execute(Request.Post(url)
                            .bodyString(bodyStr, ContentType.APPLICATION_JSON)).returnResponse();
                    break;
                case "PUT":
                    resp = executor.execute(Request.Put(url)
                            .bodyString(bodyStr, ContentType.APPLICATION_JSON)).returnResponse();
                    break;
                case "PATCH":
                    resp = executor.execute(Request.Patch(url)
                            .bodyString(bodyStr, ContentType.APPLICATION_JSON)).returnResponse();
                    break;
                default:
                    //TODO: error to process log
                    break;
            }
            if (resp != null) {
                String respStr = "- no response body -";
                if (resp.getEntity() != null && resp.getEntity().getContentLength() < 20000) {
                    StringWriter writer = new StringWriter();
                    Charset encoding = Charset.forName("utf-8");
                    if (resp.getEntity().getContentEncoding() != null) {
                        try {
                            encoding = Charset.forName(resp.getEntity().getContentEncoding().getValue());
                        } catch (Exception e) {
                            //we can do nothing here
                            logger.error(e);
                        }
                    }
                    IOUtils.copy(resp.getEntity().getContent(), writer, encoding);
                    respStr = writer.toString();
                }
                int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode >= 400) {
                    LogEntry le = new LogEntry();
                    le.setCreationDate(new Date());
                    le.setProcessId(step.getProzess().getId());
                    le.setContent(String.format("Server returned status code %d, response body was: '%s'", statusCode, respStr));
                    le.setType(LogType.ERROR);
                    le.setUserName("http step");
                    ProcessManager.saveLogEntry(le);
                    errorStep(step);
                    logger.error(respStr);
                    return;
                }
                LogEntry le = new LogEntry();
                le.setCreationDate(new Date());
                le.setProcessId(step.getProzess().getId());
                le.setContent(respStr);
                le.setType(LogType.INFO);
                le.setUserName("http step");
                ProcessManager.saveLogEntry(le);
                if (step.isHttpCloseStep()) {
                    CloseStepObjectAutomatic(step);
                }
                logger.info(respStr);
            } else {
                LogEntry le = new LogEntry();
                le.setCreationDate(new Date());
                le.setProcessId(step.getProzess().getId());
                le.setContent("error executing http request");
                le.setType(LogType.ERROR);
                le.setUserName("http step");
                ProcessManager.saveLogEntry(le);
            }
        } catch (IOException e) {
            LogEntry le = new LogEntry();
            le.setCreationDate(new Date());
            le.setProcessId(step.getProzess().getId());
            le.setContent("error executing http request: " + e.getMessage());
            le.setType(LogType.ERROR);
            le.setUserName("http step");
            ProcessManager.saveLogEntry(le);
            errorStep(step);
            logger.error(e);
        }
    }

    public int executeScriptForStepObject(Step step, String script, boolean automatic) {
        if (script == null || script.length() == 0) {
            return -1;
        }

        DigitalDocument dd = null;
        Process po = step.getProzess();
        Prefs prefs = null;
        try {
            prefs = po.getRegelsatz().getPreferences();
            Fileformat ff = po.readMetadataFile();
            if (ff == null) {
                logger.error("Metadata file is not readable for process with ID " + step.getProcessId());
                return -1;
            }
            dd = ff.getDigitalDocument();
        } catch (Exception e2) {
            logger.error("An exception occurred while reading the metadata file for process with ID " + step.getProcessId(), e2);
        }
        VariableReplacer replacer = new VariableReplacer(dd, prefs, step.getProzess(), step);
        List<String> parameterList = replacer.replaceBashScript(script);
        //        script = replacer.replace(script);
        int rueckgabe = -1;
        try {
            logger.info("Calling the shell: " + script + " for process with ID " + step.getProcessId());

            StringBuilder message = new StringBuilder();
            message.append("Calling the shell: ");
            message.append(script);

            Helper.addMessageToProcessLog(step.getProcessId(), LogType.DEBUG, message.toString());

            rueckgabe = ShellScript.callShell(parameterList, step.getProcessId());
            if (automatic) {
                if (rueckgabe == 0) {
                    step.setEditTypeEnum(StepEditType.AUTOMATIC);
                    step.setBearbeitungsstatusEnum(StepStatus.DONE);
                    if (step.getValidationPlugin() != null && step.getValidationPlugin().length() > 0) {
                        IValidatorPlugin ivp = (IValidatorPlugin) PluginLoader.getPluginByTitle(PluginType.Validation, step.getValidationPlugin());
                        ivp.setStep(step);
                        if (!ivp.validate()) {
                            step.setBearbeitungsstatusEnum(StepStatus.OPEN);
                            StepManager.saveStep(step);
                            Helper.addMessageToProcessLog(step.getProcessId(), LogType.DEBUG, "Step '" + step.getTitel() + "' opened.");
                        } else {
                            CloseStepObjectAutomatic(step);
                        }
                    } else {
                        CloseStepObjectAutomatic(step);
                    }

                } else {
                    if (rueckgabe != 99 && rueckgabe != 98) {
                        step.setEditTypeEnum(StepEditType.AUTOMATIC);
                        step.setBearbeitungsstatusEnum(StepStatus.ERROR);
                        StepManager.saveStep(step);
                        Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, "Script for '" + step.getTitel()
                                + "' did not finish successfully. Return code: " + rueckgabe);
                        logger.error("Script for '" + step.getTitel() + "' did not finish successfully for process with ID " + step.getProcessId()
                                + ". Return code: " + rueckgabe);
                    }
                }
            }
        } catch (Exception e) {
            Helper.setFehlerMeldung("An exception occured while running a script", e.getMessage());
            Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, "Exception while executing a script for '" + step.getTitel() + "': " + e
                    .getMessage());
            logger.error("Exception occurered while running a script for process with ID " + step.getProcessId(), e);
        }
        return rueckgabe;
    }

    public void executeDmsExport(Step step, boolean automatic) {
        IExportPlugin dms = null;
        if (StringUtils.isNotBlank(step.getStepPlugin())) {
            try {
                dms = (IExportPlugin) PluginLoader.getPluginByTitle(PluginType.Export, step.getStepPlugin());
            } catch (Exception e) {
                logger.error("Can't load export plugin, use default plugin for process with ID " + step.getProcessId(), e);
                dms = new ExportDms(ConfigurationHelper.getInstance().isAutomaticExportWithImages());
                //                dms = new AutomaticDmsExport(ConfigurationHelper.getInstance().isAutomaticExportWithImages());
            }
        }
        if (dms == null) {
            dms = new ExportDms(ConfigurationHelper.getInstance().isAutomaticExportWithImages());
            //            dms = new AutomaticDmsExport(ConfigurationHelper.getInstance().isAutomaticExportWithImages());
        }
        if (!ConfigurationHelper.getInstance().isAutomaticExportWithOcr()) {
            dms.setExportFulltext(false);
        }
        //        ProcessObject po = ProcessManager.getProcessObjectForId(step.getProcessId());
        try {
            boolean validate = dms.startExport(step.getProzess());
            if (validate) {
                Helper.addMessageToProcessLog(step.getProcessId(), LogType.DEBUG, "The export for process with ID '" + step.getProcessId()
                        + "' was done successfully.");
                CloseStepObjectAutomatic(step);
            } else {
                Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, "The export for process with ID '" + step.getProcessId()
                        + "' was cancelled because of validation errors: " + dms.getProblems().toString());
                errorStep(step);
            }
        } catch (DAOException | UGHException | SwapException | IOException | InterruptedException | DocStructHasNoTypeException | UghHelperException
                | ExportFileException e) {
            logger.error("Exception occurered while trying to export process with ID " + step.getProcessId(), e);
            Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, "An exception occurred during the export for process with ID " + step
                    .getProcessId() + ": " + e.getMessage());
            errorStep(step);
            return;
        }
    }

    public void errorStep(Step step) {
        step.setBearbeitungsstatusEnum(StepStatus.ERROR);
        step.setEditTypeEnum(StepEditType.AUTOMATIC);
        try {
            StepManager.saveStep(step);
        } catch (DAOException e) {
            logger.error("Error while saving a workflow step for process with ID " + step.getProcessId(), e);
        }
    }

    private void reOpenStep(Step step) {
        if (!step.getBearbeitungsstatusEnum().equals(StepStatus.OPEN)) {
            step.setBearbeitungsstatusEnum(StepStatus.OPEN);
            step.setEditTypeEnum(StepEditType.AUTOMATIC);
            step.setBearbeitungsende(new Date());
            try {
                StepManager.saveStep(step);
            } catch (DAOException e) {
                logger.error("Error while saving a workflow step for process with ID " + step.getProcessId(), e);
            }
        }

    }

    public static void extractAuthorityMetadata(Path metadataFile, Map<String, List<String>> metadataPairs) {
        XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Element> authorityMetaXpath = xFactory.compile(
                "//mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[goobi:authorityValue]",
                Filters.element(), null, mods, mets, goobiNamespace);
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(metadataFile.toString());
        } catch (JDOMException | IOException e1) {
            return;
        }
        for (Element meta : authorityMetaXpath.evaluate(doc)) {
            String name = meta.getAttributeValue("name");
            if (name == null) {
                continue;
            } else {
                String key = name + "_authority";
                List<String> values = metadataPairs.get(key);
                if (values == null) {
                    values = new ArrayList<>();
                    metadataPairs.put(key, values);
                }
                values.add(meta.getChildText("authorityValue", goobiNamespace));
            }
        }
    }

    public static void extractMetadata(Path metadataFile, Map<String, List<String>> metadataPairs) {
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(metadataFile.toString());
        } catch (JDOMException | IOException e1) {
            return;
        }
        Element root = doc.getRootElement();
        try {
            Element goobi = root.getChildren("dmdSec", mets).get(0).getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods)
                    .getChild("extension", mods).getChild("goobi", goobiNamespace);
            List<Element> metadataList = goobi.getChildren();
            addMetadata(metadataList, metadataPairs);
            for (Element el : root.getChildren("dmdSec", mets)) {
                if (el.getAttributeValue("ID").equals("DMDPHYS_0000")) {
                    Element phys = el.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild(
                            "goobi", goobiNamespace);
                    List<Element> physList = phys.getChildren();
                    addMetadata(physList, metadataPairs);
                }
            }
            // create field for "DocStruct"
            String docType = root.getChildren("structMap", mets).get(0).getChild("div", mets).getAttributeValue("TYPE");
            metadataPairs.put("DocStruct", Collections.singletonList(docType));

        } catch (Exception e) {
            logger.error("Cannot extract metadata from " + metadataFile.toString());
        }
    }

    private static void addMetadata(List<Element> elements, Map<String, List<String>> metadataPairs) {
        for (Element goobimetadata : elements) {
            String metadataType = goobimetadata.getAttributeValue("name");
            String metadataValue = "";
            if (goobimetadata.getAttributeValue("type") != null && goobimetadata.getAttributeValue("type").equals("person")) {
                Element displayName = goobimetadata.getChild("displayName", goobiNamespace);
                if (displayName != null && !displayName.getValue().equals(",")) {
                    metadataValue = displayName.getValue();
                }
            } else if (goobimetadata.getAttributeValue("type") != null && goobimetadata.getAttributeValue("type").equals("group")) {
                List<Element> groupMetadataList = goobimetadata.getChildren();
                addMetadata(groupMetadataList, metadataPairs);
            } else {
                metadataValue = goobimetadata.getValue();
            }
            if (!metadataValue.equals("")) {

                if (metadataPairs.containsKey(metadataType)) {
                    List<String> oldValue = metadataPairs.get(metadataType);
                    if (!oldValue.contains(metadataValue)) {
                        oldValue.add(metadataValue);
                        metadataPairs.put(metadataType, oldValue);
                    }
                } else {
                    List<String> list = new ArrayList<>();
                    list.add(metadataValue);
                    metadataPairs.put(metadataType, list);
                }
            }

        }
        return;
    }
}
