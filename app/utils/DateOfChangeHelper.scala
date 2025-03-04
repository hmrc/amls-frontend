/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import controllers.hvd.routes
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionStatus}
import models.tradingpremises.TradingPremises
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Call

import java.time.LocalDate
import java.time.format.DateTimeFormatter.ofPattern

trait DateOfChangeHelper extends Logging {

  case class DateOfChangeRedirect(call: Call)

  object DateOfChangeRedirect {

    val checkYourAnswers: String    = "1"
    val cashPayment: String         = "2"
    val howWillYouSellGoods: String = "3"
    val exciseGoods: String         = "4"
    val exciseGoodsEdit: String     = "5"

    def apply(key: String): DateOfChangeRedirect =
      key match {
        case `checkYourAnswers`    => DateOfChangeRedirect(routes.SummaryController.get)
        case `cashPayment`         => DateOfChangeRedirect(routes.CashPaymentController.get())
        case `howWillYouSellGoods` => DateOfChangeRedirect(routes.HowWillYouSellGoodsController.get())
        case `exciseGoods`         => DateOfChangeRedirect(routes.ExciseGoodsController.get())
        case `exciseGoodsEdit`     => DateOfChangeRedirect(routes.ExciseGoodsController.get(true))
        case _                     =>
          logger.warn(s"Could not retrieve Date of Change redirect for '$key', redirecting to Check Your Answers")
          DateOfChangeRedirect(routes.SummaryController.get)
      }
  }

  def isEligibleForDateOfChange(status: SubmissionStatus): Boolean =
    status match {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) => true
      case _                                                                     => false
    }

  private def isEligibleApplicationStatus(status: String): Boolean =
    status.contains("Approved")

  def redirectToDateOfChange[A](status: SubmissionStatus, a: Option[A], b: A): Boolean =
    !a.contains(b) && isEligibleForDateOfChange(status)

  def dateOfChangApplicable[A](status: String, a: Option[A], b: A): Boolean =
    !a.contains(b) && isEligibleApplicationStatus(status)

  def startDateFormFields(
    startDate: Option[LocalDate],
    fieldName: String = "activityStartDate"
  ): Map[String, Seq[String]] =
    startDate match {
      case Some(date) => Map(fieldName -> Seq(date.format(ofPattern("yyyy-MM-dd"))))
      case _          => Map.empty[String, Seq[String]]
    }

  implicit class TradingPremisesExtensions(tradingPremises: Option[TradingPremises])(implicit messages: Messages)
      extends DateOfChangeHelper {

    def startDate: Option[LocalDate] =
      tradingPremises.yourTradingPremises.fold[Option[LocalDate]](None)(ytp => ytp.startDate)
  }

}
