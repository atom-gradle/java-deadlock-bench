import os, re, subprocess, time, json, shutil
from collections import defaultdict

def find_harness_files(root_dir):
    harness_files = []
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if 'Harness' in file and file.endswith('.class'):
                full_path = os.path.join(root, file)
                harness_files.append(full_path)
    return harness_files

def get_class_full_name(class_file_path, root_dir):
    """
    Extract canonical class name(package name + class name) from file_path
    """
    rel_path = os.path.relpath(class_file_path, root_dir)
    # remove .class suffix
    class_name = rel_path[:-6]
    # replace path separator with '.'
    full_class_name = class_name.replace(os.sep, '.')

    return full_class_name

def run_java_command(class_full_name, working_dir):
    cmd = [
        'java',
        '-cp', '*;.',
        'pascal.taie.Main',
        '-cp', '.',
        '-m', class_full_name,
        '-a', 'pta',
        '--output-dir', '.\\out'
    ]

    print(f"\nExecuting command: {' '.join(cmd)}")
    print(f"Working dir: {working_dir}")

    try:
        result = subprocess.run(
            cmd,
            cwd=working_dir,
            capture_output=True,
            text=True,
            shell=False
        )

        if result.returncode == 0:
            print(f"Done successfully: {class_full_name}")
            if result.stdout:
                print(f"Output: {result.stdout}")
        else:
            print(f"Failed: {class_full_name}")
            print(f"Error Message: {result.stderr}")

    except Exception as e:
        print(f"RuntimeException occurs: {class_full_name}")
        print(f"Exception: {str(e)}")

def run_all_java(target_dir = "."):

    target_dir = os.path.abspath(target_dir)

    if not os.path.exists(target_dir):
        print(f"Error: Directory no exists: {target_dir}")
        return

    print(f"Searching in: {target_dir}")

    harness_files = find_harness_files(target_dir)

    if not harness_files:
        print("No class files containing 'Harness' were found.")
        return

    print(f"\nFound {len(harness_files)} Harness files:")
    for f in harness_files:
        print(f"  - {f}")

    out_dir = os.path.join(target_dir, 'out')
    os.makedirs(out_dir, exist_ok=True)

    print("\nStart executing Java command...")
    for class_file in harness_files:
        class_full_name = get_class_full_name(class_file, target_dir)
        run_java_command(class_full_name, target_dir)

def parse_cycles(content):
    """
    Split the file content into CYCLE blocks and return `[(cycle_num, block_text), ...]`.
    `block_text` does not include the `'CYCLE X'` line.
    """
    lines = content.splitlines()
    blocks = []
    current_cycle = None
    current_lines = []

    for line in lines:
        m = re.match(r'^CYCLE\s+(\d+)\s*$', line.strip())
        if m:
            if current_cycle is not None:
                blocks.append((current_cycle, current_lines))
            current_cycle = int(m.group(1))
            current_lines = []
        else:
            if current_cycle is not None:
                current_lines.append(line)

    if current_cycle is not None:
        blocks.append((current_cycle, current_lines))

    return blocks


def merge_txt(files, output):
    all_blocks = []
    for path in files:
        with open(path, 'r', encoding='utf-8') as f:
            all_blocks.extend(parse_cycles(f.read()))

    with open(output, 'w', encoding='utf-8') as f:
        for i, (_, block_lines) in enumerate(all_blocks):
            f.write(f"CYCLE {i}\n")
            for line in block_lines:
                f.write(line + "\n")


def find_graph_txt_files(search_dir=".", pattern_prefix="graph-"):
    """
    Find all graph-*.txt files in the current directory (without recursing into subdirectories).
    """
    search_dir = os.path.abspath(search_dir)
    files = []
    for name in os.listdir(search_dir):
        full = os.path.join(search_dir, name)
        if os.path.isfile(full) and name.startswith(pattern_prefix) and name.endswith(".txt"):
            files.append(full)

    # Sort by filename using natural ordering (for example, `graph-177111.txt` comes before `graph-177222.txt`).
    def sort_key(p):
        nums = re.findall(r'\d+', os.path.basename(p))
        return [int(n) for n in nums] if nums else [0]

    files.sort(key=sort_key)
    return files


def merge_generated_graphs(search_dir=".", output="merged-real-.txt"):
    """
    Merges all generated graph-*.txt into one single file
    """
    graph_files = find_graph_txt_files(search_dir)

    if not graph_files:
        print("\nNo graph-*.txt files found, skipping merge.")
        return

    print(f"\nFound {len(graph_files)} graph-*.txt files, starting merge:")
    for f in graph_files:
        print(f"  - {f}")

    merge_txt(graph_files, output)
    print(f"\nMerge complete, output file: {output}")

