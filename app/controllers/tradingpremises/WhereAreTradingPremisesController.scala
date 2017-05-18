/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.tradingpremises

import audit.AddressCreatedEvent
import config.{AMLSAuditConnector, AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionStatus}
import models.tradingpremises._
import org.joda.time.LocalDate
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{DateOfChangeHelper, FeatureToggle, RepeatingSection}
import views.html.tradingpremises._
import audit.AddressConversions._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService
  val auditConnector: AuditConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(TradingPremises(_, Some(data), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case Some(_) =>
          Ok(where_are_trading_premises(EmptyForm, edit, index))
        case _ =>
          NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(where_are_trading_premises(f, edit, index)))
        case ValidForm(_, ytp) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index)(updateTradingPremises(ytp, _))
            status <- statusService.getStatus
            result <- redirectTo(index, edit, ytp, tradingPremises, status)
          } yield result
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def updateTradingPremises(ytp: YourTradingPremises, tp: TradingPremises) = {

    val updatedYtp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](Some(ytp))(x =>
      Some(ytp.copy(startDate = x.startDate, isResidential = x.isResidential)))

    TradingPremises(
      tp.registeringAgentPremises,
      updatedYtp, tp.businessStructure, tp.agentName, tp.agentCompanyDetails,
      tp.agentPartnership, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices,
      hasChanged = true, tp.lineId, tp.status, tp.endDate
    )

  }

  private def redirectTo(index: Int, edit: Boolean, ytp: YourTradingPremises, tp: Option[TradingPremises], status: SubmissionStatus)
                        (implicit hc: HeaderCarrier) = {
    if (redirectToDateOfChange(tp, ytp) && edit && isEligibleForDateOfChange(status)) {
      Future.successful(Redirect(routes.WhereAreTradingPremisesController.dateOfChange(index)))
    } else {
      edit match {
        case true => Future.successful(Redirect(routes.SummaryController.getIndividual(index)))
        case _ =>
          auditConnector.sendEvent(AddressCreatedEvent(ytp.tradingPremisesAddress)) map { _ =>
            Redirect(routes.ActivityStartDateController.get(index, edit))
          }
      }
    }
  }

  def dateOfChange(index: Int) = FeatureToggle(ApplicationConfig.release7) {
    Authorised {
      implicit authContext => implicit request =>
        Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)),
          "summary.tradingpremises", controllers.tradingpremises.routes.WhereAreTradingPremisesController.saveDateOfChange(index)))
    }
  }

  def saveDateOfChange(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) flatMap { tradingPremises =>
        Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDateFormFields(tradingPremises.startDate)) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(
              views.html.date_of_change(
                form.withMessageFor(DateOfChange.errorPath, tradingPremises.startDateValidationMessage),
                "summary.tradingpremises", routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
          case ValidForm(_, dateOfChange) =>
            updateDataStrict[TradingPremises](index) { tp =>
              tp.yourTradingPremises.fold(tp) { ytp =>
                tp.copy(
                  yourTradingPremises = Some(ytp.copy(
                    tradingNameChangeDate = Some(dateOfChange),
                    tradingPremisesAddress = ytp.tradingPremisesAddress.copy(dateOfChange = Some(dateOfChange))
                  )))
              }
            } map { _ =>
              Redirect(routes.SummaryController.get())
            }
        }
      }
  }

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], premises: YourTradingPremises) =
    ApplicationConfig.release7 && (for {
      tp <- tradingPremises
      ytp <- tp.yourTradingPremises
    } yield (ytp.tradingName != premises.tradingName || ytp.tradingPremisesAddress != premises.tradingPremisesAddress) && tp.lineId.isDefined
      ).getOrElse(false)

}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
  override lazy val auditConnector = AMLSAuditConnector
}
