package com.ecom.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.SQLException;
import java.util.function.Supplier;

/** Retries only local database work. Never put HTTP or SOAP calls inside this callback. */
@Component
public class Transactions {
    private final TransactionTemplate tx;
    public Transactions(PlatformTransactionManager manager) {
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }
    public <T> T run(Supplier<T> work) {
        for(int attempt=0;;attempt++) {
            try { return tx.execute(s -> work.get()); }
            catch(RuntimeException e) {
                if(attempt>=4 || !serialization(e)) throw e;
                try { Thread.sleep((25L << attempt) + java.util.concurrent.ThreadLocalRandom.current().nextLong(25)); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw e; }
            }
        }
    }
    static boolean serialization(Throwable e) {
        for(Throwable t=e;t!=null;t=t.getCause()) if(t instanceof SQLException sql && "40001".equals(sql.getSQLState())) return true;
        return false;
    }
}
