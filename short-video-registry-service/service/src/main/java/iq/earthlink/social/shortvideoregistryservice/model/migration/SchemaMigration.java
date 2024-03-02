package iq.earthlink.social.shortvideoregistryservice.model.migration;

import java.io.Serializable;
import java.util.Date;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * This entity corresponds to the <code>schema_migration</code> table created by the
 * `cassandra-migration` library in order to keep track of the schema migrations already executed on
 * the Cassandra database.
 */
@Table(value = "schema_migration")
public class SchemaMigration implements Serializable {

    private static final long serialVersionUID = 6691482812022511000L;

    @PrimaryKey
    private SchemaMigrationKey schemaMigrationKey;

    @Column("executed_at")
    private Date executedAt;

    @Column("script_name")
    private String scriptName;

    @Column("script")
    private String script;

    public SchemaMigrationKey getSchemaMigrationKey() {
        return schemaMigrationKey;
    }

    public void setSchemaMigrationKey(SchemaMigrationKey schemaMigrationKey) {
        this.schemaMigrationKey = schemaMigrationKey;
    }

    public Date getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Date executedAt) {
        this.executedAt = executedAt;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
