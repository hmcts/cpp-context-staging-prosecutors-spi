package uk.gov.moj.cpp.staging.prosecutors.spi.event;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.activiti.cdi.CdiJtaProcessEngineConfiguration;
import org.activiti.cdi.spi.ProcessEngineLookup;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public class ProgrammaticProcessEngineConfiguration implements ProcessEngineLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgrammaticProcessEngineConfiguration.class.getCanonicalName());

    private ProcessEngine processEngine;


    @Override
    public ProcessEngine getProcessEngine() {
        final CdiJtaProcessEngineConfiguration cdiJtaProcessEngineConfiguration = new CdiJtaProcessEngineConfiguration();
        cdiJtaProcessEngineConfiguration.setTransactionManager(getTransactionManager());
        processEngine = cdiJtaProcessEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setDataSourceJndiName("java:/DS.stagingprosecutorsspi").setDatabaseType("postgres")
                .setHistoryLevel(HistoryLevel.NONE)
                .setTransactionsExternallyManaged(true).setJobExecutorActivate(true)
                .setAsyncExecutorEnabled(true)
                .setAsyncExecutorActivate(true)
                .setClassLoader(Thread.currentThread().getContextClassLoader())
                .buildProcessEngine();
        return processEngine;
    }

    @Override
    public void ungetProcessEngine() {
        processEngine.close();
    }

    @Override
    public int getPrecedence() {
        return 0;
    }


    public ManagedThreadFactory getThreadFactory() {
        try {
            return (ManagedThreadFactory) new InitialContext().lookup("java:jboss/ee/concurrency/factory/default");
        } catch (NamingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public TransactionManager getTransactionManager() {
        try {
            return (TransactionManager) new InitialContext().lookup("java:/TransactionManager");
        } catch (NamingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
