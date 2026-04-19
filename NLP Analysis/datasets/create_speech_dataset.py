import os
import json
import csv
import re
import argparse
import pandas as pd
from sklearn.model_selection import train_test_split

# Hardcoded list of the 15 active players in the game
PLAYERS = [
    'alice', 'bob', 'charlie', 'arthur', 'bella', 'caleb', 'david', 
    'eve', 'frank', 'grace', 'heidi', 'ivan', 'judy', 'kevin', 'leo'
]

def augment_to_minimum(df: pd.DataFrame, min_k: int) -> pd.DataFrame:
    """
    Identifies cells in the Performative x Target matrix with counts < min_k.
    Synthesizes new data points by sampling existing performatives and bijectively swapping targets.
    """
    print(f"Augmenting dataset: ensuring minimum {min_k} samples per (Performative, Target) pair...")
    augmented_rows = []
    
    # Pre-compile the regex pattern for exact name matching
    pattern = re.compile(r'\b(' + '|'.join(PLAYERS) + r')\b', flags=re.IGNORECASE)
    
    # Standardize target names for safe dictionary mapping
    df['target'] = df['target'].str.lower().str.strip()
    
    performatives = df['performative'].unique()
    
    for perf in performatives:
        perf_df = df[df['performative'] == perf]
        if len(perf_df) == 0:
            continue
            
        for target in PLAYERS:
            current_count = len(perf_df[perf_df['target'] == target])
            deficit = min_k - current_count
            
            if deficit > 0:
                # Sample 'deficit' rows with replacement
                samples = perf_df.sample(n=deficit, replace=True, random_state=42)
                
                for _, row in samples.iterrows():
                    orig_msg = str(row['message'])
                    orig_target = str(row['target'])
                    new_target = target
                    
                    # 1-to-1 Mapping Dictionary to prevent naming collisions
                    mapping = {
                        orig_target: new_target,
                        new_target: orig_target
                    }
                    
                    # Fill the rest with identity mappings
                    for p in PLAYERS:
                        if p not in mapping:
                            mapping[p] = p
                            
                    def replacer(match):
                        return mapping[match.group(1).lower()]
                        
                    # Execute simultaneous replacement
                    new_msg = pattern.sub(replacer, orig_msg)
                    
                    new_row = row.copy()
                    new_row['message'] = new_msg
                    new_row['target'] = new_target
                    augmented_rows.append(new_row)
                    
    if augmented_rows:
        augmented_df = pd.DataFrame(augmented_rows)
        df = pd.concat([df, augmented_df], ignore_index=True)
        # Shuffle dataset to distribute the synthetic rows
        df = df.sample(frac=1, random_state=42).reset_index(drop=True)
        print(f"Added {len(augmented_rows)} synthetic messages via targeted regex swapping.")
    else:
        print("No augmentation needed. All cells satisfy the minimum K constraint.")
        
    return df

def balance_data(df: pd.DataFrame, balance_perf: bool, balance_target: bool) -> pd.DataFrame:
    """Downsamples dataset. If both flags are set, balances the joint distribution (performative, target)."""
    if balance_perf and balance_target:
        df['pair'] = df['performative'] + "_" + df['target']
        min_pair = df['pair'].value_counts().min()
        df = df.groupby('pair').sample(n=min_pair, random_state=42).drop(columns=['pair'])
    elif balance_perf:
        min_p = df['performative'].value_counts().min()
        df = df.groupby('performative').sample(n=min_p, random_state=42)
    elif balance_target:
        min_t = df['target'].value_counts().min()
        df = df.groupby('target').sample(n=min_t, random_state=42)
    
    return df.reset_index(drop=True)

def save_stats_matrix(df: pd.DataFrame, output_path: str) -> None:
    """Generates and saves a 2D co-occurrence matrix of performative vs target, including marginal totals."""
    if len(df) == 0:
        return
        
    matrix = pd.crosstab(
        df['performative'], 
        df['target'], 
        margins=True, 
        margins_name="Total"
    )
    
    matrix.to_csv(output_path)
    print(f"Saved stats matrix (with marginals) to: {output_path}")

