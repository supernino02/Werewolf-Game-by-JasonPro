import os
import json
import glob
import re
import argparse
import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

def extract_game_data(data_dir):
    """Parses game logs and normalizes data into a structured format."""
    wins = {'wolf': 0, 'villager': 0}
    survivors = {'wolf': [], 'villager': []}
    mood_data = {
        'wolf': {'stress': {'global': [], 'death': []}, 'exposure': {'global': [], 'death': []}},
        'villager': {'stress': {'global': [], 'death': []}, 'exposure': {'global': [], 'death': []}}
    }
    player_outcomes = {}
    
    vic_p = re.compile(r"victim\([^,]+,([^,]+),([^)]+)\)")
    win_p = re.compile(r"winner\(([^)]+)\)")

    game_dirs = glob.glob(os.path.join(data_dir, "*"))
    for gd in game_dirs:
        if not os.path.isdir(gd): continue
        n_file = os.path.join(gd, 'narrator.json')
        if not os.path.exists(n_file): continue

        alive, roles = set(), {}
        for pf in [f for f in glob.glob(os.path.join(gd, '*.json')) if not f.endswith('narrator.json')]:
            with open(pf, 'r') as f:
                p = json.load(f)
                name, role = p['metadata']['name'], p['metadata']['role']
                alive.add(name); roles[name] = role
                if name not in player_outcomes: 
                    player_outcomes[name] = {'deaths': [], 'wins': 0, 'role': role}
                for m in ['stress', 'exposure']:
                    vals = [x['value'] for x in p.get('mood_log', []) if x['mood'] == m]
                    if vals: mood_data[role][m]['global'].append(np.mean(vals))

        with open(n_file, 'r') as f:
            log = json.load(f).get('narrator_log', [])
        
        dead_phases, vote_idx, winner = {}, 0, None
        for entry in log:
            p_name = entry.get('phase', {}).get('name', '')
            if p_name in ['resolve_hunt', 'resolve_vote']:
                vote_idx += 1
                for act in entry.get('actions', []):
                    m = vic_p.search(act.get('content', ''))
                    if m and m.group(1) in alive:
                        v = m.group(1); alive.remove(v)
                        dead_phases[v] = int(entry.get('phase_id', -1))
                        player_outcomes[v]['deaths'].append({'idx': vote_idx, 'type': 'Night' if p_name == 'resolve_hunt' else 'Day'})
            for act in entry.get('actions', []):
                m = win_p.search(act.get('content', ''))
                if m: winner = m.group(1)

        if winner:
            wins[winner] += 1
            survivors[winner].append(sum(1 for p in alive if roles[p] == winner))
            for p in [n for n, r in roles.items() if n in alive and r == winner]:
                player_outcomes[p]['wins'] += 1

        for name, ph in dead_phases.items():
            p_file = os.path.join(gd, f"{name}.json")
            if os.path.exists(p_file):
                with open(p_file, 'r') as f:
                    m_log = json.load(f).get('mood_log', [])
                    for m_type in ['stress', 'exposure']:
                        last = [x['value'] for x in m_log if int(x['phase_id']) <= ph and x['mood'] == m_type]
                        if last: mood_data[roles[name]][m_type]['death'].append(last[-1])

    return {'wins': wins, 'survivors': survivors, 'mood_data': mood_data, 'player_outcomes': player_outcomes}

def setup_style():
    sns.set_theme(style="whitegrid", rc={"axes.edgecolor": "black", "axes.linewidth": 1.2})

def plot_outcomes(data, out_dir):
    setup_style()
    # Adjusted to 15x4.5 for a wide, horizontal layout
    fig, axes = plt.subplots(1, 2, figsize=(12, 6), dpi=300)
    
    # 1. Normalized Wins
    total = sum(data['wins'].values()) or 1
    axes[0].bar(['Wolves', 'Villagers'], [data['wins']['wolf']/total, data['wins']['villager']/total], 
                color=['#d62728', '#1f77b4'], edgecolor='black', alpha=0.8)
    axes[0].set_title('Win Probability per Faction', fontsize=14, fontweight='bold')
    axes[0].set_ylabel('Probability', fontweight='bold')
    axes[0].set_ylim(0, 1.0)

    # 2. Normalized Survivors
    max_surv = max([max(v, default=0) for v in data['survivors'].values()], default=0)
    bins = np.arange(-0.5, max_surv + 1.5, 1)
    axes[1].hist(data['survivors']['wolf'], bins=bins, density=True, alpha=0.5, color='red', label='Wolves', edgecolor='black')
    axes[1].hist(data['survivors']['villager'], bins=bins, density=True, alpha=0.5, color='blue', label='Villagers', edgecolor='black')
    axes[1].set_title('Survivor Count Probability Density', fontsize=14, fontweight='bold')
    axes[1].set_xlabel('Survivors', fontweight='bold')
    axes[1].legend()

    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, '1_2_win_survivor_summary.png'))
    plt.close()

