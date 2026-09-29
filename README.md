# java-deadlock-bench

This repository provides the whole framework and tools for deadlock bench, including components like MutationGenerator, Parser and other scripts used in the paper.

## Pipeline
MutationGenerator -> Two Parsers -> Merger -> dataset -> evaluation(ML/GNN) -> training & comparision

### MutationGenerator.py
Perform lock-related mutations on plain Java projects to generate reproducible samples.

Supported:
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
python MutationGenerator.py --working-dir "YOUR_WORKING_DIR"
```
Note: `YOUR_WORKING_DIR` is a directory containing all Java projects you want to mutate. For example, the directory can be like 
-JavaProjects
|-Disruptor
|-Agrona
|-RxJava

Here we chose `Apache Collections`, `Disruptor`, `JCTools`, `Jsoup`, and `RxJava` thanks to their clear project structure and few dependencies.
The program's output goes like this.
```
[INFO] found 5 projects: ['commons-collections-master', 'disruptor-master', 'JCTools-master', 'jsoup-master', 'RxJava-4.x']
[INFO] commons-collections-master: found 370 java files in D:\Projects\commons-collections-master\src\main\java
[INFO] disruptor-master: found 71 java files in D:\Projects\disruptor-master\src\main\java
[INFO] JCTools-master: found 137 java files in D:\Projects\JCTools-master\jctools-core\src\main\java
[INFO] jsoup-master: found 98 java files in D:\Projects\jsoup-master\src\main\java
[INFO] RxJava-4.x: found 942 java files in D:\Projects\RxJava-4.x\src\main\java
[INFO] report written to ./mutated-1790643322161\mutation_report.json
[INFO] mutated 58 classes

Process finished, with exit code 0.
```

Note: MutationGenerator.py only detects "src" directory in Java projects. If you want to mutate projects like Agrona, JCTools, which doesn't directly owns a directory called "src",
or "src" is not directly settled in project root folder.

### ParserA.java
Specialized for the following Java features:
- new Thread(...) / Runnable anonymous class
- synchronized blocks and methods
- Object / custom lock object(static field) / ReentrantLock

### How to use?

1. Cloning https://github.com/pascal-lab/Tai-e-assignments to local and unzip
```bash
git clone https://github.com/pascal-lab/Tai-e-assignments.git
unzip Tai-e-assignments.zip
```

2. Replace Tai-e-assignments-main\A6\tai-e\src\main\java\pascal\taie\analysis\pta\cs\Solver.java with this file
```bash
cp ParserA.java /Tai-e-assignments-main/A6/tai-e/src/main/java/pascal/taie/analysis/pta/cs/
```

3. Delete original file and Rename this file to "Solver.java"
```bash
rm -rf Solver.java
rename ParserA.java Solver.java
```

4. run
```bash
java -cp "build\classes\java\main;lib\tai-e-assignment.jar;..\..\lib\dependencies.jar" pascal.taie.Assignment -cp src/test/resources/pta/cspta -m DeadlockSample
```

Note: `DeadlockSample` is a class file or harness you want to analyze (must contains **main()** entry method)

### ParserB.java
Supported Java features:
- new Thread(Lambda Expression)
- synchronized blocks and methods
- Object / custom lock object(instance field)

### How to use?
1. compile
```bash
javac -cp "*" ParserB.java
```

2. update(override) classes in [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/)
```bash
jar uf tai-e-0.5.4.jar ParserB.class ParserB$HeldLock.class ParserB$LockAcquisition.class ParserB$LockOrderEdge.class
```

3. extract tai-e-analyses.yml from tai-e-0.5.4.jar and modify tai-e-analyses.yml to enable features for our analysis
```
- description: whole-program pointer analysis
  analysisClass: pascal.taie.analysis.pta.PointerAnalysis
  id: pta
  options:
    cs: ci # | k-[obj|type|call][-k'h]
    only-app: true # only analyze application code
    implicit-entries: true # analyze implicit entries
    distinguish-string-constants: reflection # (distinguish reflection-relevant
    # string constants, i.e., class, method, and field names)
    # | null (do not distinguish any string constants)
    # | app (distinguish string constants in application code)
    # | all (distinguish all string constants)
    # | <predicate-class-name>
    merge-string-objects: true
    merge-string-builders: true
    merge-exception-objects: true
    handle-invokedynamic: true
    propagate-types: [ reference ] # ,null,int,long,...(other primitive types)
    advanced: null # specify advanced analysis:
    # zipper | zipper-e | zipper-e=PV
    # scaler | scaler=TST
    # mahjong | collection
    dump: false # whether dump points-to results (with contexts)
    dump-ci: false # whether dump points-to results (without contexts)
    dump-yaml: false # whether dump points-to results in yaml format
    expected-file: null # path of expected file for comparing results
    reflection-inference: string-constant # | solar | null
    reflection-log: null # path to reflection log, required when reflection option is log
    taint-config: null # path (of dir/file) to config file of taint analysis,
    taint-config-providers: [ ] # class names of taint config provider classes
    # when this path/provider is given, taint analysis will be enabled
    taint-interactive-mode: false # whether enable interactive mode for taint analysis
    spring: false # whether enable Spring Framework analysis (DI + WEC)
    plugins: [ MyPlugin ] # | [ pluginClass, ... ]
    time-limit: -1 # set time limit (in seconds) for pointer analysis,
    # -1 means no time limit
