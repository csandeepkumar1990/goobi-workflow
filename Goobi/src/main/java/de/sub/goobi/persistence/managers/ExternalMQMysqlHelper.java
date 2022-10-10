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
package de.sub.goobi.persistence.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.goobi.api.mq.ExternalCommandResult;
import org.goobi.beans.DatabaseObject;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExternalMQMysqlHelper {
    public static void insertTicket(ExternalCommandResult message) throws SQLException {
        String sql = "INSERT INTO external_mq_results " + generateInsertQuery() + generateValueQuery();
        Object[] param = generateParameter(message);
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            if (log.isTraceEnabled()) {
                log.trace(sql + ", " + Arrays.toString(param));
            }
            run.insert(connection, sql, MySQLHelper.resultSetToIntegerHandler, param);
        } finally {
            if (connection != null) {
                MySQLHelper.closeConnection(connection);
            }
        }
    }

    private static ResultSetHandler<List<ExternalCommandResult>> rsToStatusMessageListHandler = new ResultSetHandler<List<ExternalCommandResult>>() {
        @Override
        public List<ExternalCommandResult> handle(ResultSet rs) throws SQLException {
            List<ExternalCommandResult> messageList = new ArrayList<>();
            while (rs.next()) {
                messageList.add(convertRow(rs));
            }
            return messageList;
        }
    };

    private static ExternalCommandResult convertRow(ResultSet rs) throws SQLException {
        int processId = rs.getInt("ProzesseID");
        int stepId = rs.getInt("SchritteID");
        String scriptName = rs.getString("scriptName");
        return new ExternalCommandResult(processId, stepId, scriptName);
    }

    private static Object[] generateParameter(ExternalCommandResult message) {
        return new Object[] { message.getProcessId(), message.getStepId(), new Date(), message.getScriptName() };
    }

    private static String generateInsertQuery() {
        return "(ProzesseID, SchritteID, time, scriptName) VALUES ";
    }

    private static String generateValueQuery() {
        return "(?,?,?,?)";
    }

    public static int getMessagesCount(String filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM external_mq_results WHERE " + filter;
        try (Connection conn = MySQLHelper.getInstance().getConnection()) {
            return new QueryRunner().query(conn, sql, MySQLHelper.resultSetToIntegerHandler);
        }
    }

    public static List<? extends DatabaseObject> getMessageList(String order, String filter, Integer start, Integer count) throws SQLException {
        String sql = "SELECT * FROM external_mq_results";
        if (filter != null && !filter.isEmpty()) {
            sql += " WHERE " + filter;
        }
        if (order != null && !order.isEmpty()) {
            sql += " ORDER BY " + order;
        }
        if (start != null && count != null) {
            sql += " LIMIT " + start + ", " + count;
        }
        try (Connection conn = MySQLHelper.getInstance().getConnection()) {
            return new QueryRunner().query(conn, sql, rsToStatusMessageListHandler);
        }
    }
}
