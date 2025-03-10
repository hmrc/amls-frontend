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

package controllers.tradingpremises

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data._
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import forms.tradingpremises.TradingAddressFormProvider
import models.DateOfChange
import models.status.SubmissionStatus
import models.tradingpremises._
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, DateHelper, DateOfChangeHelper, RepeatingSection}
import views.html.DateOfChangeView
import views.html.tradingpremises.WhereAreTradingPremisesView

import scala.concurrent.Future

class WhereAreTradingPremisesController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val auditConnector: AuditConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: TradingAddressFormProvider,
  dateChangeFormProvider: DateOfChangeFormProvider,
  view: WhereAreTradingPremisesView,
  dateChangeView: DateOfChangeView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    getData[TradingPremises](request.credId, index) map {
      case Some(TradingPremises(_, Some(data), _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
        Ok(view(formProvider().fill(data), edit, index))
      case Some(_)                                                                     =>
        Ok(view(formProvider(), edit, index))
      case _                                                                           =>
        NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, index))),
        ytp => {
          val block = for {
            tradingPremises <- OptionT(getData[TradingPremises](request.credId, index))
            _               <- OptionT.liftF(updateDataStrict[TradingPremises](request.credId, index)(updateTradingPremises(ytp, _)))
            status          <-
              OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
            _               <- OptionT.liftF(
                                 sendAudits(
                                   ytp.tradingPremisesAddress,
                                   tradingPremises.yourTradingPremises.fold[Option[Address]](None)(_.tradingPremisesAddress.some),
                                   edit
                                 )
                               )
          } yield redirectTo(index, edit, ytp, tradingPremises, status)

          block getOrElse NotFound(notFoundView)
        }
      )
  }

  private def sendAudits(address: Address, oldAddress: Option[Address], edit: Boolean)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[AuditResult] =
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(address, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(address))
    }

  private def updateTradingPremises(ytp: YourTradingPremises, tp: TradingPremises) = {

    val updatedYtp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](Some(ytp))(x =>
      Some(ytp.copy(startDate = x.startDate, isResidential = x.isResidential))
    )

    TradingPremises(
      tp.registeringAgentPremises,
      updatedYtp,
      tp.businessStructure,
      tp.agentName,
      tp.agentCompanyDetails,
      tp.agentPartnership,
      tp.whatDoesYourBusinessDoAtThisAddress,
      tp.msbServices,
      hasChanged = true,
      tp.lineId,
      tp.status,
      tp.endDate
    )

  }

  private def redirectTo(
    index: Int,
    edit: Boolean,
    ytp: YourTradingPremises,
    tp: TradingPremises,
    status: SubmissionStatus
  ) =
    if (redirectToDateOfChange(Some(tp), ytp) && edit && isEligibleForDateOfChange(status)) {
      Redirect(routes.WhereAreTradingPremisesController.dateOfChange(index))
    } else {
      if (edit) {
        Redirect(routes.CheckYourAnswersController.get(index))
      } else {
        Redirect(routes.ActivityStartDateController.get(index, edit))
      }
    }

  def dateOfChange(index: Int): Action[AnyContent] = authAction { implicit request =>
    Ok(
      dateChangeView(
        dateChangeFormProvider(),
        "summary.tradingpremises",
        controllers.tradingpremises.routes.WhereAreTradingPremisesController.saveDateOfChange(index)
      )
    )
  }

  def saveDateOfChange(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dateChangeFormProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getDateView(formWithErrors, index))),
        dateOfChange =>
          getData[TradingPremises](request.credId, index).flatMap { tradingPremises =>
            tradingPremises.startDate match {
              case Some(date) if dateOfChange.dateOfChange.isAfter(date) =>
                updateDataStrict[TradingPremises](request.credId, index) { tp =>
                  tp.yourTradingPremises.fold(tp) { ytp =>
                    tp.copy(
                      yourTradingPremises = Some(
                        ytp.copy(
                          tradingNameChangeDate = Some(dateOfChange),
                          tradingPremisesAddress = ytp.tradingPremisesAddress.copy(dateOfChange = Some(dateOfChange))
                        )
                      )
                    )
                  }
                } map { _ =>
                  Redirect(routes.CheckYourAnswersController.get(index))
                }
              case Some(date)                                            =>
                Future.successful(
                  BadRequest(
                    getDateView(
                      dateChangeFormProvider().withError(
                        "dateOfChange",
                        messages(
                          "error.expected.tp.dateofchange.after.startdate",
                          DateHelper.formatDate(date)
                        )
                      ),
                      index
                    )
                  )
                )
              case None                                                  =>
                Future.failed(new Exception("Could not retrieve start date"))
            }
          }
      )
  }

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], premises: YourTradingPremises) =
    (
      for {
        tp  <- tradingPremises
        ytp <- tp.yourTradingPremises
      } yield (ytp.tradingName != premises.tradingName || ytp.tradingPremisesAddress != premises.tradingPremisesAddress) && tp.lineId.isDefined
    ).getOrElse(false)

  private def getDateView(form: Form[DateOfChange], index: Int)(implicit request: Request[_]): Html = dateChangeView(
    form,
    "summary.tradingpremises",
    routes.WhereAreTradingPremisesController.saveDateOfChange(index)
  )
}
