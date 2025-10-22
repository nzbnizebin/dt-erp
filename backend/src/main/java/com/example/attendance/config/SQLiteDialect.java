package com.example.attendance.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.*;

public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super();
    }

    public SQLiteDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl();
    }

    @Override
    public boolean supportsTemporaryTables() {
        return true;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }


    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return (sqlException, message, sql) -> null;
    }

    /**
     * Hibernate 6.4 的正确扩展点：注册 JDBC 类型映射。
     * 这里把 BIGINT（Long）映射为 INTEGER，以符合 SQLite 实际类型。
     */
    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        var registry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

        // 数值
        registry.addDescriptor(SqlTypes.INTEGER, IntegerJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.BIGINT, IntegerJdbcType.INSTANCE); // ✅ Long → INTEGER
        registry.addDescriptor(SqlTypes.FLOAT, FloatJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.REAL, RealJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.DOUBLE, DoubleJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.NUMERIC, DecimalJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.DECIMAL, DecimalJdbcType.INSTANCE);

        // 字符串
        registry.addDescriptor(SqlTypes.CHAR, CharJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.VARCHAR, VarcharJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.LONGVARCHAR, LongVarcharJdbcType.INSTANCE);

        // 时间
        registry.addDescriptor(SqlTypes.DATE, DateJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.TIME, TimeJdbcType.INSTANCE);
        registry.addDescriptor(SqlTypes.TIMESTAMP, TimestampJdbcType.INSTANCE);

        // 二进制
        registry.addDescriptor(SqlTypes.BLOB, BlobJdbcType.DEFAULT);

        // 布尔
        registry.addDescriptor(SqlTypes.BOOLEAN, IntegerJdbcType.INSTANCE);
    }

    /**
     * Hibernate 6.4 没有 getTypeName，但我们可以告诉它默认 SQL 名称。
     * 通过 registerKeyword("integer") 等方式帮助 Hibernate 推断 DDL。
     */
    {
        registerKeyword("integer");
        registerKeyword("real");
        registerKeyword("text");
        registerKeyword("blob");
    }
}
