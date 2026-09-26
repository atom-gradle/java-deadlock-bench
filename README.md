# java-deadlock-bench

This repository provides the whole framework and pipeline for deadlock bench, including components like MutationGenerator, Parser and other scripts used in the paper.

## Stages & Pipeline
MutationGenerator -> Parsers -> Merger -> dataset

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

## Acknowledgements
This project uses [Tai-e](https://tai-e.pascal-lab.net/) (LGPL 3.0) as the static analysis framework.
The parser in `parser-a/` is adapted from Tai-e's educational assignments, which are also licensed under LGPL 3.0.
The parser in `parser-b/` is built based on [tai-e-0.5.4.jar](https://github.com/pascal-lab/Tai-e/tree/v0.5.4/).
