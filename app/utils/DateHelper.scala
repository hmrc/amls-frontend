package utils

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object DateHelper {
  def isNotFutureDate = {
    date: LocalDate => !date.isAfter(LocalDate.now())
  }

  def formatDate(date: LocalDate) = {
    DateTimeFormat.forPattern("d MMMM yyyy").print(date)
  }
}
