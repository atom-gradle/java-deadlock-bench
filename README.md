# java-deadlock-bench

This repository provides the whole framework and tools for deadlock bench, including components like MutationGenerator, Parser and other scripts used in the paper.

## Pipeline
MutationGenerator -> Java Parser -> Pipneliner -> (dataset) -> evaluation(baseling/ML/GNN)

---

### MutationGenerator.py
Perform lock-related mutations on plain Java projects to generate reproducible samples.

Functions:
- Find classes containing lock fields / synchronized blocks
- Replace the lock variable inside synchronized(...)
- Reassign non-final lock fields
- Insert Thread.sleep inside synchronized blocks
- Add new synchronized blocks (only for Object-type lock fields)
- Generate same-class mock methods (nestable, throws Exception)
- Generate a harness for each class (top-level class, static methods
  invoked via the class name)

Supported Java features:
- new Thread(...) / Runnable anonymous class / Lambda
- Object / custom lock object / ReentrantLock

Not supported:
- wait / notify
- Other JUC utilities (ReadWriteLock, StampedLock, Semaphore, etc)

How to use?

```bash
python MutationGenerator.py --working-dir "YOUR_WORKING_DIR" --retain-all-classes
```

Note: `YOUR_WORKING_DIR` is a directory containing all Java projects you want to mutate. For example, the directory can be like

-JavaProjects
|-Disruptor
|-Agrona
|-RxJava

Here we chose `Apache Commons Collections`, `Apache Commons IO`, `Apache Commons Lang`, `Guava 32.1.2`, `Disruptor`, and `Jsoup` thanks to their clear project structure and few dependencies.
The program's output goes like this.

```
C:\Users\Admin\PycharmProjects\GNN-Ranker\.venv\Scripts\python.exe C:\Users\Admin\PycharmProjects\GNN-Ranker\MutationGenerator-dev.py 
[INFO] found 6 projects: ['commons-collections-master', 'commons-io-master', 'commons-lang-master', 'disruptor-master', 'guava-master', 'jsoup-master']
[INFO] commons-collections-master: found 367 java files in D:\Projects\commons-collections-master\src\main\java
creating harness: org.apache.commons.collections4.BagUtilsHarness
creating harness: org.apache.commons.collections4.CollectionUtilsHarness
creating harness: org.apache.commons.collections4.ListUtilsHarness
creating harness: org.apache.commons.collections4.MapUtilsHarness
creating harness: org.apache.commons.collections4.MultiSetUtilsHarness
creating harness: org.apache.commons.collections4.QueueUtilsHarness
creating harness: org.apache.commons.collections4.SetUtilsHarness
creating harness: org.apache.commons.collections4.bag.SynchronizedBagHarness
creating harness: org.apache.commons.collections4.bag.SynchronizedSortedBagHarness
creating harness: org.apache.commons.collections4.collection.SynchronizedCollectionHarness
creating harness: org.apache.commons.collections4.map.ConcurrentReferenceHashMapHarness
creating harness: org.apache.commons.collections4.map.StaticBucketMapHarness
creating harness: org.apache.commons.collections4.multiset.SynchronizedMultiSetHarness
creating harness: org.apache.commons.collections4.multiset.SynchronizedSortedMultiSetHarness
creating harness: org.apache.commons.collections4.properties.OrderedPropertiesHarness
creating harness: org.apache.commons.collections4.queue.SynchronizedQueueHarness
[INFO] commons-io-master: found 277 java files in D:\Projects\commons-io-master\src\main\java
creating harness: org.apache.commons.io.FileCleanerHarness
creating harness: org.apache.commons.io.FileCleaningTrackerHarness
creating harness: org.apache.commons.io.channels.ByteArraySeekableByteChannelHarness
creating harness: org.apache.commons.io.input.NullInputStreamHarness
creating harness: org.apache.commons.io.input.ProxyInputStreamHarness
creating harness: org.apache.commons.io.input.ProxyReaderHarness
creating harness: org.apache.commons.io.input.ReadAheadInputStreamHarness
creating harness: org.apache.commons.io.monitor.FileAlterationObserverHarness
creating harness: org.apache.commons.io.output.ByteArrayOutputStreamHarness
creating harness: org.apache.commons.io.output.LockableFileWriterHarness
[INFO] commons-lang-master: found 264 java files in D:\Projects\commons-lang-master\src\main\java
creating harness: org.apache.commons.lang3.CharSetHarness
creating harness: org.apache.commons.lang3.concurrent.LazyInitializerHarness
creating harness: org.apache.commons.lang3.concurrent.MultiBackgroundInitializerHarness
creating harness: org.apache.commons.lang3.concurrent.locks.LockingVisitorsHarness
creating harness: org.apache.commons.lang3.time.FastDateParserHarness
[INFO] disruptor-master: found 71 java files in D:\Projects\disruptor-master\src\main\java
creating harness: com.lmax.disruptor.BlockingWaitStrategyHarness
creating harness: com.lmax.disruptor.LiteBlockingWaitStrategyHarness
creating harness: com.lmax.disruptor.LiteTimeoutBlockingWaitStrategyHarness
creating harness: com.lmax.disruptor.TimeoutBlockingWaitStrategyHarness
[INFO] guava-master: found 611 java files in D:\Projects\guava-master\guava\src
creating harness: com.google.common.base.EnumsHarness
creating harness: com.google.common.base.SuppliersHarness
creating harness: com.google.common.cache.LocalCacheHarness
creating harness: com.google.common.collect.MapsHarness
creating harness: com.google.common.collect.MultimapsHarness
creating harness: com.google.common.collect.QueuesHarness
creating harness: com.google.common.collect.SetsHarness
creating harness: com.google.common.collect.SynchronizedHarness
creating harness: com.google.common.collect.TablesHarness
creating harness: com.google.common.eventbus.SubscriberHarness
creating harness: com.google.common.util.concurrent.AbstractScheduledServiceHarness
creating harness: com.google.common.util.concurrent.AbstractServiceHarness
creating harness: com.google.common.util.concurrent.ClosingFutureHarness
creating harness: com.google.common.util.concurrent.CycleDetectingLockFactoryHarness
creating harness: com.google.common.util.concurrent.DirectExecutorServiceHarness
creating harness: com.google.common.util.concurrent.ExecutionListHarness
creating harness: com.google.common.util.concurrent.InterruptibleTaskHarness
creating harness: com.google.common.util.concurrent.LazyLoggerHarness
creating harness: com.google.common.util.concurrent.ListenerCallQueueHarness
creating harness: com.google.common.util.concurrent.MonitorHarness
creating harness: com.google.common.util.concurrent.RateLimiterHarness
creating harness: com.google.common.util.concurrent.SequentialExecutorHarness
creating harness: com.google.common.util.concurrent.ServiceManagerHarness
creating harness: com.google.common.util.concurrent.StripedHarness
[INFO] jsoup-master: found 95 java files in D:\Projects\jsoup-master\src\main\java
creating harness: org.jsoup.helper.HttpConnectionHarness
creating harness: org.jsoup.nodes.ElementHarness
creating harness: org.jsoup.parser.ParserHarness
[INFO] report written to ./mutated-1790993066738\mutation_report.json
[INFO] mutated 63 classes
```

