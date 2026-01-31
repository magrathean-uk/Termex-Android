
import subprocess
import json
import os

# DB Config (provide via environment variables)
HOST = os.environ.get("TESLA_DB_HOST")
USER = os.environ.get("TESLA_DB_USER")
DB = os.environ.get("TESLA_DB_NAME")
PASS = os.environ.get("TESLA_DB_PASS")

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
    missing = [name for name, value in {
        "TESLA_DB_HOST": HOST,
        "TESLA_DB_USER": USER,
        "TESLA_DB_NAME": DB,
        "TESLA_DB_PASS": PASS,
    }.items() if not value]
    if missing:
        print("Missing required environment variables:")
        for name in missing:
            print(f"  - {name}")
        print("\nSet them before running this script. Example:")
        print("  TESLA_DB_HOST=... TESLA_DB_USER=... TESLA_DB_NAME=... TESLA_DB_PASS=... python verify_db_logic.py")
        return

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
