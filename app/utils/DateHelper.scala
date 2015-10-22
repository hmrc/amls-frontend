package utils
import org.joda.time.LocalDate

object DateHelper {
  def isNotFutureDate = {
    date: LocalDate => !date.isAfter(LocalDate.now())
  }
}