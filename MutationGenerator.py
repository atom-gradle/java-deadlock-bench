# -*- coding: utf-8 -*-
"""
Perform lock-related mutations on plain Java projects to generate
reproducible samples.
"""

import os, re, json, random, shutil, time, argparse

WORKING_DIR = r"D:\Projects"

OUTPUT_DIR = "./mutated-" + str(round(1000 * time.time()))
REPORT_PATH = os.path.join(OUTPUT_DIR, "mutation_report.json")

RETAIN_ALL_CLASSES = False

SEED = 42

P_REPLACE_LOCK = 0.5
P_REASSIGN_LOCK = 0.3
P_INSERT_SLEEP = 0.4
P_ADD_SYNC_BLOCK = 0.3
P_ADD_MOCK_METHOD = 1.0
P_ADD_CROSS_LOCK = 1.0

P_FAKE_CROSS_LOCK = 0.5

SKIP_REPLACE_LOCK_IF_CROSS = True

SLEEP_MIN = 100
SLEEP_MAX = 500

CROSS_LOCK_SLEEP_MIN = 50
CROSS_LOCK_SLEEP_MAX = 200

MAX_SLEEP_PER_CLASS = 3

NEST_DEPTH_MEAN = 2.0
NEST_DEPTH_STD = 1.5
NEST_DEPTH_MIN = 1
NEST_DEPTH_MAX = 10

CALL_DIST_MEAN = 2.5
CALL_DIST_STD = 1.5
CALL_DIST_MIN = 1
CALL_DIST_MAX = 10

MAX_HARNESS_EXTRA_METHODS = 3

CROSS_LOCK_MODE_WEIGHTS = {
    "AB_BA": 0.3,
    "ABC_CYCLE": 0.4,
    "NESTED_CROSS": 0.3,
}

LOCK_TYPES = {"ReentrantLock"}
LOCK_NAME_HINTS = ["lock", "mutex", "monitor", "semaphore", "guard"]

NON_MONITOR_LOCK_TYPES = {
    "ReentrantLock", "ReentrantReadWriteLock", "StampedLock",
    "Semaphore", "CountDownLatch", "CyclicBarrier",
}

RE_SYNC_BLOCK = re.compile(r'synchronized\s*\(\s*([^)]+?)\s*\)\s*\{', re.MULTILINE)
RE_LOCK_FIELD = re.compile(
    r'(?:private|protected|public)?\s*'
    r'(?:static\s+)?(?:final\s+)?'
    r'(\w+)\s+(\w+)\s*=\s*new\s+\1\s*\([^)]*\)\s*;',
    re.MULTILINE
)
RE_CLASS = re.compile(
    r'(?:^|\n)\s*(?:public\s+|protected\s+|private\s+)?'
    r'(?:abstract\s+|final\s+)?class\s+(\w+)',
    re.MULTILINE
)
RE_PACKAGE = re.compile(r'^\s*package\s+([\w\.]+)\s*;', re.MULTILINE)
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
# 分布采样
# ============================================================

def sample_normal_int(mean, std, lo, hi):
    v = int(round(random.gauss(mean, std)))
    return max(lo, min(hi, v))


def sample_nest_depth():
    return sample_normal_int(NEST_DEPTH_MEAN, NEST_DEPTH_STD,
                             NEST_DEPTH_MIN, NEST_DEPTH_MAX)


def sample_call_distance():
    return sample_normal_int(CALL_DIST_MEAN, CALL_DIST_STD,
                             CALL_DIST_MIN, CALL_DIST_MAX)


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
    if content is None:
        return
    path = os.path.normpath(path)
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def find_java_files(src_root):
    for dirpath, _, filenames in os.walk(src_root):
        for fn in filenames:
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def resolve_source_root(project_dir):
    for c in (
        os.path.join(project_dir, "src", "main", "java"),
        os.path.join(project_dir, "src"),
        os.path.join(project_dir, "agrona", "src", "main", "java"),
        os.path.join(project_dir, "jctools-core", "src", "main", "java"),
    ):
        if os.path.isdir(c):
            return c
    return None


