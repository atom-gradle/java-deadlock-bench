# java-deadlock-bench

[![Dataset](https://img.shields.io/badge/Dataset-Request_Access-4285F4?logo=googledrive&logoColor=white)](https://forms.gle/U73oakGxk3YrmNyu9)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Artifact for the paper *"Nocturne: A Reproducible Mutation Pipeline and Empirical Comparison of Statistical Models versus GNNs for Java Deadlock Triage"*.
It contains the training/evaluation code for all model families evaluated in the paper (classical ML, HeteroGNN),
together with the dataset construction pipeline and scripts for every configuration.

## Pipeline
Mutation Engine -> Static Parser -> Pipeliner -> (dataset) -> Evaluation & Comparison(Baseling/LR/Random Forest/SVM/XGBoost/HeteroGNN)

---

## 🗂️ Repository layout

```
dataset/                    Nocturne benchmark (.txt files)
Parser.java                 Static Parser which parses and exports candidate cycles
evaluate.py                 ML baselines (RF, XGBoost, LogReg, SVM) vs. GNN
Pipeliner.py                dataset loading, splits, resampling
run_all.sh                  automatically runs 5 * 3 = 15 regime-seed evaluations
docker/                     (todo) Docker run scripts, one per model family (see docker/README.md)
Dockerfile                  (todo) container image definition
```

## 🚀 Getting started

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

Note: mutation_generator.py only detects "src" directory in Java projects. If you want to mutate projects like Agrona, JCTools, which doesn't directly owns a directory called "src",
or "src" is not directly settled in project root folder.

If you want to retain all original classes in Java project for convinient replacement and compile, then add this param `--retain-all-classes`
```bash
python mutation_generator.py --working-dir "YOUR_WORKING_DIR" --retain-all-classes
```

---

### Parser.java
Using Static Analysis to analyze Java programs and export CYCLEs.

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
plugins: [ MyPlugin ] # | [ pluginClass, ... ]
```

4. run

 See **Pipeliner.py**

---

### Pipeliner.py
This script automatically runs `Parser.java` and then merges output(.txt) into one single txt file while automatically correcting the index of data into sequence.

**How to use?**
```bash
python Pipeliner.py --input-dir "YOUR_INPUT_DIR"
```

---

## 📦 Dataset

`dataset/` contains the eight benchmark artifacts consumed by the code.

The dataset consists of 4,096 labeled cycles (1,749 unique structures after merging), using fixed seed 42.
Total: 4096
L2(Strong): 1939
L1(weak): 1002
L0(negative): 1155

---

## 📊 Running the paper's experiment configurations

All experiments run 5 seeds (42-46) over the same fixed split.
```bash
./run_all.sh
```

If you want to run one single evaluation, please use the following command:

```bash
python evaluate.py --label_mode three_way
python evaluate.py --label_mode binary_strong
python evaluate.py --label_mode binary_weak
python evaluate.py --label_mode three_way --do_gnn_ablation
python evaluate.py --label_mode three_way --seed YOUR_SEED --do_gnn_ablation
```

> [!NOTE]
> Exact values vary by GPU and run.

---

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.

`Parser.java` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).

Here we extend sincere gratitude to Tian Tan and Yue Li from Nanjing University who developed Tai-E the excellent framework.