Note: MutationGenerator.py only detects "src" directory in Java projects. If you want to mutate projects like Agrona, JCTools, which doesn't directly owns a directory called "src",
or "src" is not directly settled in project root folder.

If you want to retain all original classes in Java project for convinient replacement and compile, then add this param `--retain-all-classes`
```bash
python MutationGenerator.py --working-dir "YOUR_WORKING_DIR" --retain-all-classes
```

---

### Parser.java
Supported Java features:
- new Thread(Lambda Expression)
- synchronized blocks and methods
- Object / custom lock object(instance field)

### How to use?
1. compile
```bash
javac -cp "*" Parser.java
```

2. update(override) classes in [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/)
```bash
jar uf tai-e-0.5.4.jar Parser.class Parser$HeldLock.class Parser$LockAcquisition.class Parser$LockOrderEdge.class
```

3. extract tai-e-analyses.yml from tai-e-0.5.4.jar and modify tai-e-analyses.yml to enable features for our analysis
```
- description: whole-program pointer analysis
```

    **cs: ci # | k-[obj|type|call][-k'h]**
    **only-app: true # only analyze application code**


    **handle-invokedynamic: true**
    
    **plugins: [ MyPlugin ] # | [ pluginClass, ... ]**
```

4. run
See **Pipeliner.py**

---

### Pipeliner.py
This script automatically runs `ParserB.java` and then merges output(.txt) into one single txt file while automatically correcting the index of data into sequence.

How to use?
```bash
python Merger.py --input-dir "YOUR_INPUT_DIR"
```

---

### evaluate.py
Uniformly evaluate the performance of three models on the "mutation ground truth (LABEL_REAL)":

  1. Rule-based baseline (replicating the logic of `classifyCycle.isReal`)
  2. Random forest (graph-level statistical features)
  3. Heterogeneous GNN (heterogeneous graph structure + edge-weight features)

Unified evaluation protocol:
  - GroupKFold by `struct_hash` (the same structure does not span train/test)
  - The three models share the same fold split to ensure a fair comparison
  - Support binary classification (`label==2` vs `label==0`) and three-class classification (2 / 1 / 0)
  - Support ablation (removing cross-layer edges / `lock_edge` / `call_edge`)

---


---

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.

`Parser.java` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).

Here we extend sincere gratitude to Tian Tan and Yue Li from Nanjing University who developed Tai-E the excellent framework.
