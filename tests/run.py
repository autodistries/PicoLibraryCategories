#!/usr/bin/env python3
"""Run host-side category identity, membership, and ordering tests."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix="library-categories-") as out:
    subprocess.run([
        "javac", "--release", "8", "-d", out,
        str(ROOT / "src/research/pico/librarycategories/CategoryModel.java"),
        str(ROOT / "src/research/pico/librarycategories/ReorderMath.java"),
        str(ROOT / "tests/ModelTests.java"),
    ], check=True)
    subprocess.run(["java", "-cp", out, "research.pico.librarycategories.ModelTests"], check=True)
