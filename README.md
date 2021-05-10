# Louis Tully

Who does your taxes?

## Introduction

This application calculates average prices, profits and taxes for stocks and
cryptocurrencies according to the Finnish legislation. The user is expected
to maintain a CSV file of trades.

### FIAT support

This application supports euros and US dollars and can provide reports in either
currency.

### Cryptocurrency support

In Finland, selling cryptocurrencies or trading cryptocurrency to some other
cryptocurrency triggers taxing. In crypto-to-crypto trades, the fiat value of
the transaction must be figured out from a reliable source and that value must
be used to calculate profits or losses which are taxed as if the cryptocurrency
was sold for fiat.

This software tries to automate the "figuring out" part by including daily
prices for cryptocurrencies.

### Disclaimer

**There are no guarantees that the calculations this application produces are
correct, so use at your own risk. None of the parties developing this project
accept any responsiblity or liability regarding the use of this software and its
related material.**

## Getting started

The prerequisites for running the application are:

* The CSV file of transactions is created.
* Clojure CLI tools are installed.
* The historical prices are up-to-date.

### Transaction as a CSV file

The application reads the transactions from a CSV file. It is recommended to
keep separate files for stocks and cryptocurrencies.

The first line of the file must be used for headers. The headers are (in order): 

* **Local** date and time in.
  [ISO 8601 format](https://en.wikipedia.org/wiki/ISO_8601)
* Ticker for asset that was sold.
* Amount that was sold.
* Ticker for asset that was bought.
* Amount that was bought.

This application calculates the prices and profits from transactions where fiat
was used. The application can also convert ```EUR``` to ```USD``` and vice versa
from 2019 onwards. 

In transactions where fiat was not used, historical daily prices are used. When
the price cannot be determined, the application throws an exception. This will
happen if the transaction does not use either euros or dollars, or if the price
history is not available for that ticker / timestamp combination.

The application does not care whether or not the tickers are real. However, the
historical prices are only available for the following tickers from the year
2019 onwards unless otherwise stated:

* ```ADA```
* ```BAT```
* ```BNB```
* ```BTC```
* ```CGLD``` (from 2020-10-20 onwards only)
* ```COMP``` (from 2020-06-16 onwards only)
* ```ETH```
* ```MKR```
* ```XLM```

Tether is also supported and is handled as US dollar.

Below is a simple example for stocks:

```csv
Timestamp,From-ticker,From-amount,To-ticker,To-amount
2019-01-01T00:00:00,EUR,1000,TSLA,2
2019-01-01T12:00:00,TSLA,1,EUR,700
2020-03-01T00:00:00,EUR,3000,AMZN,1
2020-05-01T00:00:00,AMZN,1,EUR,3500
```

For a more complicated example in cryptocurrencies, see
[this example](resources/example.csv).

### Running the application

The application expects the following command line arguments:

* Path to the CSV file relative from the project root.
* Ticker for the fiat currency (```eur``` or ```usd```) used for reporting.
* Tax rate as a decimal number. 

Below is an example command for running the application (executed inside the
project root directory).

```bash
clj -M -m louis-tully.core resources/example.csv eur 0.3 
```

The application will then print the current portfolio, average buying prices,
yearly profits and yearly taxes. It will also write a new version of the CSV
file (with ```-with-profits.csv``` suffix), which includes the profit for each
transaction where assets were sold.

### Limitations

* Trying to sell assets not available in the portfolio will cause an exception.
  No way to handle FIFO selling if there's nothing to sell.
* If non-fiat asset is traded to non-fiat asset and there's no historical price
  for those assets, the application throws an exception.
* The application does not update the historical data automatically, so most
  likely it's not up-to-date when cloning this repository. The data must be
  manually updated. See [Development](#Development) for instructions.

## Development

Installation of Clojure CLI Tools is necessary to develop this project. After
installing, REPL from command line or from an IDE can be used for the
development.

### Updating the historical data

There's a
[script for updating the historical prices from Yahoo Finance](resources/yahoo-csv/download-csv-files.sh).

## TODO

* Support for more cryptocurrencies.
* Web application?
