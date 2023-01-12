/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi-workflow
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
package org.goobi.managedbeans;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.vocabulary.Definition;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;
import org.goobi.vocabulary.VocabularyFieldValidator;
import org.goobi.vocabulary.VocabularyUploader;
import org.goobi.vocabulary.helper.ImportJsonVocabulary;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Named
@WindowScoped
@Log4j2
public class VocabularyBean extends BasicBean implements Serializable {

    private static final long serialVersionUID = -4591427229251805665L;

    @Getter
    @Setter
    private Vocabulary currentVocabulary;

    @Getter
    @Setter
    private Definition currentDefinition;

    @Getter
    private VocabRecord currentVocabRecord;

    //details up or down
    @Getter
    @Setter
    private String uiStatus;

    @Getter
    private String[] possibleDefinitionTypes = { "input", "textarea", "select", "select1", "html" };

    @Getter
    @Setter
    private transient Path importFile;
    @Getter
    private String filename;

    @Getter
    private List<MatchingField> headerOrder;

    @Getter
    @Setter
    private MatchingField selectedMatchingField;

    @Getter
    private List<SelectItem> allDefinitionNames;

    private transient List<Row> rowsToImport;

    @Getter
    @Setter
    private String importType = "merge";

    private List<Definition> removedDefinitions = null;
    private transient DataFormatter dataFormatter = new DataFormatter();

    /**
     * Constructor for class
     */
    public VocabularyBean() {
        uiStatus = "down";
        sortierung = "title";
    }

    /**
     * main method to start searching the records
     * 
     * @return path to list records
     */
    public String FilterKein() {
        VocabularyManager vm = new VocabularyManager();
        paginator = new DatabasePaginator(sortierung, filter, vm, "vocabulary_all");
        return "vocabulary_all";
    }

    /**
     * method to go to the vocabulary edition area
     * 
     * @return path to vocabulary edition area
     */
    public String editVocabulary() {
        removedDefinitions = new ArrayList<>();
        return "vocabulary_edit";
    }

    /**
     * method to start editing the records
     * 
     * @return path to list and edit records
     */
    public String editRecords() {
        // load records of selected vocabulary
        // initial first page
        VocabularyManager.getAllRecords(currentVocabulary);
        currentVocabulary.runFilter();
        currentVocabulary.setTotalNumberOfRecords(currentVocabulary.getRecords().size());
        if (!currentVocabulary.getRecords().isEmpty()) {
            currentVocabRecord = currentVocabulary.getRecords().get(0);
        } else {
            addRecord();
        }
        return "vocabulary_records";
    }

    public String uploadToServerRecords() {
        VocabularyManager.getAllRecords(currentVocabulary);
        Boolean boOK = VocabularyUploader.upload(currentVocabulary);
        if (Boolean.TRUE.equals(boOK)) {
            Helper.setMeldung(Helper.getTranslation("ExportFinished"));
            return "vocabulary_all";
        } else {
            Helper.setFehlerMeldung(Helper.getTranslation("ExportError"));
            return "";
        }
    }

    /**
     * start the edition of a new vocabulary
     * 
     * @return path to vocabulary edition area
     */
    public String newVocabulary() {
        currentVocabulary = new Vocabulary();
        return editVocabulary();
    }

    /**
     * method to save the vocabulary definitions
     * 
     * @return path to the vocabulary listing
     */
    public String saveVocabulary() {
        int numberOfMainEntries = 0;
        for (Definition def : currentVocabulary.getStruct()) {
            if (def.isMainEntry()) {
                numberOfMainEntries++;
            }
        }

        // check if one field is marked as main entry
        if (numberOfMainEntries == 0) {
            Helper.setFehlerMeldung(Helper.getTranslation("vocabularyManager_noMainEntry"));
            return "";
        } else if (numberOfMainEntries > 1) {
            Helper.setFehlerMeldung(Helper.getTranslation("vocabularyManager_wrongNumberOfMainEntries"));
            return "";
        }
        // check if title is unique
        if (VocabularyManager.isTitleUnique(currentVocabulary)) {
            VocabularyManager.saveVocabulary(currentVocabulary);
        } else {
            Helper.setFehlerMeldung(Helper.getTranslation("vocabularyManager_titleNotUnique"));
            return "";
        }
        for (Definition def : removedDefinitions) {
            VocabularyManager.deleteDefinition(def);
        }

        return cancelEdition();
    }

