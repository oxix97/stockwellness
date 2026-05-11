import os
import re

def refactor_java_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    content = "".join(lines)
    content_no_comments = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    content_no_comments = re.sub(r'//.*', '', content_no_comments)

    # 1. Detect FQCNs in code
    fqcn_pattern = re.compile(r'\b([a-z][a-z0-9]*\.(?:[a-z0-9]+\.)*[A-Z][a-zA-Z0-9]*)\b')
    fqcns = []
    lines_list = content_no_comments.split('\n')
    for line in lines_list:
        if line.strip().startswith('import ') or line.strip().startswith('package '):
            continue
        matches = fqcn_pattern.findall(line)
        for m in matches:
            if any(m.startswith(p) for p in ['java.', 'javax.', 'org.', 'com.', 'net.']) or m.startswith('org.stockwellness.'):
                fqcns.append(m)

    # 2. Extract existing imports
    import_pattern = re.compile(r'^import\s+(?:static\s+)?([\w\.]+)\s*;', re.MULTILINE)
    existing_imports = set(import_pattern.findall(content))

    # 3. Add new imports from FQCNs
    new_imports = existing_imports.copy()
    for fqcn in fqcns:
        new_imports.add(fqcn)

    # 4. Replace FQCN with SimpleName in original content
    new_content = content
    for fqcn in set(fqcns):
        simple_name = fqcn.split('.')[-1]
        new_content = re.sub(r'\b' + re.escape(fqcn) + r'\b', simple_name, new_content)

    # 5. Clean up unused imports
    final_content_no_comments = re.sub(r'/\*.*?\*/', '', new_content, flags=re.DOTALL)
    final_content_no_comments = re.sub(r'//.*', '', final_content_no_comments)
    body_only = re.sub(r'^import\s+.*?;', '', final_content_no_comments, flags=re.MULTILINE)
    
    final_imports = []
    for imp in sorted(list(new_imports)):
        simple_name = imp.split('.')[-1]
        if simple_name == '*':
            final_imports.append(imp)
            continue
        
        if re.search(r'\b' + re.escape(simple_name) + r'\b', body_only):
            final_imports.append(imp)

    # 6. Reconstruct the file
    lines = new_content.split('\n')
    output_lines = []
    package_done = False
    imports_written = False
    
    for line in lines:
        if line.strip().startswith('package '):
            output_lines.append(line)
            package_done = True
            continue
        if line.strip().startswith('import '):
            if not imports_written:
                output_lines.append("")
                java_imports = sorted([i for i in final_imports if i.startswith('java.')])
                other_imports = sorted([i for i in final_imports if not i.startswith('java.')])
                for i in java_imports:
                    output_lines.append(f"import {i};")
                if java_imports and other_imports:
                    output_lines.append("")
                for i in other_imports:
                    output_lines.append(f"import {i};")
                imports_written = True
            continue
        output_lines.append(line)

    # Remove redundant empty lines
    final_output = "\n".join(output_lines)
    while "\n\n\n" in final_output:
        final_output = final_output.replace("\n\n\n", "\n\n")

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(final_output)

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        refactor_java_file(sys.argv[1])
