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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.goobi.beans.LogEntry;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Process;
//import de.sub.goobi.beans.Schritteigenschaft;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.Template;
import org.goobi.beans.Templateproperty;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.extern.log4j.Log4j;

@Log4j
public class BeanHelper {

    public void EigenschaftHinzufuegen(Process inProzess, String inTitel, String inWert) {
        Processproperty eig = new Processproperty();
        eig.setTitel(inTitel);
        eig.setWert(inWert);
        eig.setProzess(inProzess);
        List<Processproperty> eigenschaften = inProzess.getEigenschaften();
        if (eigenschaften == null) {
            eigenschaften = new ArrayList<>();
        }
        eigenschaften.add(eig);
    }

    //	public void EigenschaftHinzufuegen(Step inSchritt, String inTitel, String inWert) {
    //		Schritteigenschaft eig = new Schritteigenschaft();
    //		eig.setTitel(inTitel);
    //		eig.setWert(inWert);
    //		eig.setSchritt(inSchritt);
    //		List<Schritteigenschaft> eigenschaften = inSchritt.getEigenschaften();
    //		if (eigenschaften == null) {
    //			eigenschaften = new ArrayList<Schritteigenschaft>();
    //		}
    //		eigenschaften.add(eig);
    //	}

    public void EigenschaftHinzufuegen(Template inVorlage, String inTitel, String inWert) {
        Templateproperty eig = new Templateproperty();
        eig.setTitel(inTitel);
        eig.setWert(inWert);
        eig.setVorlage(inVorlage);
        List<Templateproperty> eigenschaften = inVorlage.getEigenschaften();
        if (eigenschaften == null) {
            eigenschaften = new ArrayList<>();
        }
        eigenschaften.add(eig);
    }

    public void EigenschaftHinzufuegen(Masterpiece inWerkstueck, String inTitel, String inWert) {
        Masterpieceproperty eig = new Masterpieceproperty();
        eig.setTitel(inTitel);
        eig.setWert(inWert);
        eig.setWerkstueck(inWerkstueck);
        List<Masterpieceproperty> eigenschaften = inWerkstueck.getEigenschaften();
        if (eigenschaften == null) {
            eigenschaften = new ArrayList<>();
        }
        eigenschaften.add(eig);
    }

    public void SchritteKopieren(Process prozessVorlage, Process prozessKopie) {
        List<Step> mySchritte = new ArrayList<>();
        for (Step step : prozessVorlage.getSchritteList()) {

            /* --------------------------------
             * Details des Schritts
             * --------------------------------*/
            Step stepneu = new Step();
            stepneu.setTypAutomatisch(step.isTypAutomatisch());
            stepneu.setScriptname1(step.getScriptname1());
            stepneu.setScriptname2(step.getScriptname2());
            stepneu.setScriptname3(step.getScriptname3());
            stepneu.setScriptname4(step.getScriptname4());
            stepneu.setScriptname5(step.getScriptname5());

            stepneu.setTypAutomatischScriptpfad(step.getTypAutomatischScriptpfad());
            stepneu.setTypAutomatischScriptpfad2(step.getTypAutomatischScriptpfad2());
            stepneu.setTypAutomatischScriptpfad3(step.getTypAutomatischScriptpfad3());
            stepneu.setTypAutomatischScriptpfad4(step.getTypAutomatischScriptpfad4());
            stepneu.setTypAutomatischScriptpfad5(step.getTypAutomatischScriptpfad5());
            stepneu.setBatchStep(step.getBatchStep());
            stepneu.setTypScriptStep(step.getTypScriptStep());
            stepneu.setTypBeimAnnehmenAbschliessen(step.isTypBeimAnnehmenAbschliessen());
            stepneu.setTypBeimAnnehmenModul(step.isTypBeimAnnehmenModul());
            stepneu.setTypBeimAnnehmenModulUndAbschliessen(step.isTypBeimAnnehmenModulUndAbschliessen());
            stepneu.setTypModulName(step.getTypModulName());
            stepneu.setTypExportDMS(step.isTypExportDMS());
            stepneu.setTypExportRus(step.isTypExportRus());
            stepneu.setTypImagesLesen(step.isTypImagesLesen());
            stepneu.setTypImagesSchreiben(step.isTypImagesSchreiben());
            stepneu.setTypImportFileUpload(step.isTypImportFileUpload());
            stepneu.setTypMetadaten(step.isTypMetadaten());
            stepneu.setPrioritaet(step.getPrioritaet());
            stepneu.setBearbeitungsstatusEnum(step.getBearbeitungsstatusEnum());
            stepneu.setReihenfolge(step.getReihenfolge());
            stepneu.setTitel(step.getTitel());
            stepneu.setHomeverzeichnisNutzen(step.getHomeverzeichnisNutzen());
            stepneu.setProzess(prozessKopie);

            stepneu.setStepPlugin(step.getStepPlugin());
            stepneu.setValidationPlugin(step.getValidationPlugin());
            stepneu.setDelayStep(step.isDelayStep());
            stepneu.setUpdateMetadataIndex(step.isUpdateMetadataIndex());

            stepneu.setTypBeimAbschliessenVerifizieren(step.isTypBeimAbschliessenVerifizieren());

            stepneu.setGenerateDocket(step.isGenerateDocket());

            stepneu.setHttpStep(step.isHttpStep());
            stepneu.setHttpUrl(step.getHttpUrl());
            stepneu.setHttpMethod(step.getHttpMethod());
            stepneu.setHttpJsonBody(step.getHttpJsonBody());
            stepneu.setHttpCloseStep(step.isHttpCloseStep());

            /* --------------------------------
             * Benutzer übernehmen
             * --------------------------------*/
            List<User> myBenutzer = new ArrayList<>();
            for (User benneu : step.getBenutzer()) {
                myBenutzer.add(benneu);
            }
            stepneu.setBenutzer(myBenutzer);

            /* --------------------------------
             * Benutzergruppen übernehmen
             * --------------------------------*/
            List<Usergroup> myBenutzergruppen = new ArrayList<>();
            for (Usergroup grupneu : step.getBenutzergruppen()) {
                myBenutzergruppen.add(grupneu);
            }
            stepneu.setBenutzergruppen(myBenutzergruppen);

            /* Schritt speichern */
            mySchritte.add(stepneu);
        }
        prozessKopie.setSchritte(mySchritte);
    }

