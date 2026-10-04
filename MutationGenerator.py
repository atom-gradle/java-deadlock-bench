# -*- coding: utf-8 -*-
"""
Perform lock-related mutations on plain Java projects to generate
reproducible samples.
"""

import os, re, json, random, shutil, time, argparse

# replace with YOUR WORKING_DIR
WORKING_DIR = r"D:\Projects"

OUTPUT_DIR = "./mutated-" + str(round(1000 * time.time()))
REPORT_PATH = os.path.join(OUTPUT_DIR, "mutation_report.json")

RETAIN_ALL_CLASSES = False
SEED = 42

# ============================================================
# Probability
# ============================================================
P_REPLACE_LOCK = 0.5
P_REASSIGN_LOCK = 0.3
P_INSERT_SLEEP = 0.4
P_ADD_SYNC_BLOCK = 0.3
P_ADD_MOCK_METHOD = 1.0
P_ADD_CROSS_LOCK = 1.0

P_WRAP_METHOD_WITH_SYNC = 1.0
P_NEST_SYNC_BLOCK = 1.0
P_INSERT_CALL_CHAIN = 0.6
P_ADD_DEEP_NESTING = 0.9
MAX_WRAP_METHODS_PER_CLASS = 2
MAX_NEST_SYNC_PER_CLASS = 3
MAX_CALL_CHAIN_PER_CLASS = 1
MAX_DEEP_NESTING_PER_CLASS = 2

P_FAKE_CROSS_LOCK = 0.15

SKIP_REPLACE_LOCK_IF_CROSS = True

# ============================================================
# 数值
# ============================================================
SLEEP_MIN = 100
SLEEP_MAX = 500
CROSS_LOCK_SLEEP_MIN = 100
CROSS_LOCK_SLEEP_MAX = 300
MAX_SLEEP_PER_CLASS = 3

NEST_DEPTH_MEAN = 2.0
NEST_DEPTH_STD = 1.5
NEST_DEPTH_MIN = 1
NEST_DEPTH_MAX = 10

CALL_DIST_MEAN = 2.5
CALL_DIST_STD = 1.5
CALL_DIST_MIN = 1
CALL_DIST_MAX = 10

RING_TOPO_WEIGHTS = {
    "in_method_nested": 0.75,
    "chain_to_lock": 0.25,
}

RING_CHAIN_DIST_MIN = 1
RING_CHAIN_DIST_MAX = 2

MAX_HARNESS_EXTRA_METHODS = 3
CROSS_LOCK_GROUPS_PER_CLASS = 3

CROSS_LOCK_MODE_WEIGHTS = {
    "AB_BA": 0.25,
    "ABC_CYCLE": 0.25,
    "ABCD_CYCLE": 0.25,
    "NESTED_CROSS": 0.25,
}

LOCK_TYPES = {"ReentrantLock"}
LOCK_NAME_HINTS = ["lock", "mutex", "monitor", "semaphore", "guard"]

NON_MONITOR_LOCK_TYPES = {
    "ReentrantLock", "ReentrantReadWriteLock", "StampedLock",
    "Semaphore", "CountDownLatch", "CyclicBarrier",
}

ALWAYS_EXCLUDE_METHODS = {"toString", "equals", "hashCode"}

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

RE_LOCK_CALL = re.compile(r'\b(\w+)\s*\.\s*lock\s*\(\s*\)\s*;')
RE_UNLOCK_CALL = re.compile(r'\b(\w+)\s*\.\s*unlock\s*\(\s*\)\s*;')
RE_TRY_FINALLY_LOCK = re.compile(
    r'\b(\w+)\s*\.\s*lock\s*\(\s*\)\s*;\s*try\s*\{',
    re.MULTILINE
)


# ============================================================
# 采样
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


def sample_ring_chain_dist():
    return random.randint(RING_CHAIN_DIST_MIN, RING_CHAIN_DIST_MAX)


# ============================================================
# IO
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
        os.path.join(project_dir, "guava", "src"),
        os.path.join(project_dir, "flyway-core", "src", "main", "java"),
    ):
        if os.path.isdir(c):
            return c
    return None


# ============================================================
# 注释剥离 / 括号扫描
# ============================================================
def strip_comments(content):
    result = []
    i = 0
    n = len(content)
    while i < n:
        c = content[i]
        if c == '/' and i + 1 < n and content[i+1] == '/':
            j = content.find('\n', i)
            if j == -1:
                break
            i = j
            continue
        if c == '/' and i + 1 < n and content[i+1] == '*':
            j = content.find('*/', i + 2)
            if j == -1:
                break
            i = j + 2
            continue
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
# 字段区域
# ============================================================
def _field_region(content, class_name):
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
    return remove_nested_classes(class_body)


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
    if RE_LOCK_CALL.search(content):
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
        if name in ALWAYS_EXCLUDE_METHODS:
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