# ============================================================
# Fine-grained LABEL_REAL annotation (2 / 1 / 0 / -1)
# Based on add_cross_lock(fake=False/True) in mutation_report.json
# ============================================================

def build_index(report_path):
    """
    create index:
      real_entries_by_class : { class -> [ set(entry_methods of group1), ... ] }  仅 fake=False
      real_methods_by_class : { class -> set(entry_method) }
      fake_methods_by_class : { class -> set(entry_method) }
      mock_methods_by_class : { class -> set(mockMethod_*) }
      ring_helpers_by_class : { class -> set(__ring_*) }
      all_mutated_by_class  : { class -> set(all) }
    """
    with open(report_path, "r", encoding="utf-8") as f:
        report = json.load(f)

    # file -> class_name map
    file_to_class = {}
    for c in report.get("classes", []):
        file_to_class[c.get("file", "")] = c.get("class", "")

    real_entries_by_class = defaultdict(list)
    real_methods_by_class = defaultdict(set)
    fake_methods_by_class = defaultdict(set)
    mock_methods_by_class = defaultdict(set)
    ring_helpers_by_class = defaultdict(set)
    all_mutated_by_class  = defaultdict(set)

    for m in report.get("mutations", []):
        cls = m.get("class") or file_to_class.get(m.get("file", ""), "")
        if not cls:
            continue

        for mut in m.get("mutations", []):
            op = mut.get("op", "")

            if op == "add_cross_lock":
                fake = bool(mut.get("fake", False))
                entries = mut.get("entry_methods", []) or []
                if fake:
                    fake_methods_by_class[cls].update(entries)
                else:
                    real_entries_by_class[cls].append(set(entries))
                    real_methods_by_class[cls].update(entries)
                all_mutated_by_class[cls].update(entries)

            elif op == "add_mock_chain":
                chain = mut.get("chain", []) or []
                head = mut.get("head", "")
                for nm in chain:
                    mock_methods_by_class[cls].add(nm)
                    all_mutated_by_class[cls].add(nm)
                    if nm.startswith("__ring_"):
                        ring_helpers_by_class[cls].add(nm)
                if head:
                    mock_methods_by_class[cls].add(head)
                    all_mutated_by_class[cls].add(head)
                    if head.startswith("__ring_"):
                        ring_helpers_by_class[cls].add(head)

            # As a fallback, scan all possible method name fields and capture `__ring_*`.
            for key in ("entry_methods", "chain", "methods", "head"):
                v = mut.get(key)
                if isinstance(v, list):
                    for nm in v:
                        if isinstance(nm, str) and nm.startswith("__ring_"):
                            ring_helpers_by_class[cls].add(nm)
                            all_mutated_by_class[cls].add(nm)
                elif isinstance(v, str) and v.startswith("__ring_"):
                    ring_helpers_by_class[cls].add(v)
                    all_mutated_by_class[cls].add(v)

    return (dict(real_entries_by_class),
            dict(real_methods_by_class),
            dict(fake_methods_by_class),
            dict(mock_methods_by_class),
            dict(ring_helpers_by_class),
            dict(all_mutated_by_class))

# ACQ_NODE 0,HttpConnection.crossLock_g0_ring0_4945,1480,%v4,...
RE_ACQ_NODE_LINE = re.compile(r'^ACQ_NODE\s+\d+,([\w\.\$]+),')
# LOCK_EDGE 0,1,HttpConnection.xxx,1539,HttpConnection.yyy,1532,...
RE_LOCK_EDGE_LINE = re.compile(
    r'^LOCK_EDGE\s+\d+,\d+,([\w\.\$]+),-?\d+,([\w\.\$]+),'
)
RE_CYCLE_HEAD_LINE  = re.compile(r'^CYCLE\s+(\d+)\s*$')
RE_LABEL_CYCLE_LINE = re.compile(r'^LABEL_CYCLE\s+([01])\s*$')

def _split_class_method(full):
    """
    'HttpConnection.crossLock_g0_ring0_4945' -> ('HttpConnection', 'crossLock_g0_ring0_4945')
    """
    parts = full.split(".")
    if len(parts) >= 2:
        return parts[-2], parts[-1]
    return None, full

def extract_methods(cycle_text):
    """
    Extract { class_name -> set(method_name) } from CYCLE text
    """
    result = defaultdict(set)
    for line in cycle_text.splitlines():
        line = line.strip()
        m = RE_ACQ_NODE_LINE.match(line)
        if m:
            cls, meth = _split_class_method(m.group(1))
            if cls:
                result[cls].add(meth)
            continue
        m = RE_LOCK_EDGE_LINE.match(line)
        if m:
            for full in (m.group(1), m.group(2)):
                cls, meth = _split_class_method(full)
                if cls:
                    result[cls].add(meth)
    return dict(result)

