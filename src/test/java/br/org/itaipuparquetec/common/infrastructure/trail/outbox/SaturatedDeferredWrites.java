package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.concurrent.RejectedExecutionException;

public final class SaturatedDeferredWrites implements DeferredWrites {
   public void submit(Runnable write) {
      throw new RejectedExecutionException("queue is full");
   }
}
