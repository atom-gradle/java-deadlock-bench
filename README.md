# java-deadlock-bench

This repository provides the whole framework and pipeline for deadlock bench, including components like MutationGenerator, Parser and other scripts used in the paper.

## Stages & Pipeline
MutationGenerator -> Parsers -> Merger -> dataset -> GNN training

### MutationGenerator.py
Perform lock-related mutations on plain Java projects to generate
reproducible samples.

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
- Other JUC utilities (ReadWriteLock, StampedLock, Semaphore, etc.)

### ParserA
Supported Java features:
- new Thread(...) / Runnable anonymous class
- Object / custom lock object(static field) / ReentrantLock

### ParserB.java
Supported Java features:
- new Thread(Lambda Expression)
- Object / custom lock object(instance field)

### Merger.py
merges output from ParserA and ParserB into one single txt file

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

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.
The parser in `parser-a/` is adapted from Tai-e's [educational assignments 6(A6)](https://tai-e.pascal-lab.net/pa6.html), which are also licensed under LGPL 3.0.
The parser in `parser-b/` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).