# ============================================================
# 注释剥离 / 括号扫描
# ============================================================

def strip_comments(content):
    """
    剥离注释，但跳过字符串和字符字面量，避免把 "http://..." 里的 // 当注释。
    """
    result = []
    i = 0
    n = len(content)
    while i < n:
        c = content[i]
        # 行注释
        if c == '/' and i + 1 < n and content[i+1] == '/':
            j = content.find('\n', i)
            if j == -1:
                break
            i = j
            continue
        # 块注释
        if c == '/' and i + 1 < n and content[i+1] == '*':
            j = content.find('*/', i + 2)
            if j == -1:
                break
            i = j + 2
            continue
        # 字符串
        if c == '"':
            result.append(c)
            i += 1
            while i < n:
                result.append(content[i])
                if content[i] == '\\':
                    i += 1
                    if i < n:
                        result.append(content[i])
                    i += 1
                    continue
                if content[i] == '"':
                    i += 1
                    break
                i += 1
            continue
        # 字符
        if c == '\'':
            result.append(c)
            i += 1
            while i < n:
                result.append(content[i])
                if content[i] == '\\':
                    i += 1
                    if i < n:
                        result.append(content[i])
                    i += 1
                    continue
                if content[i] == '\'':
                    i += 1
                    break
                i += 1
            continue
        result.append(c)
        i += 1
    return ''.join(result)


def _scan_balanced_braces(text, start):
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
    """只删匿名内部类，保留命名内部类。"""
    result = []
    i = 0
    n = len(body)
    while i < n:
        m_anon = re.search(
            r'\bnew\s+[\w\.]+(?:<[^>{}]*>)?\s*\([^)]*\)\s*\{',
            body[i:]
        )
        if not m_anon:
            result.append(body[i:])
            break

        result.append(body[i:i + m_anon.start()])
        j = i + m_anon.end()
        j = _scan_balanced_braces(body, j)
        i = j

    return ''.join(result)


# ============================================================
# 字段区域抽取
# ============================================================

def _field_region(content, class_name):
    """
    抽取类体的字段区域：从类体开始到第一个方法/构造器声明之前。
    """
    clean = strip_comments(content)
    class_pattern = re.compile(
        r'\bclass\s+' + re.escape(class_name) + r'\b[^{]*\{',
        re.MULTILINE
    )
    m = class_pattern.search(clean)
    if not m:
        return ""

    start = m.end()
    end = _scan_balanced_braces(clean, start)
    body = clean[start:end - 1]

    method_decl = re.search(
        r'\n[ \t]*(?:(?:public|protected|private|static|final|synchronized|abstract|native|default)\s+)*'
        r'(?:[\w<>\[\],\s\?\.]+?\s+)?'
        r'[\w]+\s*\([^)]*\)\s*(?:throws\s+[^\n{};]+)?\s*\{',
        body
    )
    if method_decl:
        return body[:method_decl.start()]
    return body


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


def _infer_field_type(content, name, class_name):
    region = _field_region(content, class_name)
    if not region:
        return "Object"
    pat = re.compile(
        r'(?:^|\n)[ \t]*'
        r'(?:(?:private|protected|public|final|static|volatile|transient)\s+)*'
        r'([\w\.]+)(?:\s*<[^;{}]*>)?(?:\s*\[\])?\s+'
        + re.escape(name) + r'\s*[;=]',
        re.MULTILINE
    )
    m = pat.search(region)
    if m:
        return m.group(1)
    return "Object"


def parse_lock_fields(content):
    class_name = parse_class_name(content) or ""
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
            "inferred": False,
        })
        seen.add(name)

    for m in RE_SYNC_BLOCK.finditer(content):
        expr = m.group(1).strip()
        if expr == "this":
            continue
        if expr.isupper():
            continue
        if re.fullmatch(r"[A-Za-z_]\w*", expr) and expr not in seen:
            inferred_type = _infer_field_type(content, expr, class_name)
            fields.append({
                "type": inferred_type,
                "name": expr,
                "is_static": False,
                "is_final": True,
                "inferred": True,
            })
            seen.add(expr)

    return fields


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


