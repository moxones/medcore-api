package com.medical.medcore.shared.tenancy;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String APPLY_CONTEXT = """
            SELECT set_config('app.current_tenant_id', ?, false),
                   set_config('app.current_user_id', ?, false),
                   set_config('app.rls_bypass', ?, false)
            """;

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return applyContext(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return applyContext(super.getConnection(username, password));
    }

    private Connection applyContext(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(APPLY_CONTEXT)) {
            statement.setString(1, asText(TenantContext.getTenantId()));
            statement.setString(2, asText(TenantContext.getCurrentUserId()));
            statement.setString(3, TenantContext.isCrossTenant() ? "on" : "off");
            statement.execute();
            return connection;
        } catch (SQLException failure) {
            connection.close();
            throw failure;
        }
    }

    private static String asText(Long value) {
        return value != null ? value.toString() : "";
    }
}
