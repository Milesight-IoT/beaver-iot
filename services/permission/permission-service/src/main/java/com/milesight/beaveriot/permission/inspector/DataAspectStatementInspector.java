package com.milesight.beaveriot.permission.inspector;

import com.milesight.beaveriot.permission.context.DataAspectContext;
import lombok.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/12/5 15:12
 */
public class DataAspectStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        String tenantSql = doTenantInspect(sql);
        return doDataPermissionInspect(tenantSql);
    }

    @SneakyThrows
    private String doTenantInspect(String sql) {
        Statement statement = CCJSqlParserUtil.parse(sql);
        if (statement instanceof Select selectStatement) {
            PlainSelect plainSelect = selectStatement.getPlainSelect();
            addTenantCondition(plainSelect);
        } else if (statement instanceof Update updateStatement) {
            addTenantCondition(updateStatement);
        } else if (statement instanceof Delete deleteStatement) {
            addTenantCondition(deleteStatement);
        } else if (statement instanceof Insert insertStatement) {
            addTenantCondition(insertStatement);
        }
        return statement.toString();
    }

    @SneakyThrows
    private String doDataPermissionInspect(String sql) {
        Statement statement = CCJSqlParserUtil.parse(sql);
        if (statement instanceof Select selectStatement) {
            PlainSelect plainSelect = selectStatement.getPlainSelect();
            addDataPermissionCondition(plainSelect);
        }
        return statement.toString();
    }

    private void addTenantCondition(PlainSelect plainSelect) {
        String tableName = null;
        String alias = null;
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table table) {
            tableName = table.getName();
            alias = table.getAlias() != null ? table.getAlias().getName() : tableName;
        }
        if (tableName == null || alias == null) {
            return;
        }
        String tenantId = null;
        String columnName = null;
        if (DataAspectContext.isTenantEnabled(tableName)) {
            DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext(tableName);
            if (tenantContext != null) {
                tenantId = tenantContext.getTenantId();
                columnName = tenantContext.getTenantColumnName();
            }
        }
        if (tenantId == null || columnName == null) {
            return;
        }
        Column column = new Column(alias + "." + columnName);
        Expression tenantExpression = new EqualsTo(column, new StringValue(tenantId));

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(tenantExpression);
        } else {
            Parenthesis originalWhere = new Parenthesis(plainSelect.getWhere());
            plainSelect.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    originalWhere, tenantExpression));
        }
    }

    private void addTenantCondition(Update updateStatement) {
        Table table = updateStatement.getTable();
        String tableName = table.getName();
        String alias = table.getAlias() != null ? table.getAlias().getName() : tableName;

        String tenantId = null;
        String columnName = null;
        if (DataAspectContext.isTenantEnabled(tableName)) {
            DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext(tableName);
            if (tenantContext != null) {
                tenantId = tenantContext.getTenantId();
                columnName = tenantContext.getTenantColumnName();
            }
        }
        if (tenantId == null || columnName == null) {
            return;
        }
        Column column = new Column(alias + "." + columnName);
        Expression tenantExpression = new EqualsTo(column, new StringValue(tenantId));

        if (updateStatement.getWhere() == null) {
            updateStatement.setWhere(tenantExpression);
        } else {
            Parenthesis originalWhere = new Parenthesis(updateStatement.getWhere());
            updateStatement.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    originalWhere, tenantExpression));
        }
    }

    private void addTenantCondition(Delete deleteStatement) {
        Table table = deleteStatement.getTable();
        String tableName = table.getName();
        String alias = table.getAlias() != null ? table.getAlias().getName() : tableName;

        String tenantId = null;
        String columnName = null;
        if (DataAspectContext.isTenantEnabled(tableName)) {
            DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext(tableName);
            if (tenantContext != null) {
                tenantId = tenantContext.getTenantId();
                columnName = tenantContext.getTenantColumnName();
            }
        }
        if (tenantId == null || columnName == null) {
            return;
        }
        Column column = new Column(alias + "." + columnName);
        Expression tenantExpression = new EqualsTo(column, new StringValue(tenantId));

        if (deleteStatement.getWhere() == null) {
            deleteStatement.setWhere(tenantExpression);
        } else {
            Parenthesis originalWhere = new Parenthesis(deleteStatement.getWhere());
            deleteStatement.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    originalWhere, tenantExpression));
        }
    }

    private void addTenantCondition(Insert insertStatement) {
        List<Column> columns = insertStatement.getColumns();
        String tableName = insertStatement.getTable().getName();
        ExpressionList<?> itemList = insertStatement.getValues().getExpressions();

        String tenantId = null;
        String columnName;
        if (DataAspectContext.isTenantEnabled(tableName)) {
            DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext(tableName);
            if (tenantContext != null) {
                tenantId = tenantContext.getTenantId();
                columnName = tenantContext.getTenantColumnName();
            } else {
                columnName = null;
            }
        } else {
            columnName = null;
        }
        if (tenantId == null || columnName == null) {
            return;
        }
        boolean tenantIdPresent = columns.stream()
                .anyMatch(column -> column.getColumnName().equalsIgnoreCase(columnName));

        if (!tenantIdPresent) {
            columns.add(new Column(columnName));
        }

        if (!itemList.isEmpty() && itemList.get(0) instanceof ExpressionList) {
            for (ExpressionList expressions : (ExpressionList<ExpressionList>) itemList) {

                if (!tenantIdPresent) {
                    expressions.add(new StringValue(tenantId));
                }

                // Ensure the columns and expressions lists are of the same size for each row
                if (columns.size() != expressions.size()) {
                    throw new IllegalStateException("The number of columns and values do not match.");
                }
            }
        } else {
            List<Expression> expressions = (ExpressionList<Expression>) itemList;

            if (!tenantIdPresent) {
                expressions.add(new StringValue(tenantId));
            }

            // Ensure the columns and expressions lists are of the same size
            if (columns.size() != expressions.size()) {
                throw new IllegalStateException("The number of columns and values do not match.");
            }
        }
    }

    private void addDataPermissionCondition(PlainSelect plainSelect) {
        String tableName = null;
        String alias = null;
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table table) {
            tableName = table.getName();
            alias = table.getAlias() != null ? table.getAlias().getName() : tableName;
        }
        if (tableName == null || alias == null) {
            return;
        }
        List<Long> dataIds = new ArrayList<>();
        String columnName = null;
        if (DataAspectContext.isDataPermissionEnabled(tableName)) {
            DataAspectContext.DataPermissionContext dataPermissionContext = DataAspectContext.getDataPermissionContext(tableName);
            if (dataPermissionContext != null) {
                dataIds = dataPermissionContext.getDataIds();
                columnName = dataPermissionContext.getDataColumnName();
            }
        }
        if (dataIds.isEmpty() || columnName == null) {
            return;
        }
        Column column = new Column(alias + "." + columnName);
        Expression expression = new InExpression(column, new ExpressionList(dataIds.stream()
                .map(LongValue::new)
                .collect(Collectors.toList())));

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(expression);
        } else {
            Parenthesis originalWhere = new Parenthesis(plainSelect.getWhere());
            plainSelect.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    originalWhere, expression));
        }
    }

}