```

4. run
```bash
java -cp "*" 
```


### Merger.py
This script merges output(.txt) from ParserA and ParserB into one single txt file. It automatically correct the index of data into sequence.

How to use?
```bash
python Merger.py --input-dir "YOUR_INPUT_DIR"
```

### train.py
This is a PyTorch Geometric script for heterogeneous GNN-based deadlock ranking, using two node types (lock and acq) and four edge types.

1. Data Parsing (parse_graph_file, load_all_graphs)
Reads graph-*.txt files and converts each CYCLE block into a HeteroData object.

Node types:
lock — 4-dim features: [is_static, 1, 1, pts_size]
acq — 4-dim features: [is_static, is_sync, nesting, pts_size]
Edge types (4 total):
('lock','lock_edge','lock') — 4-dim attrs (same_class, call_distance, may_alias, same_lock_obj)
('acq','call_edge','acq') — 4-dim attrs (same_class, call_distance, same_thread, is_recursive)
('acq','acq_to_lock','lock') — 1-dim constant edge attr
('lock','lock_to_acq','acq') — 1-dim constant edge attr
Each graph gets a binary label y (cycle present or not) and a cycle_id.
Empty lines/comments are skipped without triggering a flush; a new CYCLE tag flushes the previous block.

2. Model (HeteroGINClassifier)
Linear encoders project lock and acq node features into a shared hidden_dim.
Separate edge encoders for the three distinct edge feature dimensions; edge features are aggregated into source node embeddings via index_add.
num_layers of HeteroConv:
GINConv for intra-type edges (lock→lock, acq→acq)
SAGEConv for cross-type edges (acq→lock, lock→acq)
Per-node-type LayerNorm (chosen over BatchNorm to handle empty node types) + ReLU.
Global sum pooling per node type, concatenated, then an MLP classifier producing a single logit.

3. Training / Evaluation
Loss: BCEWithLogitsLoss with pos_weight to handle class imbalance.
Optimizer: Adam + StepLR (step 30, gamma 0.5).
Metrics: accuracy, precision, recall, F1, AUC-ROC, MRR, Precision@K, Recall@K (K = 5, 10, 20).
Best model selected by validation F1; final evaluation on test set with per-sample probability printout.
Model and args saved to a .pt file.

4. Main (main)
Args: epochs, batch_size, lr, hidden_dim, num_layers, threshold, seed, save path.
Loads train/val/test dirs, prints label distributions, trains, evaluates, and saves the best model.

### evaluate.py
This script evaluates the quanlity of data and compares ML and GNN.
- Does not use CYCLE_FEATURES as model input (to avoid label leakage)
- Structure-based deduplication + structure-grouped LOSO cross-validation
- Rule-based baseline + statistical baseline + heterogeneous GNN comparison

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.

`ParserA.java` is adapted from Tai-e's [educational assignments 6(A6)](https://tai-e.pascal-lab.net/pa6.html), which are also licensed under LGPL 3.0.

`ParserB.java` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).

Here we extend sincere gratitude to Tan Tian and Yue Li from Nanjing University who developed Tai-E the excellent framework.
