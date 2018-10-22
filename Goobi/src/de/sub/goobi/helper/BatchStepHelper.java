package de.sub.goobi.helper;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.beans.ErrorProperty;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.managedbeans.StepBean;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.jobs.HistoryAnalyserJob;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IExportPlugin;
import org.goobi.production.plugin.interfaces.IValidatorPlugin;
import org.goobi.production.properties.AccessCondition;
import org.goobi.production.properties.ProcessProperty;
import org.goobi.production.properties.PropertyParser;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.PropertyType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.metadaten.MetadatenVerifizierung;
import de.sub.goobi.persistence.managers.HistoryManager;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.Getter;
import lombok.Setter;

public class BatchStepHelper {

    private List<Step> steps;
    private static final Logger logger = Logger.getLogger(BatchStepHelper.class);
    private Step currentStep;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private List<ProcessProperty> processPropertyList;
    private ProcessProperty processProperty;
    private Map<Integer, PropertyListObject> containers = new TreeMap<>();
    private Integer container;
    private String myProblemStep;
    private String mySolutionStep;
    private String problemMessage;
    private String solutionMessage;
    private String processName = "";
    @Getter @Setter private String content = "";
    @Getter @Setter private String secondContent = "";
    @Getter @Setter private String thirdContent = "";
    private HashMap <Integer, Boolean> containerAccess;

    private String script;
    private WebDav myDav = new WebDav();
    private List<String> processNameList = new ArrayList<>();
    @Getter
    private Map<String, List<String>> displayableMetadataMap;

    public BatchStepHelper(List<Step> steps) {
        this.steps = steps;
        for (Step s : steps) {

            this.processNameList.add(s.getProzess().getTitel());
        }
        if (steps.size() > 0) {
            this.currentStep = steps.get(0);
            this.processName = this.currentStep.getProzess().getTitel();
            loadProcessProperties(this.currentStep);
            loadDisplayableMetadata(currentStep);
        }
    }

    public BatchStepHelper(List<Step> steps, Step inStep) {
        this.steps = steps;
        for (Step s : steps) {
            this.processNameList.add(s.getProzess().getTitel());
        }
        this.currentStep = inStep;
        this.processName = inStep.getProzess().getTitel();
        loadProcessProperties(this.currentStep);
        loadDisplayableMetadata(currentStep);
    }