    public void WerkstueckeKopieren(Process prozessVorlage, Process prozessKopie) {
        List<Masterpiece> myWerkstuecke = new ArrayList<>();
        for (Masterpiece werk : prozessVorlage.getWerkstuecke()) {
            /* --------------------------------
             * Details des Werkstücks
             * --------------------------------*/
            Masterpiece werkneu = new Masterpiece();
            werkneu.setProzess(prozessKopie);

            /* --------------------------------
             * Eigenschaften des Schritts
             * --------------------------------*/
            List<Masterpieceproperty> myEigenschaften = new ArrayList<>();
            for (Iterator<Masterpieceproperty> iterator = werk.getEigenschaften().iterator(); iterator.hasNext();) {
                Masterpieceproperty eig = iterator.next();
                Masterpieceproperty eigneu = new Masterpieceproperty();
                eigneu.setIstObligatorisch(eig.isIstObligatorisch());
                eigneu.setType(eig.getType());
                eigneu.setTitel(eig.getTitel());
                eigneu.setWert(eig.getWert());
                eigneu.setWerkstueck(werkneu);
                myEigenschaften.add(eigneu);
            }
            werkneu.setEigenschaften(myEigenschaften);

            /* Schritt speichern */
            myWerkstuecke.add(werkneu);
        }
        prozessKopie.setWerkstuecke(myWerkstuecke);
    }

    public void EigenschaftenKopieren(Process prozessVorlage, Process prozessKopie) {
        List<Processproperty> myEigenschaften = new ArrayList<>();
        for (Iterator<Processproperty> iterator = prozessVorlage.getEigenschaftenList().iterator(); iterator.hasNext();) {
            Processproperty eig = iterator.next();
            Processproperty eigneu = new Processproperty();
            eigneu.setIstObligatorisch(eig.isIstObligatorisch());
            eigneu.setType(eig.getType());
            eigneu.setTitel(eig.getTitel());
            eigneu.setWert(eig.getWert());
            eigneu.setProzess(prozessKopie);
            myEigenschaften.add(eigneu);
        }
        prozessKopie.setEigenschaften(myEigenschaften);
    }