def classify_cycle(methods_by_class,
                   real_entries_by_class,
                   real_methods_by_class,
                   fake_methods_by_class,
                   mock_methods_by_class,
                   ring_helpers_by_class,
                   all_mutated_by_class):
    """
    returnVal: 2 / 1 / 0 / -1
    """
    if not methods_by_class:
        return -1

    scores = []
    for cls, methods in methods_by_class.items():
        real_set = real_methods_by_class.get(cls, set())
        fake_set = fake_methods_by_class.get(cls, set())
        mock_set = mock_methods_by_class.get(cls, set())
        ring_set = ring_helpers_by_class.get(cls, set())
        all_set  = all_mutated_by_class.get(cls, set())
        groups   = real_entries_by_class.get(cls, [])

        if not all_set:
            continue  # This class has not been mutated by the mutator.

        real_hits = methods & real_set
        fake_hits = methods & fake_set
        mock_hits = methods & mock_set
        ring_hits = methods & ring_set

        # If fake cycles are mixed in -> classify as 0 (fake cycles interfere with real cycle detection).
        if fake_hits:
            scores.append(0)
            continue

        # Complete group inside cycle -> 2 (strong).
        full_group_hit = any(g and g.issubset(methods) for g in groups)
        if full_group_hit:
            scores.append(2)
            continue

        # At least 2 real cycle entries -> 2 (second strongest).
        if len(real_hits) >= 2:
            scores.append(2)
            continue

        # 1 real cycle entry / ring helper / mock -> 1 (weak).
        if real_hits or ring_hits or mock_hits:
            scores.append(1)
            continue

        # Has been mutated by the mutator but none are related to real cycles -> 0.
        scores.append(0)

    if not scores:
        return 0
    return max(scores)

def process_one_graph_fine(path, index, dry_run=False):
    """
    For a single graph file, insert `LABEL_REAL 2/1/0/-1` after each `LABEL_CYCLE` line.
    Return a statistics dict.
    """
    (real_entries_by_class,
     real_methods_by_class,
     fake_methods_by_class,
     mock_methods_by_class,
     ring_helpers_by_class,
     all_mutated_by_class) = index

    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        lines = f.read().splitlines()

    out_lines = []
    counter = {2: 0, 1: 0, 0: 0, -1: 0}

    for line in lines:
        out_lines.append(line)
        if RE_LABEL_CYCLE_LINE.match(line):
            # find the closest CYCLE n from end to start
            j = len(out_lines) - 1
            while j >= 0 and not RE_CYCLE_HEAD_LINE.match(out_lines[j]):
                j -= 1
            cycle_text = "\n".join(out_lines[j:])

            methods_by_class = extract_methods(cycle_text)
            label_real = classify_cycle(
                methods_by_class,
                real_entries_by_class,
                real_methods_by_class,
                fake_methods_by_class,
                mock_methods_by_class,
                ring_helpers_by_class,
                all_mutated_by_class,
            )
            out_lines.append(f"LABEL_REAL {label_real}")
            counter[label_real] += 1

    if not dry_run:
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(out_lines))

    return counter


def annotate_label_real_fine(merged_file, report_path, dry_run=False):
    """
    Firstly copy the merged txt to graph-merged-xxx-copy.txt
    Then mark on the copied txt file
    """
    if not os.path.isfile(report_path):
        print(f"[label_real_fine] report not found: {report_path}, skip")
        return

    print(f"[label_real_fine] loading report: {report_path}")
    index = build_index(report_path)

    # get a copy
    base, ext = os.path.splitext(merged_file)
    copy_file = f"{base}-copy{ext}"
    shutil.copyfile(merged_file, copy_file)
    print(f"[label_real_fine] copied {merged_file} -> {copy_file}")

    # mark
    counter = process_one_graph_fine(copy_file, index, dry_run=dry_run)

    total = sum(counter.values())
    print(f"[label_real_fine] {copy_file}: total={total} "
          f"L2(strong)={counter[2]} L1(weak)={counter[1]} "
          f"L0(negative)={counter[0]} UNK={counter[-1]}")

    return copy_file


def main():
    target_dir = "."

    # 1. Call Java Parser
    run_all_java(target_dir)

    # 2. Merge all generated graph-*.txt
    timestamp = str(round(1000 * time.time()))
    output_dir = "graph-merged-" + timestamp + ".txt"
    merge_generated_graphs(search_dir=target_dir, output=output_dir)

    # 3. Mark LABEL_REAL 2/1/0/-1
    annotate_label_real_fine(output_dir, "mutation_report.json", dry_run=False)

if __name__ == "__main__":
    main()
