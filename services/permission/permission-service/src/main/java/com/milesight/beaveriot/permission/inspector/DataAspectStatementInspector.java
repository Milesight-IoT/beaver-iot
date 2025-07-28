package com.milesight.beaveriot.permission.inspector;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.milesight.beaveriot.permission.context.DataAspectContext;
import com.milesight.beaveriot.permission.enums.ColumnDataType;
import lombok.*;
import lombok.extern.slf4j.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 * @date 2024/12/5 15:12
 */
@Slf4j
public class DataAspectStatementInspector implements StatementInspector {

    private static final String TENANT_ID_COLUMN_PLACEHOLDER = "${TENANT_ID_COL}";
    private static final String TENANT_ID_VALUE_PLACEHOLDER = "${TENANT_ID_VALUE}";
    private static final String DATA_ID_COLUMN_PLACEHOLDER = "${DATA_ID_COL}";

    private static final Cache<String, SqlInfo> sqlTemplateCache = CacheBuilder.newBuilder()
            .initialCapacity(1000)
            .maximumSize(10000)
            .concurrencyLevel(8)
            .expireAfterAccess(7, TimeUnit.DAYS)
            .build();

    private static final int DEFAULT_THREAD_SIZE = (Runtime.getRuntime().availableProcessors() + 1) / 2;

