
import subprocess
import json
import os

# DB Config
HOST = "172.17.17.53"
USER = "teslamate"
DB = "teslamate"
PASS = "852266iA"

def run_query(label, sql):
    print(f"\n--- {label} ---")
    print(f"Query: {sql}")
    env = os.environ.copy()
    env['PGPASSWORD'] = PASS
    
    cmd = [
        "psql",
        "-h", HOST,
        "-U", USER,
        "-d", DB,
        "-c", sql
    ]
    
    try:
        result = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if result.returncode == 0:
            print("SUCCESS:")
            print(result.stdout)
            return True
        else:
            print("ERROR:")
            print(result.stderr)
            return False
    except Exception as e:
        print(f"EXCEPTION: {e}")
        return False

def main():
    print("Starting DB Verification...")
    
    # 1. Fetch Cars
    cars_sql = "SELECT id, vin, model, trim_badging, efficiency FROM cars ORDER BY id ASC;"
    if not run_query("Fetch Cars", cars_sql):
        return

    # Assuming we found car_id 1 from the previous query (which we saw in earlier manual verification)
    car_id = 1
    print(f"Using Car ID: {car_id} for further tests")
    
    # 2. Fetch Status (Positions)
    status_sql = f"""
        SELECT date, battery_level, usable_battery_level, ideal_battery_range_km, odometer, outside_temp 
        FROM positions 
        WHERE car_id = {car_id} 
        ORDER BY date DESC 
        LIMIT 1;
    """
    run_query("Fetch Latest Status", status_sql)
    
    # 3. Fetch Recent Drives
    drives_sql = f"""
        SELECT id, start_date, end_date, distance, duration_min 
        FROM drives 
        WHERE car_id = {car_id} 
        ORDER BY start_date DESC 
        LIMIT 5;
    """
    run_query("Fetch Recent Drives", drives_sql)
    
    # 4. Fetch Recent Charges
    charges_sql = f"""
        SELECT id, start_date, charge_energy_added, start_battery_level, end_battery_level 
        FROM charging_processes 
        WHERE car_id = {car_id} 
        ORDER BY start_date DESC 
        LIMIT 5;
    """
    run_query("Fetch Recent Charges", charges_sql)
    
    # 5. Battery Health Data (Complex Aggregate)
    health_sql = f"""
        SELECT date, battery_level, ideal_battery_range_km 
        FROM charges 
        WHERE charging_process_id IN (
            SELECT id FROM charging_processes WHERE car_id = {car_id} ORDER BY start_date DESC LIMIT 5
        )
        LIMIT 5;
    """
    run_query("Fetch Battery Health Inputs", health_sql)
    
    print("\nVerification Complete.")

if __name__ == "__main__":
    main()
