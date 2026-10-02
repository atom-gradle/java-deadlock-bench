import os,re,subprocess,sys,time
from pathlib import Path

def find_harness_files(root_dir):
    """
    查找所有文件名包含Harness的class文件
    """
    harness_files = []
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if 'Harness' in file and file.endswith('.class'):
                full_path = os.path.join(root, file)
                harness_files.append(full_path)
    return harness_files

def get_class_full_name(class_file_path, root_dir):
    """
    从class文件路径提取完整类名（包名+类名）
    """
    # 获取相对于根目录的路径
    rel_path = os.path.relpath(class_file_path, root_dir)

    # 去掉.class后缀
    class_name = rel_path[:-6]

    # 将路径分隔符替换为点号，得到完整的类名
    full_class_name = class_name.replace(os.sep, '.')

    return full_class_name

def run_java_command(class_full_name, working_dir):
    """
    执行Java命令
    """

    """
    cmd = [
        'java',
        '-cp', '"*;."',
        'pascal.taie.Main',
        '-cp', '"."',
        '-m', class_full_name,
        '-a', 'pta',
        '--output-dir', '".\\out"'
    ]
    """

    cmd = [
        'java',
        '-cp', '*;.',
        'pascal.taie.Main',
        '-cp', '.',
        '-m', class_full_name,
        '-a', 'pta',
        '--output-dir', '.\\out'
    ]

    print(f"\n执行命令: {' '.join(cmd)}")
    print(f"工作目录: {working_dir}")

    try:
        result = subprocess.run(
            cmd,
            cwd=working_dir,
            capture_output=True,
            text=True,
            shell=False  # Windows下需要shell=True来支持通配符
        )

        if result.returncode == 0:
            print(f"✓ 成功执行: {class_full_name}")
            if result.stdout:
                print(f"输出: {result.stdout}")
        else:
            print(f"✗ 执行失败: {class_full_name}")
            print(f"错误信息: {result.stderr}")

    except Exception as e:
        print(f"✗ 执行异常: {class_full_name}")
        print(f"异常信息: {str(e)}")

def run_all_java(target_dir = "."):

    # 转换为绝对路径
    target_dir = os.path.abspath(target_dir)

    if not os.path.exists(target_dir):
        print(f"错误: 目录不存在: {target_dir}")
        return

    print(f"搜索目录: {target_dir}")

    # 查找所有Harness文件
    harness_files = find_harness_files(target_dir)

    if not harness_files:
        print("未找到包含'Harness'的class文件")
        return

    print(f"\n找到 {len(harness_files)} 个Harness文件:")
    for f in harness_files:
        print(f"  - {f}")

    # 确保输出目录存在
    out_dir = os.path.join(target_dir, 'out')
    os.makedirs(out_dir, exist_ok=True)

    # 对每个文件执行Java命令
    print("\n开始执行Java命令...")
    for class_file in harness_files:
        class_full_name = get_class_full_name(class_file, target_dir)
        run_java_command(class_full_name, target_dir)

def parse_cycles(content):
    """
    将文件内容按 CYCLE 块切分，返回 [(cycle_num, block_text), ...]
    block_text 不包含 'CYCLE X' 这一行。
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
    查找当前目录下所有 graph-*.txt 文件（不递归子目录）
    """
    search_dir = os.path.abspath(search_dir)
    files = []
    for name in os.listdir(search_dir):
        full = os.path.join(search_dir, name)
        if os.path.isfile(full) and name.startswith(pattern_prefix) and name.endswith(".txt"):
            files.append(full)

    # 按文件名自然排序（比如 graph-real-1 (2).txt 排在 graph-real-1 (10).txt 前面）
    def sort_key(p):
        nums = re.findall(r'\d+', os.path.basename(p))
        return [int(n) for n in nums] if nums else [0]

    files.sort(key=sort_key)
    return files


def merge_generated_graphs(search_dir=".", output="merged-real-.txt"):
    """
    把刚生成的所有 graph-*.txt 合并起来
    """
    graph_files = find_graph_txt_files(search_dir)

    if not graph_files:
        print("\n未找到任何 graph-*.txt 文件，跳过合并。")
        return

    print(f"\n找到 {len(graph_files)} 个 graph-*.txt 文件，开始合并:")
    for f in graph_files:
        print(f"  - {f}")

    merge_txt(graph_files, output)
    print(f"\n✓ 合并完成，输出文件: {output}")

def main():
    target_dir = "."

    # 1. 先执行所有 Java 命令
    run_all_java(target_dir)

    # 2. 再把生成的 graph-*.txt 合并
    timestamp = str(round(1000 * time.time()))
    output_dir = "graph-real-" + timestamp + ".txt"
    merge_generated_graphs(search_dir=target_dir, output=output_dir)


if __name__ == "__main__":
    main()