def build_dataset(input_directory: str, output_csv_path: str, split_ratio: float = None, 
                  balance_perf: bool = False, balance_target: bool = False, 
                  save_stats: bool = False, at_least: int = None) -> None:
    
    extraction_pattern = re.compile(r'^speech\(([^,]+),\s*"(.*?)",\s*([a-zA-Z_]+)\(([^)]+)\)\)$')
    
    total_files_processed = 0
    extracted_rows = []
    skipped_errors = 0

    print(f"Scanning {input_directory} for JSON logs...")

    for root, _, files in os.walk(input_directory):
        if 'narrator.json' in files:
            file_path = os.path.join(root, 'narrator.json')
            total_files_processed += 1
            player_data_cache = {}
            
            try:
                with open(file_path, 'r', encoding='utf-8') as jf:
                    data = json.load(jf)
                    
                    narrator_log = data.get('narrator_log', [])
                    for phase in narrator_log:
                        actions = phase.get('actions', [])
                        
                        for action in actions:
                            content = action.get('content', '')
                            action_phase_id = int(action.get('phase_id', -1))
                            
                            if content.startswith('speech('):
                                match = extraction_pattern.match(content)
                                if match:
                                    speaker = match.group(1).lower().strip()
                                    message = match.group(2)
                                    performative = match.group(3)
                                    target = match.group(4).lower().strip()
                                    
                                    # Strict filter: target must be in PLAYERS
                                    if target not in PLAYERS:
                                        continue
                                    
                                    if message.strip().strip('"') == 'error':
                                        skipped_errors += 1
                                        continue
                                    
                                    current_stress = None
                                    current_exposure = None
                                    
                                    if speaker not in player_data_cache:
                                        speaker_file = os.path.join(root, f"{speaker}.json")
                                        if os.path.exists(speaker_file):
                                            with open(speaker_file, 'r', encoding='utf-8') as sf:
                                                player_data_cache[speaker] = json.load(sf).get('mood_log', [])
                                        else:
                                            player_data_cache[speaker] = []
                                            
                                    for mood_event in player_data_cache[speaker]:
                                        mood_phase = int(mood_event.get('phase_id', -1))
                                        if mood_phase <= action_phase_id:
                                            if mood_event['mood'] == 'stress':
                                                current_stress = mood_event['value']
                                            elif mood_event['mood'] == 'exposure':
                                                current_exposure = mood_event['value']
                                        else:
                                            break
                                    
                                    extracted_rows.append({
                                        'speaker': speaker,
                                        'message': message,
                                        'performative': performative,
                                        'target': target,
                                        'mood_stress': round(current_stress, 3) if current_stress is not None else None,
                                        'mood_exposure': round(current_exposure, 3) if current_exposure is not None else None
                                    })
                                else:
                                    print(f"WARNING: Regex failed to match syntax in {file_path}. Content: {content}")
                                    
            except json.JSONDecodeError:
                print(f"ERROR: Invalid JSON formatting in {file_path}")
            except Exception as e:
                print(f"ERROR: Processing {file_path} failed. Reason: {e}")

    df = pd.DataFrame(extracted_rows)
    print(f"Extraction Complete. Processed {total_files_processed} files. Found {len(df)} valid entries.")

    if len(df) == 0:
        print("No valid data extracted. Aborting.")
        return

    # Phase 2: Augmentation (Synthesizing up to K)
    if at_least is not None:
        if not (balance_perf and balance_target):
            raise ValueError("ERROR: --at-least flag strictly requires both --balance-perf and --balance-target to be set.")
        df = augment_to_minimum(df, at_least)

    # Phase 3: Global Balancing (Downsampling to uniform distribution)
    if balance_perf or balance_target:
        print("Applying global dataset downsampling/balancing...")
        df = balance_data(df, balance_perf, balance_target)
        print(f"Final balanced dataset size: {len(df)} entries.")

    # Phase 4: Output Primary
    df.to_csv(output_csv_path, index=False, quoting=csv.QUOTE_ALL)
    print(f"Saved primary dataset to: {output_csv_path}")

    out_dir = os.path.dirname(output_csv_path) if os.path.dirname(output_csv_path) else "."
    
    if save_stats:
        save_stats_matrix(df, os.path.join(out_dir, "merged_stats.csv"))

    # Phase 5: Stratified Splitting
    if split_ratio is not None:
        if not (0.0 < split_ratio < 1.0):
            print("ERROR: --split must be a float between 0.0 and 1.0. Splitting aborted.")
            return
            
        print(f"Applying Stratified Split (Test Ratio: {split_ratio})...")
        
        # Determine exact stratification logic
        if balance_perf and balance_target:
            stratify_col = df['performative'] + "_" + df['target']
        elif balance_perf:
            stratify_col = df['performative']
        elif balance_target:
            stratify_col = df['target']
        else:
            stratify_col = df['performative'] 
            
        train_df, test_df = train_test_split(
            df, 
            test_size=split_ratio, 
            stratify=stratify_col, 
            random_state=42
        )

        train_path = os.path.join(out_dir, "train.csv")
        test_path = os.path.join(out_dir, "test.csv")
        
        train_df.to_csv(train_path, index=False, quoting=csv.QUOTE_ALL)
        test_df.to_csv(test_path, index=False, quoting=csv.QUOTE_ALL)
        
        print(f"Saved strictly stratified train split ({len(train_df)} rows) to: {train_path}")
        print(f"Saved strictly stratified test split ({len(test_df)} rows) to: {test_path}")

        if save_stats:
            save_stats_matrix(train_df, os.path.join(out_dir, "train_stats.csv"))
            save_stats_matrix(test_df, os.path.join(out_dir, "test_stats.csv"))

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract speech logs and build balanced, augmented datasets.")
    parser.add_argument("--input-dir", type=str, default="../GAMES_TO_BUILD_DATASET", help="Directory containing JSON logs.")
    parser.add_argument("--output-csv", type=str, default="merged.csv", help="Output path for the merged CSV.")
    parser.add_argument("--split", type=float, default=None, help="Ratio for the test split (e.g., 0.2 for 20% test data).")
    parser.add_argument("--balance-perf", action="store_true", help="Balance dataset based on Performative classes.")
    parser.add_argument("--balance-target", action="store_true", help="Balance dataset based on Target classes.")
    parser.add_argument("--save-stats", action="store_true", help="Save a matrix of (performative, target) occurrences.")
    parser.add_argument("--at-least", type=int, default=None, help="Synthesize missing targets via regex swap to ensure a minimum of K occurrences per (Performative, Target). Requires balancing flags.")
    
    args = parser.parse_args()
    
    if os.path.exists(args.input_dir):
        try:
            build_dataset(
                input_directory=args.input_dir, 
                output_csv_path=args.output_csv,
                split_ratio=args.split,
                balance_perf=args.balance_perf,
                balance_target=args.balance_target,
                save_stats=args.save_stats,
                at_least=args.at_least
            )
        except ValueError as e:
            print(e)
    else:
        print(f"ERROR: The directory {args.input_dir} does not exist.")