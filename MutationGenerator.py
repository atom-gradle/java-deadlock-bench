# -*- coding: utf-8 -*-
"""
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

Supported constructs:
- new Thread(...) / Runnable anonymous class / Lambda
- Object / custom lock object / ReentrantLock

Not supported:
- wait / notify
- Other JUC utilities (ReadWriteLock, StampedLock, Semaphore, etc.)
"""

import os,re,json,random,shutil,time

WORKING_DIR = r"D:\Projects"

# mutated/project_name/package_name/...
formatted_time = time.strftime("%m%d-%H%M%S", time.localtime(time.time())) + f"{int((time.time() % 1) * 1000):03d}"
OUTPUT_DIR = "./mutated-" + str(round(1000 * time.time()))

# OUTPUT_DIR/mutation_report.json
REPORT_PATH = os.path.join(OUTPUT_DIR, "mutation_report.json")

SEED = 42

# mutation possibility
P_REPLACE_LOCK = 0.5
P_REASSIGN_LOCK = 0.3
P_INSERT_SLEEP = 0.4
P_ADD_SYNC_BLOCK = 0.3
P_ADD_MOCK_METHOD = 0.6
P_MOCK_NESTED = 0.5

# Thread.sleep 参数范围（毫秒）
SLEEP_MIN = 100
SLEEP_MAX = 500

# 每个类最多插入 sleep 次数
MAX_SLEEP_PER_CLASS = 3

# mock 方法最大嵌套深度
MOCK_MAX_DEPTH = 3

# 识别锁字段的类型
LOCK_TYPES = {
    "ReentrantLock",
}

# 锁字段名字关键词
LOCK_NAME_HINTS = ["lock", "mutex", "monitor", "semaphore", "guard"]

# synchronized 块正则
RE_SYNC_BLOCK = re.compile(
    r'synchronized\s*\(\s*([^)]+?)\s*\)\s*\{',
    re.MULTILINE
)

# 锁字段声明正则
RE_LOCK_FIELD = re.compile(
    r'(?:private|protected|public)?\s*'
    r'(?:static\s+)?(?:final\s+)?'
    r'(\w+)\s+(\w+)\s*=\s*new\s+\1\s*\([^)]*\)\s*;',
    re.MULTILINE
)

# 类声明
RE_CLASS = re.compile(
    r'(?:^|\n)\s*(?:public\s+|protected\s+|private\s+)?'
    r'(?:abstract\s+|final\s+)?class\s+(\w+)',
    re.MULTILINE
)

# 包声明
RE_PACKAGE = re.compile(r'^\s*package\s+([\w\.]+)\s*;', re.MULTILINE)

# public 方法
RE_PUBLIC_METHOD = re.compile(
    r'(?:^|\n)[ \t]*public[ \t]+'
    r'(?:static[ \t]+)?(?:final[ \t]+)?(?:synchronized[ \t]+)?'
    r'(?:abstract[ \t]+)?(?:native[ \t]+)?'
    r'(?!class\b|interface\b|enum\b)'
    r'([^\n{};]+?)[ \t]+(\w+)[ \t]*\(([^)]*)\)[ \t\n]*'
    r'(?:throws[ \t]+[^\n{};]+)?[ \t\n]*\{',
    re.MULTILINE
)

# ============================================================
# IO 工具
# ============================================================

def read_file(path):
    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception:
        return None


