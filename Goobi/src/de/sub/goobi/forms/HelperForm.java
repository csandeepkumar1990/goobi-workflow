package de.sub.goobi.forms;

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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.goobi.beans.Docket;
import org.goobi.beans.Ruleset;
import org.goobi.beans.Step;
import org.goobi.production.GoobiVersion;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;
import org.reflections.Reflections;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.DocketManager;
import de.sub.goobi.persistence.managers.RulesetManager;
import de.sub.goobi.persistence.managers.StepManager;
import ugh.dl.Fileformat;

@ManagedBean(name = "HelperForm")
@SessionScoped
public class HelperForm {

    private Boolean massImportAllowed = null;

    private boolean showError = false;

    // TODO re-added temporary for compiling issues
    public static final String MAIN_JSF_PATH = "/newpages";

    public String getBuildVersion() {
        return GoobiVersion.getBuildversion();
    }

    public String getVersion() {
        return "3.0";
    }

    // TODO: Change the defaults
    public String getApplicationHeaderTitle() {

        String rueck = ConfigurationHelper.getInstance().getApplicationHeaderTitle();
        return rueck;
    }

    public String getApplicationTitle() {
        String rueck = ConfigurationHelper.getInstance().getApplicationTitle();
        return rueck;
    }

    public String getApplicationWebsiteUrl() {
        return getServletPathAsUrl();
    }

    public String getApplicationWebsiteMsg() {
        String rueck = ConfigurationHelper.getInstance().getApplicationWebsiteMsg();
        return Helper.getTranslation(rueck);
    }

    public String getApplicationHomepageMsg() {
        String rueck = ConfigurationHelper.getInstance().getApplicationHomepageMsg();
        return Helper.getTranslation(rueck);
    }

    public boolean getAnonymized() {
        return ConfigurationHelper.getInstance().isAnonymizeData();
    }

    public List<SelectItem> getRegelsaetze() throws DAOException {
        List<SelectItem> myPrefs = new ArrayList<SelectItem>();
        List<Ruleset> temp = RulesetManager.getRulesets("titel", null, null, null);
        for (Iterator<Ruleset> iter = temp.iterator(); iter.hasNext();) {
            Ruleset an = iter.next();
            myPrefs.add(new SelectItem(an, an.getTitel(), null));
        }
        return myPrefs;
    }

    public List<SelectItem> getDockets() {
        List<SelectItem> answer = new ArrayList<SelectItem>();
        try {
            List<Docket> temp = DocketManager.getDockets("name", null, null, null);
            for (Docket d : temp) {
                answer.add(new SelectItem(d, d.getName(), null));
            }
        } catch (DAOException e) {

        }

        return answer;
    }

    public List<SelectItem> getFileFormats() {
        ArrayList<SelectItem> ffs = new ArrayList<SelectItem>();

        Set<Class<? extends Fileformat>> formatSet = new Reflections("ugh.fileformats.*").getSubTypesOf(Fileformat.class);
        for (Class<? extends Fileformat> cl : formatSet) {
            try {
                Fileformat ff = cl.newInstance();
                if (ff.isExportable()) {
                    ffs.add(new SelectItem(ff.getDisplayName(), null));
                }
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }
        return ffs;
    }

    public List<SelectItem> getFileFormatsInternalOnly() {
        ArrayList<SelectItem> ffs = new ArrayList<SelectItem>();

        Set<Class<? extends Fileformat>> formatSet = new Reflections("ugh.fileformats.*").getSubTypesOf(Fileformat.class);
        for (Class<? extends Fileformat> cl : formatSet) {
            try {
                Fileformat ff = cl.newInstance();
                if (ff.isWritable()) {
                    ffs.add(new SelectItem(ff.getDisplayName(), null));
                }
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }

        }
        return ffs;
    }

    public List<SelectItem> getStepStatusList() {
        List<SelectItem> ssl = new ArrayList<SelectItem>();

        SelectItem locked = new SelectItem("0", Helper.getTranslation("statusGesperrt"));
        ssl.add(locked);

        SelectItem open = new SelectItem("1", Helper.getTranslation("statusOffen"));
        ssl.add(open);

        SelectItem inWork = new SelectItem("2", Helper.getTranslation("statusInBearbeitung"));
        ssl.add(inWork);

        SelectItem finished = new SelectItem("3", Helper.getTranslation("statusAbgeschlossen"));
        ssl.add(finished);

        SelectItem error = new SelectItem("4", Helper.getTranslation("statusError"));
        ssl.add(error);

        SelectItem deactivated = new SelectItem("5", Helper.getTranslation("statusDeactivated"));
        ssl.add(deactivated);

        return ssl;
    }

    public List<SelectItem> getStepPriorityList() {
        List<SelectItem> ssl = new ArrayList<SelectItem>();
        SelectItem s1 = new SelectItem("0", Helper.getTranslation("normalePrioritaet"));
        ssl.add(s1);
        SelectItem s2 = new SelectItem("1", Helper.getTranslation("badgePriority1"));
        ssl.add(s2);
        SelectItem s3 = new SelectItem("2", Helper.getTranslation("badgePriority2"));
        ssl.add(s3);
        SelectItem s4 = new SelectItem("3", Helper.getTranslation("badgePriority3"));
        ssl.add(s4);
        SelectItem s5 = new SelectItem("10", Helper.getTranslation("badgeCorrection"));
        ssl.add(s5);
        return ssl;
    }

    public String getServletPathAsUrl() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        ExternalContext external = context.getExternalContext();
        return external.getRequestContextPath() + "/";
    }

