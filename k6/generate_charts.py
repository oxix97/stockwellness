import os
import json
import glob
import matplotlib.pyplot as plt
import numpy as np

def parse_k6_json(file_path):
    """Parse k6 summary JSON and extract key metrics."""
    try:
        with open(file_path, 'r') as f:
            data = json.load(f)
        
        metrics = data.get('metrics', {})
        
        # Structure check: some k6 versions might have different nesting
        duration_metric = metrics.get('http_req_duration', {})
        if 'values' in duration_metric:
            p95 = duration_metric['values'].get('p(95)', 0)
        else:
            p95 = duration_metric.get('p(95)', 0)
            
        reqs_metric = metrics.get('http_reqs', {})
        if 'values' in reqs_metric:
            reqs = reqs_metric['values'].get('count', 0)
        else:
            reqs = reqs_metric.get('count', 0)
            
        return {'p95': p95, 'reqs': reqs}
    except Exception as e:
        print(f"Error parsing {file_path}: {e}")
        return None

def aggregate_data(files):
    """Average metrics across multiple test runs."""
    if not files:
        return None
    
    parsed_results = [parse_k6_json(f) for f in files]
    valid_results = [r for r in parsed_results if r is not None]
    
    if not valid_results:
        return None
        
    avg_p95 = sum(r['p95'] for r in valid_results) / len(valid_results)
    avg_reqs = sum(r['reqs'] for r in valid_results) / len(valid_results)
    
    return {'p95': avg_p95, 'reqs': avg_reqs}

def save_comparison_chart(scenario, metric_name, before_val, after_val, unit, output_path):
    """Generate and save a before/after bar chart."""
    plt.figure(figsize=(8, 6))
    
    labels = ['Before', 'After']
    values = [before_val, after_val]
    # Colors: Before (Reddish), After (Greenish/Bluish)
    colors = ['#E57373', '#81C784'] 
    
    bars = plt.bar(labels, values, color=colors, width=0.6)
    
    plt.title(f'{scenario}: {metric_name}', fontsize=14, pad=20)
    plt.ylabel(unit, fontsize=12)
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    
    # Add value labels on top of bars
    for bar in bars:
        height = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2., height + (height * 0.01),
                 f'{height:,.2f}' if isinstance(height, float) else f'{height:,}',
                 ha='center', va='bottom', fontsize=11, fontweight='bold')
    
    # Calculate improvement percentage
    if before_val > 0:
        improvement = ((before_val - after_val) / before_val) * 100 if 'Latency' in metric_name else ((after_val - before_val) / before_val) * 100
        direction = "Reduced" if 'Latency' in metric_name else "Increased"
        plt.figtext(0.5, 0.01, f'Performance {direction} by {abs(improvement):.1f}%', 
                    ha='center', fontsize=10, bbox={"facecolor":"orange", "alpha":0.2, "pad":5})

    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(output_path)
    plt.close()

def main():
    # Paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    results_dir = os.path.join(base_dir, 'results')
    charts_dir = os.path.join(results_dir, 'charts')
    
    if not os.path.exists(charts_dir):
        os.makedirs(charts_dir)
        print(f"Created directory: {charts_dir}")

    # Discover scenarios by scanning files
    all_files = glob.glob(os.path.join(results_dir, '*.json'))
    scenarios = set()
    for f in all_files:
        name = os.path.basename(f)
        if '-before-' in name:
            scenarios.add(name.split('-before-')[0])
        elif '-after-' in name:
            scenarios.add(name.split('-after-')[0])

    if not scenarios:
        print("No 'before' or 'after' JSON files found in results/ directory.")
        return

    print(f"Found scenarios: {', '.join(scenarios)}")

    for scenario in scenarios:
        before_files = glob.glob(os.path.join(results_dir, f'{scenario}-before-*.json'))
        after_files = glob.glob(os.path.join(results_dir, f'{scenario}-after-*.json'))

        before_data = aggregate_data(before_files)
        after_data = aggregate_data(after_files)

        if not before_data or not after_data:
            print(f"Skipping {scenario}: Missing comparison data (Before: {len(before_files)}, After: {len(after_files)})")
            continue

        print(f"Processing {scenario}...")
        
        # 1. Latency Chart (p95)
        latency_path = os.path.join(charts_dir, f'{scenario}-latency.png')
        save_comparison_chart(
            scenario, 'p95 Latency', 
            before_data['p95'], after_data['p95'], 
            'ms (Lower is better)', latency_path
        )
        
        # 2. Throughput Chart (Request Count)
        throughput_path = os.path.join(charts_dir, f'{scenario}-throughput.png')
        save_comparison_chart(
            scenario, 'Throughput (Total Requests)', 
            before_data['reqs'], after_data['reqs'], 
            'Count (Higher is better)', throughput_path
        )

    print(f"\nSuccess! Charts generated in: {charts_dir}")

if __name__ == '__main__':
    main()
