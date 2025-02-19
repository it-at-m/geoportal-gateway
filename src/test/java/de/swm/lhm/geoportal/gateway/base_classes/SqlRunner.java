package de.swm.lhm.geoportal.gateway.base_classes;

import de.swm.lhm.geoportal.gateway.base_classes.config.AdminManagerH2DbConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;


@ActiveProfiles("h2db")
@ExtendWith({SpringExtension.class})
@Import({AdminManagerH2DbConfig.class})
public abstract class SqlRunner extends FileLoader {

    @Autowired
    protected DatabaseClient databaseClient;

    protected void runSql(String... sqls) {
        Arrays.stream(sqls).forEach(this::runSql);

    }

    protected void runSql(String sql) {

        Arrays.stream(sql.split(";"))
                .forEach(statement -> {
                    statement = statement + ";";
                    databaseClient.sql(statement)
                            .fetch()
                            .one()
                            .block();
                });

    }

}
