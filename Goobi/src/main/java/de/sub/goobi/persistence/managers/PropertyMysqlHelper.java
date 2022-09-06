package de.sub.goobi.persistence.managers;

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
 */
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Templateproperty;

import de.sub.goobi.helper.enums.PropertyType;
import lombok.extern.log4j.Log4j2;

@Log4j2
class PropertyMysqlHelper implements Serializable {
    private static final long serialVersionUID = 5175567943231852013L;

    public static List<Processproperty> getProcessPropertiesForProcess(int processId) throws SQLException {
        String sql = "SELECT * FROM prozesseeigenschaften WHERE prozesseID = ? ORDER BY container, Titel";
        Connection connection = null;
        Object[] param = { processId };
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, resultSetToPropertyListHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static ResultSetHandler<Processproperty> resultSetToPropertyHandler = new ResultSetHandler<Processproperty>() {
        @Override
        public Processproperty handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    int id = rs.getInt("prozesseeigenschaftenID");
                    String title = rs.getString("Titel");
                    String value = rs.getString("Wert");
                    Boolean mandatory = rs.getBoolean("IstObligatorisch");
                    int type = rs.getInt("DatentypenID");
                    String choice = rs.getString("Auswahl");
                    int processId = rs.getInt("prozesseID");
                    Timestamp time = rs.getTimestamp("creationDate");
                    Date creationDate = null;
                    if (time != null) {
                        creationDate = new Date(time.getTime());
                    }
                    int container = rs.getInt("container");
                    Processproperty pe = new Processproperty();
                    pe.setId(id);
                    pe.setTitel(title);
                    pe.setWert(value);
                    pe.setIstObligatorisch(mandatory);
                    pe.setType(PropertyType.getById(type));
                    pe.setAuswahl(choice);
                    pe.setProcessId(processId);
                    pe.setCreationDate(creationDate);
                    pe.setContainer(container);
                    return pe;
                }
            } finally {
                rs.close();
            }
            return null;
        }
    };

    public static ResultSetHandler<List<Processproperty>> resultSetToPropertyListHandler = new ResultSetHandler<List<Processproperty>>() {
        @Override
        public List<Processproperty> handle(ResultSet rs) throws SQLException {
            List<Processproperty> properties = new ArrayList<Processproperty>();
            try {
                while (rs.next()) { // implies that rs != null, while the case rs == null will be thrown as an Exception
                    int id = rs.getInt("prozesseeigenschaftenID");
                    String title = rs.getString("Titel");
                    String value = rs.getString("Wert");
                    Boolean mandatory = rs.getBoolean("IstObligatorisch");
                    int type = rs.getInt("DatentypenID");
                    String choice = rs.getString("Auswahl");
                    int processId = rs.getInt("prozesseID");
                    Timestamp time = rs.getTimestamp("creationDate");
                    Date creationDate = null;
                    if (time != null) {
                        creationDate = new Date(time.getTime());
                    }
                    int container = rs.getInt("container");
                    Processproperty pe = new Processproperty();
                    pe.setId(id);
                    pe.setTitel(title);
                    pe.setWert(value);
                    pe.setIstObligatorisch(mandatory);
                    pe.setType(PropertyType.getById(type));
                    pe.setAuswahl(choice);
                    pe.setProcessId(processId);
                    pe.setCreationDate(creationDate);
                    pe.setContainer(container);
                    properties.add(pe);
                }
            } finally {
                rs.close();
            }
            return properties;
        }
    };

    public static ResultSetHandler<List<Templateproperty>> resultSetToTemplatePropertyListHandler = new ResultSetHandler<List<Templateproperty>>() {
        @Override
        public List<Templateproperty> handle(ResultSet rs) throws SQLException {
            List<Templateproperty> properties = new ArrayList<Templateproperty>();
            try {
                while (rs.next()) {
                    int id = rs.getInt("vorlageneigenschaftenID");
                    String title = rs.getString("Titel");
                    String value = rs.getString("Wert");
                    Boolean mandatory = rs.getBoolean("IstObligatorisch");
                    int type = rs.getInt("DatentypenID");
                    String choice = rs.getString("Auswahl");
                    int templateId = rs.getInt("vorlagenID");
                    Timestamp time = rs.getTimestamp("creationDate");
                    Date creationDate = null;
                    if (time != null) {
                        creationDate = new Date(time.getTime());
                    }
                    int container = rs.getInt("container");
                    Templateproperty ve = new Templateproperty();
                    ve.setId(id);
                    ve.setTitel(title);
                    ve.setWert(value);
                    ve.setIstObligatorisch(mandatory);
                    ve.setType(PropertyType.getById(type));
                    ve.setAuswahl(choice);
                    ve.setTemplateId(templateId);
                    ve.setCreationDate(creationDate);
                    ve.setContainer(container);
                    properties.add(ve);
                }
            } finally {
                rs.close();
            }
            return properties;
        }
    };

    public static ResultSetHandler<Templateproperty> resultSetToTemplatePropertyHandler = new ResultSetHandler<Templateproperty>() {
        @Override
        public Templateproperty handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    int id = rs.getInt("vorlageneigenschaftenID");
                    String title = rs.getString("Titel");
                    String value = rs.getString("Wert");
                    Boolean mandatory = rs.getBoolean("IstObligatorisch");
                    int type = rs.getInt("DatentypenID");
                    String choice = rs.getString("Auswahl");
                    int templateId = rs.getInt("vorlagenID");
                    Timestamp time = rs.getTimestamp("creationDate");
                    Date creationDate = null;
                    if (time != null) {
                        creationDate = new Date(time.getTime());
                    }
                    int container = rs.getInt("container");
                    Templateproperty ve = new Templateproperty();
                    ve.setId(id);
                    ve.setTitel(title);
                    ve.setWert(value);
                    ve.setIstObligatorisch(mandatory);
                    ve.setType(PropertyType.getById(type));
                    ve.setAuswahl(choice);
                    ve.setTemplateId(templateId);
                    ve.setCreationDate(creationDate);
                    ve.setContainer(container);
                    return ve;
                }
            } finally {
                rs.close();
            }
            return null;
        }
    };

    public static ResultSetHandler<List<Masterpieceproperty>> resultSetToMasterpiecePropertyListHandler =
            new ResultSetHandler<List<Masterpieceproperty>>() {
                @Override
                public List<Masterpieceproperty> handle(ResultSet rs) throws SQLException {
                    List<Masterpieceproperty> properties = new ArrayList<Masterpieceproperty>();
                    try {
                        while (rs.next()) {
                            int id = rs.getInt("werkstueckeeigenschaftenID");
                            String title = rs.getString("Titel");
                            String value = rs.getString("Wert");
                            Boolean mandatory = rs.getBoolean("IstObligatorisch");
                            int type = rs.getInt("DatentypenID");
                            String choice = rs.getString("Auswahl");
                            int templateId = rs.getInt("werkstueckeID");
                            Timestamp time = rs.getTimestamp("creationDate");
                            Date creationDate = null;
                            if (time != null) {
                                creationDate = new Date(time.getTime());
                            }
                            int container = rs.getInt("container");
                            Masterpieceproperty ve = new Masterpieceproperty();
                            ve.setId(id);
                            ve.setTitel(title);
                            ve.setWert(value);
                            ve.setIstObligatorisch(mandatory);
                            ve.setType(PropertyType.getById(type));
                            ve.setAuswahl(choice);
                            ve.setMasterpieceId(templateId);
                            ve.setCreationDate(creationDate);
                            ve.setContainer(container);
                            properties.add(ve);
                        }
                    } finally {
                        rs.close();
                    }
                    return properties;
                }
            };

    public static ResultSetHandler<Masterpieceproperty> resultSetToMasterpiecePropertyHandler = new ResultSetHandler<Masterpieceproperty>() {
        @Override
        public Masterpieceproperty handle(ResultSet rs) throws SQLException {
            try {
                if (rs.next()) {
                    int id = rs.getInt("werkstueckeeigenschaftenID");
                    String title = rs.getString("Titel");
                    String value = rs.getString("Wert");
                    Boolean mandatory = rs.getBoolean("IstObligatorisch");
                    int type = rs.getInt("DatentypenID");
                    String choice = rs.getString("Auswahl");
                    int templateId = rs.getInt("werkstueckeID");
                    Timestamp time = rs.getTimestamp("creationDate");
                    Date creationDate = null;
                    if (time != null) {
                        creationDate = new Date(time.getTime());
                    }
                    int container = rs.getInt("container");
                    Masterpieceproperty ve = new Masterpieceproperty();
                    ve.setId(id);
                    ve.setTitel(title);
                    ve.setWert(value);
                    ve.setIstObligatorisch(mandatory);
                    ve.setType(PropertyType.getById(type));
                    ve.setAuswahl(choice);
                    ve.setMasterpieceId(templateId);
                    ve.setCreationDate(creationDate);
                    ve.setContainer(container);
                    return ve;
                }
            } finally {
                rs.close();
            }
            return null;
        }
    };

    public static void saveProcessproperty(Processproperty pe) throws SQLException {
        if (pe.getProcessId() == 0 && pe.getProzess() != null) {
            pe.setProcessId(pe.getProzess().getId());
        }
        if (pe.getId() == null) {
            insertProcessproperty(pe);
        } else {
            updateProcessproperty(pe);
        }
    }

    private static void insertProcessproperty(Processproperty pe) throws SQLException {
        String sql =
                "INSERT INTO prozesseeigenschaften (Titel, WERT, IstObligatorisch, DatentypenID, Auswahl, prozesseID, creationDate, container) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] param = { pe.getTitel(), pe.getWert(), pe.isIstObligatorisch(), pe.getType().getId(), pe.getAuswahl(), pe.getProzess().getId(),
                pe.getCreationDate() == null ? null : new Timestamp(pe.getCreationDate().getTime()), pe.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            if (log.isTraceEnabled()) {
                log.trace(sql + ", " + Arrays.toString(param));
            }
            Integer id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
            if (id != null) {
                pe.setId(id);
            }
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }

    }

    private static void updateProcessproperty(Processproperty pe) throws SQLException {
        String sql =
                "UPDATE prozesseeigenschaften set Titel = ?,  WERT = ?, IstObligatorisch = ?, DatentypenID = ?, Auswahl = ?, prozesseID = ?, creationDate = ?, container = ? WHERE prozesseeigenschaftenID = "
                        + pe.getId();
        Object[] param = { pe.getTitel(), pe.getWert(), pe.isIstObligatorisch(), pe.getType().getId(), pe.getAuswahl(), pe.getProzess().getId(),
                pe.getCreationDate() == null ? null : new Timestamp(pe.getCreationDate().getTime()), pe.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            run.update(connection, sql, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void deleteProcessProperty(Processproperty property) throws SQLException {
        if (property.getId() != null) {
            Connection connection = null;
            try {
                connection = MySQLHelper.getInstance().getConnection();
                QueryRunner run = new QueryRunner();
                String sql = "DELETE FROM prozesseeigenschaften WHERE prozesseeigenschaftenID = " + property.getId();
                run.update(connection, sql);
            } finally {
                if (connection != null) {
                    MySQLHelper.closeConnection(connection);
                }
            }
        }
    }

    public static List<String> getDistinctPropertyTitles() throws SQLException {
        String sql = "select distinct titel from prozesseeigenschaften order by Titel";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            if (log.isTraceEnabled()) {
                log.trace(sql);
            }
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToStringListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<String> getDistinctTemplatePropertyTitles() throws SQLException {
        String sql = "select distinct titel from vorlageneigenschaften order by Titel";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            if (log.isTraceEnabled()) {
                log.trace(sql);
            }
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToStringListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<String> getDistinctMasterpiecePropertyTitles() throws SQLException {
        String sql = "select distinct titel from werkstueckeeigenschaften order by Titel";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            if (log.isTraceEnabled()) {
                log.trace(sql);
            }
            return new QueryRunner().query(connection, sql, MySQLHelper.resultSetToStringListHandler);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Templateproperty> getTemplateProperties(int templateId) throws SQLException {
        String sql = "SELECT * FROM vorlageneigenschaften WHERE vorlagenID = ? ORDER BY Titel";
        Connection connection = null;
        Object[] param = { templateId };
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, resultSetToTemplatePropertyListHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static Templateproperty saveTemplateproperty(Templateproperty property) throws SQLException {
        if (property.getTemplateId() == null && property.getVorlage() != null) {
            property.setTemplateId(property.getVorlage().getId());
        }

        if (property.getId() == null) {
            return insertTemplateproperty(property);
        } else {
            updateTemplateproperty(property);
            return property;
        }
    }

    private static void updateTemplateproperty(Templateproperty property) throws SQLException {
        String sql =
                "UPDATE vorlageneigenschaften set Titel = ?,  WERT = ?, IstObligatorisch = ?, DatentypenID = ?, Auswahl = ?, vorlagenID = ?, creationDate = ?, container = ? WHERE vorlageneigenschaftenID = "
                        + property.getId();
        Object[] param = { property.getTitel(), property.getWert(), property.isIstObligatorisch(), property.getType().getId(), property.getAuswahl(),
                property.getVorlage().getId(), property.getCreationDate() == null ? null : new Timestamp(property.getCreationDate().getTime()),
                property.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            run.update(connection, sql, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static Templateproperty insertTemplateproperty(Templateproperty property) throws SQLException {
        String sql =
                "INSERT INTO vorlageneigenschaften (Titel, WERT, IstObligatorisch, DatentypenID, Auswahl, vorlagenID, creationDate, container) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] param = { property.getTitel(), property.getWert(), property.isIstObligatorisch(), property.getType().getId(), property.getAuswahl(),
                property.getVorlage().getId(), property.getCreationDate() == null ? null : new Timestamp(property.getCreationDate().getTime()),
                property.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            int id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
            property.setId(id);
            return property;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void deleteTemplateProperty(Templateproperty property) throws SQLException {
        if (property.getId() != null) {
            Connection connection = null;
            try {
                connection = MySQLHelper.getInstance().getConnection();
                QueryRunner run = new QueryRunner();
                String sql = "DELETE FROM vorlageneigenschaften WHERE vorlageneigenschaftenID = " + property.getId();
                run.update(connection, sql);
            } finally {
                if (connection != null) {
                    MySQLHelper.closeConnection(connection);
                }
            }
        }
    }

    public static List<Masterpieceproperty> getMasterpieceProperties(int templateId) throws SQLException {
        String sql = "SELECT * FROM werkstueckeeigenschaften WHERE werkstueckeID = ? ORDER BY container, Titel";
        Connection connection = null;
        Object[] param = { templateId };
        try {
            connection = MySQLHelper.getInstance().getConnection();
            return new QueryRunner().query(connection, sql, resultSetToMasterpiecePropertyListHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static Masterpieceproperty saveMasterpieceProperty(Masterpieceproperty property) throws SQLException {
        if (property.getMasterpieceId() == null && property.getWerkstueck() != null) {
            property.setMasterpieceId(property.getWerkstueck().getId());
        }

        if (property.getId() == null) {
            return insertMasterpieceproperty(property);
        } else {
            updateMasterpieceproperty(property);
            return property;
        }
    }

    private static void updateMasterpieceproperty(Masterpieceproperty property) throws SQLException {
        String sql =
                "UPDATE werkstueckeeigenschaften set Titel = ?,  WERT = ?, IstObligatorisch = ?, DatentypenID = ?, Auswahl = ?, werkstueckeID = ?, creationDate = ?, container = ? WHERE werkstueckeeigenschaftenID = "
                        + property.getId();
        Object[] param = { property.getTitel(), property.getWert(), property.isIstObligatorisch(), property.getType().getId(), property.getAuswahl(),
                property.getWerkstueck().getId(), property.getCreationDate() == null ? null : new Timestamp(property.getCreationDate().getTime()),
                property.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            run.update(connection, sql, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static Masterpieceproperty insertMasterpieceproperty(Masterpieceproperty property) throws SQLException {
        String sql =
                "INSERT INTO werkstueckeeigenschaften (Titel, WERT, IstObligatorisch, DatentypenID, Auswahl, werkstueckeID, creationDate, container) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] param = { property.getTitel(), property.getWert(), property.isIstObligatorisch(), property.getType().getId(), property.getAuswahl(),
                property.getWerkstueck().getId(), property.getCreationDate() == null ? null : new Timestamp(property.getCreationDate().getTime()),
                property.getContainer() };
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            int id = run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
            property.setId(id);
            return property;
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static void deleteMasterpieceProperty(Masterpieceproperty property) throws SQLException {
        if (property.getId() != null) {
            Connection connection = null;
            try {
                connection = MySQLHelper.getInstance().getConnection();
                QueryRunner run = new QueryRunner();
                String sql = "DELETE FROM werkstueckeeigenschaften WHERE werkstueckeeigenschaftenID = " + property.getId();
                run.update(connection, sql);
            } finally {
                if (connection != null) {
                    MySQLHelper.closeConnection(connection);
                }
            }
        }
    }
}
