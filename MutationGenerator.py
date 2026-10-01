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
- Generate a linear mock call chain (realistic long-distance calls)
- Insert the chain head into an original method body
- Generate a harness for each class

Supported constructs:
- new Thread(...) / Runnable anonymous class / Lambda
- Object / custom lock object / ReentrantLock

Not supported:
- wait / notify
- Other JUC utilities (ReadWriteLock, StampedLock, Semaphore, etc.)
"""

import os,re,json,random,shutil,time, argparse

WORKING_DIR = r"D:\Projects2"

# mutated/project_name/package_name/...
formatted_time = time.strftime("%m%d-%H%M%S", time.localtime(time.time())) + f"{int((time.time() % 1) * 1000):03d}"
OUTPUT_DIR = "./mutated-" + str(round(1000 * time.time()))

# OUTPUT_DIR/mutation_report.json
REPORT_PATH = os.path.join(OUTPUT_DIR, "mutation_report.json")

# 是否保留原项目中的所有类（True 时不跳过不含锁的类，便于直接编译整个项目）
RETAIN_ALL_CLASSES = False

SEED = 42

# mutation possibility
P_REPLACE_LOCK = 0.5
P_REASSIGN_LOCK = 0.3
P_INSERT_SLEEP = 0.4
P_ADD_SYNC_BLOCK = 0.3
P_ADD_MOCK_METHOD = 1.0

# Thread.sleep(milliseconds)
SLEEP_MIN = 100
SLEEP_MAX = 500

# 每个类最多插入 sleep 次数
MAX_SLEEP_PER_CLASS = 3

# mock 链最大长度
MOCK_MAX_DEPTH = 5

# 识别锁字段的类型
LOCK_TYPES = {
    "ReentrantLock",
}

# 锁字段名字关键词
LOCK_NAME_HINTS = ["lock", "mutex", "monitor", "semaphore", "guard"]

# synchronized blocks regex
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

# class regex
RE_CLASS = re.compile(
    r'(?:^|\n)\s*(?:public\s+|protected\s+|private\s+)?'
    r'(?:abstract\s+|final\s+)?class\s+(\w+)',
    re.MULTILINE
)

RE_CLASS_ALL = re.compile(
    r'(?:^|\n)\s*(?:public\s+|protected\s+|private\s+)?'
    r'(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+|static\s+)?'
    r'(?:class|interface|record|enum)\s+(\w+)',
    re.MULTILINE
)

# package regex
RE_PACKAGE = re.compile(r'^\s*package\s+([\w\.]+)\s*;', re.MULTILINE)

# public methods
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
# IO tools
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
    candidate3 = os.path.join(project_dir, "agrona", "src", "main", "java")
    if os.path.isdir(candidate3):
        return candidate3
    candidate4 = os.path.join(project_dir, "jctools-core", "src", "main", "java")
    if os.path.isdir(candidate4):
        return candidate4
    return None


# ============================================================
# 注释剥离
# ============================================================

def strip_comments(content):
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    content = re.sub(r'//(?!/)[^\n]*', '', content)
    return content


def _scan_balanced_braces(text, start):
    """
    从 text[start] 开始（start 指向 { 之后的位置），扫描到匹配的 } 之后。
    跳过字符串字面量 "..." 和字符字面量 '...'，避免括号计数被字面量里的 {} 干扰。
    """
    n = len(text)
    depth = 1
    i = start
    in_string = False
    in_char = False
    while i < n and depth > 0:
        c = text[i]
        if in_string:
            if c == '\\':
                i += 2
                continue
            if c == '"':
                in_string = False
        elif in_char:
            if c == '\\':
                i += 2
                continue
            if c == '\'':
                in_char = False
        else:
            if c == '"':
                in_string = True
            elif c == '\'':
                in_char = True
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
        i += 1
    return i


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
            j += 1
            j = _scan_balanced_braces(body, j)
            i = j
        else:
            j = i + m.end()
            j = _scan_balanced_braces(body, j)
            i = j

    return ''.join(result)


# ============================================================
# parsers
# ============================================================

def parse_package(content):
    m = RE_PACKAGE.search(content)
    return m.group(1) if m else None

def parse_class_name(content):
    clean = strip_comments(content)
    m = RE_CLASS.search(clean)
    return m.group(1) if m else None

def need_to_parse(content):
    clean = strip_comments(content)
    return RE_CLASS.search(clean) is not None

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
        if expr == "this":
            continue
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
    clean = strip_comments(content)
    class_pattern = re.compile(
        r'\bclass\s+' + re.escape(class_name) + r'\b[^{]*\{',
        re.MULTILINE
    )
    m = class_pattern.search(clean)
    if not m:
        return None
    start = m.end()
    end = _scan_balanced_braces(clean, start)
    return clean[start:end - 1]


def get_first_constructor_args(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return None
    class_body_no_nested = remove_nested_classes(class_body)
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

    any_ctor_pattern = re.compile(
        r'(?:^|\n)[ \t]*(?:public|protected|private)?[ \t]*'
        + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    any_ctors = list(any_ctor_pattern.finditer(class_body_no_nested))

    if not any_ctors:
        return True

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
        # 去掉 final
        p = re.sub(r"\bfinal\b", "", p).strip()
        # 提取参数类型名（泛型前的原始类型）
        # 形如 "Collection<C> collection" -> "Collection"
        # 形如 "Enumeration<? extends C> enumeration" -> "Enumeration"
        # 形如 "C... elements" -> "C"
        m = re.match(r'([\w\.]+)', p)
        if not m:
            args.append("null")
            continue
        typ = m.group(1)
        # 基本类型和 String 用具体值
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
        elif typ.endswith("...") or p.endswith("..."):
            base = typ[:-3] if typ.endswith("...") else typ
            args.append(f"null")
        else:
            # 其他引用类型，用 (Type) null
            args.append(f"null")
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
        candidates = [n for n in names if n != old]
        if not candidates:
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
        "name": method_name,
    }]


def _insert_call_into_method(content, method, call_line):
    """在方法体开头插入一行调用。"""
    method_name = method["name"]
    pattern = re.compile(
        r'([ \t]*(?:public|protected|private)?[ \t]*(?:static[ \t]+)?'
        r'(?:final[ \t]+)?(?:synchronized[ \t]+)?'
        r'[\w<>\[\],\s\?\.]+?[ \t]+' + re.escape(method_name) + r'[ \t]*\([^)]*\)[ \t\n]*'
        r'(?:throws[ \t]+[^\n{};]+)?[ \t\n]*\{)',
        re.MULTILINE
    )
    m = pattern.search(content)
    if not m:
        return content
    insert_pos = m.end()
    line_start = content.rfind('\n', 0, m.start()) + 1
    indent_match = re.match(r'[ \t]*', content[line_start:m.start()])
    indent = indent_match.group(0) if indent_match else ""
    inner_indent = indent + "    "
    insertion = f"\n{inner_indent}{call_line}"
    return content[:insert_pos] + insertion + content[insert_pos:]


def _build_mock_chain(methods, depth_limit, sync_block_name=None,
                      exclude_method=None):
    """
    生成一条线性 mock 链：
        mockMethod_0 → mockMethod_1 → ... → mockMethod_N → 原始方法

    每个方法只被一个方法调用，不扇入，不循环。
    返回 (chain_list, head_name, tail_target)。
    chain_list 里每个元素是 {"name": ..., "code": ...}。
    """
    # 选链尾调用的原始方法：优先非静态实例方法，排除插入点

    # print(f"[DEBUG] _build_mock_chain depth_limit={depth_limit}")
    candidates = [m for m in methods
                  if m["is_synchronized"]
                  and not m.get("is_static")
                  and (exclude_method is None or m["name"] != exclude_method["name"])]
    if not candidates:
        candidates = [m for m in methods
                      if not m.get("is_static")
                      and (exclude_method is None or m["name"] != exclude_method["name"])]
    if not candidates:
        candidates = [m for m in methods
                      if exclude_method is None or m["name"] != exclude_method["name"]]
    tail_target = random.choice(candidates) if candidates else None

    tail_call = ""
    if tail_target:
        args = ", ".join(simple_args_for(tail_target["params"]))
        tail_call = f"{tail_target['name']}({args});"
    # print(f"[DEBUG] _build_mock_chain: depth_limit={depth_limit}, range={list(range(depth_limit - 1, -1, -1))}")
    # 从尾到头生成
    chain = []
    next_call = tail_call
    mid_depth = depth_limit // 2
    for depth in range(depth_limit - 1, -1, -1):
        method_name = f"mockMethod_{depth}_{random.randint(0, 9999)}"
        sync_line = ""
        if sync_block_name and depth == mid_depth:
            sync_line = f"{sync_block_name}();"
        method_code = f"""
    public void {method_name}() throws Exception {{
        {next_call}
        {sync_line}
    }}
