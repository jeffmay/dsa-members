package zio
package csv

import com.github.tototoshi

type CSVFormat = tototoshi.csv.CSVFormat
object CsvFormat {

  object Default extends tototoshi.csv.DefaultCSVFormat
}