    public String getItmPathAsUrl() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80    
        String reqUrl = scheme + "://" + serverName + ":" + serverPort + "/itm/";
        return reqUrl;
    }

    public String getServletPathWithHostAsUrl() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80
        String contextPath = request.getContextPath(); // /mywebapp
        String reqUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
        return reqUrl;
    }

    public String getContextPath() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String contextPath = request.getContextPath();
        return contextPath;
    }

    public boolean getMessagesExist() {
        return FacesContextHelper.getCurrentFacesContext().getMessages().hasNext();
    }

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public boolean getMassImportAllowed() {
        if (massImportAllowed == null) {
            if (ConfigurationHelper.getInstance().isMassImportAllowed()) {

                massImportAllowed = !PluginLoader.getPluginList(PluginType.Import).isEmpty();
            } else {
                massImportAllowed = false;
            }
        }
        return massImportAllowed;
    }

    public boolean getIsIE() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        if (request.getHeader("User-Agent").contains("MSIE")) {
            return true;
        } else {
            return false;
        }
    }

    public String getUserAgent() {
        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        return request.getHeader("User-Agent");
    }

    public boolean isLdapIsWritable() {
        return ConfigurationHelper.getInstance().isUseLdap() && !ConfigurationHelper.getInstance().isLdapReadOnly();
    }

    public boolean isPasswordIsChangable() {
        return !ConfigurationHelper.getInstance().isLdapReadOnly();
    }

    public boolean isShowError() {
        return showError;
    }

    public boolean isUseUii() {
        return ConfigurationHelper.getInstance().isUseIntrandaUi();
    }

    public void setShowError(boolean showError) {
        this.showError = showError;
    }

    public void executeScriptsForStep(int id) {
        HelperSchritte hs = new HelperSchritte();
        Step s = StepManager.getStepById(id);
        hs.executeAllScriptsForStep(s, false);
    }

    public void executeHttpCallForStep(int id) {
        HelperSchritte hs = new HelperSchritte();
        Step s = StepManager.getStepById(id);
        s.setHttpCloseStep(false);
        hs.runHttpStep(s);
    }

    public List<SelectItem> getPossibleShortcuts() {
        List<SelectItem> ret = new ArrayList<>();
        ret.add(new SelectItem("ctrl", Helper.getTranslation("mets_key_ctrl")));
        ret.add(new SelectItem("alt", Helper.getTranslation("mets_key_alt")));
        ret.add(new SelectItem("ctrl+shift", Helper.getTranslation("mets_key_ctrlShift")));
        ret.add(new SelectItem("alt+shift", Helper.getTranslation("mets_key_altShift")));
        ret.add(new SelectItem("ctrl+alt", Helper.getTranslation("mets_key_ctrlAlt")));
        return ret;
    }

    /**
     * 
     * @return build date written by the ant scrip
     */

    public String getBuildDate() {
        return GoobiVersion.getBuilddate();
    }
}
