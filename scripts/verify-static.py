#!/usr/bin/env python3
"""Conservative Java delimiter/call-arity/SQL preparation checks. Does not compile/type-check Java."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
source = root / 'src'
# Keep literal values as tokens so SQL can be checked without evaluating Java.
pattern = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*|::|\S')
files = list(source.rglob('*.java'))
tokenized = {}
for path in files:
    tokens = [m.group() for m in pattern.finditer(path.read_text()) if not m.group().startswith(('//', '/*'))]
    stack = []
    for token in tokens:
        if token in ('(', '{', '['):
            stack.append(token)
        elif token in (')', '}', ']'):
            assert stack and stack.pop() == {')':'(', '}':'{', ']':'['}[token], (path, token)
    assert not stack, (path, stack)
    tokenized[path] = tokens

services = root / 'src/main/java/com/healtouch/service'
arities = {}
for path in services.glob('*.java'):
    # Public method signatures contain no nested parentheses in this project.
    for match in re.finditer(r'public\s+(?:static\s+)?[\w<>?,\[\]. ]+?\s+(\w+)\s*\(([^()]*)\)\s*(?:throws [^{]+)?\{', path.read_text()):
        params = match.group(2)
        level = 0
        count = 1 if params.strip() else 0
        for character in params:
            if character == '<': level += 1
            if character == '>': level -= 1
            if character == ',' and level == 0: count += 1
        arities.setdefault((path.stem, match.group(1)), set()).add(count)
components = {'auth':'AuthService','patients':'PatientService','users':'UserService','catalog':'CatalogService',
              'billing':'BillingService','deposits':'DepositService','refunds':'RefundService',
              'dashboard':'DashboardService','permissions':'PermissionService','backups':'BackupService'}
calls = 0
for path in (root / 'src/main/java/com/healtouch/ui').glob('*.java'):
    for component, method in re.findall(r'services\.(\w+)\.(\w+)\(', path.read_text()):
        if component in components:
            assert (component, method) == ('backups', 'problem'), (path, 'Direct UI service call', component, method)
for path, tokens in tokenized.items():
    if '/ui/' not in str(path): continue
    for index in range(len(tokens)-9):
        if tokens[index:index+5] not in (['DbWork','.','read','(','services'], ['DbWork','.','write','(','services']): continue
        component, method = tokens[index+6], tokens[index+8]
        assert tokens[index+7] == '::', (path, component, method)
        j, depth, count = index+9, 1, 0
        angle = 0
        while depth:
            token = tokens[j]
            if token in ('(', '[', '{'): depth += 1
            if token in (')', ']', '}'): depth -= 1
            if token == '<': angle += 1
            if token == '>' and angle: angle -= 1
            if token == ',' and depth == 1 and angle == 0: count += 1
            j += 1
        assert count in arities.get((components[component], method), set()), (path, component, method, count, arities.get((components[component],method)))
        calls += 1

con = sqlite3.connect(':memory:')
for path in sorted((root / 'src/main/resources/db/migration').glob('*.sql')):
    con.executescript(path.read_text())
con.execute('CREATE TABLE flyway_schema_history(version TEXT,success INTEGER)')
checked = 0
for path, tokens in tokenized.items():
    for index in range(len(tokens)-3):
        if tokens[index:index+2] != ['prepareStatement','(']: continue
        j, fragments = index+2, []
        while j < len(tokens) and tokens[j].startswith('"'):
            # Java SQL strings use only standard quote/backslash escapes here.
            fragments.append(bytes(tokens[j][1:-1], 'utf-8').decode('unicode_escape') if '\\' in tokens[j] else tokens[j][1:-1])
            j += 1
            if tokens[j] == '+':
                j += 1
                if not tokens[j].startswith('"'): break
            else: break
        if not fragments or tokens[j] not in (')', ','): continue
        query = ''.join(fragments)
        if not query.lstrip().upper().startswith(('SELECT', 'INSERT', 'UPDATE', 'DELETE')): continue
        try:
            con.execute('EXPLAIN '+query, [None]*query.count('?')).fetchall()
        except sqlite3.Error as error:
            raise AssertionError((path, query, str(error))) from error
        checked += 1
print(f'PASS: balanced Java delimiters in {len(files)} files; {calls} UI service references match declared arities; {checked} literal SQL statements prepare against V3.')
print('NOT_EXECUTED: Java grammar parsing, type-checking, compilation, JVM tests, Flyway and JavaFX runtime checks.')
