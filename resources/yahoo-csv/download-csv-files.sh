#!/bin/bash

echo "Downloading CSV files..."
TIMESTAMP=$(date +%s)
curl "https://query1.finance.yahoo.com/v7/finance/download/ADA-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "ADA-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/BAT-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "BAT-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/BNB-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "BNB-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/BTC-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "BTC-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/CELO-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "CGLD-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/COMP-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "COMP-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/ETH-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "ETH-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/EURUSD=X?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "EUR-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/MKR-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "MKR-USD.csv"
curl "https://query1.finance.yahoo.com/v7/finance/download/XLM-USD?period1=1546300800&period2=$TIMESTAMP&interval=1d&events=history&includeAdjustedClose=true" -o "XLM-USD.csv"

echo "done!"
