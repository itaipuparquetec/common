package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

public final class ImmediateDeferredWrites implements DeferredWrites {
   public void submit(Runnable write) {
      write.run();
   }
}