def write_file(path, content):
    path = os.path.normpath(path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def find_java_files(src_root):
    for dirpath, _, filenames in os.walk(src_root):
        for fn in filenames:
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def resolve_source_root(project_dir):
    candidate1 = os.path.join(project_dir, "src", "main", "java")
    if os.path.isdir(candidate1):
        return candidate1
    candidate2 = os.path.join(project_dir, "src")
    if os.path.isdir(candidate2):
        return candidate2
    return None


# ============================================================
# 注释剥离
# ============================================================

def strip_comments(content):
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    content = re.sub(r'//(?!/)[^\n]*', '', content)
    return content

def remove_nested_classes(body):
    """去掉嵌套类声明和匿名内部类，保留其他代码。"""
    result = []
    i = 0
    n = len(body)
    while i < n:
        m_class = re.search(r'\b(?:class|interface|enum)\s+\w+', body[i:])
        m_anon = re.search(r'\bnew\s+\w+(?:<[^>]*>)?\s*\([^)]*\)\s*\{', body[i:])

        candidates = []
        if m_class:
            candidates.append((m_class.start(), 'class', m_class))
        if m_anon:
            candidates.append((m_anon.start(), 'anon', m_anon))

        if not candidates:
            result.append(body[i:])
            break

        candidates.sort(key=lambda x: x[0])
        pos, kind, m = candidates[0]

        result.append(body[i:i + pos])

        if kind == 'class':
            j = i + m.end()
            while j < n and body[j] != '{':
                j += 1
            if j >= n:
                break
            depth = 1
            j += 1
            while j < n and depth > 0:
                if body[j] == '{':
                    depth += 1
                elif body[j] == '}':
                    depth -= 1
                j += 1
            i = j
        else:
            j = i + m.end() - 1
            depth = 1
            while j < n and depth > 0:
                if body[j] == '{':
                    depth += 1
                elif body[j] == '}':
                    depth -= 1
                j += 1
            i = j

    return ''.join(result)


# ============================================================
# 解析工具
# ============================================================

def parse_package(content):
    m = RE_PACKAGE.search(content)
    return m.group(1) if m else None


def parse_class_name(content):
    clean = strip_comments(content)
    m = RE_CLASS.search(clean)
    return m.group(1) if m else None


def parse_lock_fields(content):
    fields = []
    seen = set()

    for m in RE_LOCK_FIELD.finditer(content):
        typ, name = m.group(1), m.group(2)
        is_lock_type = typ in LOCK_TYPES
        is_lock_name = any(h in name.lower() for h in LOCK_NAME_HINTS)
        if typ == "Object":
            if not is_lock_name:
                continue
        else:
            if not (is_lock_type or is_lock_name):
                continue
        decl = m.group(0)
        fields.append({
            "type": typ,
            "name": name,
            "is_static": "static" in decl,
            "is_final": "final" in decl,
        })
        seen.add(name)

    for m in RE_SYNC_BLOCK.finditer(content):
        expr = m.group(1).strip()
        # 排除 this
        if expr == "this":
            continue
        # 排除全大写常量（可能是缓存/常量，不是锁）
        if expr.isupper():
            continue
        if re.fullmatch(r"[A-Za-z_]\w*", expr) and expr not in seen:
            fields.append({
                "type": "Object",
                "name": expr,
                "is_static": False,
                "is_final": True,
                "inferred": True,
            })
            seen.add(expr)

    return fields


def parse_class_modifiers(content, class_name):
    clean = strip_comments(content)
    pattern = re.compile(
        r'(?:^|\n)\s*((?:(?:public|protected|private|abstract|final|static)\s+)*)'
        r'class\s+' + re.escape(class_name) + r'\b',
        re.MULTILINE
    )
    m = pattern.search(clean)
    if not m:
        return []
    return m.group(1).split()


def is_public_class(content, class_name):
    return 'public' in parse_class_modifiers(content, class_name)


def _get_class_body(content, class_name):
    """返回顶层类体的文本（不含外层大括号）。找不到返回 None。"""
    clean = strip_comments(content)
    class_pattern = re.compile(
        r'\bclass\s+' + re.escape(class_name) + r'\b[^{]*\{',
        re.MULTILINE
    )
    m = class_pattern.search(clean)
    if not m:
        return None
    start = m.end()
    depth = 1
    i = start
    while i < len(clean) and depth > 0:
        c = clean[i]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
        i += 1
    return clean[start:i - 1]

def get_first_constructor_args(content, class_name):
    """返回第一个非 private 构造器的参数列表，没有就返回 None。"""
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return None
    class_body_no_nested = remove_nested_classes(class_body)
    # 只匹配 public / protected / package-private，不匹配 private
    pattern = re.compile(
        r'(?:^|\n)[ \t]*(?:public|protected)?[ \t]*'
        + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    m = pattern.search(class_body_no_nested)
    if not m:
        return None
    return m.group(1).strip()

def has_no_arg_constructor(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return False

    class_body_no_nested = remove_nested_classes(class_body)

    # 找所有显式构造器（不限修饰符）
    any_ctor_pattern = re.compile(
        r'(?:^|\n)[ \t]*(?:public|protected|private)?[ \t]*'
        + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    any_ctors = list(any_ctor_pattern.finditer(class_body_no_nested))

    # 没有显式构造器 -> Java 默认提供 public 无参构造器
    if not any_ctors:
        return True

    # 有显式构造器，检查是否有 public 无参的
    public_ctor_pattern = re.compile(
        r'(?:^|\n)[ \t]*public[ \t]+' + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    for c in public_ctor_pattern.finditer(class_body_no_nested):
        params = c.group(1).strip()
        if not params:
            return True

    return False

def has_lock(content):
    if RE_SYNC_BLOCK.search(content):
        return True
    if parse_lock_fields(content):
        return True
    return False


def parse_public_methods(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return []

    class_body_no_nested = remove_nested_classes(class_body)

    methods = []
    for mm in RE_PUBLIC_METHOD.finditer(class_body_no_nested):
        ret_type = mm.group(1).strip()
        name = mm.group(2)
        params = mm.group(3).strip()
        if name == class_name:
            continue
        matched_text = class_body_no_nested[mm.start():mm.end()]
        is_static = "static" in matched_text
        is_sync = "synchronized" in matched_text
        methods.append({
            "return_type": ret_type,
            "name": name,
            "params": params,
            "is_static": is_static,
            "is_synchronized": is_sync,
        })
    return methods

def simple_args_for(params):
    if not params.strip():
        return []
    args = []
    for p in params.split(","):
        p = p.strip()
        if not p:
            continue
        # 含泛型通配符 ? super / ? extends，直接用 Object
        if '?' in p or ' super ' in p or ' extends ' in p:
            args.append("(Object) null")
            continue
        # 去掉泛型参数
        p_clean = re.sub(r"<[^>]*>", "", p).strip()
        p_clean = re.sub(r"\bfinal\b", "", p_clean).strip()
        parts = p_clean.split()
        if len(parts) < 2:
            args.append("(Object) null")
            continue
        typ = parts[-2]
        if typ in ("int", "Integer"):
            args.append("0")
        elif typ in ("long", "Long"):
            args.append("0L")
        elif typ in ("boolean", "Boolean"):
            args.append("false")
        elif typ in ("double", "Double"):
            args.append("0.0")
        elif typ in ("float", "Float"):
            args.append("0.0f")
        elif typ in ("String", "CharSequence"):
            args.append('""')
        elif len(typ) == 1 and typ.isupper():
            # 泛型参数 T/E/K/V，用 Object
            args.append("(Object) null")
        elif typ.endswith("..."):
            # varargs
            args.append(f"({typ[:-3]}[]) null")
        else:
            args.append(f"({typ}) null")
    return args


# ============================================================
# 变异操作
# ============================================================

def mutate_replace_lock(content, lock_fields):
    if not lock_fields:
        return content, []
    records = []
    names = [f["name"] for f in lock_fields]

    def repl(m):
        old = m.group(1).strip()
        if not re.fullmatch(r"[A-Za-z_]\w*", old):
            return m.group(0)
        # 从 names 里排除 old，避免选到同一个
        candidates = [n for n in names if n != old]
        if not candidates:
            # 只有 old 一个锁字段，不替换
            return m.group(0)
        new = random.choice(candidates)
        records.append({"op": "replace_lock", "old": old, "new": new})
        return f"synchronized ({new}) {{"

    new_content = RE_SYNC_BLOCK.sub(repl, content)
    return new_content, records


def mutate_reassign_lock(content, lock_fields, class_name):
    candidates = [f for f in lock_fields if not f.get("is_final", False)]
    if not candidates:
        return content, []
    target = random.choice(candidates)
    method_name = f"reassignLock_{target['name']}"
    method_code = f"""
    public void {method_name}() {{
        this.{target['name']} = new {target['type']}();
    }}
"""
    idx = content.rfind("}")
    if idx == -1:
        return content, []
    new_content = content[:idx] + method_code + content[idx:]
    return new_content, [{
        "op": "reassign_lock",
        "field": target["name"],
        "type": target["type"],
    }]


def mutate_insert_sleep(content):
    records = []
    count = [0]

    def repl(m):
        if count[0] >= MAX_SLEEP_PER_CLASS:
            return m.group(0)
        sleep_ms = random.randint(SLEEP_MIN, SLEEP_MAX)
        records.append({"op": "insert_sleep", "ms": sleep_ms})
        count[0] += 1
        line_start = content.rfind('\n', 0, m.start()) + 1
        indent_match = re.match(r'[ \t]*', content[line_start:m.start()])
        indent = indent_match.group(0) if indent_match else ""
        return (m.group(0)
                + f"\n{indent}    try {{ Thread.sleep({sleep_ms}); }} "
                  f"catch (InterruptedException e) {{ }}")

    new_content = RE_SYNC_BLOCK.sub(repl, content)
    return new_content, records


def mutate_add_sync_block(content, lock_fields):
    candidates = [f for f in lock_fields if f["type"] == "Object"]
    if not candidates:
        return content, []
    target = random.choice(candidates)
    method_name = f"syncBlock_{target['name']}_{random.randint(0, 9999)}"
    method_code = f"""
    public void {method_name}() {{
        synchronized ({target['name']}) {{
            // injected synchronized block
        }}
    }}
"""
    idx = content.rfind("}")
    if idx == -1:
        return content, []
    new_content = content[:idx] + method_code + content[idx:]
    return new_content, [{
        "op": "add_sync_block",
        "field": target["name"],
    }]


def mutate_add_mock_method(content, class_name, methods, lock_fields, depth=0):
    if depth >= MOCK_MAX_DEPTH:
        return content, []

    method_name = f"mockMethod_{depth}_{random.randint(0, 9999)}"

    concurrent = [m for m in methods if m["is_synchronized"]]
    target_method = random.choice(concurrent) if concurrent else (
        random.choice(methods) if methods else None
    )

    call_line = ""
    if target_method:
        args = ", ".join(simple_args_for(target_method["params"]))
        call_line = f"{target_method['name']}({args});"

    sub_content = content
    sub_records = []
    nested_line = ""
    if random.random() < P_MOCK_NESTED and depth + 1 < MOCK_MAX_DEPTH:
        sub_content, sub_records = mutate_add_mock_method(
            content, class_name, methods, lock_fields, depth + 1
        )
        if sub_records:
            nested_name = sub_records[-1]["name"]
            nested_line = f"{nested_name}();"

    method_code = f"""
    public void {method_name}() throws Exception {{
        {call_line}
        {nested_line}
    }}
"""
    idx = sub_content.rfind("}")
    if idx == -1:
        return sub_content, []
    new_content = sub_content[:idx] + method_code + sub_content[idx:]

    records = [{
        "op": "add_mock_method",
        "name": method_name,
        "call": target_method["name"] if target_method else None,
        "nested": bool(nested_line),
    }] + sub_records

    return new_content, records


def mutate_class(content, class_name, lock_fields, methods):
    records = []
    new_content = content

    if random.random() < P_REPLACE_LOCK:
        new_content, r = mutate_replace_lock(new_content, lock_fields)
        records.extend(r)

    if random.random() < P_REASSIGN_LOCK:
        new_content, r = mutate_reassign_lock(new_content, lock_fields, class_name)
        records.extend(r)

    if random.random() < P_INSERT_SLEEP:
        new_content, r = mutate_insert_sleep(new_content)
        records.extend(r)

    if random.random() < P_ADD_SYNC_BLOCK:
        new_content, r = mutate_add_sync_block(new_content, lock_fields)
        records.extend(r)

    if random.random() < P_ADD_MOCK_METHOD:
        new_content, r = mutate_add_mock_method(
            new_content, class_name, methods, lock_fields, depth=0
        )
        records.extend(r)

    return new_content, records


# ============================================================
# Harness 生成
# ============================================================

def gen_harness(pkg, class_name, methods, project_out_dir, ctor_args=None, use_null_instance=None):
    """
    生成 harness，放在原包下（同包可访问 protected/package-private 构造器）。
    """
    harness_pkg = pkg or ""
    harness_class = class_name + "Harness"
    path = os.path.normpath(os.path.join(
        project_out_dir,
        harness_pkg.replace(".", "/") if harness_pkg else "",
        harness_class + ".java",
    ))

    lines = []
    if harness_pkg:
        lines.append(f"package {harness_pkg};")
        lines.append("")

    lines.append(f"public class {harness_class} {{")
    lines.append("    public static void main(String[] args) throws Exception {")

    if use_null_instance:
        lines.append(f"        {class_name} instance = null;")
    elif ctor_args is None:
        lines.append(f"        {class_name} instance = new {class_name}();")
    else:
        args = ", ".join(simple_args_for(ctor_args))
        lines.append(f"        {class_name} instance = new {class_name}({args});")

    for m in methods:
        args = ", ".join(simple_args_for(m["params"]))
        if m.get("is_static"):
            call_expr = f"{class_name}.{m['name']}({args})"
        else:
            call_expr = f"instance.{m['name']}({args})"

        lines.append(f"        // call {m['name']}")
        if m["return_type"] == "void":
            lines.append(
                f"        try {{ {call_expr}; }} catch (Throwable t) {{ }}"
            )
        else:
            lines.append(
                f"        try {{ Object _r = {call_expr}; }} catch (Throwable t) {{ }}"
            )
        lines.append("")

    instance_methods = [m for m in methods if not m.get("is_static")]
    if len(instance_methods) >= 2:
        m1, m2 = instance_methods[0], instance_methods[1]
        a1 = ", ".join(simple_args_for(m1["params"]))
        a2 = ", ".join(simple_args_for(m2["params"]))
        lines.append("        Thread t1 = new Thread(() -> {")
        lines.append(
            f"            try {{ instance.{m1['name']}({a1}); }} "
            f"catch (Throwable t) {{ }}"
        )
        lines.append("        });")
        lines.append("        Thread t2 = new Thread(() -> {")
        lines.append(
            f"            try {{ instance.{m2['name']}({a2}); }} "
            f"catch (Throwable t) {{ }}"
        )
        lines.append("        });")
        lines.append("        t1.start(); t2.start();")
        lines.append("        t1.join(); t2.join();")

    lines.append("    }")
    lines.append("}")

    content = "\n".join(lines)
    write_file(path, content)
    return path

# ============================================================
# 单个项目处理
# ============================================================

def process_one_project(project_dir, project_name, OUTPUT_DIR, report):
    src_root = resolve_source_root(project_dir)
    if src_root is None:
        print(f"[SKIP] {project_name}: no src dir")
        return

    project_out_dir = os.path.join(OUTPUT_DIR, project_name)

    java_files = list(find_java_files(src_root))
    print(f"[INFO] {project_name}: found {len(java_files)} java files in {src_root}")

    for jf in java_files:
        if os.path.basename(jf) == "package-info.java":
            continue

        content = read_file(jf)
        if content is None:
            continue
        if not has_lock(content):
            continue

        class_name = parse_class_name(content)
        if not class_name:
            print(f"[SKIP] {jf}: cannot parse class name")
            continue

        pkg = parse_package(content)
        lock_fields = parse_lock_fields(content)
        methods = parse_public_methods(content, class_name)

        report["classes"].append({
            "project": project_name,
            "file": jf,
            "package": pkg,
            "class": class_name,
            "lock_fields": lock_fields,
            "methods": [m["name"] for m in methods],
        })

        new_content, mutations = mutate_class(
            content, class_name, lock_fields, methods
        )

        rel = os.path.relpath(jf, src_root)
        out_path = os.path.normpath(os.path.join(project_out_dir, rel))
        write_file(out_path, new_content)

        report["mutations"].append({
            "project": project_name,
            "file": jf,
            "out": out_path,
            "mutations": mutations,
        })

        # 决定构造器参数（不限 public）
        if has_no_arg_constructor(content, class_name):
            ctor_args = None
            use_null_instance = False
        else:
            ctor_args = get_first_constructor_args(content, class_name)
            if ctor_args is None:
                # 只有 private 构造器，用 null 声明实例
                ctor_args = None
                use_null_instance = True
            else:
                use_null_instance = False

        harness_path = gen_harness(
            pkg, class_name, methods, project_out_dir,
            ctor_args=ctor_args,
            use_null_instance=use_null_instance,
        )

        report["harnesses"].append({
            "project": project_name,
            "class": class_name,
            "harness": harness_path,
        })


# ============================================================
# 主流程
# ============================================================

def process_all_projects(working_dir, OUTPUT_DIR):
    report = {
        "working_dir": working_dir,
        "classes": [],
        "mutations": [],
        "harnesses": [],
        "errors": [],
    }

    if not os.path.isdir(working_dir):
        print(f"[ERROR] working dir not found: {working_dir}")
        return report

    project_names = [
        d for d in os.listdir(working_dir)
        if os.path.isdir(os.path.join(working_dir, d))
    ]

    print(f"[INFO] found {len(project_names)} projects: {project_names}")

    for pname in project_names:
        pdir = os.path.join(working_dir, pname)
        try:
            process_one_project(pdir, pname, OUTPUT_DIR, report)
        except Exception as e:
            report["errors"].append({
                "project": pname,
                "error": str(e),
            })
            print(f"[ERROR] {pname}: {e}")

    write_file(REPORT_PATH, json.dumps(report, indent=2, ensure_ascii=False))
    print(f"[INFO] report written to {REPORT_PATH}")
    print(f"[INFO] mutated {len(report['mutations'])} classes")
    return report


def main():
    random.seed(SEED)

    if os.path.exists(OUTPUT_DIR):
        shutil.rmtree(OUTPUT_DIR)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    process_all_projects(WORKING_DIR, OUTPUT_DIR)

if __name__ == "__main__":
    main()