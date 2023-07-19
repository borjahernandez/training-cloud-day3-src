#!/bin/bash

# Number of rows to generate
num_rows=$1

# Output file name
output_file="vehicle-n-sensor.json"

# Generate random data and save it to the output file
for ((i=1; i<=num_rows; i++))
do
    vehicle_id=$2
    engine_temperature=$(awk -v min=80 -v max=100 'BEGIN{srand(); printf "%.0f\n", min+rand()*(max-min)}')
    average_rpm=$((RANDOM%3000+1000))
    pressure_tyre_1=$(awk -v min=2.0 -v max=2.5 'BEGIN{srand(); printf "%.1f\n", min+rand()*(max-min)}')
    pressure_tyre_2=$(awk -v min=2.0 -v max=2.5 'BEGIN{srand(); printf "%.1f\n", min+rand()*(max-min)}')
    pressure_tyre_3=$(awk -v min=2.0 -v max=2.5 'BEGIN{srand(); printf "%.1f\n", min+rand()*(max-min)}')
    pressure_tyre_4=$(awk -v min=2.0 -v max=2.5 'BEGIN{srand(); printf "%.1f\n", min+rand()*(max-min)}')

    echo "{\"vehicle_id\":$vehicle_id,\"engine_temperature\":$engine_temperature,\"average_rpm\":$average_rpm,\"pressure_tyre_1\":$pressure_tyre_1,\"pressure_tyre_2\":$pressure_tyre_2,\"pressure_tyre_3\":$pressure_tyre_3,\"pressure_tyre_4\":$pressure_tyre_4}" >> "$output_file"
done

echo "Generated $num_rows rows of random data in $output_file."