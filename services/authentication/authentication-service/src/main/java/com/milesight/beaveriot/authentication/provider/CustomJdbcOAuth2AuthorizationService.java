package com.milesight.beaveriot.authentication.provider;

import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

import java.sql.Types;
import java.util.Date;

/**
 * @author loong
 * @date 2024/10/29 10:16
 */
public class CustomJdbcOAuth2AuthorizationService extends JdbcOAuth2AuthorizationService implements CustomOAuth2AuthorizationService {

    private static final String TABLE_NAME = "oauth2_authorization";

    private static final String PRINCIPAL_FILTER = "principal_name = ?";
    private static final String REMOVE_PRINCIPAL_SQL = "DELETE FROM " + TABLE_NAME + " WHERE " + PRINCIPAL_FILTER;

    private static final String ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at < ?";
    private static final String REMOVE_EXPIRED_ACCESS_TOKEN_PRINCIPAL_SQL = "DELETE FROM " + TABLE_NAME + " WHERE " + PRINCIPAL_FILTER + " AND " + ACCESS_TOKEN_EXPIRES_AT;

    public CustomJdbcOAuth2AuthorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {
        super(jdbcOperations, registeredClientRepository);
    }

    @Override
    public void removeByPrincipalName(String principalName) {
        //FIXME Temporarily allow the same user to generate multiple valid tokens
        Assert.notNull(principalName, "principalName cannot be null");
//        SqlParameterValue[] parameters = new SqlParameterValue[] {
//                new SqlParameterValue(Types.VARCHAR, principalName)
//        };
//        PreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters);
//        getJdbcOperations().update(REMOVE_PRINCIPAL_SQL, pss);

        SqlParameterValue[] parameters = new SqlParameterValue[]{
                new SqlParameterValue(Types.VARCHAR, principalName),
                new SqlParameterValue(Types.TIMESTAMP, new Date())
        };
        PreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters);
        getJdbcOperations().update(REMOVE_EXPIRED_ACCESS_TOKEN_PRINCIPAL_SQL, pss);
    }

}