def parse_lock_unlock_methods(content, class_name):
    class_body = _get_class_body(content, class_name)
    if class_body is None:
        return []
    body_no_anon = remove_nested_classes(class_body)

    methods = []
    for mm in RE_PUBLIC_METHOD.finditer(body_no_anon):
        if _brace_depth_at(body_no_anon, mm.start()) != 0:
            continue
        name = mm.group(2)
        if name in ALWAYS_EXCLUDE_METHODS:
            continue
        open_brace = mm.end() - 1
        end = _scan_balanced_braces(body_no_anon, open_brace + 1)
        method_body = body_no_anon[open_brace + 1:end - 1]

        lock_names = set(m.group(1) for m in RE_LOCK_CALL.finditer(method_body))
        lock_names |= set(m.group(1) for m in RE_TRY_FINALLY_LOCK.finditer(method_body))
        if not lock_names:
            continue

        style = "try_finally" if RE_TRY_FINALLY_LOCK.search(method_body) else "lock_unlock"
        matched_text = body_no_anon[mm.start():mm.end()]
        methods.append({
            "return_type": mm.group(1).strip(),
            "name": name,
            "params": mm.group(3).strip(),
            "is_static": "static" in matched_text,
            "is_synchronized": "synchronized" in matched_text,
            "lock_fields": sorted(lock_names),
            "style": style,
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
# 方法体定位
# ============================================================

def _locate_method_body(content, method_name):
    pat = re.compile(
        r'([ \t]*(?:public|protected|private)?[ \t]*(?:static[ \t]+)?'
        r'(?:final[ \t]+)?(?:synchronized[ \t]+)?'
        r'(?:abstract[ \t]+)?(?:native[ \t]+)?'
        r'[\w<>\[\],\s\?\.]+?[ \t]+' + re.escape(method_name) +
        r'[ \t]*\([^)]*\)[ \t\n]*(?:throws[ \t]+[^\n{};]+)?[ \t\n]*\{)',
        re.MULTILINE
    )
    m = pat.search(content)
    if not m:
        return None
    open_brace = m.end() - 1
    close_brace = _scan_balanced_braces(content, open_brace + 1) - 1
    if close_brace <= open_brace:
        return None
    return (open_brace, close_brace, m.start())


def _method_body_has_lock(content, method_name):
    loc = _locate_method_body(content, method_name)
    if loc is None:
        return False
    open_brace, close_brace, _ = loc
    body = content[open_brace + 1:close_brace]
    if RE_SYNC_BLOCK.search(body):
        return True
    if RE_LOCK_CALL.search(body):
        return True
    if RE_TRY_FINALLY_LOCK.search(body):
        return True
    return False


# ============================================================
# 锁字段声明工具
# ============================================================

def _collect_usable_locks(lock_fields):
    seen = set()
    out = []
    for f in lock_fields:
        if f.get("is_static", False):
            continue
        typ = f.get("type", "Object")
        if typ in NON_MONITOR_LOCK_TYPES and typ != "ReentrantLock":
            continue
        n = f["name"]
        if n in seen:
            continue
        seen.add(n)
        out.append({
            "name": n,
            "inferred": f.get("inferred", False),
            "type": typ,
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


def _class_end_pos(content, class_name):
    """
    返回**原始 content** 中类体最后 '}' 的位置。
    关键：使用 strip_comments 后找到 '}' 的位置，然后在原始 content 上
    找到对应的字符——为了对齐，使用"从末尾倒数"的策略：
    找最后一个 `\n}` 出现在文件里的位置（且该 `}` 是类的闭合括号）。
    """
    clean = strip_comments(content)
    class_pattern = re.compile(
        r'\bclass\s+' + re.escape(class_name) + r'\b[^{]*\{',
        re.MULTILINE
    )
    m = class_pattern.search(clean)
    if not m:
        return None
    # 在 clean 上找类的结束 '}'
    start = m.end()
    end_in_clean = _scan_balanced_braces(clean, start) - 1
    # 因为 strip_comments 会删掉注释字符，end_in_clean 的位置不能直接用到原 content 上
    # 简单策略：在原始 content 中查找类体最后的 '}'——最后一个非空行以 '}' 结尾
    # 更精确：从 end_in_clean 对应的内容附近（±2000 字符）在原始 content 里找相同的 '}' 位置
    # 保守做法：直接从 content 尾部向前找最后一个单独的 '}'
    idx = content.rfind('}')
    while idx >= 0:
        # 检查前面是不是空白+换行（即一个独立的 '}'）
        j = idx - 1
        while j >= 0 and content[j] in ' \t\r\n':
            j -= 1
        # 找当前 '}' 是否在类体内（用括号计数快速验证）
        return idx  # 直接返回最后一个 '}'，它是类的结束
    return None


def _ensure_locks(content, usable, k, class_name):
    locks = []
    extra = []

    for f in usable:
        if len(locks) >= k:
            break
        n = f["name"]
        if n in locks:
            continue
        locks.append(n)
        if f.get("inferred") and not _field_exists(content, n, class_name):
            if n not in extra:
                extra.append(n)

    while len(locks) < k:
        nm = f"__crossLock_{random.randint(1000, 9999)}"
        while nm in locks or nm in extra:
            nm = f"__crossLock_{random.randint(1000, 9999)}"
        locks.append(nm)
        extra.append(nm)

    decl = "".join(
        f"\n    private final Object {n} = new Object();\n" for n in extra
    )
    return locks, decl


def _ensure_class_has_a_lock(content, class_name):
    fields = parse_lock_fields(content)
    usable = _collect_usable_locks(fields)
    if usable:
        return content, usable[0]["name"]

    pos = _class_end_pos(content, class_name)
    if pos is None:
        return content, None
    lock_name = f"__crossLock_{random.randint(1000, 9999)}"
    decl = f"\n\n    private final Object {lock_name} = new Object();\n"
    new_content = content[:pos] + decl + content[pos:]
    return new_content, lock_name


def _insert_decls_at_class_end(content, decl, class_name):
    """
    把 decl 插入到类体末尾（紧挨最后的 '}' 之前）。
    关键修复：在 '}' 前面**强制换行**，避免跟注释串（如 `// moved into url as get params`）。
    """
    if not decl:
        return content
    pos = _class_end_pos(content, class_name)
    if pos is None:
        return content
    # 前面留 2 个换行，避免跟已有注释串
    decl_padded = "\n\n" + decl.strip("\n") + "\n"
    return content[:pos] + decl_padded + content[pos:]


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
# 需求 1：方法体嵌套
# ============================================================

def mutate_wrap_method_with_sync(content, class_name, method, lock_name,
                                 sleep_ms=None, use_new_lock=True):
    loc = _locate_method_body(content, method["name"])
    if loc is None:
        return content, [], ""

    open_brace, close_brace, mstart = loc
    body_text = content[open_brace + 1:close_brace]
    inner_locks = set(
        m.group(1).strip() for m in RE_SYNC_BLOCK.finditer(body_text)
    )

    wrap_lock = lock_name
    extra_decl = ""
    if use_new_lock and wrap_lock in inner_locks:
        new_lock = f"__crossLock_{random.randint(1000, 9999)}"
        wrap_lock = new_lock
        extra_decl = f"\n    private final Object {new_lock} = new Object();\n"

    line_start = content.rfind('\n', 0, mstart) + 1
    indent_match = re.match(r'[ \t]*', content[line_start:mstart])
    indent = indent_match.group(0) if indent_match else ""
    inner_indent = indent + "    "

    body = content[open_brace + 1:close_brace]
    indented_body = "\n".join(
        (inner_indent + ln) if ln.strip() else ln
        for ln in body.split("\n")
    )

    sleep_line = ""
    if sleep_ms is not None:
        sleep_line = (
            f"\n{inner_indent}try {{ Thread.sleep({sleep_ms}); }} "
            f"catch (InterruptedException e) {{ }}"
        )

    wrapped = (
        f"\n{inner_indent}synchronized ({wrap_lock}) {{{sleep_line}\n"
        f"{indented_body}\n{inner_indent}}}"
    )
    new_content = content[:open_brace + 1] + wrapped + content[close_brace:]

    return new_content, [{
        "op": "wrap_method_with_sync",
        "method": method["name"],
        "lock": wrap_lock,
        "sleep": sleep_ms,
    }], extra_decl


def mutate_nest_inside_sync_block(content, class_name, base_lock_name, sleep_ms=None):
    records = []
    extra_decls = []
    matches = list(RE_SYNC_BLOCK.finditer(content))
    if not matches:
        return content, [], []

    for m in reversed(matches):
        if len(records) >= MAX_NEST_SYNC_PER_CLASS:
            break
        old_lock = m.group(1).strip()

        open_brace = m.end() - 1
        close_brace = _scan_balanced_braces(content, open_brace + 1) - 1
        if close_brace <= open_brace:
            continue

        all_lock_names = set(m2.group(1).strip()
                             for m2 in RE_SYNC_BLOCK.finditer(content))
        all_lock_names.add(old_lock)

        new_lock = base_lock_name
        extra_decl = ""
        if new_lock in all_lock_names:
            new_lock = f"__crossLock_{random.randint(1000, 9999)}"
            extra_decl = f"\n    private final Object {new_lock} = new Object();\n"

        line_start = content.rfind('\n', 0, m.start()) + 1
        indent = re.match(r'[ \t]*', content[line_start:m.start()]).group(0)
        inner_indent = indent + "    "

        sleep_line = ""
        if sleep_ms is not None:
            sleep_line = (
                f"\n{inner_indent}    try {{ Thread.sleep({sleep_ms}); }} "
                f"catch (InterruptedException e) {{ }}"
            )

        body = content[open_brace + 1:close_brace]
        indented_body = "\n".join(
            (inner_indent + ln) if ln.strip() else ln
            for ln in body.split("\n")
        )
        injected = (
            f"\n{inner_indent}synchronized ({new_lock}) {{{sleep_line}\n"
            f"{indented_body}\n{inner_indent}}}"
        )
        content = content[:open_brace + 1] + injected + content[close_brace:]

        records.append({
            "op": "nest_inside_sync",
            "outer_lock": old_lock,
            "inner_lock": new_lock,
            "sleep": sleep_ms,
        })
        if extra_decl:
            extra_decls.append(extra_decl)

    return content, records, extra_decls


def mutate_add_deep_nesting(content, class_name, lock_fields, methods,
                            depth=None):
    """
    返回 (new_content, records, decl, host_method_name)
    """
    if not methods:
        return content, [], "", None

    if depth is None:
        depth = random.choice([2, 3, 4])
    depth = max(2, depth)

    usable = _collect_usable_locks(lock_fields)
    locks, decl = _ensure_locks(content, usable, depth, class_name)

    candidates = [m for m in methods
                  if not m.get("is_static")
                  and m["name"] not in ALWAYS_EXCLUDE_METHODS
                  and not m["name"].startswith("mockMethod_")
                  and not m["name"].startswith("crossLock_")
                  and not m["name"].startswith("__")
                  and not m["name"].startswith("syncBlock_")]
    if not candidates:
        return content, [], "", None
    host = random.choice(candidates)
    loc = _locate_method_body(content, host["name"])
    if loc is None:
        return content, [], "", None
    open_brace, close_brace, mstart = loc

    line_start = content.rfind('\n', 0, mstart) + 1
    indent = re.match(r'[ \t]*', content[line_start:mstart]).group(0)
    inner_indent = indent + "    "

    body = content[open_brace + 1:close_brace]
    indented_body = "\n".join(
        (inner_indent + ln) if ln.strip() else ln
        for ln in body.split("\n")
    )

    sleep_ms = random.randint(SLEEP_MIN, SLEEP_MAX)

    nest_lines = []
    cur_indent = inner_indent
    for lk in locks:
        nest_lines.append(f"{cur_indent}synchronized ({lk}) {{")
        nest_lines.append(
            f"{cur_indent}    try {{ Thread.sleep({sleep_ms}); }} "
            f"catch (InterruptedException e) {{ }}"
        )
        cur_indent += "    "
    nest_lines.append(indented_body)
    for _ in locks:
        cur_indent = cur_indent[:-4]
        nest_lines.append(f"{cur_indent}}}")

    nested = "\n" + "\n".join(nest_lines)
    new_content = content[:open_brace + 1] + nested + content[close_brace:]

    return new_content, [{
        "op": "add_deep_nesting",
        "host_method": host["name"],
        "depth": depth,
        "locks": locks,
        "sleep_ms": sleep_ms,
    }], decl, host["name"]


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


def mutate_insert_call_chain(content, method, methods, target_method,
                             depth=None):
    if depth is None:
        depth = sample_call_distance()

    chain = []
    next_call = ""
    if target_method is not None:
        args = ", ".join(simple_args_for(target_method["params"]))
        next_call = f"this.{target_method['name']}({args});"

    for d in range(depth):
        nm = f"__distChain_{method['name']}_{d}_{random.randint(0, 9999)}"
        code = f"""
    private void {nm}() throws Exception {{
        {next_call}
    }}
"""
        chain.append({"name": nm, "code": code})
        next_call = f"{nm}();"

    chain.reverse()

    idx = content.rfind("}")
    if idx == -1:
        return content, []
    chain_code = "\n".join(c["code"] for c in chain)
    content = content[:idx] + chain_code + content[idx:]

    head = chain[0]["name"]
    call_line = (
        f"try {{\n"
        f"    {head}();\n"
        f"}} catch (Exception e) {{\n"
        f"    throw new RuntimeException(e);\n"
        f"}}"
    )
    content = _insert_call_into_method(content, method, call_line)

    return content, [{
        "op": "insert_call_chain",
        "into": method["name"],
        "depth": depth,
        "chain": [c["name"] for c in chain],
        "target": target_method["name"] if target_method else None,
    }]


# ============================================================
# 交叉加锁变异
# ============================================================

def _fake_guard_code():
    return """
    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }
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


def _mode_for_group():
    return _weighted_choice(CROSS_LOCK_MODE_WEIGHTS)


def _gen_nested_method_entry(entry_name, lock_seq, sleep_ms, fake, next_entry):
    lines = []
    if fake:
        outer = lock_seq[:-1]
        inner = lock_seq[-1]
        indent = "        "
        for lk in outer:
            lines.append(f"{indent}synchronized ({lk}) {{")
            lines.append(
                f"{indent}    try {{ Thread.sleep({sleep_ms}); }} "
                f"catch (InterruptedException e) {{ }}"
            )
            indent += "    "
        lines.append(f"{indent}if (__crossLockGuard()) {{")
        indent += "    "
        lines.append(f"{indent}synchronized ({inner}) {{")
        indent += "    "
        lines.append(f"{indent}{next_entry}();")
        indent = indent[:-4]
        lines.append(f"{indent}}}")
        indent = indent[:-4]
        lines.append(f"{indent}}}")
        for _ in outer:
            indent = indent[:-4]
            lines.append(f"{indent}}}")
    else:
        indent = "        "
        for lk in lock_seq:
            lines.append(f"{indent}synchronized ({lk}) {{")
            lines.append(
                f"{indent}    try {{ Thread.sleep({sleep_ms}); }} "
                f"catch (InterruptedException e) {{ }}"
            )
            indent += "    "
        lines.append(f"{indent}{next_entry}();")
        for _ in lock_seq:
            indent = indent[:-4]
            lines.append(f"{indent}}}")

    body = "\n".join(lines)
    return f"\n    public void {entry_name}() throws Exception {{\n{body}\n    }}\n"


def _gen_chain_method_entry(entry_name, lock_seq, sleep_ms, fake, next_entry, chain_dist):
    tag = f"{entry_name}_{random.randint(0, 9999)}"
    chain_names = [f"__ring_{tag}_m{i}" for i in range(chain_dist)]
    hold_idx = random.randint(0, max(0, chain_dist - 1))

    lines = []
    prev_call = f"{next_entry}();"
    for i in range(chain_dist - 1, -1, -1):
        mname = chain_names[i]
        if i == hold_idx:
            if fake:
                body = (
                    f"        if (__crossLockGuard()) {{\n"
                    f"            synchronized ({lock_seq[0]}) {{\n"
                    f"                try {{ Thread.sleep({sleep_ms}); }} catch (InterruptedException e) {{ }}\n"
                    f"                {prev_call}\n"
                    f"            }}\n"
                    f"        }} else {{\n"
                    f"            {prev_call}\n"
                    f"        }}\n"
                )
            else:
                body = (
                    f"        synchronized ({lock_seq[0]}) {{\n"
                    f"            try {{ Thread.sleep({sleep_ms}); }} catch (InterruptedException e) {{ }} \n"
                    f"            {prev_call}\n"
                    f"        }}\n"
                )
        else:
            body = (
                f"        try {{ Thread.sleep({sleep_ms}); }} catch (InterruptedException e) {{ }}\n"
                f"        {prev_call}\n"
            )
        lines.append(f"\n    private void {mname}() throws Exception {{\n{body}    }}\n")
        prev_call = f"{mname}();"

    entry_body = (
        f"        try {{ Thread.sleep({sleep_ms}); }} catch (InterruptedException e) {{ }}\n"
        f"        {prev_call}\n"
    )
    lines.append(f"\n    public void {entry_name}() throws Exception {{\n{entry_body}    }}\n")

    return "".join(lines)


def _gen_ring_methods(group_id, ring_index, my_lock, next_ring_entry,
                      sleep_ms, fake, topo, chain_dist):
    entry_name = f"crossLock_g{group_id}_ring{ring_index}_{random.randint(0, 9999)}"

    if topo == "in_method_nested":
        code = _gen_nested_method_entry(
            entry_name, [my_lock], sleep_ms, fake, next_ring_entry
        )
    else:
        code = _gen_chain_method_entry(
            entry_name, [my_lock], sleep_ms, fake, next_ring_entry, chain_dist
        )

    return code, entry_name


def mutate_add_cross_lock(content, lock_fields, class_name,
                          lock_unlock_methods=None):
    """
    返回 (new_content, records, info, extra_decl)
    """
    if random.random() >= P_ADD_CROSS_LOCK:
        return content, [], None, ""

    all_recs = []
    all_methods = []
    all_locks_used = []
    fake_flags = []
    all_decls = []

    lock_pool = _collect_usable_locks(lock_fields)
    existing = {f["name"] for f in lock_pool}
    if lock_unlock_methods:
        for m in lock_unlock_methods:
            for lf in m["lock_fields"]:
                if lf in existing:
                    continue
                typ = _infer_field_type(content, lf, class_name)
                lock_pool.append({
                    "name": lf,
                    "inferred": True,
                    "type": typ,
                })
                existing.add(lf)

    used_locks = set()

    for group_i in range(CROSS_LOCK_GROUPS_PER_CLASS):
        fake = random.random() < P_FAKE_CROSS_LOCK
        mode = _mode_for_group()

        if mode == "AB_BA":
            k = 2
        elif mode == "ABC_CYCLE":
            k = 3
        elif mode == "ABCD_CYCLE":
            k = 4
        elif mode == "NESTED_CROSS":
            k = max(3, sample_nest_depth())
        else:
            k = 2

        usable = [f for f in lock_pool if f["name"] not in used_locks]
        if not usable:
            usable = list(lock_pool)

        locks, decl = _ensure_locks(content, usable, k, class_name)
        all_decls.append(decl)

        s1 = random.randint(CROSS_LOCK_SLEEP_MIN, CROSS_LOCK_SLEEP_MAX)

        ring_entries = [
            f"crossLock_g{group_i}_ring{i}_{random.randint(0, 9999)}"
            for i in range(k)
        ]

        ring_codes = []
        ring_topos = []
        for i in range(k - 1, -1, -1):
            my_lock = locks[i]
            next_entry = ring_entries[(i + 1) % k]
            topo = _weighted_choice(RING_TOPO_WEIGHTS)
            chain_dist = max(1, sample_ring_chain_dist())

            code, entry_name = _gen_ring_methods(
                group_id=group_i,
                ring_index=i,
                my_lock=my_lock,
                next_ring_entry=next_entry,
                sleep_ms=s1,
                fake=fake,
                topo=topo,
                chain_dist=chain_dist,
            )
            code = code.replace(entry_name, ring_entries[i])
            ring_codes.append(code)
            ring_topos.append(topo)

        group_code = "".join(reversed(ring_codes))

        idx = content.rfind("}")
        if idx == -1:
            break

        guard_code = _fake_guard_code() if fake else ""
        content = content[:idx] + guard_code + group_code + content[idx:]

        all_recs.append({
            "op": "add_cross_lock",
            "group": group_i,
            "mode": mode,
            "locks": locks,
            "entry_methods": ring_entries,
            "fake": fake,
            "sleep_ms": s1,
            "topos": ring_topos,
        })
        all_methods.extend(ring_entries)
        all_locks_used.extend(locks)
        fake_flags.append(fake)
        used_locks.update(locks)

    if not all_recs:
        return content, [], None, ""

    decl_lines = []
    seen_decl = set()
    for d in all_decls:
        if not d:
            continue
        for line in d.splitlines():
            line = line.rstrip()
            if not line.strip():
                continue
            m = re.search(r'Object\s+(\w+)\s*=', line)
            if m and m.group(1) in seen_decl:
                continue
            if m:
                seen_decl.add(m.group(1))
            decl_lines.append(line)
    merged_decl = ("\n" + "\n".join(decl_lines) + "\n") if decl_lines else ""

    info = {
        "mode": all_recs[0]["mode"],
        "locks": all_locks_used,
        "methods": all_methods,
        "fake": any(fake_flags),
        "groups": all_recs,
    }
    return content, all_recs, info, merged_decl


# ============================================================
# mock 链（v11 修复版）
# ============================================================

def _build_mock_chain(content, class_name, methods, depth_limit,
                      lock_name=None, sync_block_name=None,
                      exclude_method=None,
                      must_call_method=None,
                      mutated_methods=None):
    """
    v11 逻辑：
    - must_call_method 优先
    - 否则从 mutated_methods（被变异加了 synchronized 的方法名集合）中选一个
    - 否则选原始源码里有 synchronized 块的方法
    - 否则选 is_synchronized 方法
    - 都没有：链尾不调源码方法
    """
    mutated_methods = mutated_methods or set()
    tail_target = None

    # 1. must_call_method
    if must_call_method is not None:
        for m in methods:
            if m["name"] == must_call_method:
                tail_target = m
                break

    # 2. 从 mutated_methods 里选
    if tail_target is None and mutated_methods:
        candidates = [m for m in methods
                      if m["name"] in mutated_methods
                      and m["name"] not in ALWAYS_EXCLUDE_METHODS
                      and not m.get("is_static")
                      and (exclude_method is None
                           or m["name"] != exclude_method["name"])]
        if candidates:
            tail_target = random.choice(candidates)

    # 3. 原始源码就有 synchronized 块
    if tail_target is None:
        locked = [m for m in methods
                  if m["name"] not in ALWAYS_EXCLUDE_METHODS
                  and not m["name"].startswith("mockMethod_")
                  and not m["name"].startswith("crossLock_")
                  and not m["name"].startswith("__")
                  and not m["name"].startswith("syncBlock_")
                  and not m.get("is_static")
                  and _method_body_has_lock(content, m["name"])
                  and (exclude_method is None or m["name"] != exclude_method["name"])]
        if locked:
            tail_target = random.choice(locked)

    # 4. is_synchronized 方法
    if tail_target is None:
        locked = [m for m in methods
                  if m["name"] not in ALWAYS_EXCLUDE_METHODS
                  and not m["name"].startswith("mockMethod_")
                  and not m["name"].startswith("crossLock_")
                  and not m["name"].startswith("__")
                  and m.get("is_synchronized")
                  and not m.get("is_static")
                  and (exclude_method is None or m["name"] != exclude_method["name"])]
        if locked:
            tail_target = random.choice(locked)

    # 构造 tail_call
    tail_call = ""
    if tail_target is not None:
        args = ", ".join(simple_args_for(tail_target["params"]))
        if lock_name:
            tail_call = (
                f"synchronized ({lock_name}) {{\n"
                f"    {tail_target['name']}({args});\n"
                f"}}"
            )
        else:
            tail_call = f"{tail_target['name']}({args});"

    chain = []
    next_call = tail_call

    if lock_name and depth_limit >= 2:
        hold_idx = random.randint(1, depth_limit - 1)
    else:
        hold_idx = -1

    for depth in range(depth_limit - 1, -1, -1):
        method_name = f"mockMethod_{depth}_{random.randint(0, 9999)}"
        sync_line = f"{sync_block_name}();" if (sync_block_name and depth == hold_idx) else ""

        if lock_name and depth == hold_idx:
            sleep_here = random.randint(SLEEP_MIN, SLEEP_MAX)
            body_lines = []
            body_lines.append(f"        try {{ Thread.sleep({sleep_here}); }} catch (InterruptedException e) {{ }}")
            body_lines.append(f"        synchronized ({lock_name}) {{")
            if next_call:
                for ln in next_call.split("\n"):
                    body_lines.append(f"            {ln}" if ln.strip() else ln)
            if sync_line:
                body_lines.append(f"            {sync_line}")
            body_lines.append("        }")
            body = "\n".join(body_lines)
            method_code = f"\n    public void {method_name}() throws Exception {{\n{body}\n    }}\n"
        else:
            body_lines = []
            if next_call:
                for ln in next_call.split("\n"):
                    body_lines.append(f"        {ln}" if ln.strip() else ln)
            if sync_line:
                body_lines.append(f"        {sync_line}")
            body = "\n".join(body_lines) if body_lines else ""
            method_code = f"\n    public void {method_name}() throws Exception {{\n{body}\n    }}\n"

        chain.append({"name": method_name, "code": method_code})
        next_call = f"{method_name}();"

    chain.reverse()
    return chain, chain[0]["name"], tail_target


def mutate_add_mock_chain(content, class_name, methods,
                          insert_into_method=None,
                          sync_block_name=None, lock_name=None,
                          must_call_method=None,
                          mutated_methods=None):
    depth_limit = sample_call_distance()
    chain, head_name, tail_target = _build_mock_chain(
        content, class_name, methods, depth_limit,
        lock_name=lock_name,
        sync_block_name=sync_block_name,
        exclude_method=insert_into_method,
        must_call_method=must_call_method,
        mutated_methods=mutated_methods,
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
        "lock_used": lock_name,
        "chain": [c["name"] for c in chain],
    }]


# ============================================================
# 组合变异
# ============================================================

def mutate_class(content, class_name, lock_fields, methods):
    records = []
    new_content = content
    sync_block_name = None
    pending_decls = []
    # 记录被变异器加了 synchronized 的方法名
    mutated_methods_with_lock = set()

    new_content, existing_lock = _ensure_class_has_a_lock(new_content, class_name)
    if existing_lock:
        records.append({"op": "ensure_class_lock", "lock": existing_lock})
        lock_fields = parse_lock_fields(new_content)

    lock_unlock_methods = parse_lock_unlock_methods(new_content, class_name)

    # 1a. 给含锁方法套一层 synchronized
    if lock_unlock_methods and lock_fields and random.random() < P_WRAP_METHOD_WITH_SYNC:
        k = min(MAX_WRAP_METHODS_PER_CLASS, len(lock_unlock_methods))
        for m in random.sample(lock_unlock_methods, k):
            lf = random.choice(lock_fields)["name"]
            new_content, r, extra_decl = mutate_wrap_method_with_sync(
                new_content, class_name, m, lf,
                sleep_ms=random.randint(SLEEP_MIN, SLEEP_MAX),
                use_new_lock=True,
            )
            records.extend(r)
            if extra_decl:
                pending_decls.append(extra_decl)
            if r:
                mutated_methods_with_lock.add(m["name"])

    # 1b. 在已有 synchronized 块内部再嵌套一层
    if lock_fields and random.random() < P_NEST_SYNC_BLOCK:
        base = random.choice(lock_fields)["name"]
        new_content, r, extra_decls = mutate_nest_inside_sync_block(
            new_content, class_name, base,
            sleep_ms=random.randint(SLEEP_MIN, SLEEP_MAX),
        )
        records.extend(r)
        pending_decls.extend(extra_decls)

    # 1c. 方法内 2/3/4 层嵌套
    deep_nested_hosts = []
    for _ in range(MAX_DEEP_NESTING_PER_CLASS):
        if random.random() >= P_ADD_DEEP_NESTING:
            break
        new_content, r, decl, host_name = mutate_add_deep_nesting(
            new_content, class_name, lock_fields, methods,
        )
        if r:
            records.extend(r)
            if decl:
                pending_decls.append(decl)
            if host_name:
                deep_nested_hosts.append(host_name)
                mutated_methods_with_lock.add(host_name)
            lock_fields = parse_lock_fields(new_content)

    # 2. 给真实含锁方法插入调用距离链
    if lock_unlock_methods and random.random() < P_INSERT_CALL_CHAIN:
        k = min(MAX_CALL_CHAIN_PER_CLASS, len(lock_unlock_methods))
        for m in random.sample(lock_unlock_methods, k):
            target = random.choice(methods) if methods else None
            new_content, r = mutate_insert_call_chain(
                new_content, m, methods, target
            )
            records.extend(r)

    # 3. 交叉锁环
    new_content, cross_recs, cross_info, cross_decl = mutate_add_cross_lock(
        new_content, lock_fields, class_name,
        lock_unlock_methods=lock_unlock_methods,
    )
    records.extend(cross_recs)
    cross_hit = bool(cross_info)

    if cross_decl:
        pending_decls.append(cross_decl)

    # 4. 基础变异
    do_replace = random.random() < P_REPLACE_LOCK
    if cross_hit and SKIP_REPLACE_LOCK_IF_CROSS:
        do_replace = False
        records.append({"op": "skip_replace_lock", "reason": "cross_lock_hit"})

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

    # 5. mock 链
    if random.random() < P_ADD_MOCK_METHOD:
        insert_target = None
        candidates = [m for m in methods
                      if m["is_synchronized"]
                      and not m.get("is_static")
                      and m["name"] not in ALWAYS_EXCLUDE_METHODS]
        if not candidates:
            candidates = [m for m in methods
                          if not m.get("is_static")
                          and m["name"] not in ALWAYS_EXCLUDE_METHODS]
        if candidates:
            insert_target = random.choice(candidates)

        mock_lock = None
        if lock_fields:
            mock_lock = random.choice(lock_fields)["name"]

        # 关键：mock 链尾优先调"被变异加了 synchronized 的方法"
        must_call = None
        if mutated_methods_with_lock:
            must_call = random.choice(list(mutated_methods_with_lock))
        elif deep_nested_hosts:
            must_call = random.choice(deep_nested_hosts)

        new_content, r = mutate_add_mock_chain(
            new_content, class_name, methods,
            insert_into_method=insert_target,
            sync_block_name=sync_block_name,
            lock_name=mock_lock,
            must_call_method=must_call,
            mutated_methods=mutated_methods_with_lock,
        )
        records.extend(r)

    # 6. 统一把 pending_decls 插入到类体末尾
    if pending_decls:
        merged_lines = []
        seen_names = set()
        for d in pending_decls:
            if not d:
                continue
            for line in d.splitlines():
                line = line.rstrip()
                if not line.strip():
                    continue
                m = re.search(r'Object\s+(\w+)\s*=', line)
                if m and m.group(1) in seen_names:
                    continue
                if m:
                    seen_names.add(m.group(1))
                merged_lines.append(line)
        if merged_lines:
            decl = "\n".join(merged_lines)
            new_content = _insert_decls_at_class_end(
                new_content, decl, class_name
            )

    return new_content, records, cross_info


# ============================================================
# Harness
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

    other_methods = [m for m in methods
                     if not m["name"].startswith("mockMethod_")
                     and m["name"] not in ALWAYS_EXCLUDE_METHODS]

    cross_methods = []
    cross_method_names = set()
    real_ring_method_names = set()
    if cross_lock_info:
        cross_method_names = set(cross_lock_info.get("methods", []))
        for g in cross_lock_info.get("groups", []):
            if not g.get("fake"):
                for nm in g.get("entry_methods", []):
                    real_ring_method_names.add(nm)
        for g in cross_lock_info.get("groups", []):
            for nm in g.get("entry_methods", []):
                for m in other_methods:
                    if m["name"] == nm:
                        cross_methods.append(m)
                        break

    real_ring_methods = [m for m in cross_methods
                         if m["name"] in real_ring_method_names]
    fake_ring_methods = [m for m in cross_methods
                         if m["name"] not in real_ring_method_names]

    threads = []
    threads.extend(real_ring_methods)
    threads.extend(fake_ring_methods)

    all_mock_methods = [m for m in methods
                        if m["name"].startswith("mockMethod_")
                        and m["name"] not in ALWAYS_EXCLUDE_METHODS]
    all_mock_methods.sort(key=lambda m: m["name"])
    mock_heads = [m for m in all_mock_methods
                  if m["name"].startswith("mockMethod_0_")]
    if not mock_heads and all_mock_methods:
        mock_heads = [all_mock_methods[0]]

    remaining = [m for m in mock_heads if m not in threads]
    MAX_THREADS = max(16, len(threads) + 4)
    while len(threads) < MAX_THREADS and remaining:
        chosen = random.choice(remaining)
        threads.append(chosen)
        remaining.remove(chosen)

    while len(threads) < 2 and (mock_heads + cross_methods):
        cand = random.choice(mock_heads + cross_methods)
        if cand not in threads:
            threads.append(cand)
        else:
            break

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
    parser.add_argument("--working-dir", dest="working_dir", default=WORKING_DIR)
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