    /**
     * method to to delete an existing vocabulary
     * 
     * @return path to the vocabulary listing
     */
    public String deleteVocabulary() {
        if (currentVocabulary.getId() != null) {
            VocabularyManager.deleteVocabulary(currentVocabulary);
        }
        return cancelEdition();
    }

    /**
     * some cleanup and then go to overview page again
     * 
     * @return path to vocabulary listing
     */
    public String cancelEdition() {
        if (removedDefinitions != null) {
            removedDefinitions.clear();
        }
        return FilterKein();
    }

    public void deleteDefinition() {
        if (currentDefinition != null && currentVocabulary != null) {
            currentVocabulary.getStruct().remove(currentDefinition);
            removedDefinitions.add(currentDefinition);
        }
    }

    public void addDefinition() {
        currentVocabulary.getStruct().add(new Definition());
    }

    public void addRecord() {
        VocabRecord rec = new VocabRecord();
        List<Field> fieldList = new ArrayList<>();
        for (Definition definition : currentVocabulary.getStruct()) {
            Field field = new Field(definition.getLabel(), definition.getLanguage(), "", definition);
            fieldList.add(field);
        }
        rec.setFields(fieldList);
        currentVocabulary.getRecords().add(rec);
        currentVocabRecord = rec;
    }

    public void deleteRecord() {
        currentVocabulary.getRecords().remove(currentVocabRecord);
        VocabularyManager.deleteRecord(currentVocabRecord);
        editRecords();
    }

    public String cancelRecordEdition() {
        return cancelEdition();
    }

    /**
     * Save the current records. First it gets validated, if all required fields are filled and if the unique fields are unique. If this is not the
     * case, the records and fields are marked for the user and the saving is aborted. Otherwise the records get saved
     * 
     * @return
     */
    public void saveRecordEdition() {
        System.out.println("Saving...");
        // If the new content is not valid, the vocabulary should not be saved.
        if (!VocabularyFieldValidator.validateRecords(this.currentVocabulary, this.currentVocabRecord)) {
            return;
        }

        VocabularyManager.saveRecord(this.currentVocabulary.getId(), this.currentVocabRecord);

        // The current vocabulary must be stored by its id because editRecords() reloads all vocabulary and initializes all entries with new objects.
        // The old currentVocabRecord is not contained in the new list and would have no effect during the selection.
        int id = this.currentVocabRecord.getId();
        this.editRecords();
        this.setCurrentVocabRecord(this.getVocabRecordById(id));
    }

    /**
     * probably unneeded reload method to stay on the same page
     */
    public void Reload() {

    }

    /**
     * create an excel result and send it to the response output stream
     */
    public void downloadRecords() {
        VocabularyManager.getAllRecords(currentVocabulary);
        String title = currentVocabulary.getTitle();
        String description = currentVocabulary.getDescription();
        List<Definition> definitionList = currentVocabulary.getStruct();
        List<VocabRecord> recordList = currentVocabulary.getRecords();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet((StringUtils.isBlank(description) ? title : title + " - " + description).replace("/", ""));

        // create header
        Row headerRow = sheet.createRow(0);
        int columnCounter = 0;
        for (Definition definition : definitionList) {
            headerRow.createCell(columnCounter)
                    .setCellValue(StringUtils.isNotBlank(definition.getLanguage()) ? definition.getLabel() + " (" + definition.getLanguage() + ")"
                            : definition.getLabel());
            columnCounter = columnCounter + 1;
        }

        int rowCounter = 1;
        // add records
        for (VocabRecord record : recordList) {
            Row resultRow = sheet.createRow(rowCounter);
            columnCounter = 0;
            for (Definition definition : definitionList) {
                resultRow.createCell(columnCounter).setCellValue(record.getFieldValue(definition));
                columnCounter = columnCounter + 1;
            }
            rowCounter = rowCounter + 1;
        }

        // write result into output stream
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();

        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        OutputStream out;
        try {
            out = response.getOutputStream();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + title + ".xlsx\"");
            wb.write(out);
            out.flush();

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
        try {
            wb.close();
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * allow file upload for vocabulary records
     * 
     * @param event
     */
    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile upload = event.getFile();
            copyFile(upload.getFileName(), upload.getInputStream());
            loadUploadedFile();
        } catch (IOException e) {
            log.error("Error while uploading files", e);
        }
    }

