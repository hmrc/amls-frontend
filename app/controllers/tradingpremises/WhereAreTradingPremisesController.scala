/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data._
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.status.SubmissionStatus
import models.tradingpremises._
import org.joda.time.LocalDate
import play.api.mvc.Request
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.tradingpremises._

import scala.concurrent.Future

class WhereAreTradingPremisesController @Inject () (
                                                     val dataCacheConnector: DataCacheConnector,
                                                     val statusService: StatusService,
                                                     val auditConnector: AuditConnector,
                                                     val authAction: AuthAction
                                                   )extends RepeatingSection with DefaultBaseController with DateOfChangeHelper with FormHelpers {



  def get(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      getData[TradingPremises](request.credId, index) map {
        case Some(TradingPremises(_, Some(data), _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case Some(_) =>
          Ok(where_are_trading_premises(EmptyForm, edit, index))
        case _ =>
          NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(where_are_trading_premises(f, edit, index)))
        case ValidForm(_, ytp) =>
          val block = for {
            tradingPremises <- OptionT(getData[TradingPremises](request.credId, index))
            _ <- OptionT.liftF(updateDataStrict[TradingPremises](request.credId, index)(updateTradingPremises(ytp, _)))
            status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
            _ <- OptionT.liftF(
              sendAudits(ytp.tradingPremisesAddress, tradingPremises.yourTradingPremises.fold[Option[Address]](None)(_.tradingPremisesAddress.some), edit)
            )
          } yield redirectTo(index, edit, ytp, tradingPremises, status)

          block getOrElse NotFound(notFoundView)
      }
  }

  private def sendAudits(address: Address, oldAddress: Option[Address], edit: Boolean)
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(address, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(address))
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

  private def redirectTo(index: Int, edit: Boolean, ytp: YourTradingPremises, tp: TradingPremises, status: SubmissionStatus)
                        (implicit hc: HeaderCarrier) = {
    if (redirectToDateOfChange(Some(tp), ytp) && edit && isEligibleForDateOfChange(status)) {
      Redirect(routes.WhereAreTradingPremisesController.dateOfChange(index))
    } else {
      edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index))
        case _ => Redirect(routes.ActivityStartDateController.get(index, edit))
      }
    }
  }

  def dateOfChange(index: Int) = authAction.async {
    implicit request =>
        Future(Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)),
          "summary.tradingpremises", controllers.tradingpremises.routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
  }

  def saveDateOfChange(index: Int) = authAction.async {
    implicit request =>
      getData[TradingPremises](request.credId, index) flatMap { tradingPremises =>
        Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDateFormFields(tradingPremises.startDate)) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(
              views.html.date_of_change(
                form.withMessageFor(DateOfChange.errorPath, tradingPremises.startDateValidationMessage),
                "summary.tradingpremises", routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
          case ValidForm(_, dateOfChange) =>
            updateDataStrict[TradingPremises](request.credId, index) { tp =>
              tp.yourTradingPremises.fold(tp) { ytp =>
                tp.copy(
                  yourTradingPremises = Some(ytp.copy(
                    tradingNameChangeDate = Some(dateOfChange),
                    tradingPremisesAddress = ytp.tradingPremisesAddress.copy(dateOfChange = Some(dateOfChange))
                  )))
              }
            } map { _ =>
              Redirect(routes.DetailedAnswersController.get(1))
            }
        }
      }
  }

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], premises: YourTradingPremises) =
    (
      for {
        tp <- tradingPremises
        ytp <- tp.yourTradingPremises
      } yield (ytp.tradingName != premises.tradingName || ytp.tradingPremisesAddress != premises.tradingPremisesAddress) && tp.lineId.isDefined
    ).getOrElse(false)
}