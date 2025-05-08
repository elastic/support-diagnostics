# Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
# or more contributor license agreements. Licensed under the Elastic License
# 2.0; you may not use this file except in compliance with the Elastic License
# 2.0.

from pathlib import Path

import nox

BASE_DIR = Path(__file__).parent
SOURCE_FILES = (
    "noxfile.py",
    "export_model_snapshot.py",
    "import_model_snapshot.py",
    "tests/",
)


@nox.session(python=["3.12"], reuse_venv=True)
def format(session):
    session.install("black==25.1.0")
    session.install("isort==6.0.1")
    session.run("black", "--target-version=py312", *SOURCE_FILES)
    session.run("isort", *SOURCE_FILES)
    lint(session)


@nox.session(python=["3.12"], reuse_venv=True)
def lint(session):
    session.install("black==25.1.0")
    session.install("isort==6.0.1")
    session.run("black", "--check", "--diff", "--target-version=py312", *SOURCE_FILES)
    session.run("isort", "--check", "--diff", *SOURCE_FILES)


@nox.session(python=["3.12"], reuse_venv=True)
def test(session):
    session.run("poetry", "install", external=True)

    pytest_args = ("poetry", "run", "pytest", "--cov-report", "lcov")
    session.run(*pytest_args, *(session.posargs or ("tests/",)), external=True)