def _ctor_region(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return ""
    body = remove_nested_classes(class_body)
    return body


def get_first_constructor_args(content, class_name):
    body = _ctor_region(content, class_name)
    if not body:
        return None
    pattern = re.compile(
        r'(?:^|\n)[ \t]*(?:public|protected|private)?[ \t]*'
        + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    m = pattern.search(body)
    if not m:
        return None
    return m.group(1).strip()


def has_no_arg_constructor(content, class_name):
    body = _ctor_region(content, class_name)
    if not body:
        return False

    any_ctor_pattern = re.compile(
        r'(?:^|\n)[ \t]*(?:public|protected|private)?[ \t]*'
        + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    if not list(any_ctor_pattern.finditer(body)):
        return True

    public_ctor_pattern = re.compile(
        r'(?:^|\n)[ \t]*public[ \t]+' + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    for c in public_ctor_pattern.finditer(body):
        if not c.group(1).strip():
            return True

    return False


def has_private_ctor(content, class_name):
    body = _ctor_region(content, class_name)
    if not body:
        return False
    pat = re.compile(
        r'(?:^|\n)[ \t]*private[ \t]+' + re.escape(class_name) + r'[ \t]*\(([^)]*)\)[ \t]*'
        r'(?:throws[ \t]+[^\n{};]+)?\{',
        re.MULTILINE
    )
    for c in pat.finditer(body):
        if not c.group(1).strip():
            return True
    return False


def has_lock(content):
    if RE_SYNC_BLOCK.search(content):
        return True
    if parse_lock_fields(content):
        return True
    return False


def _brace_depth_at(text, pos):
    depth = 0
    i = 0
    in_string = False
    in_char = False
    while i < pos:
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
    return depth


def parse_public_methods(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return []

    class_body_no_anon = remove_nested_classes(class_body)

    methods = []
    seen_sig = set()
    for mm in RE_PUBLIC_METHOD.finditer(class_body_no_anon):
        depth = _brace_depth_at(class_body_no_anon, mm.start())
        if depth != 0:
            continue

        ret_type = mm.group(1).strip()
        name = mm.group(2)
        params = mm.group(3).strip()
        if name == class_name:
            continue
        sig = (name, params)
        if sig in seen_sig:
            continue
        seen_sig.add(sig)
        matched_text = class_body_no_anon[mm.start():mm.end()]
        methods.append({
            "return_type": ret_type,
            "name": name,
            "params": params,
            "is_static": "static" in matched_text,
            "is_synchronized": "synchronized" in matched_text,
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
        p = re.sub(r"\bfinal\b", "", p).strip()
        m_var = re.match(r'([\w\.]+)\s*\.\.\.', p)
        if m_var:
            args.append(f"({m_var.group(1)}[]) null")
            continue
        m = re.match(r'([\w\.]+)', p)
        if not m:
            args.append("null")
            continue
        typ = m.group(1)
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
        else:
            args.append(f"({typ}) null")
    return args


# ============================================================
# 基础变异
# ============================================================

def mutate_replace_lock(content, lock_fields):
    if not lock_fields:
        return content, []
    records = []
    names = [f["name"] for f in lock_fields]
    replaced_positions = set()

    def repl(m):
        old = m.group(1).strip()
        if not re.fullmatch(r"[A-Za-z_]\w*", old):
            return m.group(0)
        key = m.start()
        if key in replaced_positions:
            return m.group(0)
        candidates = [n for n in names if n != old]
        if not candidates:
            return m.group(0)
        new = random.choice(candidates)
        replaced_positions.add(key)
        records.append({"op": "replace_lock", "old": old, "new": new})
        return f"synchronized ({new}) {{"

    return RE_SYNC_BLOCK.sub(repl, content), records


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

    return RE_SYNC_BLOCK.sub(repl, content), records


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


# ============================================================
# 交叉加锁变异
# ============================================================

def _collect_usable_locks(lock_fields):
    seen = set()
    out = []
    for f in lock_fields:
        if f.get("is_static", False):
            continue
        if f.get("type") in NON_MONITOR_LOCK_TYPES:
            continue
        n = f["name"]
        if n in seen:
            continue
        seen.add(n)
        out.append({
            "name": n,
            "inferred": f.get("inferred", False),
            "type": f.get("type", "Object"),
        })
    return out


def _field_exists(content, name, class_name):
    region = _field_region(content, class_name)
    if not region:
        return False
    pat = re.compile(
        r'(?:^|\n)[ \t]*'
        r'(?:(?:private|protected|public|final|static|volatile|transient)\s+)*'
        r'(?:[\w\.]+)(?:\s*<[^;{}]*>)?(?:\s*\[\])?\s+'
        + re.escape(name) + r'\s*[;=]',
        re.MULTILINE
    )
    return pat.search(region) is not None


def _ensure_locks(content, usable, k, class_name):
    locks = [f["name"] for f in usable[:k]]
    extra = []

    for f in usable[:k]:
        if f.get("inferred") and not _field_exists(content, f["name"], class_name):
            if f["name"] not in extra:
                extra.append(f["name"])

    while len(locks) < k:
        nm = f"__crossLock_{random.randint(1000, 9999)}"
        if nm in locks or nm in extra:
            continue
        locks.append(nm)
        extra.append(nm)

    if extra:
        decl = "".join(
            f"\n    private final Object {n} = new Object();\n" for n in extra
        )
        return locks, decl
    return locks, None


def _fake_guard_code():
    return """
    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }
"""


def _gen_cross_method(method_name, lock_seq, sleep_ms, fake):
    outer = lock_seq[:-1] if fake and len(lock_seq) >= 2 else lock_seq
    inner = lock_seq[-1] if fake and len(lock_seq) >= 2 else None

    body_lines = []
    for i, lk in enumerate(outer):
        body_lines.append(f"        synchronized ({lk}) {{")
        if i == 0 and sleep_ms is not None:
            body_lines.append(
                f"            try {{ Thread.sleep({sleep_ms}); }} "
                f"catch (InterruptedException e) {{ }}"
            )
    if inner is not None:
        body_lines.append("            if (__crossLockGuard()) {")
        body_lines.append(f"                synchronized ({inner}) {{ }}")
        body_lines.append("            }")
    for _ in outer:
        body_lines.append("        }")

    body = "\n".join(body_lines)
    return f"""
    public void {method_name}() {{
{body}
    }}
"""


def _weighted_choice(weights):
    items = list(weights.items())
    total = sum(w for _, w in items)
    r = random.random() * total
    acc = 0.0
    for k, w in items:
        acc += w
        if r <= acc:
            return k
    return items[-1][0]


def mutate_add_cross_lock(content, lock_fields, class_name):
    if random.random() >= P_ADD_CROSS_LOCK:
        return content, [], None

    usable = _collect_usable_locks(lock_fields)
    fake = random.random() < P_FAKE_CROSS_LOCK

    if len(usable) >= 3:
        mode = _weighted_choice(CROSS_LOCK_MODE_WEIGHTS)
    else:
        mode = "AB_BA"

    depth = max(2, sample_nest_depth())

    if mode == "AB_BA":
        locks, decl = _ensure_locks(content, usable, max(2, depth), class_name)
    elif mode == "ABC_CYCLE":
        locks, decl = _ensure_locks(content, usable, max(3, depth), class_name)
    elif mode == "NESTED_CROSS":
        locks, decl = _ensure_locks(content, usable, max(3, depth), class_name)
    else:
        return content, [], None

    methods = []
    method_codes = []
    s1 = random.randint(CROSS_LOCK_SLEEP_MIN, CROSS_LOCK_SLEEP_MAX)

    if mode == "AB_BA":
        a, b = locks[0], locks[1]
        m1 = f"crossLock_1_{random.randint(0, 9999)}"
        m2 = f"crossLock_2_{random.randint(0, 9999)}"
        method_codes.append(_gen_cross_method(m1, [a, b], s1, fake))
        method_codes.append(_gen_cross_method(m2, [b, a], None, fake))
        methods = [m1, m2]

    elif mode == "ABC_CYCLE":
        a, b, c = locks[0], locks[1], locks[2]
        m1 = f"crossLock_1_{random.randint(0, 9999)}"
        m2 = f"crossLock_2_{random.randint(0, 9999)}"
        m3 = f"crossLock_3_{random.randint(0, 9999)}"
        method_codes.append(_gen_cross_method(m1, [a, b], s1, fake))
        method_codes.append(_gen_cross_method(m2, [b, c], None, fake))
        method_codes.append(_gen_cross_method(m3, [c, a], None, fake))
        methods = [m1, m2, m3]

    elif mode == "NESTED_CROSS":
        seq_fwd = locks[:depth]
        seq_bwd = list(reversed(seq_fwd))
        m1 = f"crossLock_1_{random.randint(0, 9999)}"
        m2 = f"crossLock_2_{random.randint(0, 9999)}"
        method_codes.append(_gen_cross_method(m1, seq_fwd, s1, fake))
        method_codes.append(_gen_cross_method(m2, seq_bwd, None, fake))
        methods = [m1, m2]

    idx = content.rfind("}")
    if idx == -1:
        return content, [], None

    guard_code = _fake_guard_code() if fake else ""
    prefix = (decl if decl else "") + guard_code
    new_content = content[:idx] + prefix + "".join(method_codes) + content[idx:]

    info = {
        "mode": mode,
        "locks": locks,
        "methods": methods,
        "fake": fake,
        "nest_depth": depth,
    }
    rec = {
        "op": "add_cross_lock",
        "mode": mode,
        "locks": locks,
        "methods": methods,
        "fake": fake,
        "nest_depth": depth,
    }
    return new_content, [rec], info


# ============================================================
# mock 链
# ============================================================

def _insert_call_into_method(content, method, call_line):
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
    indented = "\n".join(
        (inner_indent + ln) if ln.strip() else ln
        for ln in call_line.split("\n")
    )
    return content[:insert_pos] + f"\n{indented}" + content[insert_pos:]

def _build_mock_chain(methods, depth_limit, sync_block_name=None,
                      exclude_method=None):
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

    chain = []
    next_call = tail_call
    mid_depth = depth_limit // 2
    for depth in range(depth_limit - 1, -1, -1):
        method_name = f"mockMethod_{depth}_{random.randint(0, 9999)}"
        sync_line = f"{sync_block_name}();" if (sync_block_name and depth == mid_depth) else ""
        method_code = f"""
    public void {method_name}() throws Exception {{
        {next_call}
        {sync_line}
    }}
"""
        chain.append({"name": method_name, "code": method_code})
        next_call = f"{method_name}();"

    chain.reverse()
    return chain, chain[0]["name"], tail_target


def mutate_add_mock_chain(content, methods, insert_into_method=None,
                          sync_block_name=None):
    depth_limit = sample_call_distance()
    chain, head_name, tail_target = _build_mock_chain(
        methods, depth_limit,
        sync_block_name=sync_block_name,
        exclude_method=insert_into_method,
    )
    if not chain:
        return content, []
    idx = content.rfind("}")
    if idx == -1:
        return content, []
    chain_code = "\n".join(c["code"] for c in chain)
    new_content = content[:idx] + chain_code + content[idx:]

    if insert_into_method is not None:
        call_line = (
            f"try {{\n"
            f"    {head_name}();\n"
            f"}} catch (Exception e) {{\n"
            f"    throw new RuntimeException(e);\n"
            f"}}"
        )
        new_content = _insert_call_into_method(
            new_content, insert_into_method, call_line
        )

    return new_content, [{
        "op": "add_mock_chain",
        "head": head_name,
        "length": len(chain),
        "tail_target": tail_target["name"] if tail_target else None,
        "inserted_into": insert_into_method["name"] if insert_into_method else None,
        "chain": [c["name"] for c in chain],
    }]


# ============================================================
# 组合变异
# ============================================================

def mutate_class(content, class_name, lock_fields, methods):
    records = []
    new_content = content
    sync_block_name = None

    new_content, cross_recs, cross_info = mutate_add_cross_lock(
        new_content, lock_fields, class_name
    )
    records.extend(cross_recs)
    cross_hit = bool(cross_info)

    do_replace = random.random() < P_REPLACE_LOCK
    if cross_hit and SKIP_REPLACE_LOCK_IF_CROSS:
        do_replace = False
        records.append({"op": "skip_replace_lock",
                        "reason": "cross_lock_hit"})

    if do_replace:
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

    return new_content, records, cross_info


# ============================================================
# Harness 生成
# ============================================================

def _emit_call(lines, class_name, m, indent="        "):
    args = ", ".join(simple_args_for(m["params"]))
    if m.get("is_static"):
        call_expr = f"{class_name}.{m['name']}({args})"
    else:
        call_expr = f"instance.{m['name']}({args})"
    if m["return_type"] == "void":
        lines.append(f"{indent}try {{ {call_expr}; }} catch (Throwable t) {{ }}")
    else:
        lines.append(f"{indent}try {{ Object _r = {call_expr}; }} catch (Throwable t) {{ }}")


def gen_harness(pkg, class_name, methods, project_out_dir,
                ctor_args=None, use_null_instance=None,
                cross_lock_info=None, need_reflection=False):
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
    if need_reflection:
        lines.append("import java.lang.reflect.Constructor;")
        lines.append("")

    lines.append(f"public class {harness_class} {{")
    lines.append("    public static void main(String[] args) throws Exception {")

    if need_reflection:
        lines.append(
            f"        Constructor<{class_name}> __ctor = "
            f"{class_name}.class.getDeclaredConstructor();"
        )
        lines.append("        __ctor.setAccessible(true);")
        lines.append(f"        {class_name} instance = __ctor.newInstance();")
    elif use_null_instance:
        lines.append(f"        {class_name} instance = null;")
    elif ctor_args is None:
        lines.append(f"        {class_name} instance = new {class_name}();")
    else:
        args = ", ".join(simple_args_for(ctor_args))
        lines.append(f"        {class_name} instance = new {class_name}({args});")

    mock_methods = [m for m in methods if m["name"].startswith("mockMethod_")]
    mock_methods.sort(key=lambda m: m["name"])
    other_methods = [m for m in methods if not m["name"].startswith("mockMethod_")]

    cross_methods = []
    cross_method_names = set()
    if cross_lock_info:
        cross_method_names = set(cross_lock_info.get("methods", []))
        for nm in cross_lock_info.get("methods", []):
            for m in other_methods:
                if m["name"] == nm:
                    cross_methods.append(m)
                    break

    # ------------------------------------------------------------
    # 关键修改：抽样方法排除 crossLock
    # crossLock 只在多线程段调用，避免污染线程归属
    # ------------------------------------------------------------
    sampleable = [m for m in other_methods if m["name"] not in cross_method_names]
    sampled_others = []
    if sampleable:
        k = min(MAX_HARNESS_EXTRA_METHODS, len(sampleable))
        sampled_others = random.sample(sampleable, k)

    # 顺序调用段：只有抽样方法（不含 crossLock）
    seq = sampled_others
    for m in seq:
        lines.append(f"        // call {m['name']}")
        _emit_call(lines, class_name, m)
        lines.append("")

    # 多线程段：每个 crossLock 单独占一个线程
    threads = []
    threads.extend(cross_methods)
    if mock_methods:
        threads.append(mock_methods[0])

    remaining = [m for m in (mock_methods + sampled_others) if m not in threads]
    while len(threads) < 4 and remaining:
        chosen = random.choice(remaining)
        threads.append(chosen)
        remaining.remove(chosen)
    while len(threads) < 2 and (mock_methods + sampled_others):
        threads.append(random.choice(mock_methods + sampled_others))

    if threads:
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
                f"            try {{ {call_expr}; }} catch (Throwable t) {{ }}"
            )
            lines.append("        });")

        for var in thread_vars:
            lines.append(f"        {var}.start();")
        for var in thread_vars:
            lines.append(f"        {var}.join(5000);")

    lines.append("    }")
    lines.append("}")
    content = "\n".join(lines)
    write_file(path, content)
    return path

# ============================================================
# 项目处理
# ============================================================

def _decide_instance(content, class_name):
    if has_no_arg_constructor(content, class_name):
        return None, False, False
    if has_private_ctor(content, class_name):
        return None, False, True
    ctor_args = get_first_constructor_args(content, class_name)
    if ctor_args is None:
        return None, True, False
    return ctor_args, False, False


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

        if not need_to_parse(content):
            rel = os.path.relpath(jf, src_root)
            out_path = os.path.normpath(os.path.join(project_out_dir, rel))
            write_file(out_path, content)
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

        new_content, mutations, cross_info = mutate_class(
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
        if not new_methods:
            continue

        ctor_args, use_null_instance, need_refl = _decide_instance(content, class_name)

        harness_path = gen_harness(
            pkg, class_name, new_methods, project_out_dir,
            ctor_args=ctor_args,
            use_null_instance=use_null_instance,
            cross_lock_info=cross_info,
            need_reflection=need_refl,
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
        methods = parse_public_methods(content, class_name) if class_name else []

        if lock_present:
            report["classes"].append({
                "project": project_name,
                "file": jf,
                "package": pkg,
                "class": class_name,
                "lock_fields": lock_fields,
                "methods": [m["name"] for m in methods],
            })

        cross_info = None
        if lock_present and class_name:
            new_content, mutations, cross_info = mutate_class(
                content, class_name, lock_fields, methods
            )
        else:
            new_content = content
            mutations = []

        rel = os.path.relpath(jf, src_root)
        out_path = os.path.normpath(os.path.join(project_out_dir, rel))
        write_file(out_path, new_content)

        if lock_present and class_name:
            report["mutations"].append({
                "project": project_name,
                "file": jf,
                "out": out_path,
                "mutations": mutations,
            })

            new_methods = parse_public_methods(new_content, class_name)
            if not new_methods:
                continue

            print(f"creating harness: {pkg}.{class_name}Harness")
            ctor_args, use_null_instance, need_refl = _decide_instance(content, class_name)

            harness_path = gen_harness(
                pkg, class_name, new_methods, project_out_dir,
                ctor_args=ctor_args,
                use_null_instance=use_null_instance,
                cross_lock_info=cross_info,
                need_reflection=need_refl,
            )

            report["harnesses"].append({
                "project": project_name,
                "class": class_name,
                "harness": harness_path,
            })


# ============================================================
# 主流程
# ============================================================

def process_all_projects(working_dir, OUTPUT_DIR, retain_all=False):
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
            report["errors"].append({"project": pname, "error": str(e)})
            print(f"[ERROR] {pname}: {e}")

    write_file(REPORT_PATH, json.dumps(report, indent=2, ensure_ascii=False))
    print(f"[INFO] report written to {REPORT_PATH}")
    print(f"[INFO] mutated {len(report['mutations'])} classes")
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--working-dir", dest="working_dir",
                        default=WORKING_DIR)
    parser.add_argument("--output-dir", dest="output_dir", default=None)
    parser.add_argument("--seed", dest="seed", type=int, default=SEED)
    parser.add_argument("--retain-all-classes", dest="retain_all_classes",
                        action="store_true", default=None)
    args = parser.parse_args()

    random.seed(args.seed)

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