    public void ScanvorlagenKopieren(Process prozessVorlage, Process prozessKopie) {
        List<Template> myVorlagen = new ArrayList<>();
        for (Template vor : prozessVorlage.getVorlagen()) {
            /* --------------------------------
             * Details der Vorlage
             * --------------------------------*/
            Template vorneu = new Template();
            vorneu.setHerkunft(vor.getHerkunft());
            vorneu.setProzess(prozessKopie);

            /* --------------------------------
             * Eigenschaften des Schritts
             * --------------------------------*/
            List<Templateproperty> myEigenschaften = new ArrayList<>();
            for (Iterator<Templateproperty> iterator = vor.getEigenschaften().iterator(); iterator.hasNext();) {
                Templateproperty eig = iterator.next();
                Templateproperty eigneu = new Templateproperty();
                eigneu.setIstObligatorisch(eig.isIstObligatorisch());
                eigneu.setType(eig.getType());
                eigneu.setTitel(eig.getTitel());
                eigneu.setWert(eig.getWert());
                eigneu.setVorlage(vorneu);
                myEigenschaften.add(eigneu);
            }
            vorneu.setEigenschaften(myEigenschaften);

            /* Schritt speichern */
            myVorlagen.add(vorneu);
        }
        prozessKopie.setVorlagen(myVorlagen);
    }

    public String WerkstueckEigenschaftErmitteln(Process myProzess, String inEigenschaft) {
        String Eigenschaft = "";
        for (Masterpiece myWerkstueck : myProzess.getWerkstueckeList()) {
            for (Masterpieceproperty eigenschaft : myWerkstueck.getEigenschaftenList()) {
                if (eigenschaft.getTitel().equals(inEigenschaft)) {
                    Eigenschaft = eigenschaft.getWert();
                }
            }
        }
        return Eigenschaft;
    }

    public String ScanvorlagenEigenschaftErmitteln(Process myProzess, String inEigenschaft) {
        String Eigenschaft = "";
        for (Template myVorlage : myProzess.getVorlagenList()) {
            for (Templateproperty eigenschaft : myVorlage.getEigenschaftenList()) {
                if (eigenschaft.getTitel().equals(inEigenschaft)) {
                    Eigenschaft = eigenschaft.getWert();
                }
            }
        }
        return Eigenschaft;
    }

    /**
     * Allow to change the process template after process generation. <br />
     * If a task exists in both templates, the old status remains.
     * 
     * metadata, images and all other data are not touched
     * 
     * @param processToChange process to change
     * @param template new process template
     * @return
     */

    public boolean changeProcessTemplate(Process processToChange, Process template) {
        List<Step> oldTaskList = new ArrayList<>(processToChange.getSchritte());

        // remove tasks from process
        processToChange.setSchritte(new ArrayList<Step>());
        // copy tasks from template to process
        SchritteKopieren(template, processToChange);

        // set task progress
        for (Step newTask : processToChange.getSchritte()) {
            for (Step oldTask : oldTaskList) {
                if (oldTask.getTitel().equals(newTask.getTitel()) && oldTask.getBearbeitungsstatusEnum().equals(StepStatus.DONE)) {
                    // if oldTask is finished, keep status, date, user in new task
                    newTask.setBearbeitungsbeginn(oldTask.getBearbeitungsbeginn());
                    newTask.setBearbeitungsende(oldTask.getBearbeitungsende());
                    newTask.setBearbeitungsstatusEnum(oldTask.getBearbeitungsstatusEnum());
                    newTask.setBearbeitungsbenutzer(oldTask.getBearbeitungsbenutzer());
                    break;
                }
            }
        }

        // remove old tasks from database
        for (Step oldTask : oldTaskList) {
            StepManager.deleteStep(oldTask);
        }
        // update properties for template name + id
        for (Processproperty property : processToChange.getEigenschaften()) {
            if (property.getTitel().equals("Template")) {
                property.setWert(template.getTitel());
            } else if (property.getTitel().equals("TemplateID")) {
                property.setWert(String.valueOf(template.getId()));
            }
        }

        // add text to process log

        LogEntry logEntry = new LogEntry();
        logEntry.setContent("Changed process template to " + template.getTitel());
        logEntry.setCreationDate(new Date());
        logEntry.setProcessId(processToChange.getId());
        logEntry.setType(LogType.DEBUG);
        logEntry.setUserName(Helper.getCurrentUser().getNachVorname());
        processToChange.getProcessLog().add(logEntry);

        try {
            // if no open task was found, open first locked  task
            for (Step newTask : processToChange.getSchritte()) {
                if (newTask.getBearbeitungsstatusEnum().equals(StepStatus.OPEN)) {
                    break;
                } else if (newTask.getBearbeitungsstatusEnum().equals(StepStatus.LOCKED)) {
                    newTask.setBearbeitungsstatusEnum(StepStatus.OPEN);
                    break;
                }
            }
            // TODO what happens if task is automatic?

            // save new tasks
            ProcessManager.saveProcess(processToChange);
        } catch (DAOException e) {
            log.error(e);
            return false;
        }
        return true;
    }
}
