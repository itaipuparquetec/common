package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

public final class FakeTransactionManager implements PlatformTransactionManager {
   private final List<Integer> propagationsBegun = new ArrayList();
   private int commits;
   private int rollbacks;

   public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
      this.propagationsBegun.add(definition.getPropagationBehavior());
      return new SimpleTransactionStatus();
   }

   public void commit(TransactionStatus status) {
      ++this.commits;
   }

   public void rollback(TransactionStatus status) {
      ++this.rollbacks;
   }

   public List<Integer> propagationsBegun() {
      return this.propagationsBegun;
   }

   public int commits() {
      return this.commits;
   }

   public int rollbacks() {
      return this.rollbacks;
   }
}
