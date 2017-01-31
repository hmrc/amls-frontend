package utils

import config.ApplicationConfig
import models.tradingpremises.TradingPremises
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

trait DateOfChangeHelper {

  def redirectToDateOfChange[A](a: Option[A], b: A) = {
    ApplicationConfig.release7 && !a.contains(b)
  }

  def startDateFormFields(startDate: Option[LocalDate], fieldName: String = "activityStartDate") = {
    startDate match {
      case Some(date) => Map(fieldName -> Seq(date.toString("yyyy-MM-dd")))
      case _ => Map.empty[String, Seq[String]]
    }
  }

  implicit class TradingPremisesExtensions(tradingPremises: Option[TradingPremises]) extends DateOfChangeHelper {

    def startDate = tradingPremises.yourTradingPremises.fold[Option[LocalDate]](None)(ytp => Some(ytp.startDate))

    def startDateValidationMessage =
      Messages("error.expected.tp.dateofchange.after.startdate", startDate.fold("")(_.toString("dd-MM-yyyy")))
  }

}