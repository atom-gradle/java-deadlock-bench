import re

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
            # 遇到新的 CYCLE，先把上一个存起来
            if current_cycle is not None:
                blocks.append((current_cycle, current_lines))
            current_cycle = int(m.group(1))
            current_lines = []
        else:
            if current_cycle is not None:
                current_lines.append(line)

    # 最后一个块
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

if __name__ == '__main__':
    files_to_merge = [f"graph-real-1 ({i}).txt" for i in range(1,10)]
    merge_txt(files_to_merge, 'merged-real-.txt')