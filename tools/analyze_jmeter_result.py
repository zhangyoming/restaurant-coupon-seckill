#!/usr/bin/env python3
"""Analyze a JMeter JTL CSV file and generate a Markdown summary.

Usage:
  python tools/analyze_jmeter_result.py reports/core.jtl reports/core_summary.md
"""
import csv
import math
import sys
from collections import defaultdict
from pathlib import Path


def percentile(values, p):
    if not values:
        return 0
    values = sorted(values)
    k = (len(values) - 1) * p
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return values[int(k)]
    return values[f] * (c - k) + values[c] * (k - f)


def summarize(rows):
    by_label = defaultdict(list)
    start_times = []
    end_times = []
    for r in rows:
        label = r.get('label') or r.get('samplerData') or 'UNKNOWN'
        elapsed = float(r.get('elapsed', 0))
        success = str(r.get('success', '')).lower() == 'true'
        ts = int(float(r.get('timeStamp', 0)))
        by_label[label].append((elapsed, success, ts))
        if ts:
            start_times.append(ts)
            end_times.append(ts + int(elapsed))
    duration_sec = max((max(end_times) - min(start_times)) / 1000, 0.001) if start_times and end_times else 0.001
    result = []
    for label, items in by_label.items():
        elapsed = [x[0] for x in items]
        success_count = sum(1 for _, ok, _ in items if ok)
        total = len(items)
        result.append({
            'label': label,
            'total': total,
            'success': success_count,
            'fail': total - success_count,
            'success_rate': success_count / total * 100 if total else 0,
            'avg': sum(elapsed) / total if total else 0,
            'min': min(elapsed) if elapsed else 0,
            'max': max(elapsed) if elapsed else 0,
            'p90': percentile(elapsed, 0.90),
            'p95': percentile(elapsed, 0.95),
            'p99': percentile(elapsed, 0.99),
            'qps': total / duration_sec,
        })
    result.sort(key=lambda x: x['label'])
    return result, duration_sec


def main():
    if len(sys.argv) < 3:
        print('Usage: python analyze_jmeter_result.py <input.jtl> <output.md>')
        sys.exit(1)
    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    with input_path.open('r', encoding='utf-8-sig', newline='') as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    summary, duration = summarize(rows)
    lines = []
    lines.append('# JMeter 压测结果汇总')
    lines.append('')
    lines.append(f'- 原始结果文件：`{input_path}`')
    lines.append(f'- 总请求数：{len(rows)}')
    lines.append(f'- 测试持续时间：{duration:.2f} 秒')
    lines.append('')
    lines.append('| 接口 | 请求数 | 成功 | 失败 | 成功率 | QPS | 平均响应(ms) | P90(ms) | P95(ms) | P99(ms) | 最大(ms) |')
    lines.append('|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|')
    for s in summary:
        lines.append('| {label} | {total} | {success} | {fail} | {success_rate:.2f}% | {qps:.2f} | {avg:.2f} | {p90:.2f} | {p95:.2f} | {p99:.2f} | {max:.2f} |'.format(**s))
    lines.append('')
    lines.append('## 结论填写建议')
    lines.append('')
    lines.append('- 如果失败主要集中在库存抢完之后，需要结合业务判断，不一定是系统异常。')
    lines.append('- 如果失败主要是 5xx、连接超时或 MQ 堆积，需要检查数据库连接池、RabbitMQ 消费并发、Redis 响应时间和 JVM 资源。')
    lines.append('- 核心抢券接口建议重点关注平均响应、P95、P99 和 RabbitMQ 消费完成后的最终成功数。')
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text('\n'.join(lines), encoding='utf-8')
    print(f'Wrote {output_path}')


if __name__ == '__main__':
    main()
