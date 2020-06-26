package de.sub.goobi.metadaten;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi-workflow
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

import ugh.dl.DocStruct;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.TreeNode;

public class TreeNodeStruct3 extends TreeNode {

    private DocStruct struct;
    private String firstImage;
    private String lastImage;
    private String zblNummer;
    private String mainTitle;
    private String ppnDigital;
    private String identifier;
    private String zblSeiten;
    private String partNumber;
    private String dateIssued;

    private boolean einfuegenErlaubt = true;

    /**
     * Konstruktoren
     */
    public TreeNodeStruct3() {
    }

    /* =============================================================== */

    public TreeNodeStruct3(boolean expanded, String label, String id) {
        this.expanded = expanded;
        this.label = label;
        this.id = id;
        this.children = new ArrayList<TreeNode>();
    }

    /* =============================================================== */

    public TreeNodeStruct3(String label, DocStruct struct) {
        this.label = label;
        this.struct = struct;
    }

    /* =============================================================== */

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMainTitle() {

        int maxSize = ConfigurationHelper.getInstance().getMetsEditorMaxTitleLength();
        if (maxSize > 0 && this.mainTitle != null && this.mainTitle.length() > maxSize) {
            return this.mainTitle.substring(0, maxSize - 1);
        }

        return this.mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getPpnDigital() {
        return this.ppnDigital;
    }

    public void setPpnDigital(String ppnDigital) {
        this.ppnDigital = ppnDigital;
    }

    public String getFirstImage() {
        return this.firstImage;
    }

    public void setFirstImage(String firstImage) {
        this.firstImage = firstImage;
    }

    public String getLastImage() {
        return this.lastImage;
    }

    public void setLastImage(String lastImage) {
        this.lastImage = lastImage;
    }

    public DocStruct getStruct() {
        return this.struct;
    }

    public void setStruct(DocStruct struct) {
        this.struct = struct;
    }

    public String getZblNummer() {
        return this.zblNummer;
    }

    public void setZblNummer(String zblNummer) {
        this.zblNummer = zblNummer;
    }

    public String getDescription() {
        return this.label;
    }

    public void setDescription(String description) {
        this.label = description;
    }

    public boolean isEinfuegenErlaubt() {
        return this.einfuegenErlaubt;
    }

    public void setEinfuegenErlaubt(boolean einfuegenErlaubt) {
        this.einfuegenErlaubt = einfuegenErlaubt;
    }

    public String getZblSeiten() {
        return this.zblSeiten;
    }

    public void setZblSeiten(String zblSeiten) {
        this.zblSeiten = zblSeiten;
    }

    public String getDateIssued() {
        return dateIssued;
    }

    public void setDateIssued(String dateIssued) {
        this.dateIssued = dateIssued;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getMetadataPopup() {
        StringBuilder answer = new StringBuilder();
        answer.append("<dl>");

        if (!mainTitle.isEmpty()) {
            answer.append("<dt>Maintitle:</dt><dd>" + mainTitle + "</dd>");
        }

        if (!firstImage.isEmpty()) {
            answer.append("<dt>Startimage:</dt><dd>" + firstImage + "</dd>");
        }

        if (!zblSeiten.isEmpty()) {
            answer.append("<dt>ZBL-Seiten:</dt><dd>" + zblSeiten + "</dd>");
        }

        if (!zblNummer.isEmpty()) {
            answer.append("<dt>ZBL-ID:</dt><dd>" + zblNummer + "</dd>");
        }

        if (!ppnDigital.isEmpty()) {
            answer.append("<dt>PPN-Digital:</dt><dd>" + ppnDigital + "</dd>");
        }

        if (!dateIssued.isEmpty()) {
            answer.append("<dt>DateIssued:</dt><dd>" + dateIssued + "</dd>");
        }

        if (!partNumber.isEmpty()) {
            answer.append("<dt>PartNumber:</dt><dd>" + partNumber + "</dd>");
        }

        answer.append("</dl>");
        return answer.toString();
    }

}
