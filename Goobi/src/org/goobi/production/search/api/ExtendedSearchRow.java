package org.goobi.production.search.api;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - https://goobi.io
 *          - https://www.intranda.com 
 *          - https://github.com/intranda/goobi
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
 */
import org.goobi.production.flow.statistics.hibernate.FilterString;

import de.sub.goobi.helper.Helper;

public class ExtendedSearchRow {

    private String fieldName;

    private String fieldOperand;

    // process title, id, batch
    private String fieldValue;

    // step
    private String stepStatus;

    private String stepName;

    // project
    private String projectName;

    // properties
    private String processPropertyName;

    private String processPropertyValue;

    private String templatePropertyName;

    private String templatePropertyValue;

    private String masterpiecePropertyName;

    private String masterpiecePropertyValue;

    private String metadataName;

    private String metadataValue;

    public String createSearchString() {
        String value = "";
        if (fieldName.equals("PROCESSTITLE") && !fieldValue.isEmpty()) {
            value = "\"" + this.fieldOperand + this.fieldValue + "\" ";
        } else if (fieldName.equals("PROCESSID") && !fieldValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.ID + this.fieldValue + "\" ";
        }

        else if (fieldName.equals("BATCH") && !fieldValue.isEmpty()) {
            value = "\"" + FilterString.BATCH + this.fieldValue + "\" ";
        }

        else if (fieldName.equals("PROJECT") && !this.projectName.equals(Helper.getTranslation("notSelected"))) {
            value = "\"" + this.fieldOperand + FilterString.PROJECT + this.projectName + "\" ";

        } else if (fieldName.equals("METADATA") && !this.metadataName.equals(Helper.getTranslation("notSelected")) && !metadataValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.METADATA + metadataName + ":" + metadataValue + "\" ";

        }

        else if (fieldName.equals("PROCESSPROPERTY") && !processPropertyName.equals(Helper.getTranslation("notSelected"))
                && !processPropertyValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.PROCESSPROPERTY + this.processPropertyName + ":" + processPropertyValue + "\" ";
        }

        else if (fieldName.equals("WORKPIECE") && !masterpiecePropertyName.equals(Helper.getTranslation("notSelected"))
                && !masterpiecePropertyValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.WORKPIECE + this.masterpiecePropertyName + ":" + masterpiecePropertyValue + "\" ";
        }

        else if (fieldName.equals("TEMPLATE") && !templatePropertyName.equals(Helper.getTranslation("notSelected"))
                && !templatePropertyValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.TEMPLATE + this.templatePropertyName + ":" + templatePropertyValue + "\" ";
        }

        else if (fieldName.equals("STEP") && !stepStatus.equals(Helper.getTranslation("notSelected")) && !stepName.isEmpty()
                && !stepName.equals(Helper.getTranslation("notSelected"))) {
            value = "\"" + this.fieldOperand + this.stepStatus + ":" + this.stepName + "\" ";
        }

        else if (fieldName.equals("PROCESSLOG") && !fieldValue.isEmpty()) {
            value = "\"" + this.fieldOperand + FilterString.PROCESSLOG + fieldValue + "\" ";
        }

        return value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldOperand() {
        return fieldOperand;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public String getStepStatus() {
        return stepStatus;
    }

    public String getStepName() {
        return stepName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProcessPropertyName() {
        return processPropertyName;
    }

    public String getProcessPropertyValue() {
        return processPropertyValue;
    }

    public String getTemplatePropertyName() {
        return templatePropertyName;
    }

    public String getTemplatePropertyValue() {
        return templatePropertyValue;
    }

    public String getMasterpiecePropertyName() {
        return masterpiecePropertyName;
    }

    public String getMasterpiecePropertyValue() {
        return masterpiecePropertyValue;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFieldOperand(String fieldOperand) {
        this.fieldOperand = fieldOperand;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public void setStepStatus(String stepStatus) {
        this.stepStatus = stepStatus;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProcessPropertyName(String processPropertyName) {
        this.processPropertyName = processPropertyName;
    }

    public void setProcessPropertyValue(String processPropertyValue) {
        this.processPropertyValue = processPropertyValue;
    }

    public void setTemplatePropertyName(String templatePropertyName) {
        this.templatePropertyName = templatePropertyName;
    }

    public void setTemplatePropertyValue(String templatePropertyValue) {
        this.templatePropertyValue = templatePropertyValue;
    }

    public void setMasterpiecePropertyName(String masterpiecePropertyName) {
        this.masterpiecePropertyName = masterpiecePropertyName;
    }

    public void setMasterpiecePropertyValue(String masterpiecePropertyValue) {
        this.masterpiecePropertyValue = masterpiecePropertyValue;
    }

    public void setMetadataName(String metadataName) {
        this.metadataName = metadataName;
    }

    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }
}
