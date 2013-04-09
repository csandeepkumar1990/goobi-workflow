package de.sub.goobi.persistence.managers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.log4j.Logger;
import org.goobi.beans.Docket;

import de.sub.goobi.persistence.apache.MySQLHelper;
import de.sub.goobi.persistence.apache.MySQLUtils;

class DocketMysqlHelper {
    private static final Logger logger = Logger.getLogger(DocketMysqlHelper.class);

    public static List<Docket> getDockets(String order, String filter, Integer start, Integer count) throws SQLException {
        Connection connection = MySQLHelper.getInstance().getConnection();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM dockets");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }
        if (order != null && !order.isEmpty()) {
            sql.append(" ORDER BY " + order);
        }
        if (start != null && count != null) {
            sql.append(" LIMIT " + start + ", " + count);
        }
        try {
            logger.debug(sql.toString());
            List<Docket> ret = new QueryRunner().query(connection, sql.toString(), DocketManager.resultSetToDocketListHandler);
            return ret;
        } finally {
            MySQLHelper.closeConnection(connection);
        }
    }

    public static int getDocketCount(String order, String filter) throws SQLException {
        Connection connection = MySQLHelper.getInstance().getConnection();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(docketID) FROM dockets");
        if (filter != null && !filter.isEmpty()) {
            sql.append(" WHERE " + filter);
        }
        try {
            logger.debug(sql.toString());
            return new QueryRunner().query(connection, sql.toString(), MySQLUtils.resultSetToIntegerHandler);
        } finally {
            MySQLHelper.closeConnection(connection);
        }
    }

    public static Docket getDocketById(int id) throws SQLException {
        Connection connection = MySQLHelper.getInstance().getConnection();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM dockets WHERE docketID = " + id);
        try {
            logger.debug(sql.toString());
            Docket ret = new QueryRunner().query(connection, sql.toString(), DocketManager.resultSetToDocketHandler);
            return ret;
        } finally {
            MySQLHelper.closeConnection(connection);
        }
    }

    public static void saveDocket(Docket ro) throws SQLException {
        Connection connection = MySQLHelper.getInstance().getConnection();
        try {
            QueryRunner run = new QueryRunner();
            StringBuilder sql = new StringBuilder();

            if (ro.getId() == null) {

                String propNames = "name, file";
                String propValues = "'" + ro.getName() + "','" + ro.getFile() + "'";
                sql.append("INSERT INTO dockets (");
                sql.append(propNames);
                sql.append(") VALUES (");
                sql.append(propValues);
                sql.append(")");
            } else {
                sql.append("UPDATE dockets SET ");
                sql.append("name = '" + ro.getName() + "', ");
                sql.append("file = '" + ro.getFile() + "' ");
                sql.append(" WHERE docketID = " + ro.getId() + ";");
            }
            logger.debug(sql.toString());
            run.update(connection, sql.toString());
        } finally {
            MySQLHelper.closeConnection(connection);
        }
    }

    public static void deleteDocket(Docket ro) throws SQLException {
        if (ro.getId() != null) {
            Connection connection = MySQLHelper.getInstance().getConnection();
            try {
                QueryRunner run = new QueryRunner();
                String sql = "DELETE FROM dockets WHERE docketID = " + ro.getId() + ";";
                logger.debug(sql);
                run.update(connection, sql);
            } finally {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    public static List<Docket> getAllDockets() throws SQLException {
        Connection connection = MySQLHelper.getInstance().getConnection();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM dockets");

        try {
            logger.debug(sql.toString());
            List<Docket> ret = new QueryRunner().query(connection, sql.toString(), DocketManager.resultSetToDocketListHandler);
            return ret;
        } finally {
            MySQLHelper.closeConnection(connection);
        }
    }
}
