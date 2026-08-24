#!/usr/bin/env python3
"""DoD 覆盖率统计（行覆盖率，含 -data 模块，排除模型/枚举/配置）"""
import csv, sys
from collections import defaultdict
CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else 'gateway-coverage/target/site/jacoco-aggregate/jacoco.csv'
EXCLUDE_PKG = ('dataobject', '/dto', 'enums', 'entity', 'config', 'autoconfigure')
EXCLUDE_CLS = ('Do', 'Request', 'Response')
def is_core(pkg, cls):
    if not pkg.startswith('com.codingas.gateway.'): return False
    if any(x in pkg.lower() for x in EXCLUDE_PKG): return False
    if cls.endswith(EXCLUDE_CLS): return False
    return True
doms = defaultdict(lambda: [0, 0])
with open(CSV_PATH) as f:
    for r in csv.DictReader(f):
        if not is_core(r['PACKAGE'], r['CLASS']): continue
        dom = r['PACKAGE'].split('.')[3]
        doms[dom][0] += int(r['LINE_COVERED']); doms[dom][1] += int(r['LINE_MISSED'])
print(f"{'域':<12}{'覆盖/总':<12}{'覆盖率':<10}{'DoD(≥90%)'}")
for dom in sorted(doms):
    c, m = doms[dom]; t = c + m
    pct = c / t * 100 if t else 0
    print(f"{dom:<12}{c}/{t:<10}{pct:.2f}%{'':<6}{'✓' if pct >= 90 else '✗'}")