    public List<Step> getSteps() {
        return this.steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Step getCurrentStep() {
        return this.currentStep;
    }

    public void setCurrentStep(Step currentStep) {
        this.currentStep = currentStep;
    }

    /*
     * properties
     */

    public ProcessProperty getProcessProperty() {
        return this.processProperty;
    }

    public void setProcessProperty(ProcessProperty processProperty) {
        this.processProperty = processProperty;
    }

    public List<ProcessProperty> getProcessProperties() {
        return this.processPropertyList;
    }

    public int getPropertyListSize() {
        return this.processPropertyList.size();
    }

    public List<String> getProcessNameList() {
        return this.processNameList;
    }

    public void setProcessNameList(List<String> processNameList) {
        this.processNameList = processNameList;
    }

    public String getProcessName() {
        return this.processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
        for (Step s : this.steps) {
            if (s.getProzess().getTitel().equals(processName)) {
                this.currentStep = s;
                loadProcessProperties(this.currentStep);
                loadDisplayableMetadata(currentStep);
                //try to load the same step in step-managed-bean
                StepBean sb = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
                sb.setMySchritt(s);
                break;
            }
        }
    }

    public void saveCurrentProperty() {
        List<ProcessProperty> ppList = getContainerProperties();
        for (ProcessProperty pp : ppList) {
            this.processProperty = pp;
            if (!this.processProperty.isValid()) {
                Helper.setFehlerMeldung("Property " + this.processProperty.getName() + " is not valid");
                return;
            }
            if (this.processProperty.getProzesseigenschaft() == null) {
                Processproperty pe = new Processproperty();
                pe.setProzess(this.currentStep.getProzess());
                this.processProperty.setProzesseigenschaft(pe);
                this.currentStep.getProzess().getEigenschaften().add(pe);
            }
            this.processProperty.transfer();

            Process p = this.currentStep.getProzess();
            List<Processproperty> props = p.getEigenschaftenList();
            for (Processproperty pe : props) {
                if (pe.getTitel() == null) {
                    p.getEigenschaften().remove(pe);
                }
            }
            if (!this.processProperty.getProzesseigenschaft().getProzess().getEigenschaften().contains(this.processProperty
                    .getProzesseigenschaft())) {
                this.processProperty.getProzesseigenschaft().getProzess().getEigenschaften().add(this.processProperty.getProzesseigenschaft());
            }
            PropertyManager.saveProcessProperty(processProperty.getProzesseigenschaft());
        }
        Helper.setMeldung("Property saved");
    }

    public void saveCurrentPropertyForAll() {
        List<ProcessProperty> ppList = getContainerProperties();
        for (ProcessProperty pp : ppList) {
            this.processProperty = pp;
            if (!this.processProperty.isValid()) {
                Helper.setFehlerMeldung("Property " + this.processProperty.getName() + " is not valid");
                return;
            }
            if (this.processProperty.getProzesseigenschaft() == null) {
                Processproperty pe = new Processproperty();
                pe.setProzess(this.currentStep.getProzess());
                this.processProperty.setProzesseigenschaft(pe);
                this.currentStep.getProzess().getEigenschaften().add(pe);
            }
            this.processProperty.transfer();

            Processproperty prop = processProperty.getProzesseigenschaft();
            for (Step s : this.steps) {
                Process process = s.getProzess();
                boolean match = false;
                for (Processproperty processPe : process.getEigenschaftenList()) {
                    if (processPe.getTitel() != null) {
                        if (prop.getTitel().equals(processPe.getTitel()) && prop.getContainer() == processPe.getContainer()) {
                            processPe.setWert(prop.getWert());
                            PropertyManager.saveProcessProperty(processPe);
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    Processproperty p = new Processproperty();
                    p.setTitel(prop.getTitel());
                    p.setWert(prop.getWert());
                    p.setContainer(prop.getContainer());
                    p.setType(prop.getType());
                    p.setProzess(process);
                    process.getEigenschaften().add(p);
                    PropertyManager.saveProcessProperty(p);
                }
            }
        }
        Helper.setMeldung("Properties saved");
    }

    public HashMap<Integer, Boolean> getContainerAccess() {
        return containerAccess;
    }

    public int getSizeOfDisplayableMetadata() {
        return  displayableMetadataMap.size();
    }


    private void loadDisplayableMetadata(Step s) {

        displayableMetadataMap = new LinkedHashMap<>();
        List<String> possibleMetadataNames = PropertyParser.getInstance().getDisplayableMetadataForStep(s);
        if (possibleMetadataNames.isEmpty()) {
            return;
        }

        for (String metadataName : possibleMetadataNames) {
            List<String> values = MetadataManager.getAllMetadataValues(s.getProzess().getId(), metadataName);
            if (!values.isEmpty()) {
                displayableMetadataMap.put(metadataName, values);
            }
        }
    }

    private void loadProcessProperties(Step s) {
        containerAccess = new HashMap<>();
        this.containers = new TreeMap<>();
        this.processPropertyList = PropertyParser.getInstance().getPropertiesForStep(s);
        List<Process> pList = new ArrayList<>();
        for (Step step : this.steps) {
            pList.add(step.getProzess());
        }
        for (ProcessProperty pt : this.processPropertyList) {
            if (pt.getContainer()!=0 && pt.getCurrentStepAccessCondition() != AccessCondition.READ){
                containerAccess.put(pt.getContainer(), true);
            }
            if (pt.getProzesseigenschaft() == null) {
                Processproperty pe = new Processproperty();
                pe.setProzess(s.getProzess());
                pt.setProzesseigenschaft(pe);
                s.getProzess().getEigenschaften().add(pe);
                pt.transfer();
            }
            if (!this.containers.keySet().contains(pt.getContainer())) {
                PropertyListObject plo = new PropertyListObject(pt.getContainer());
                plo.addToList(pt);
                this.containers.put(pt.getContainer(), plo);
            } else {
                PropertyListObject plo = this.containers.get(pt.getContainer());
                plo.addToList(pt);
                this.containers.put(pt.getContainer(), plo);
            }
        }

        for (Process p : pList) {
            for (Processproperty pe : p.getEigenschaftenList()) {
                if (!this.containers.keySet().contains(pe.getContainer())) {
                    this.containers.put(pe.getContainer(), null);
                }
            }
        }
    }

    public Map<Integer, PropertyListObject> getContainers() {
        return this.containers;
    }

    public int getContainersSize() {
        if (this.containers == null) {
            return 0;
        }
        return this.containers.size();
    }

    public List<ProcessProperty> getSortedProperties() {
        Comparator<ProcessProperty> comp = new ProcessProperty.CompareProperties();
        Collections.sort(this.processPropertyList, comp);
        return this.processPropertyList;
    }

    public List<ProcessProperty> getContainerlessProperties() {
        List<ProcessProperty> answer = new ArrayList<>();
        for (ProcessProperty pp : this.processPropertyList) {
            if (pp.getContainer() == 0 && pp.getName() != null) {
                answer.add(pp);
            }
        }
        return answer;
    }

    public Integer getContainer() {
        return this.container;
    }

    public void setContainer(Integer container) {
        this.container = container;
        if (container != null && container > 0) {
            this.processProperty = getContainerProperties().get(0);
        }
    }

    public List<ProcessProperty> getContainerProperties() {
        List<ProcessProperty> answer = new ArrayList<>();

        if (this.container != null && this.container > 0) {
            for (ProcessProperty pp : this.processPropertyList) {
                if (pp.getContainer() == this.container && pp.getName() != null) {
                    answer.add(pp);
                }
            }
        } else {
            answer.add(this.processProperty);
        }

        return answer;
    }

    public String duplicateContainerForSingle() {
        Integer currentContainer = this.processProperty.getContainer();
        List<ProcessProperty> plist = new ArrayList<>();
        // search for all properties in container
        for (ProcessProperty pt : this.processPropertyList) {
            if (pt.getContainer() == currentContainer) {
                plist.add(pt);
            }
        }
        int newContainerNumber = 0;
        if (currentContainer > 0) {
            newContainerNumber++;
            // find new unused container number
            boolean search = true;
            while (search) {
                if (!this.containers.containsKey(newContainerNumber)) {
                    search = false;
                } else {
                    newContainerNumber++;
                }
            }
        }
        // clone properties
        for (ProcessProperty pt : plist) {
            ProcessProperty newProp = pt.getClone(newContainerNumber);
            this.processPropertyList.add(newProp);
            this.processProperty = newProp;
            saveCurrentProperty();
        }
        loadProcessProperties(this.currentStep);

        return "";
    }

    private void saveStep() {
        Process p = this.currentStep.getProzess();
        List<Processproperty> props = p.getEigenschaftenList();
        for (Processproperty pe : props) {
            if (pe.getTitel() == null) {
                p.getEigenschaften().remove(pe);
            }
        }
        try {
            ProcessManager.saveProcessInformation(this.currentStep.getProzess());
            StepManager.saveStep(currentStep);
        } catch (DAOException e) {
            logger.error(e);
        }
    }

    public String duplicateContainerForAll() {
        Integer currentContainer = this.processProperty.getContainer();
        List<ProcessProperty> plist = new ArrayList<>();
        // search for all properties in container
        for (ProcessProperty pt : this.processPropertyList) {
            if (pt.getContainer() == currentContainer) {
                plist.add(pt);
            }
        }

        int newContainerNumber = 0;
        if (currentContainer > 0) {
            newContainerNumber++;
            boolean search = true;
            while (search) {
                if (!this.containers.containsKey(newContainerNumber)) {
                    search = false;
                } else {
                    newContainerNumber++;
                }
            }
        }
        // clone properties
        for (ProcessProperty pt : plist) {
            ProcessProperty newProp = pt.getClone(newContainerNumber);
            this.processPropertyList.add(newProp);
            this.processProperty = newProp;
            saveCurrentPropertyForAll();
        }
        loadProcessProperties(this.currentStep);
        return "";
    }

    /*
     * Error management
     */

    public String ReportProblemForSingle() {

        this.myDav.UploadFromHome(this.currentStep.getProzess());
        reportProblem();
        saveStep();
        this.problemMessage = "";
        this.myProblemStep = "";
        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    public String ReportProblemForAll() {
        for (Step s : this.steps) {
            this.currentStep = s;
            this.myDav.UploadFromHome(this.currentStep.getProzess());
            reportProblem();
            saveStep();
        }
        this.problemMessage = "";
        this.myProblemStep = "";
        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    private void reportProblem() {
        Date myDate = new Date();
        this.currentStep.setBearbeitungsstatusEnum(StepStatus.LOCKED);
        this.currentStep.setEditTypeEnum(StepEditType.MANUAL_SINGLE);
        this.currentStep.setPrioritaet(Integer.valueOf(10));
        currentStep.setBearbeitungszeitpunkt(new Date());
        User ben = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
        if (ben != null) {
            currentStep.setBearbeitungsbenutzer(ben);
        }
        this.currentStep.setBearbeitungsbeginn(null);

        try {
            Step temp = null;
            for (Step s : this.currentStep.getProzess().getSchritteList()) {
                if (s.getTitel().equals(this.myProblemStep)) {
                    temp = s;
                }
            }
            if (temp != null) {
                temp.setBearbeitungsstatusEnum(StepStatus.ERROR);
                temp.setCorrectionStep();
                temp.setBearbeitungsende(null);
                ErrorProperty se = new ErrorProperty();

                se.setTitel(Helper.getTranslation("Korrektur notwendig"));
                se.setWert("[" + this.formatter.format(new Date()) + ", " + ben.getNachVorname() + "] " + this.problemMessage);
                se.setType(PropertyType.messageError);
                se.setCreationDate(myDate);
                se.setSchritt(temp);
                String message = Helper.getTranslation("KorrekturFuer") + " " + temp.getTitel() + ": " + this.problemMessage;
                LogEntry logEntry = new LogEntry();
                logEntry.setContent(message);
                logEntry.setCreationDate(new Date());
                logEntry.setProcessId(currentStep.getProzess().getId());
                logEntry.setType(LogType.ERROR);

                logEntry.setUserName(ben.getNachVorname());

                ProcessManager.saveLogEntry(logEntry);

                temp.getEigenschaften().add(se);
                StepManager.saveStep(temp);
                HistoryManager.addHistory(myDate, temp.getReihenfolge().doubleValue(), temp.getTitel(), HistoryEventType.stepError.getValue(), temp
                        .getProzess().getId());

                /*
                 * alle Schritte zwischen dem aktuellen und dem Korrekturschritt wieder schliessen
                 */

                List<Step> alleSchritteDazwischen = StepManager.getSteps("Reihenfolge desc", " schritte.prozesseID = " + currentStep.getProzess()
                .getId() + " AND Reihenfolge <= " + currentStep.getReihenfolge() + "  AND Reihenfolge > " + temp.getReihenfolge(), 0,
                Integer.MAX_VALUE);

                //              List<Step> alleSchritteDazwischen = Helper.getHibernateSession().createCriteria(Step.class)
                //                      .add(Restrictions.le("reihenfolge", this.currentStep.getReihenfolge()))
                //                      .add(Restrictions.gt("reihenfolge", temp.getReihenfolge())).addOrder(Order.asc("reihenfolge")).createCriteria("prozess")
                //                      .add(Restrictions.idEq(this.currentStep.getProzess().getId())).list();
                for (Iterator<Step> iter = alleSchritteDazwischen.iterator(); iter.hasNext();) {
                    Step step = iter.next();
                    if (!step.getBearbeitungsstatusEnum().equals(StepStatus.DEACTIVATED)){
                        step.setBearbeitungsstatusEnum(StepStatus.LOCKED);
                    }
                    step.setCorrectionStep();
                    step.setBearbeitungsende(null);
                    ErrorProperty seg = new ErrorProperty();
                    seg.setTitel(Helper.getTranslation("Korrektur notwendig"));
                    seg.setWert(Helper.getTranslation("KorrekturFuer") + " " + temp.getTitel() + ": " + this.problemMessage);
                    seg.setSchritt(step);
                    seg.setType(PropertyType.messageImportant);
                    seg.setCreationDate(new Date());
                    step.getEigenschaften().add(seg);
                }
            }

            /*
             * den Prozess aktualisieren, so dass der Sortierungshelper gespeichert wird
             */
            ProcessManager.saveProcessInformation(currentStep.getProzess());
        } catch (DAOException e) {
        }
    }

    public List<SelectItem> getPreviousStepsForProblemReporting() {
        List<SelectItem> answer = new ArrayList<>();
        List<Step> alleVorherigenSchritte = StepManager.getSteps("Reihenfolge desc", " schritte.prozesseID = " + this.currentStep.getProzess().getId()
                + " AND Reihenfolge < " + this.currentStep.getReihenfolge(), 0, Integer.MAX_VALUE);

        for (Step s : alleVorherigenSchritte) {
            answer.add(new SelectItem(s.getTitel(), s.getTitelMitBenutzername()));
        }
        return answer;
    }

    public int getSizeOfPreviousStepsForProblemReporting() {
        return getPreviousStepsForProblemReporting().size();
    }

    public List<SelectItem> getNextStepsForProblemSolution() {
        List<SelectItem> answer = new ArrayList<>();
        List<Step> alleNachfolgendenSchritte = StepManager.getSteps("Reihenfolge", " schritte.prozesseID = " + this.currentStep.getProzess().getId()
                + " AND Reihenfolge > " + this.currentStep.getReihenfolge() + " AND prioritaet = 10", 0, Integer.MAX_VALUE);

        for (Step s : alleNachfolgendenSchritte) {
            answer.add(new SelectItem(s.getTitel(), s.getTitelMitBenutzername()));
        }
        return answer;
    }

    public int getSizeOfNextStepsForProblemSolution() {
        return getNextStepsForProblemSolution().size();
    }

    public String SolveProblemForSingle() {
        solveProblem();
        saveStep();
        this.solutionMessage = "";
        this.mySolutionStep = "";

        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    public String SolveProblemForAll() {
        for (Step s : this.steps) {
            this.currentStep = s;
            solveProblem();
            saveStep();
        }
        this.solutionMessage = "";
        this.mySolutionStep = "";

        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    private void solveProblem() {
        Date now = new Date();
        this.myDav.UploadFromHome(this.currentStep.getProzess());
        this.currentStep.setBearbeitungsstatusEnum(StepStatus.DONE);
        this.currentStep.setBearbeitungsende(now);
        this.currentStep.setEditTypeEnum(StepEditType.MANUAL_SINGLE);
        currentStep.setBearbeitungszeitpunkt(new Date());
        User ben = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
        if (ben != null) {
            currentStep.setBearbeitungsbenutzer(ben);
        }

        try {
            Step temp = null;
            for (Step s : this.currentStep.getProzess().getSchritteList()) {
                if (s.getTitel().equals(this.mySolutionStep)) {
                    temp = s;
                }
            }
            if (temp != null) {
                /*
                 * alle Schritte zwischen dem aktuellen und dem Korrekturschritt wieder schliessen
                 */
                List<Step> alleSchritteDazwischen = StepManager.getSteps("Reihenfolge", " schritte.prozesseID = " + this.currentStep.getProzess()
                .getId() + " AND Reihenfolge >= " + this.currentStep.getReihenfolge() + "  AND Reihenfolge <= " + temp.getReihenfolge(), 0,
                Integer.MAX_VALUE);

                for (Iterator<Step> iter = alleSchritteDazwischen.iterator(); iter.hasNext();) {
                    Step step = iter.next();
                    if (!step.getBearbeitungsstatusEnum().equals(StepStatus.DEACTIVATED)){
                        step.setBearbeitungsstatusEnum(StepStatus.DONE);
                    }
                    step.setBearbeitungsende(now);
                    step.setPrioritaet(Integer.valueOf(0));
                    if (step.getId().intValue() == temp.getId().intValue()) {
                        step.setBearbeitungsstatusEnum(StepStatus.OPEN);
                        step.setCorrectionStep();
                        step.setBearbeitungsende(null);
                        step.setBearbeitungszeitpunkt(now);
                    }
                    ErrorProperty seg = new ErrorProperty();
                    seg.setTitel(Helper.getTranslation("Korrektur durchgefuehrt"));
                    seg.setWert("[" + this.formatter.format(new Date()) + ", " + ben.getNachVorname() + "] " + Helper.getTranslation(
                            "KorrekturloesungFuer") + " " + temp.getTitel() + ": " + this.solutionMessage);
                    seg.setSchritt(step);
                    seg.setType(PropertyType.messageImportant);
                    seg.setCreationDate(new Date());
                    step.getEigenschaften().add(seg);
                    StepManager.saveStep(step);
                }
            }
            String message = Helper.getTranslation("KorrekturloesungFuer") + " " + temp.getTitel() + ": " + this.solutionMessage;

            LogEntry logEntry = new LogEntry();
            logEntry.setContent(message);
            logEntry.setCreationDate(new Date());
            logEntry.setProcessId(currentStep.getProzess().getId());
            logEntry.setType(LogType.INFO);

            logEntry.setUserName(ben.getNachVorname());

            ProcessManager.saveLogEntry(logEntry);

            /*
             * den Prozess aktualisieren, so dass der Sortierungshelper gespeichert wird
             */
            ProcessManager.saveProcessInformation(currentStep.getProzess());
        } catch (DAOException e) {
        }
    }

    public String getProblemMessage() {
        return this.problemMessage;
    }

    public void setProblemMessage(String problemMessage) {
        this.problemMessage = problemMessage;
    }

    public String getMyProblemStep() {
        return this.myProblemStep;
    }

    public void setMyProblemStep(String myProblemStep) {
        this.myProblemStep = myProblemStep;
    }

    public String getSolutionMessage() {
        return this.solutionMessage;
    }

    public void setSolutionMessage(String solutionMessage) {
        this.solutionMessage = solutionMessage;
    }

    public String getMySolutionStep() {
        return this.mySolutionStep;
    }

    public void setMySolutionStep(String mySolutionStep) {
        this.mySolutionStep = mySolutionStep;
    }

    /**
     * sets new value for wiki field
     * 
     * @param inString
     */


    public void addLogEntry() {
        if (StringUtils.isNotBlank(content)) {
            User user = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
            LogEntry logEntry = new LogEntry();
            logEntry.setContent(content);
            logEntry.setSecondContent(secondContent);
            logEntry.setThirdContent(thirdContent);
            logEntry.setCreationDate(new Date());
            logEntry.setProcessId(currentStep.getProzess().getId());
            logEntry.setType(LogType.USER);
            logEntry.setUserName(user.getNachVorname());
            ProcessManager.saveLogEntry(logEntry);
            currentStep.getProzess().getProcessLog().add(logEntry);
            this.content = "";
            secondContent = "";
            thirdContent = "";
        }
    }

    public void addLogEntryForAll() {
        if (StringUtils.isNotBlank(content)) {
            User user = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
            for (Step s : this.steps) {
                LogEntry logEntry = new LogEntry();
                logEntry.setContent(content);
                logEntry.setSecondContent(secondContent);
                logEntry.setThirdContent(thirdContent);
                logEntry.setCreationDate(new Date());
                logEntry.setProcessId(s.getProzess().getId());
                logEntry.setType(LogType.USER);
                logEntry.setUserName(user.getNachVorname());
                s.getProzess().getProcessLog().add(logEntry);
                ProcessManager.saveLogEntry(logEntry);
            }
            this.content = "";
            secondContent = "";
            thirdContent = "";
        }
    }

    /*
     * actions
     */

    public String getScript() {
        return this.script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void executeScript() {
        for (Step step : this.steps) {

            if (step.getAllScripts().containsKey(this.script)) {
                Step so = StepManager.getStepById(step.getId());
                String scriptPath = step.getAllScripts().get(this.script);

                new HelperSchritte().executeScriptForStepObject(so, scriptPath, false);

            }
        }
    }

    public void ExportDMS() {
        for (Step step : this.steps) {
            IExportPlugin dms = null;
            if (StringUtils.isNotBlank(step.getStepPlugin())) {
                try {
                    dms = (IExportPlugin) PluginLoader.getPluginByTitle(PluginType.Export, step.getStepPlugin());
                } catch (Exception e) {
                    logger.error("Can't load export plugin, use default plugin", e);
                    dms = new ExportDms();
                }
            }
            if (dms == null) {
                dms = new ExportDms();
            }
            try {
                dms.startExport(step.getProzess());
            } catch (Exception e) {
                Helper.setFehlerMeldung("Error on export", e.getMessage());
                logger.error(e);
            }
        }
    }

    public String BatchDurchBenutzerZurueckgeben() {

        for (Step s : this.steps) {

            this.myDav.UploadFromHome(s.getProzess());
            s.setBearbeitungsstatusEnum(StepStatus.OPEN);
            if (s.isCorrectionStep()) {
                s.setBearbeitungsbeginn(null);
            }
            s.setEditTypeEnum(StepEditType.MANUAL_MULTI);
            currentStep.setBearbeitungszeitpunkt(new Date());
            User ben = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
            if (ben != null) {
                currentStep.setBearbeitungsbenutzer(ben);
            }

            try {
                //                ProcessManager.saveProcess(s.getProzess());
                StepManager.saveStep(s);
            } catch (DAOException e) {
            }
        }
        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    public String BatchDurchBenutzerAbschliessen() {

        // for (ProcessProperty pp : this.processPropertyList) {
        // this.processProperty = pp;
        // saveCurrentPropertyForAll();
        // }
        HelperSchritte helper = new HelperSchritte();
        for (Step s : this.steps) {
            boolean error = false;
            if (s.getValidationPlugin() != null && s.getValidationPlugin().length() > 0) {
                IValidatorPlugin ivp = (IValidatorPlugin) PluginLoader.getPluginByTitle(PluginType.Validation, s.getValidationPlugin());
                if (ivp != null) {
                    ivp.setStep(s);
                    if (!ivp.validate()) {
                        error = true;
                    }
                } else {
                    Helper.setFehlerMeldung("ErrorLoadingValidationPlugin");
                }
            }

            if (s.isTypImagesSchreiben()) {
                try {
                    HistoryAnalyserJob.updateHistory(s.getProzess());
                } catch (Exception e) {
                    Helper.setFehlerMeldung("Error while calculation of storage and images", e);
                }
            }

            if (s.isTypBeimAbschliessenVerifizieren()) {
                if (s.isTypMetadaten() && ConfigurationHelper.getInstance().isUseMetadataValidation()) {
                    MetadatenVerifizierung mv = new MetadatenVerifizierung();
                    mv.setAutoSave(true);
                    if (!mv.validate(s.getProzess())) {
                        error = true;
                    }
                }
                if (s.isTypImagesSchreiben()) {
                    MetadatenImagesHelper mih = new MetadatenImagesHelper(null, null);
                    try {
                        if (!mih.checkIfImagesValid(s.getProzess().getTitel(), s.getProzess().getImagesOrigDirectory(false))) {
                            error = true;
                        }
                    } catch (Exception e) {
                        Helper.setFehlerMeldung("Error on image validation: ", e);
                    }
                }

                loadProcessProperties(s);

                for (ProcessProperty prop : processPropertyList) {

                    if (prop.getCurrentStepAccessCondition().equals(AccessCondition.WRITEREQUIRED) && (prop.getValue() == null || prop.getValue()
                            .equals(""))) {
                        String[] parameter = { prop.getName(), s.getProzess().getTitel() };
                        Helper.setFehlerMeldung(Helper.getTranslation("BatchPropertyEmpty", parameter));
                        error = true;
                    } else if (!prop.isValid()) {
                        String[] parameter = { prop.getName(), s.getProzess().getTitel() };
                        Helper.setFehlerMeldung(Helper.getTranslation("BatchPropertyValidation", parameter));
                        error = true;
                    }
                }
            }
            if (!error) {
                this.myDav.UploadFromHome(s.getProzess());
                Step so = StepManager.getStepById(s.getId());
                so.setEditTypeEnum(StepEditType.MANUAL_MULTI);
                helper.CloseStepObjectAutomatic(so);
            }
        }
        StepBean asf = (StepBean) Helper.getManagedBeanValue("#{AktuelleSchritteForm}");
        return asf.FilterAlleStart();
    }

    public List<String> getScriptnames() {
        List<String> answer = new ArrayList<>();
        answer.addAll(getCurrentStep().getAllScripts().keySet());
        return answer;
    }

    public List<Integer> getContainerList() {
        return new ArrayList<>(this.containers.keySet());
    }

    // needed for junit
    public void setProcessPropertyList(List<ProcessProperty> processPropertyList) {
        this.processPropertyList = processPropertyList;
    }
}
