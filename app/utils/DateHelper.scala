package utils

import org.joda.time.{LocalDate, LocalDateTime}
import org.joda.time.format.DateTimeFormat

object DateHelper {

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def isNotFutureDate = {
    date: LocalDate => !date.isAfter(LocalDate.now())
  }

  def formatDate(date: LocalDate) = {
    DateTimeFormat.forPattern("d MMMM yyyy").print(date)
  }

  def formatDate(date: LocalDateTime) = {
    DateTimeFormat.forPattern("d MMMM yyyy").print(date)
  }
}
