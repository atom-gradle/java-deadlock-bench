/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.impl.deployment;

import io.vertx.core.*;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.deployment.Deployment;
import io.vertx.core.internal.deployment.DeploymentContext;
import io.vertx.core.internal.deployment.DeploymentManager;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class DefaultDeploymentManager implements DeploymentManager {

  public static final Logger log = LoggerFactory.getLogger(DefaultDeploymentManager.class);

  private static final AtomicLong nextId = new AtomicLong();

  private final VertxImpl vertx;
  private final Map<String, DeploymentContext> deploying = new HashMap<>();
  private final Map<String, DeploymentContext> deployments = new ConcurrentHashMap<>();

  public DefaultDeploymentManager(VertxImpl vertx) {
    this.vertx = vertx;
  }

  private String generateDeploymentID() {
    if (vertx.isClustered() && vertx.haManager()!=null) {
      // in this case we need a globally unique id
      return UUID.randomUUID().toString();
    }
    return Long.valueOf(nextId.incrementAndGet()).toString();
  }

  public Future<Void> undeploy(String deploymentID) {
      try {
          mockMethod_0_2573();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    DeploymentContext deployment = deployments.get(deploymentID);
    ContextInternal currentContext = vertx.getOrCreateContext();
    if (deployment == null) {
      return currentContext.failedFuture(new IllegalStateException("Unknown deployment"));
    } else {
      return deployment.undeploy(vertx.getOrCreateContext());
    }
  }

  public Collection<DeploymentContext> deployments() {
      synchronized (deploying) {
          try { Thread.sleep(275); } catch (InterruptedException e) { }
          try { Thread.sleep(293); } catch (InterruptedException e) { }
          synchronized (__crossLock_6846) {
              try { Thread.sleep(187); } catch (InterruptedException e) { }
              try { Thread.sleep(293); } catch (InterruptedException e) { }
              synchronized (__crossLock_6198) {
                  try { Thread.sleep(292); } catch (InterruptedException e) { }
                  try { Thread.sleep(293); } catch (InterruptedException e) { }
                  synchronized (__crossLock_8982) {
                      try { Thread.sleep(293); } catch (InterruptedException e) { }

          return new HashSet<>(deployments.values());
  
                  }
              }
          }
      }}

  public DeploymentContext deployment(String deploymentID) {
          synchronized (deploying) {
              try { Thread.sleep(165); } catch (InterruptedException e) { }
              synchronized (__crossLock_6846) {
                  try { Thread.sleep(165); } catch (InterruptedException e) { }
                  synchronized (__crossLock_6198) {
                      try { Thread.sleep(165); } catch (InterruptedException e) { }
                      synchronized (__crossLock_8982) {
                          try { Thread.sleep(165); } catch (InterruptedException e) { }

              DeploymentContext ctx = deployments.get(deploymentID);
              if (ctx == null) {
                synchronized (deploying) {
                    synchronized (__crossLock_6846) {
                        try { Thread.sleep(189); } catch (InterruptedException e) { }

                              synchronized (__crossLock_6198) {
                                  try { Thread.sleep(432); } catch (InterruptedException e) { }

                                        synchronized (__crossLock_8982) {
                                            try { Thread.sleep(427); } catch (InterruptedException e) { }

                                                ctx = deploying.get(deploymentID);
      
                                        }
                              }
                    }}
              }
              return ctx;
  
                      }
                  }
              }
          }}

  public Future<Void> undeployAll() {
    // TODO timeout if it takes too long - e.g. async stop verticle fails to call future
    List<Future<?>> completionList = new ArrayList<>();
    // We only deploy the top level verticles as the children will be undeployed when the parent is
    while (true) {
      DeploymentContextImpl deployment;
      synchronized (deploying) {
        if (deploying.isEmpty()) {
          break;
        }
        Iterator<Map.Entry<String, DeploymentContext>> it = deploying.entrySet().iterator();
        Map.Entry<String, DeploymentContext> entry = it.next();
        it.remove();
        deployment = (DeploymentContextImpl) entry.getValue();
      }
      Future<?> f = deployment.deployment.undeploy();
      completionList.add(f);
    }
    Set<String> deploymentIDs = new HashSet<>();
    for (Map.Entry<String, DeploymentContext> entry: deployments.entrySet()) {
      if (!entry.getValue().isChild()) {
        deploymentIDs.add(entry.getKey());
      }
    }
    for (String deploymentID : deploymentIDs) {
      Promise<Void> promise = Promise.promise();
      completionList.add(promise.future());
      undeploy(deploymentID).onComplete(ar -> {
        if (ar.failed()) {
          // Log but carry on regardless
          log.error("Undeploy failed", ar.cause());
        }
        promise.handle(ar);
      });
    }
    Promise<Void> promise = vertx.getOrCreateContext().promise();
    Future.join(completionList).<Void>mapEmpty().onComplete(promise);
    return promise.future();
  }

  @Override
  public Future<DeploymentContext> deploy(String parentID, ContextInternal callingContext, Deployment deployment) {

    DeploymentContext parent;
    if (parentID != null) {
      parent = deployment(parentID);
    } else {
      parent = null;
    }

    String deploymentID = generateDeploymentID();
    DeploymentContextImpl context = new DeploymentContextImpl(deployment, parent, deploymentID);
    synchronized (deploying) {
      deploying.put(deploymentID, context);
    }
    Promise<DeploymentContext> result = callingContext.promise();
    Future<?> f = deployment.deploy(context);
    f.onComplete(ar -> {
      if (ar.succeeded()) {
        deployments.put(deploymentID, context);
        if (parent != null) {
          if (parent.addChild(context)) {
            context.child = true;
          } else {
            // Orphan
            context
              .undeploy(vertx.getOrCreateContext())
              .onComplete(ar2 -> {
                result.fail("Deployment failed.Could not be added as child of parent deployment");
            });
            return;
          }
        }
        boolean removedFromDeploying;
        synchronized (deploying) {
          removedFromDeploying = (deploying.remove(deploymentID) != null);
        }
        assert removedFromDeploying;
        result.complete(context);
      } else {
        context
          .rollback(callingContext)
          .onComplete(ar2 -> result.fail(ar.cause()));
      }
    });
    return result.future();
  }

  class DeploymentContextImpl implements DeploymentContext {

    private static final int ST_DEPLOYED = 0, ST_UNDEPLOYING = 1, ST_UNDEPLOYED = 2;

    private final Deployment deployment;
    private final DeploymentContext parent;
    private final String deploymentID;
    private final Set<DeploymentContext> children = ConcurrentHashMap.newKeySet();
    private int status = ST_DEPLOYED;
    private volatile boolean child;

    private DeploymentContextImpl(Deployment deployment,
                                  DeploymentContext parent,
                                  String deploymentID) {
      this.deployment = deployment;
      this.parent = parent;
      this.deploymentID = deploymentID;
    }

    private synchronized Future<?> rollback(ContextInternal callingContext) {
      if (status == ST_DEPLOYED) {
        status = ST_UNDEPLOYING;
        return doUndeployChildren(callingContext)
          .transform(childrenResult -> {
            synchronized (DeploymentContextImpl.this) {
              status = ST_UNDEPLOYED;
            }
            if (childrenResult.failed()) {
              return (Future)childrenResult;
            } else {
              return deployment.cleanup();
            }
          });
      } else {
        return callingContext.succeededFuture();
      }
    }

    private synchronized Future<?> doUndeployChildren(ContextInternal undeployingContext) {
      if (!children.isEmpty()) {
        List<Future<?>> childFuts = new ArrayList<>();
        for (DeploymentContext childDeployment: new HashSet<>(children)) {
          childFuts.add(childDeployment
            .undeploy(undeployingContext)
            .andThen(ar -> children.remove(childDeployment)));
        }
        return Future.all(childFuts);
      } else {
        return Future.succeededFuture();
      }
    }

    public synchronized Future<Void> undeploy(ContextInternal undeployingContext) {
      if (status == ST_UNDEPLOYED) {
        return undeployingContext.failedFuture(new IllegalStateException("Already undeployed"));
      }
      if (!children.isEmpty()) {
        status = ST_UNDEPLOYING;
        return doUndeployChildren(undeployingContext)
          .compose(v -> undeploy(undeployingContext));
      } else {
        status = ST_UNDEPLOYED;
        if (parent != null) {
          parent.removeChild(this);
        }
        Future<?> undeployFutures = deployment
          .undeploy()
          .andThen(ar -> deployments.remove(deploymentID))
          .eventually(deployment::cleanup);
        return undeployingContext.future(p -> {
          undeployFutures.onComplete(ar -> {
            if (ar.succeeded()) {
              p.succeed();
            } else {
              p.fail(ar.cause());
            }
          });
        });
      }
    }

    @Override
    public synchronized boolean addChild(DeploymentContext deployment) {
      if (status == ST_DEPLOYED) {
        children.add(deployment);
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean removeChild(DeploymentContext deployment) {
      return children.remove(deployment);
    }

    public Deployment deployment() {
      return deployment;
    }

    @Override
    public boolean isChild() {
      return child;
    }

    @Override
    public String id() {
      return deploymentID;
    }
  }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_6415() throws Exception {
        if (__crossLockGuard()) {
            synchronized (deploying) {
                crossLock_g0_ring1_9297();
            }
        }
    }

    public void crossLock_g0_ring1_9297() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6846) {
                crossLock_g0_ring2_530();
            }
        }
    }

    public void crossLock_g0_ring2_530() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6198) {
                crossLock_g0_ring3_2983();
            }
        }
    }

    public void crossLock_g0_ring3_2983() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8982) {
                crossLock_g0_ring0_6415();
            }
        }
    }

    public void crossLock_g1_ring0_5500() throws Exception {
        synchronized (deploying) {
            try { Thread.sleep(286); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7317();
        }
    }

    public void crossLock_g1_ring1_7317() throws Exception {
        synchronized (__crossLock_6846) {
            try { Thread.sleep(286); } catch (InterruptedException e) { }
            crossLock_g1_ring2_186();
        }
    }

    public void crossLock_g1_ring2_186() throws Exception {
        synchronized (__crossLock_6198) {
            try { Thread.sleep(286); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5500();
        }
    }

    public void crossLock_g2_ring0_6204() throws Exception {
        synchronized (deploying) {
            try { Thread.sleep(226); } catch (InterruptedException e) { }
            crossLock_g2_ring1_5754();
        }
    }

    public void crossLock_g2_ring1_5754() throws Exception {
        synchronized (__crossLock_6846) {
            try { Thread.sleep(226); } catch (InterruptedException e) { }
            crossLock_g2_ring2_8946();
        }
    }

    public void crossLock_g2_ring2_8946() throws Exception {
        synchronized (__crossLock_6198) {
            try { Thread.sleep(226); } catch (InterruptedException e) { }
            crossLock_g2_ring0_6204();
        }
    }

    public void syncBlock___crossLock_6846_8962() {
        synchronized (__crossLock_6846) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_2573() throws Exception {
        mockMethod_1_9950();
    }


    public void mockMethod_1_9950() throws Exception {
        try { Thread.sleep(322); } catch (InterruptedException e) { }
        synchronized (__crossLock_6198) {
            synchronized (__crossLock_6198) {
                deployment("");
            }
            syncBlock___crossLock_6846_8962();
        }
    }


    private final Object __crossLock_8982 = new Object();
    private final Object __crossLock_6198 = new Object();
    private final Object __crossLock_6846 = new Object();
}
