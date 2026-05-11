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
        stripped = line.strip()
        if stripped.startswith('import ') or stripped.startswith('package '):
            continue
        matches = fqcn_pattern.findall(line)
        for m in matches:
            if any(m.startswith(p) for p in ['java.', 'javax.', 'org.', 'com.', 'net.']) or m.startswith('org.stockwellness.'):
                fqcns.append(m)

    # 2. Extract existing imports (preserving static status)
    # Using a list of tuples (is_static, path)
    # Updated regex to include '*'
    existing_imports = []
    import_lines_raw = re.findall(r'^import\s+(?:(static)\s+)?([\w\.\*]+)\s*;', content, re.MULTILINE)
    for is_static_str, path in import_lines_raw:
        existing_imports.append((is_static_str == 'static', path))

    # 3. Add new imports from FQCNs
    new_imports = set(existing_imports)
    for fqcn in fqcns:
        new_imports.add((False, fqcn))

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
    for is_static, path in sorted(list(new_imports), key=lambda x: x[1]):
        simple_name = path.split('.')[-1]
        if simple_name == '*':
            final_imports.append((is_static, path))
            continue
        
        if re.search(r'\b' + re.escape(simple_name) + r'\b', body_only):
            final_imports.append((is_static, path))

    # 6. Reconstruct the file
    lines = new_content.split('\n')
    output_lines = []
    imports_written = False
    
    # Check if there are ANY imports in the original file
    has_imports = any(line.strip().startswith('import ') for line in lines)

    if not has_imports and final_imports:
        # If no imports were there but we have new ones, insert after package
        for line in lines:
            output_lines.append(line)
            if line.strip().startswith('package '):
                output_lines.append("")
                # write all imports
                java_imports = sorted([i for i in final_imports if i[1].startswith('java.')], key=lambda x: (x[0], x[1]))
                other_imports = sorted([i for i in final_imports if not i[1].startswith('java.')], key=lambda x: (x[0], x[1]))
                for is_static, path in java_imports:
                    prefix = "static " if is_static else ""
                    output_lines.append(f"import {prefix}{path};")
                if java_imports and other_imports:
                    output_lines.append("")
                for is_static, path in other_imports:
                    prefix = "static " if is_static else ""
                    output_lines.append(f"import {prefix}{path};")
                imports_written = True
    else:
        for line in lines:
            if line.strip().startswith('package '):
                output_lines.append(line)
                continue
            if line.strip().startswith('import '):
                if not imports_written:
                    output_lines.append("")
                    # Group by java.* vs others
                    java_imports = sorted([i for i in final_imports if i[1].startswith('java.')], key=lambda x: (x[0], x[1]))
                    other_imports = sorted([i for i in final_imports if not i[1].startswith('java.')], key=lambda x: (x[0], x[1]))
                    
                    for is_static, path in java_imports:
                        prefix = "static " if is_static else ""
                        output_lines.append(f"import {prefix}{path};")
                    
                    if java_imports and other_imports:
                        output_lines.append("")
                    
                    for is_static, path in other_imports:
                        prefix = "static " if is_static else ""
                        output_lines.append(f"import {prefix}{path};")
                    
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