    /**
     * internal method to manage the file upload for vocabulary records
     */
    private void loadUploadedFile() {
        InputStream file = null;

        if (importFile.getFileName().toString().endsWith(".json")) {

            List<VocabRecord> records = ImportJsonVocabulary.convertJsonVocabulary(currentVocabulary, importFile);
            VocabularyManager.saveVocabulary(currentVocabulary);
            VocabularyManager.insertNewRecords(records, currentVocabulary.getId());

            Helper.setMeldung("Imported records: " + records.size());

        } else {

            try {
                file = new FileInputStream(importFile.toFile());
                log.debug("Importing file {}", importFile.toString());
                BOMInputStream in = new BOMInputStream(file, false);
                try (Workbook wb = WorkbookFactory.create(in)) {
                    Sheet sheet = wb.getSheetAt(0);
                    Iterator<Row> rowIterator = sheet.rowIterator();
                    Row headerRow = rowIterator.next();
                    int numberOfCells = headerRow.getLastCellNum();
                    headerOrder = new ArrayList<>(numberOfCells);
                    log.debug("Found {} cell(s)", numberOfCells);

                    rowsToImport = new LinkedList<>();
                    for (int i = 0; i < numberOfCells; i++) {
                        Cell cell = headerRow.getCell(i);
                        if (cell != null) {
                            String value = dataFormatter.formatCellValue(cell).trim();
                            headerOrder.add(new MatchingField(value, i, CellReference.convertNumToColString(i), this));
                        }
                    }
                    log.debug("read header");
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        rowsToImport.add(row);
                    }
                    log.debug("Found {} rows to import", rowsToImport.size());
                    for (MatchingField mf : headerOrder) {
                        String excelTitle = mf.getColumnHeader();
                        if (excelTitle.matches(".*\\(.{3}\\)")) {
                            String titlePart = excelTitle.substring(0, excelTitle.lastIndexOf("(")).trim();
                            String languagePart = excelTitle.substring(excelTitle.lastIndexOf("(") + 1, excelTitle.lastIndexOf(")")).trim();
                            for (Definition def : currentVocabulary.getStruct()) {
                                if (def.getLabel().equals(titlePart) && def.getLanguage().equals(languagePart)) {
                                    mf.setAssignedField(def);
                                }
                            }
                        } else {
                            String titlePart = excelTitle.trim();
                            for (Definition def : currentVocabulary.getStruct()) {
                                if (def.getLabel().equals(titlePart) && StringUtils.isBlank(def.getLanguage())) {
                                    mf.setAssignedField(def);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Helper.setFehlerMeldung("file not readable", e);
                log.error(e);
            } finally {
                if (file != null) {
                    try {
                        file.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
    }

    public void copyFile(String fileName, InputStream in) {
        OutputStream out = null;
        try {
            String extension = fileName.substring(fileName.indexOf("."));
            importFile = Files.createTempFile(fileName, extension); // NOSONAR, temp file is save to use
            out = new FileOutputStream(importFile.toFile());
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * navigate to the excel upload area
     * 
     * @return path to the excel upload area
     */
    public String uploadRecords() {
        VocabularyManager.getAllRecords(currentVocabulary);
        allDefinitionNames = new ArrayList<>();
        allDefinitionNames.add(new SelectItem("", "-"));
        for (Definition definition : currentVocabulary.getStruct()) {
            String definitionName;
            if (StringUtils.isNotBlank(definition.getLanguage())) {
                definitionName = definition.getLabel() + " (" + definition.getLanguage() + ")";
            } else {
                definitionName = definition.getLabel();
            }
            allDefinitionNames.add(new SelectItem(definitionName, definitionName));
        }

        headerOrder = null;
        filename = null;
        importFile = null;
        return "vocabulary_upload";
    }

    /**
     * Checks if the assigned field is used in a different {@link MatchingField}. If this is the case, the other assignment is removed
     * 
     * @param currentField
     */
    private void updateFieldList(MatchingField currentField) {
        for (MatchingField other : headerOrder) {
            if (!other.equals(currentField) && other.getAssignedField() != null) {
                if (other.getCurrentDefinition().equals(currentField.getCurrentDefinition())) {
                    other.setAssignedField(null);
                }
            }
        }
    }

    /**
     * Import the records from the excel file. First all old records are deleted, then for each row a new record is created. The fields are filled
     * based on the configured {@link MatchingField}s
     * 
     * @return
     */
    public String importRecords() {
        if (importType.equals("remove")) {
            // if selected, remove existing entries of this vocabulary
            VocabularyManager.deleteAllRecords(currentVocabulary);
            currentVocabulary.setRecords(new ArrayList<>());
        }
        if (importType.equals("remove") || importType.equals("add")) {
            List<VocabRecord> recordsToAdd = new ArrayList<>(rowsToImport.size());
            for (Row row : rowsToImport) {
                VocabRecord record = new VocabRecord();
                List<Field> fieldList = new ArrayList<>();
                for (MatchingField mf : headerOrder) {
                    if (mf.getAssignedField() != null) {
                        String cellValue = getCellValue(row.getCell(mf.getColumnOrderNumber()));
                        if (StringUtils.isNotBlank(cellValue)) {
                            Field field = new Field(mf.getAssignedField().getLabel(), mf.getAssignedField().getLanguage(), cellValue,
                                    mf.getAssignedField());
                            fieldList.add(field);
                        }
                    }
                }
                if (!fieldList.isEmpty()) {
                    recordsToAdd.add(record);
                    log.debug("Created record");
                    addFieldToRecord(record, fieldList);
                }
            }
            VocabularyManager.insertNewRecords(recordsToAdd, currentVocabulary.getId());
            log.debug("Stored {} new records", recordsToAdd.size());
        }

        if (importType.equals("merge")) {
            List<VocabRecord> newRecords = new ArrayList<>();
            List<VocabRecord> updateRecords = new ArrayList<>();
            // get main entry row
            Integer mainEntryColumnNumber = null;
            for (MatchingField mf : headerOrder) {
                if (mf.getAssignedField() != null && mf.getAssignedField().isMainEntry()) {
                    mainEntryColumnNumber = mf.getColumnOrderNumber();
                }
            }
            for (Row row : rowsToImport) {
                // search for existing records based on the value of the main entry
                VocabRecord recordToUpdate = null;
                if (mainEntryColumnNumber != null) {
                    String uniqueIdentifierEntry = getCellValue(row.getCell(mainEntryColumnNumber));
                    outerloop: for (VocabRecord vr : currentVocabulary.getRecords()) {
                        for (Field field : vr.getFields()) {
                            if (field.getDefinition().isMainEntry() && uniqueIdentifierEntry.equals(field.getValue())) {
                                recordToUpdate = vr;
                                break outerloop;
                            }
                        }
                    }
                    if (recordToUpdate != null) {
                        log.debug("merged row with existing record");
                        updateRecords.add(recordToUpdate);
                        // update existing record
                        for (MatchingField mf : headerOrder) {
                            if (mf.getAssignedField() != null) {
                                Field fieldToUpdate = null;
                                for (Field field : recordToUpdate.getFields()) {
                                    if (field.getDefinition() != null && mf.getAssignedField().equals(field.getDefinition())) {
                                        fieldToUpdate = field;
                                        break;
                                    }
                                }
                                String cellValue = getCellValue(row.getCell(mf.getColumnOrderNumber()));
                                if (fieldToUpdate == null) {
                                    fieldToUpdate = new Field(mf.getAssignedField().getLabel(), mf.getAssignedField().getLanguage(), cellValue,
                                            mf.getAssignedField());
                                    recordToUpdate.getFields().add(fieldToUpdate);
                                } else {
                                    fieldToUpdate.setValue(cellValue);
                                }
                            }
                        }
                        continue;
                    } else {
                        // create new record
                        log.debug("create new record.");
                        VocabRecord record = new VocabRecord();
                        List<Field> fieldList = new ArrayList<>();
                        for (MatchingField mf : headerOrder) {
                            if (mf.getAssignedField() != null) {
                                String cellValue = getCellValue(row.getCell(mf.getColumnOrderNumber()));
                                if (StringUtils.isNotBlank(cellValue)) {
                                    Field field = new Field(mf.getAssignedField().getLabel(), mf.getAssignedField().getLanguage(), cellValue,
                                            mf.getAssignedField());
                                    fieldList.add(field);
                                }

                            }
                        }
                        if (!fieldList.isEmpty()) {
                            addFieldToRecord(record, fieldList);
                            newRecords.add(record);
                        }
                    }
                }
            }
            if (!newRecords.isEmpty()) {
                log.debug("Created {} new record(s)", newRecords.size());
                VocabularyManager.insertNewRecords(newRecords, currentVocabulary.getId());
            }
            if (!updateRecords.isEmpty()) {
                log.debug("Updated {} record(s)", updateRecords.size());
                VocabularyManager.batchUpdateRecords(updateRecords, currentVocabulary.getId());
            }
        }
        return FilterKein();
    }

    /**
     * method to add a field to the existing record
     * 
     * @param record Record to use
     * @param fieldList List of fields to add to the record
     */
    private void addFieldToRecord(VocabRecord record, List<Field> fieldList) {
        for (Definition def : currentVocabulary.getStruct()) {
            boolean fieldExists = false;
            for (Field f : fieldList) {
                if (def.getId().equals(f.getDefinition().getId())) {
                    fieldExists = true;
                    break;
                }
            }
            if (!fieldExists) {
                Field emptyField = new Field(def.getLabel(), def.getLanguage(), "", def);
                fieldList.add(emptyField);
            }
        }
        record.setFields(fieldList);
        currentVocabulary.getRecords().add(record);
    }

    /**
     * If true, allow uploading of the vocabularies
     * 
     * @return
     */
    public Boolean useAuthorityServer() {
        return VocabularyUploader.isActive();
    }

    /**
     * returns the value of the current cell as string
     */
    private String getCellValue(Cell cell) {
        String value = "";
        if (cell != null) {
            value = dataFormatter.formatCellValue(cell).trim();
        }
        return value;
    }

    /**
     * This class is used to match the excel columns and the vocabulary fields
     */
    @Data
    @RequiredArgsConstructor
    public class MatchingField implements Serializable {

        private static final long serialVersionUID = 7037009721345445066L;

        /**
         * Name of the header of the current column within the excel file
         */
        @NonNull
        private String columnHeader;

        /**
         * Internal order number of the current column within the excel file
         */
        @NonNull
        private Integer columnOrderNumber;

        /**
         * Displayed label the current column within the excel file (1=A, 2=B, 3=C, ...)
         */
        @NonNull
        private String columnLetter;

        /**
         * Reference to the managed bean
         */
        @NonNull
        private VocabularyBean bean;

        /**
         * field in which the current data is imported
         */
        private Definition assignedField;

        /**
         * Creates a label to identify the assigned field
         * 
         * @return
         */
        public String getCurrentDefinition() {
            if (assignedField == null) {
                return "-";
            }
            String definitionName;
            if (StringUtils.isNotBlank(assignedField.getLanguage())) {
                definitionName = assignedField.getLabel() + " (" + assignedField.getLanguage() + ")";
            } else {
                definitionName = assignedField.getLabel();
            }
            return definitionName;
        }

        /**
         * Set the assigned field based on the selected label. If a new field is set, all other fields are checked if the current field was already
         * selected. If this is the case, the selection is removed from the other field
         * 
         * @param value
         */
        public void setCurrentDefinition(String value) {

            if (StringUtils.isNotBlank(value) && !"-".equals(value)) {
                if (value.matches(".*\\(.*\\)")) { //NOSONAR, regex is not vulnerable to backtracking
                    String titlePart = value.substring(0, value.lastIndexOf("(")).trim();
                    String languagePart = value.substring(value.lastIndexOf("(") + 1, value.lastIndexOf(")")).trim();
                    for (Definition def : currentVocabulary.getStruct()) {
                        if (def.getLabel().equals(titlePart) && def.getLanguage().equals(languagePart)) {
                            assignedField = def;
                            break;
                        }
                    }
                } else {
                    for (Definition def : currentVocabulary.getStruct()) {
                        if (def.getLabel().equals(value) && StringUtils.isBlank(def.getLanguage())) {
                            assignedField = def;
                        }
                    }
                }
                bean.updateFieldList(this);
            } else {
                assignedField = null;
            }
        }
    }

    /**
     * method to set the current record to use
     * 
     * @param currentVocabRecord the record to use
     */
    public void setCurrentVocabRecord(VocabRecord currentVocabRecord) {
        if (this.hasInvalidVocabRecords()) {
            // The currentVocabRecord must be cached by its id because the list of vocabulary records is reloaded due to inconsistencies in the user interface
            // TODO: This code block (reloading of records) gets unnecessary when vocabRecord.fields.value is only set after it was validated.
            int id = this.currentVocabRecord.getId();
            VocabularyManager.getAllRecords(this.currentVocabulary);
            this.currentVocabRecord = this.getVocabRecordById(id);
        } else {
            this.currentVocabRecord = currentVocabRecord;
        }
    }

    /**
     * Returns true if the current list of vocabulary records has invalid records and false if all are valid.
     *
     * @return true If there are invalid records and false otherwise
     */
    private boolean hasInvalidVocabRecords() {
        for (VocabRecord record : this.currentVocabulary.getRecords()) {
            if (!record.isValid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the vocabulary record from the current vocabulary list with the given id. The advantage of this method in contrast to the one of the
     * mysql-helper is that this method returns the same object and not an equal object. If the object can be found, the vocabulary record with that
     * id is returned. Otherwise, null is returned.
     * 
     * @param id The id of the required vocabulary record
     * @return The vocabulary record or null if it could not be found
     */
    private VocabRecord getVocabRecordById(int id) {
        for (VocabRecord vocabulary : this.currentVocabulary.getRecords()) {
            if (vocabulary.getId() == id) {
                return vocabulary;
            }
        }
        return null;
    }

    public void validateFieldValue(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        java.util.Map<String, Object> attributes = component.getAttributes();
        System.out.println("Number of attributes: " + attributes.size());
        System.out.println("id" + attributes.get("id"));
        System.out.println("value" + attributes.get("value"));
        VocabularyBean bean = VocabularyFieldValidator.extractVocabularyBean(context);
        Vocabulary vocabulary = bean.getCurrentVocabulary();
        VocabRecord record = bean.getCurrentVocabRecord();
        boolean success = VocabularyFieldValidator.validateRecords(vocabulary, record);
        if (!success) {
            // TODO: Replace this key by a custom key (depending on cause of error)
            String messageKey = "vocabularyManager_validation_fieldIsRequired";
            String translation = Helper.getTranslation(messageKey);
            FacesMessage message = new FacesMessage(translation, translation);
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }
}
