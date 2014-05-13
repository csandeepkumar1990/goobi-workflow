package org.goobi.production.flow.helper;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;

public class SearchResultHelper {

    private List<SelectItem> possibleColumns = new ArrayList<SelectItem>();

    public List<SelectItem> getPossibleColumns() {
        return possibleColumns;
    }

    public SearchResultHelper() {
        List<String> columnWhiteList = ConfigurationHelper.getInstance().getDownloadColumnWhitelist();

        SelectItem processData = new SelectItem("processData", Helper.getTranslation("processData"), Helper.getTranslation("processData"), true);
        // static data
        possibleColumns.add(processData);

        {
            SelectItem item = new SelectItem("prozesse.Titel", Helper.getTranslation("prozesse.Titel"));
            possibleColumns.add(item);
        }
        {
            SelectItem item = new SelectItem("prozesse.ProzesseID", Helper.getTranslation("prozesse.ProzesseID"));
            possibleColumns.add(item);
        }
        {
            SelectItem item = new SelectItem("prozesse.erstellungsdatum", Helper.getTranslation("prozesse.erstellungsdatum"));
            possibleColumns.add(item);
        }
        {
            SelectItem item = new SelectItem("prozesse.sortHelperImages", Helper.getTranslation("prozesse.sortHelperImages"));
            possibleColumns.add(item);
        }
        {
            SelectItem item = new SelectItem("prozesse.sortHelperMetadata", Helper.getTranslation("prozesse.sortHelperMetadata"));
            possibleColumns.add(item);
        }
        {
            SelectItem item = new SelectItem("projekte.Titel", Helper.getTranslation("projekte.Titel"));
            possibleColumns.add(item);
        }

        if (columnWhiteList == null || columnWhiteList.isEmpty()) {
            return;
        }

        // data from configuration

        List<String> processTitles = PropertyManager.getDistinctProcessPropertyTitles();
        if (!processTitles.isEmpty()) {

            for (String title : processTitles) {
                if (columnWhiteList.contains(title)) {
                    SelectItem item = new SelectItem("prozesseeigenschaften." + title, Helper.getTranslation("prozesseeigenschaften." + title));
                    possibleColumns.add(item);
                }
            }
        }

        List<String> templateTitles = PropertyManager.getDistinctTemplatePropertyTitles();
        if (!templateTitles.isEmpty()) {
            List<SelectItem> subList = new ArrayList<>();

            for (String title : templateTitles) {
                if (columnWhiteList.contains(title)) {
                    SelectItem item = new SelectItem("vorlageneigenschaften." + title, Helper.getTranslation("vorlageneigenschaften." + title));
                    subList.add(item);
                }
            }
            if (!subList.isEmpty()) {
                SelectItem templateData =
                        new SelectItem("templateData", Helper.getTranslation("templateData"), Helper.getTranslation("templateData"), true);
                possibleColumns.add(templateData);
                possibleColumns.addAll(subList);
            }
        }

        List<String> masterpiecePropertyTitles = PropertyManager.getDistinctMasterpiecePropertyTitles();
        if (!masterpiecePropertyTitles.isEmpty()) {
            List<SelectItem> subList = new ArrayList<>();

            for (String title : masterpiecePropertyTitles) {
                if (columnWhiteList.contains(title)) {
                    SelectItem item = new SelectItem("werkstueckeeigenschaften." + title, Helper.getTranslation("werkstueckeeigenschaften." + title));
                    subList.add(item);
                }
            }
            if (!subList.isEmpty()) {
                SelectItem masterpieceData =
                        new SelectItem("masterpieceData", Helper.getTranslation("masterpieceData"), Helper.getTranslation("masterpieceData"), true);
                possibleColumns.add(masterpieceData);
                possibleColumns.addAll(subList);
            }
        }

        List<String> metadataTitles = MetadataManager.getDistinctMetadataNames();
        if (!metadataTitles.isEmpty()) {
            List<SelectItem> subList = new ArrayList<>();

            for (String title : metadataTitles) {
                if (columnWhiteList.contains(title)) {
                    SelectItem item = new SelectItem("metadata." + title, Helper.getTranslation("metadata." + title));
                    subList.add(item);
                }
            }

            if (!subList.isEmpty()) {
                SelectItem metadataData =
                        new SelectItem("metadataData", Helper.getTranslation("metadataData"), Helper.getTranslation("metadataData"), true);
                possibleColumns.add(metadataData);
                possibleColumns.addAll(subList);
            }
        }
    }

    public HSSFWorkbook getResult(List<SearchColumn> columnList, String filter, boolean showClosedProcesses, boolean showArchivedProjects) {
        @SuppressWarnings("rawtypes")
        List list = search(columnList, filter, showClosedProcesses, showArchivedProjects);
        
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("Search results");

        // create title row
        int titleColumnNumber = 0;
        HSSFRow title = sheet.createRow(0);
        for (SearchColumn sc : columnList) {
            HSSFCell titleCell = title.createCell(titleColumnNumber++);
            titleCell.setCellValue(Helper.getTranslation(sc.getValue()));
        }
        
        
        int rowNumber = 1;
        for (Object obj : list) {
            Object[] objArr = (Object[]) obj;
            HSSFRow row = sheet.createRow(rowNumber++);
            int columnNumber = 0;
            for (Object entry : objArr) {
                HSSFCell cell = row.createCell(columnNumber++);
                cell.setCellValue((String) entry);

            }
        }
        return wb;
    }

    @SuppressWarnings("rawtypes")
    private List search(List<SearchColumn> columnList, String filter, boolean showClosedProcesses, boolean showArchivedProjects) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");

        // add column labels to query
        for (SearchColumn sc : columnList) {
            sb.append(sc.getTableName() + "." + sc.getColumnName() + ", ");
        }
        int length = sb.length();
        sb = sb.replace(length - 2, length, "");

        sb.append(" FROM prozesse ");

       boolean leftJoin=false;

        for (SearchColumn sc : columnList) {
            String clause = sc.getJoinClause();
            if (!clause.isEmpty()) {
                if (!leftJoin) {
                    sb.append(" LEFT JOIN ") ;
                } else {
                    sb.append(" JOIN ") ;
                }
                sb.append(clause);
            }
        }
       
        String sql = FilterHelper.criteriaBuilder(filter, false, null, null, null, true, false);
        if (!sql.isEmpty()) {
            sql = sql + " AND ";
        }
        sql = sql + " prozesse.istTemplate = false ";

        if (!showClosedProcesses) {
            if (!sql.isEmpty()) {
                sql = sql + " AND ";
            }
            sql = sql + " prozesse.sortHelperStatus <> '100000000' ";
        }
        if (!showArchivedProjects) {
            if (!sql.isEmpty()) {
                sql = sql + " AND ";
            }
            sql = sql + " prozesse.ProjekteID not in (select ProjekteID from projekte where projectIsArchived = true) ";
        }
        sb.append(" WHERE ");
        sb.append(sql);
       
        List list = ProcessManager.runSQL(sb.toString());
        return list;
    }
}