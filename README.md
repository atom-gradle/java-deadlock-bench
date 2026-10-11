# java-deadlock-bench

This repository provides the whole framework and tools for deadlock bench, including components like MutationGenerator, Parser and other scripts used in the paper.

## Pipeline
Generate Mutation -> Java Parser -> Pipeliner -> (dataset) -> Evaluation & Comparison(Baseling/LR/Random Forest/SVM/XGBoost/HeteroGNN)

---

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
```sh
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

### evaluate.py
Uniformly evaluate the performance of three models on the "mutation ground truth (LABEL_REAL)":

**How to use?**
```bash
python evaluate.py --label_mode three_way
python evaluate.py --label_mode binary_strong
python evaluate.py --label_mode binary_weak
python evaluate.py --label_mode three_way --do_gnn_ablation
python evaluate.py --label_mode three_way --seed 2026 --do_gnn_ablation
```

---

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.

`Parser.java` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).

Here we extend sincere gratitude to Tian Tan and Yue Li from Nanjing University who developed Tai-E the excellent framework.