    private static final ExecutorService jsqlParserExecutor = new ThreadPoolExecutor(
            DEFAULT_THREAD_SIZE,
            DEFAULT_THREAD_SIZE,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("jsqlParser-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!jsqlParserExecutor.isShutdown()) {
                log.debug("jsqlParserExecutor is shutting down...");
                jsqlParserExecutor.shutdown();
            }
        }, "jsqlParser-shutdown-hook"));
    }

    @Override
    @SneakyThrows
    public String inspect(String sql) {
        SqlInfo cachedSql = sqlTemplateCache.getIfPresent(sql);
        if (cachedSql == null) {
            Statement statement = CCJSqlParserUtil.parse(sql, jsqlParserExecutor, null);
            TableInfo tableInfo = getTableInfo(statement);
            String tableName = tableInfo.tableName;
            doTenantInspect(statement);
            doDataPermissionInspect(statement);
            cachedSql = new SqlInfo(tableName, tableInfo.alias, statement.toString());
            sqlTemplateCache.put(sql, cachedSql);
        }

        String tableName = cachedSql.tableName;
        String tableAlias = cachedSql.tableAlias == null ? tableName : cachedSql.tableAlias;
        String sqlTemplate = cachedSql.sqlTemplate;

        DataAspectContext.TenantContext tenantContext = DataAspectContext.getTenantContext(tableName);
        if (tenantContext != null && StringUtils.hasText(tenantContext.getTenantId())) {
            String tenantId = tenantContext.getTenantId();
            String columnName = tableAlias + "." + tenantContext.getTenantColumnName();
            sqlTemplate = sqlTemplate.replace(TENANT_ID_COLUMN_PLACEHOLDER, columnName)
                    .replace(TENANT_ID_VALUE_PLACEHOLDER, tenantId);
        } else {
            sqlTemplate = sqlTemplate.replace(TENANT_ID_COLUMN_PLACEHOLDER, "'1'")
                    .replace(TENANT_ID_VALUE_PLACEHOLDER, "1");
        }

        DataAspectContext.DataPermissionContext dataPermissionContext = DataAspectContext.getDataPermissionContext(tableName);
        if (dataPermissionContext != null && !CollectionUtils.isEmpty(dataPermissionContext.getDataIds())) {
            String columnName = tableAlias + "." + dataPermissionContext.getDataColumnName();
            String dataIdListString = ColumnDataType.STRING.equals(dataPermissionContext.getDataType())
                    ? concatStringTypeColumnData(dataPermissionContext.getDataIds())
                    : String.join(",", dataPermissionContext.getDataIds());
            sqlTemplate = sqlTemplate.replace(DATA_ID_COLUMN_PLACEHOLDER, columnName)
                    .replace("IN ()", "IN (" + dataIdListString + ")");
        } else {
            sqlTemplate = sqlTemplate.replace(DATA_ID_COLUMN_PLACEHOLDER, "1")
                    .replace("IN ()", "= 1");
        }

        return sqlTemplate;
    }

    private static String concatStringTypeColumnData(List<String> data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            stringBuilder.append("'").append(data.get(i)).append("'");
            if (i != data.size() - 1) {
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }

    private void doTenantInspect(Statement statement) {
        if (statement instanceof Select selectStatement) {
            PlainSelect plainSelect = selectStatement.getPlainSelect();
            plainSelect.setWhere(wrapWhereExpressionWithTenantCondition(plainSelect.getWhere()));
        } else if (statement instanceof Update updateStatement) {
            updateStatement.setWhere(wrapWhereExpressionWithTenantCondition(updateStatement.getWhere()));
        } else if (statement instanceof Delete deleteStatement) {
            deleteStatement.setWhere(wrapWhereExpressionWithTenantCondition(deleteStatement.getWhere()));
        } else if (statement instanceof Insert insertStatement) {
            addTenantValue(insertStatement);
        }
    }

    private void doDataPermissionInspect(Statement statement) {
        if (statement instanceof Select selectStatement) {
            PlainSelect plainSelect = selectStatement.getPlainSelect();
            addDataPermissionCondition(plainSelect);
        }
    }

    private TableInfo getTableInfo(Statement statement) {
        if (statement instanceof Select selectStatement) {
            FromItem fromItem = selectStatement.getPlainSelect().getFromItem();
            if (fromItem instanceof Table table) {
                return getTableInfo(table);
            }
        } else if (statement instanceof Update updateStatement) {
            return getTableInfo(updateStatement.getTable());
        } else if (statement instanceof Delete deleteStatement) {
            return getTableInfo(deleteStatement.getTable());
        } else if (statement instanceof Insert insertStatement) {
            return getTableInfo(insertStatement.getTable());
        }
        log.error("Unsupported SQL statement: {}", statement);
        throw new IllegalArgumentException("Unsupported SQL statement");
    }

    private TableInfo getTableInfo(Table table) {
        return new TableInfo(table.getName(), table.getAlias() != null ? table.getAlias().getName() : table.getName());
    }

    private Expression wrapWhereExpressionWithTenantCondition(Expression originalWhere) {
        Column column = new Column(TENANT_ID_COLUMN_PLACEHOLDER);
        Expression tenantExpression = new EqualsTo(column, new StringValue(TENANT_ID_VALUE_PLACEHOLDER));
        if (originalWhere == null) {
            return tenantExpression;
        } else {
            return new AndExpression(new Parenthesis(originalWhere), tenantExpression);
        }
    }

    private void addTenantValue(Insert insertStatement) {
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
                    expressions.add(new StringValue(TENANT_ID_VALUE_PLACEHOLDER));
                }

                // Ensure the columns and expressions lists are of the same size for each row
                if (columns.size() != expressions.size()) {
                    throw new IllegalStateException("The number of columns and values do not match.");
                }
            }
        } else {
            List<Expression> expressions = (ExpressionList<Expression>) itemList;

            if (!tenantIdPresent) {
                expressions.add(new StringValue(TENANT_ID_VALUE_PLACEHOLDER));
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
        Column column = new Column(DATA_ID_COLUMN_PLACEHOLDER);
        // Use 'IN ()' as a placeholder
        Expression expression = new InExpression(column, new Parenthesis(new ExpressionList<>()));

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(expression);
        } else {
            Parenthesis originalWhere = new Parenthesis(plainSelect.getWhere());
            plainSelect.setWhere(new AndExpression(originalWhere, expression));
        }
    }

    private record SqlInfo(String tableName, String tableAlias, String sqlTemplate) {
    }

    private record TableInfo(String tableName, String alias) {
    }

}
