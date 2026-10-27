# java-deadlock-bench

[![Dataset](https://img.shields.io/badge/Dataset-Request_Access-4285F4?logo=googledrive&logoColor=white)](https://forms.gle/U73oakGxk3YrmNyu9)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Artifact and dataset for the paper *"Nocturne: A Reproducible Mutation Pipeline and Empirical Study of Statistical Models versus GNNs for Java Deadlock Triage"*.
It contains the training/evaluation code for all model families evaluated in the paper (classical ML, HeteroGNN),
together with the dataset construction pipeline scripts for every configuration.

## 🔍 Pipeline & Overview
Mutation Engine -> Static Parser -> Pipeliner -> (dataset) -> Evaluation & Comparison(Baseline/Rule-LR/LR/Random Forest/SVM/XGBoost/HeteroGNN)

---

## 🗂️ Repository layout

```
aggregated/                 Outputs of `aggregate_seeds.py`
dataset/                    Nocturne benchmark (8 .txt files)
output/                     Outputs of 10 experiments (the three with "abl" suffix include GNN ablation study)
projects/                   The eight mutated projects (see The open-source projects we mutated)
aggregate_seeds.py          Aggregates multi-seed summary JSONs, computes metric statistics, and runs paired significance tests comparing HeteroGNN to baselines.
mutation_generator.py       Performs lock-related mutations on Java projects to generate reproducible samples.
Parser.java                 Analyzes Java programs (.class) and export CYCLEs using Static Analysis.
pipeliner.py                Automatically runs Parser.java, merges output (.txt) into one single txt file and then label each CYCLE
evaluate.py                 ML (LogReg, SVM, Random Forest, XGBoost) vs. GNN, ablation study
compareGNNs.py              Runs comparisons between GIN, GCN, GAT, GraphSAGE (binary_strong regime, seed 42)
run15.sh                    Automatically runs 5 * 3 = 15 regime-seed evaluations
run3ablation.sh             Automatically runs 3 evaluations including GNN ablation study under seeds 42-44
```

---


## 📊 Running the paper's experiment configurations

Major experiments run 5 seeds (42-46) over the same fixed split. Please ensure that `evaluate.py` and `dataset/`(directory) are placed in the same directory.

```bash
nohup bash run15.sh > run15.log 2>&1 &
nohup bash run3ablation.sh > run3ablation.log 2>&1 &
```

If you want to run one single evaluation, please use the following command:

```bash
python evaluate.py --label_mode binary_strong
python evaluate.py --label_mode binary_loose
python evaluate.py --label_mode three_way
python evaluate.py --label_mode binary_strong --do_gnn_ablation
python evaluate.py --label_mode binary_strong --seed YOUR_SEED --do_gnn_ablation
```

> [!NOTE]
> Exact values may vary by GPU and run.

## 🚀 Detailed usage instructions for Mutation Engine and Static Parser

### mutation_generator.py
Perform lock-related mutations on Java projects to generate reproducible samples.

**How to use?**

```bash
python mutation_generator.py --working-dir "YOUR_WORKING_DIR" --retain-all-classes
```

Note: `YOUR_WORKING_DIR` is a directory containing all Java projects you want to mutate. For example, the directory can be like

```
-Projects
|-reactor-master
|-guava-master
|-commons-collections-master
```

Note: `mutation_generator.py` only detects "standard src directory structure" in Java projects. If you want to mutate projects like Agrona, JCTools, which doesn't directly owns a directory named "src",
or "src" is not directly settled in project root folder, please manually add the path to the script.

If you want to retain all original classes in Java project for convenience in replacement and compile, then add this param `--retain-all-classes`

```bash
python mutation_generator.py --working-dir "YOUR_WORKING_DIR" --retain-all-classes
```

---

### Parser.java

**How to use?**
1. compile
```bash
javac -cp "*" Parser.java
```

2. update(override) classes in [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/)
```bash
jar uf tai-e-0.5.4.jar Parser.class Parser$HeldLock.class Parser$LockAcquisition.class Parser$LockOrderEdge.class
```

3. extract tai-e-analyses.yml from tai-e-0.5.4.jar and modify tai-e-analyses.yml to enable features for our analysis
```bash
jar xf tai-e-0.5.4.jar tai-e-analyses.yml
```

modify the following attributes:

**from**

```
cs: ci # | k-[obj|type|call][-k'h]
only-app: false # only analyze application code
handle-invokedynamic: false
plugins: # | [ pluginClass, ... ]
```

**to**

```
cs: 1-call # | k-[obj|type|call][-k'h]
only-app: true # only analyze application code
handle-invokedynamic: true
plugins: [ Parser ] # | [ pluginClass, ... ]
```

4. run

 See **pipeliner.py**

---

### pipeliner.py

**How to use?**
```bash
python pipeliner.py --input-dir "YOUR_INPUT_DIR"
```

---

## 📦 Dataset

`dataset/` contains the eight benchmark artifacts consumed by the code.

The dataset consists of 4170 labeled cycles, using fixed seed 42.

Total: 4170
L2(strong): 2590
L1(weak): 1243
L0(negative): 337

---

## 📂 The open-source projects we mutated
| Project & Link | Version |
|-------|-------|
| [Apache Commons Collections](https://github.com/apache/commons-collections) | 4.6.0 |
| [Apache Commons IO](https://github.com/apache/commons-io) | 2.22.0 |
| [Apache Commons Pool](https://github.com/apache/commons-pool) | 3.0.0 |
| [Guava](http://guava.dev/releases/32.1.2-jre/api/docs/) | 32.1.2-jre |
| [H2 Database](https://github.com/h2database/h2database) | 2.5.252 |
| [Reactor Core](https://github.com/reactor/reactor-core) | 3.8.7 |
| [Rapidoid](https://github.com/rapidoid/rapidoid) | 5.1.0 |
| [Vert.x Core](https://github.com/eclipse-vertx/vert.x) | 5.2.2 |

---

## 💐 Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.

`Parser.java` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).

Here we extend sincere gratitude to Tian Tan and Yue Li from Nanjing University who developed Tai-E the excellent framework.