def plot_moods(data, out_dir):
    setup_style()
    fig, axes = plt.subplots(2, 2, figsize=(12, 6), dpi=300, sharex=True)
    
    for c, role in enumerate(['wolf', 'villager']):
        for r, mood in enumerate(['stress', 'exposure']):
            ax = axes[r, c]
            g, d = data['mood_data'][role][mood]['global'], data['mood_data'][role][mood]['death']
            
            # CHANGED: Using Neutral Colors (Grey for Global, Orange for Death) to avoid clashing with Wolf/Villager colors
            if g: sns.kdeplot(g, ax=ax, fill=True, label='Global Mean', color='grey', alpha=0.4)
            if d: sns.kdeplot(d, ax=ax, fill=True, label='At Death', color='darkorange', alpha=0.6)
            
            ax.set_title(f"{role.capitalize()} - {mood.capitalize()}", fontweight='bold')
            if c == 1 and r == 0: ax.legend(loc='upper right')

    # ADDED: Explicit Top Title
    fig.suptitle("Probability Density of Player Moods: Global vs. At Time of Death", fontsize=16, fontweight='bold')
    
    plt.tight_layout()
    fig.subplots_adjust(top=0.88) 
    plt.savefig(os.path.join(out_dir, '4_7_mood_distributions.png'))
    plt.close()

def plot_players(data, out_dir, bin_days=1):
    setup_style()
    out = data['player_outcomes']
    w = sorted([p for p, d in out.items() if d['role'] == 'wolf'])[:3]
    v = sorted([p for p, d in out.items() if d['role'] == 'villager'])[:3]
    
    fig, axes = plt.subplots(3, 2, figsize=(12, 6), dpi=300, sharex=True)
    
    # Calculate global max rounds
    max_vote = max([d['idx'] for p in out.values() for d in p['deaths']], default=0)
    max_r = (max_vote + 1) // 2
    
    # Calculate the number of bins based on the bin_days argument
    num_bins = (max_r + bin_days - 1) // bin_days if max_r > 0 else 1
    x_pos = np.arange(num_bins + 1)
    
    # Generate dynamic X-axis labels (e.g., "1", "2-3", "4-5")
    x_labels = []
    for i in range(num_bins):
        start_round = i * bin_days + 1
        end_round = min((i + 1) * bin_days, max_r)
        if start_round == end_round:
            x_labels.append(str(start_round))
        else:
            x_labels.append(f"{start_round}-{end_round}")
    x_labels.append('WIN')

    for i in range(3):
        for j, p_list in enumerate([w, v]):
            ax = axes[i, j]
            if i >= len(p_list): 
                ax.axis('off')
                continue
            
            p = p_list[i]
            d_log = out[p]['deaths']
            
            # Use num_bins instead of max_r for array sizes
            n, d = np.zeros(num_bins), np.zeros(num_bins)
            for x in d_log:
                r_idx = (x['idx'] + 1) // 2 - 1
                b_idx = r_idx // bin_days  # Map the specific round to its respective bin
                if x['type'] == 'Night': 
                    n[b_idx] += 1
                else: 
                    d[b_idx] += 1
                
            total = sum(n) + sum(d) + out[p]['wins'] or 1
            pn, pd, pw = list(n/total)+[0], list(d/total)+[0], [0]*num_bins + [out[p]['wins']/total]
            
            ax.bar(x_pos, pn, color='#2c3e50', alpha=0.8, edgecolor='black', label='Die (Night)')
            ax.bar(x_pos, pd, bottom=pn, color='#f39c12', alpha=0.8, edgecolor='black', label='Die (Day)')
            ax.bar(x_pos, pw, bottom=np.array(pn)+np.array(pd), color='#27ae60', alpha=0.8, edgecolor='black', label='Win')
            
            ax.set_title(f"{p} ({out[p]['role'].capitalize()})", fontsize=10, fontweight='bold')
            ax.set_ylim(0, 1.1)
            
            if j == 0: 
                ax.set_ylabel('Probability', fontweight='bold', fontsize=9)
            
            if i == 0 and j == 1: 
                ax.legend(loc='upper right', fontsize=8)
            
            if i == 2:
                ax.set_xticks(x_pos)
                # Slightly rotate labels if binning is used to fit the "X-Y" strings
                ax.set_xticklabels(x_labels, rotation=45 if bin_days > 1 else 0)
                ax.set_xlabel(f"Game Round (Binned by {bin_days})" if bin_days > 1 else "Game Round", fontweight='bold', fontsize=11)

    fig.suptitle("Probability to Die in a Specific Round (Night/Day) or Win", fontsize=16, fontweight='bold')

    plt.tight_layout()
    fig.subplots_adjust(top=0.88)
    plt.savefig(os.path.join(out_dir, '6_player_outcomes_grid.png'))
    plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", type=str, required=True)
    parser.add_argument("--output-dir", type=str, default="professional_plots")
    
    # ADDED: The new bin-days argument
    parser.add_argument("--bin-days", type=int, default=1, help="Number of rounds to group together in the player outcomes plot to avoid bar clutter.")
    
    args = parser.parse_args()
    
    if not os.path.exists(args.output_dir): 
        os.makedirs(args.output_dir)
        
    print(f"Loading data from {args.data_dir}...")
    payload = extract_game_data(args.data_dir)
    
    if payload:
        print("Generating professional images...")
        plot_outcomes(payload, args.output_dir)
        plot_moods(payload, args.output_dir)
        
        # CHANGED: Pass the bin_days argument
        plot_players(payload, args.output_dir, args.bin_days)
        
        print(f"Success! Images saved in {args.output_dir}")
    else:
        print("No data found.")