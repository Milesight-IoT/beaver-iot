package com.milesight.beaveriot.permission.inspector;

import com.milesight.beaveriot.permission.context.DataAspectContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import org.hibernate.resource.jdbc.spi.StatementInspector;

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
        String dataPermissionSql = doDataPermissionInspect(tenantSql);
        return dataPermissionSql;
    }

    private String doTenantInspect(String sql) {
        if (DataAspectContext.isTenantEnabled()) {
            DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext();
            if (tenantContext != null) {
                Long tenantId = tenantContext.getTenantId();
                String columnName = tenantContext.getTenantColumnName();
                try {
                    Statement statement = CCJSqlParserUtil.parse(sql);

                    if (statement instanceof Select) {
                        Select selectStatement = (Select) statement;
                        SelectBody selectBody = selectStatement.getSelectBody();
                        if (selectBody instanceof PlainSelect) {
                            PlainSelect plainSelect = (PlainSelect) selectBody;
                            addTenantCondition(plainSelect, columnName, tenantId);
                        }
                    } else if (statement instanceof Update) {
                        Update updateStatement = (Update) statement;
                        addTenantCondition(updateStatement, columnName, tenantId);
                    } else if (statement instanceof Delete) {
                        Delete deleteStatement = (Delete) statement;
                        addTenantCondition(deleteStatement, columnName, tenantId);
                    } else if (statement instanceof Insert) {
                        Insert insertStatement = (Insert) statement;
                        addTenantCondition(insertStatement, columnName, tenantId);
                    }
                    return statement.toString();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse SQL: " + sql, e);
                }
            }
        }
        return sql;
    }

    private String doDataPermissionInspect(String sql) {
        if (DataAspectContext.isDataPermissionEnabled()) {
            DataAspectContext.DataPermissionContext dataPermissionContext = DataAspectContext.getDataPermissionContext();
            if (dataPermissionContext != null) {
                List<Long> dataIds = dataPermissionContext.getDataIds();
                String columnName = dataPermissionContext.getDataColumnName();
                try {
                    Statement statement = CCJSqlParserUtil.parse(sql);

                    if (statement instanceof Select) {
                        Select selectStatement = (Select) statement;
                        SelectBody selectBody = selectStatement.getSelectBody();
                        if (selectBody instanceof PlainSelect) {
                            PlainSelect plainSelect= (PlainSelect) selectBody;
                            addDataPermissionCondition(plainSelect, columnName, dataIds);
                        }
                    }
                    return statement.toString();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse SQL: " + sql, e);
                }
            }
        }
        return sql;
    }

    private void addTenantCondition(PlainSelect plainSelect, String columnName, Long tenantId) {
        Column column = new Column(columnName);
        Expression tenantExpression = new EqualsTo(column, new LongValue(tenantId));

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(tenantExpression);
        } else {
            plainSelect.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    plainSelect.getWhere(), tenantExpression));
        }
    }

    private void addTenantCondition(Update updateStatement, String columnName, Long tenantId) {
        Column column = new Column(columnName);
        Expression tenantExpression = new EqualsTo(column, new LongValue(tenantId));

        if (updateStatement.getWhere() == null) {
            updateStatement.setWhere(tenantExpression);
        } else {
            updateStatement.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    updateStatement.getWhere(), tenantExpression));
        }
    }

    private void addTenantCondition(Delete deleteStatement, String columnName, Long tenantId) {
        Column column = new Column(columnName);
        Expression tenantExpression = new EqualsTo(column, new LongValue(tenantId));

        if (deleteStatement.getWhere() == null) {
            deleteStatement.setWhere(tenantExpression);
        } else {
            deleteStatement.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    deleteStatement.getWhere(), tenantExpression));
        }
    }

    private void addTenantCondition(Insert insertStatement, String columnName, Long tenantId) {
        List<Column> columns = insertStatement.getColumns();
        ItemsList itemsList = insertStatement.getItemsList();

        boolean tenantIdPresent = columns.stream()
                .anyMatch(column -> column.getColumnName().equalsIgnoreCase(columnName));

        if (!tenantIdPresent) {
            columns.add(new Column(columnName));
        }

        if (itemsList instanceof ExpressionList) {
            ExpressionList expressionList = (ExpressionList) itemsList;
            List<Expression> expressions = expressionList.getExpressions();

            if (tenantIdPresent) {
//                for (int i = 0; i < columns.size(); i++) {
//                    if (columns.get(i).getColumnName().equalsIgnoreCase(columnName)) {
//                        expressions.set(i, new LongValue(tenantId));
//                        break;
//                    }
//                }
            } else {
                expressions.add(new LongValue(tenantId));
            }

            // Ensure the columns and expressions lists are of the same size
            if (columns.size() != expressions.size()) {
                throw new IllegalStateException("The number of columns and values do not match.");
            }
        } else if (itemsList instanceof MultiExpressionList) {
            MultiExpressionList multiExpressionList = (MultiExpressionList) itemsList;
            List<ExpressionList> expressionLists = multiExpressionList.getExprList();

            for (ExpressionList expressionList : expressionLists) {
                List<Expression> expressions = expressionList.getExpressions();

                if (tenantIdPresent) {
//                    for (int i = 0; i < columns.size(); i++) {
//                        if (columns.get(i).getColumnName().equalsIgnoreCase(columnName)) {
//                            expressions.set(i, new LongValue(tenantId));
//                            break;
//                        }
//                    }
                } else {
                    expressions.add(new LongValue(tenantId));
                }

                // Ensure the columns and expressions lists are of the same size for each row
                if (columns.size() != expressions.size()) {
                    throw new IllegalStateException("The number of columns and values do not match.");
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported ItemsList type: " + itemsList.getClass().getName());
        }
    }

    private void addDataPermissionCondition(PlainSelect plainSelect, String columnName, List<Long> dataIds) {
        Column column = new Column(columnName);
        Expression expression = new InExpression(column, new ExpressionList(dataIds.stream()
                .map(LongValue::new)
                .collect(Collectors.toList())));

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(expression);
        } else {
            plainSelect.setWhere(new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                    plainSelect.getWhere(), expression));
        }
    }

}