"""
        chain.append({"name": method_name, "code": method_code})
        next_call = f"{method_name}();"

    chain.reverse()
    head_name = chain[0]["name"]
    return chain, head_name, tail_target


def mutate_add_mock_chain(content, methods, insert_into_method=None,
                          sync_block_name=None):
    """
    在 content 里新增一条线性 mock 链。
    如果 insert_into_method 非 None，把链头的调用插入到该方法体开头。
    """
    chain, head_name, tail_target = _build_mock_chain(
        methods, MOCK_MAX_DEPTH,
        sync_block_name=sync_block_name,
        exclude_method=insert_into_method,
    )

    # print(f"[DEBUG] mutate_add_mock_chain: chain_len={len(chain)}, head={head_name}, tail={tail_target}")
    if not chain:
        return content, []

    idx = content.rfind("}")
    if idx == -1:
        return content, []
    chain_code = "\n".join(c["code"] for c in chain)
    new_content = content[:idx] + chain_code + content[idx:]

    if insert_into_method is not None:
        new_content = _insert_call_into_method(
            new_content, insert_into_method, f"{head_name}();"
        )

    records = [{
        "op": "add_mock_chain",
        "head": head_name,
        "length": len(chain),
        "tail_target": tail_target["name"] if tail_target else None,
        "inserted_into": insert_into_method["name"] if insert_into_method else None,
        "chain": [c["name"] for c in chain],
    }]

    # print(f"[DEBUG] mutate_add_mock_chain: new_content contains {chain_code.count('mockMethod')} mockMethod")

    return new_content, records


def mutate_class(content, class_name, lock_fields, methods):
    records = []
    new_content = content
    sync_block_name = None

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
        if r and r[0].get("op") == "add_sync_block":
            sync_block_name = r[0].get("name")

    if random.random() < P_ADD_MOCK_METHOD:
        # 选插入点：优先有锁的实例方法
        insert_target = None
        candidates = [m for m in methods if m["is_synchronized"] and not m.get("is_static")]
        if not candidates:
            candidates = [m for m in methods if not m.get("is_static")]
        if candidates:
            insert_target = random.choice(candidates)
        new_content, r = mutate_add_mock_chain(
            new_content, methods,
            insert_into_method=insert_target,
            sync_block_name=sync_block_name,
        )
        records.extend(r)

    return new_content, records


# ============================================================
# Harness 生成
# ============================================================

def gen_harness(pkg, class_name, methods, project_out_dir, ctor_args=None, use_null_instance=None):
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

    mock_methods = [m for m in methods if m["name"].startswith("mockMethod_")]
    mock_methods.sort(key=lambda m: m["name"])
    other_methods = [m for m in methods if not m["name"].startswith("mockMethod_")]
    ordered = mock_methods + other_methods

    for m in ordered:
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

    # 多线程模拟：随机 1-4 个线程，每个线程随机选一个方法
    # 优先包含 mock 链头，其次随机选原始方法
    callable_methods = list(ordered)  # 所有可调用方法
    # 去掉返回类型为构造器或不可调用的
    callable_methods = [m for m in callable_methods if m["name"] != class_name]

    # 随机 1-4 个线程
    num_threads = random.randint(2, 4)
    if callable_methods:
        threads = []
        # 优先保证链头被调用一次
        head_method = None
        if mock_methods:
            head_method = mock_methods[0]
            threads.append(head_method)

        # 从剩余方法里随机选
        remaining = [m for m in callable_methods if m is not head_method]
        while len(threads) < num_threads and remaining:
            chosen = random.choice(remaining)
            threads.append(chosen)
            remaining.remove(chosen)

        # 如果线程数还不够（方法太少），重复选
        while len(threads) < num_threads and callable_methods:
            threads.append(random.choice(callable_methods))

        # 生成线程代码
        thread_vars = []
        for i, m in enumerate(threads):
            var = f"t{i+1}"
            thread_vars.append(var)
            args = ", ".join(simple_args_for(m["params"]))
            if m.get("is_static"):
                call_expr = f"{class_name}.{m['name']}({args})"
            else:
                call_expr = f"instance.{m['name']}({args})"
            lines.append(f"        Thread {var} = new Thread(() -> {{")
            lines.append(
                f"            try {{ {call_expr}; }} "
                f"catch (Throwable t) {{ }}"
            )
            lines.append("        });")

        # start + join
        for var in thread_vars:
            lines.append(f"        {var}.start();")
        for var in thread_vars:
            lines.append(f"        {var}.join();")

    lines.append("    }")  # 闭合 main
    lines.append("}")  # 闭合 class
    content = "\n".join(lines)
    write_file(path, content)
    return path

# ============================================================
# process one single project
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
        content = read_file(jf)
        if content is None:
            continue

        if os.path.basename(jf) == "package-info.java":
            rel = os.path.relpath(jf, src_root)
            out_path = os.path.normpath(os.path.join(project_out_dir, rel))
            write_file(out_path, content)  # 写原始内容
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

        new_methods = parse_public_methods(new_content, class_name)

        if has_no_arg_constructor(content, class_name):
            ctor_args = None
            use_null_instance = False
        else:
            ctor_args = get_first_constructor_args(content, class_name)
            if ctor_args is None:
                ctor_args = None
                use_null_instance = True
            else:
                use_null_instance = False

        harness_path = gen_harness(
            pkg, class_name, new_methods, project_out_dir,
            ctor_args=ctor_args,
            use_null_instance=use_null_instance,
        )

        report["harnesses"].append({
            "project": project_name,
            "class": class_name,
            "harness": harness_path,
        })

def process_one_project_retain_all(project_dir, project_name, OUTPUT_DIR, report):
    src_root = resolve_source_root(project_dir)
    if src_root is None:
        print(f"[SKIP] {project_name}: no src dir")
        return

    project_out_dir = os.path.join(OUTPUT_DIR, project_name)

    java_files = list(find_java_files(src_root))
    print(f"[INFO] {project_name}: found {len(java_files)} java files in {src_root}")

    for jf in java_files:
        content = read_file(jf)
        if content is None:
            continue

        if not need_to_parse(content):
            rel = os.path.relpath(jf, src_root)
            out_path = os.path.normpath(os.path.join(project_out_dir, rel))
            write_file(out_path, content)
            continue

        lock_present = has_lock(content)

        class_name = parse_class_name(content)

        pkg = parse_package(content)
        lock_fields = parse_lock_fields(content)
        methods = parse_public_methods(content, class_name)

        if lock_present:
            report["classes"].append({
                "project": project_name,
                "file": jf,
                "package": pkg,
                "class": class_name,
                "lock_fields": lock_fields,
                "methods": [m["name"] for m in methods],
            })

        if lock_present:
            new_content, mutations = mutate_class(
                content, class_name, lock_fields, methods
            )
        else:
            new_content = content
            mutations = []

        rel = os.path.relpath(jf, src_root)
        out_path = os.path.normpath(os.path.join(project_out_dir, rel))
        write_file(out_path, new_content)

        # creating harness
        if lock_present:
            print(f"creating harness: f{pkg}.f{class_name}Harness")
            report["mutations"].append({
                "project": project_name,
                "file": jf,
                "out": out_path,
                "mutations": mutations,
            })

            new_methods = parse_public_methods(new_content, class_name)

            if has_no_arg_constructor(content, class_name):
                ctor_args = None
                use_null_instance = False
            else:
                ctor_args = get_first_constructor_args(content, class_name)
                if ctor_args is None:
                    ctor_args = None
                    use_null_instance = True
                else:
                    use_null_instance = False

            harness_path = gen_harness(
                pkg, class_name, new_methods, project_out_dir,
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

def process_all_projects(working_dir, OUTPUT_DIR, retain_all = False):
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
            if retain_all:
                process_one_project_retain_all(pdir, pname, OUTPUT_DIR, report)
            else:
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
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--working-dir",
        dest="working_dir",
        default=WORKING_DIR,
        help=r"Root directory containing Java projects (e.g. D:\Projects)",
    )
    parser.add_argument(
        "--output-dir",
        dest="output_dir",
        default=None,
        help="Output directory for mutated projects (default: auto-generated)",
    )
    parser.add_argument(
        "--seed",
        dest="seed",
        type=int,
        default=SEED,
        help="Random seed",
    )
    parser.add_argument(
        "--retain-all-classes",
        dest="retain_all_classes",
        action="store_true",
        default=None,
        help="Retain all classes from the original project (not only those "
             "containing locks), so the project can still be compiled.",
    )
    args = parser.parse_args()

    random.seed(args.seed)

    # 用运行时参数覆盖全局常量
    global RETAIN_ALL_CLASSES, OUTPUT_DIR, REPORT_PATH
    if args.retain_all_classes:
        RETAIN_ALL_CLASSES = True

    if args.output_dir:
        OUTPUT_DIR = args.output_dir
        REPORT_PATH = os.path.join(OUTPUT_DIR, "mutation_report.json")

    output_dir = args.output_dir if args.output_dir else OUTPUT_DIR

    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)

    process_all_projects(args.working_dir, output_dir, RETAIN_ALL_CLASSES)

if __name__ == "__main__":
    main()