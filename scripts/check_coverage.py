#!/usr/bin/env python3
"""
Usage:
  python3 scripts/check_coverage.py <reports_dir> <threshold_percent> [module ...]

Finds all JaCoCo report.xml files under <reports_dir>, aggregates LINE coverage
across matching modules, and exits non-zero if coverage < <threshold_percent>.
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def module_name(root: Path, report_xml: Path) -> str:
    try:
        return report_xml.relative_to(root).parts[0]
    except ValueError:
        return report_xml.parent.parent.parent.parent.name


def collect_coverage(reports_dir: str, included_modules: set[str]) -> tuple[int, int]:
    root = Path(reports_dir)
    total_covered = 0
    total_missed = 0
    found = False

    for report_xml in root.rglob("report.xml"):
        module = module_name(root, report_xml)
        if included_modules and module not in included_modules:
            continue
        found = True
        tree = ET.parse(report_xml)
        for counter in tree.getroot().findall("counter"):
            if counter.get("type") == "LINE":
                total_covered += int(counter.get("covered", 0))
                total_missed += int(counter.get("missed", 0))
                print(f"  {module}: covered={counter.get('covered')}, missed={counter.get('missed')}")

    if not found:
        print("ERROR: No JaCoCo report.xml files found.")
        if included_modules:
            print(f"Included modules: {', '.join(sorted(included_modules))}")
        print("Ensure tests ran with enableUnitTestCoverage = true in the debug buildType.")
        sys.exit(1)

    return total_covered, total_missed


def main() -> None:
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <reports_dir> <threshold_percent>")
        sys.exit(1)

    reports_dir = sys.argv[1]
    threshold = float(sys.argv[2])
    included_modules = set(sys.argv[3:])

    print("Coverage by module:")
    if included_modules:
        print(f"Included modules: {', '.join(sorted(included_modules))}")
    covered, missed = collect_coverage(reports_dir, included_modules)
    total = covered + missed

    if total == 0:
        print("\nWARNING: No coverable lines found. "
              "Skipping gate — add production source files and tests.")
        sys.exit(0)

    pct = covered / total * 100
    print(f"\nAggregate line coverage: {covered}/{total} = {pct:.1f}%  (required: {threshold}%)")

    if pct < threshold:
        print(f"FAIL: {pct:.1f}% is below the required {threshold}%.")
        sys.exit(1)

    print("PASS: Coverage threshold met.")


if __name__ == "__main__":
    main()
