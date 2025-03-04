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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.tradingpremises.ActivityStartDateFormProvider
import models.tradingpremises._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.ActivityStartDateView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ActivityStartDateController @Inject() (
  override val messagesApi: MessagesApi,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: ActivityStartDateFormProvider,
  view: ActivityStartDateView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    getData[TradingPremises](request.credId, index) map {
      case Some(tpSection) =>
        tpSection.yourTradingPremises match {
          case Some(YourTradingPremises(_, tradingPremisesAddress, _, None, _))            =>
            Ok(view(formProvider(), index, edit, tradingPremisesAddress))
          case Some(YourTradingPremises(_, tradingPremisesAddress, _, Some(startDate), _)) =>
            Ok(view(formProvider().fill(ActivityStartDate(startDate)), index, edit, tradingPremisesAddress))
          case _                                                                           =>
            NotFound(notFoundView)
        }
      case _               =>
        NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => handleInvalidForm(request.credId, index, edit, formWithErrors),
        data => handleValidForm(request.credId, index, edit, data)
      )
  }

  private def handleValidForm(credId: String, index: Int, edit: Boolean, data: ActivityStartDate): Future[Result] =
    for {
      _ <- updateDataStrict[TradingPremises](credId, index) { tp =>
             val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x =>
               Some(x.copy(startDate = Some(data.startDate)))
             )
             tp.copy(yourTradingPremises = ytp, hasChanged = true)
           }
    } yield
      if (edit) {
        Redirect(routes.CheckYourAnswersController.get(index))
      } else {
        Redirect(routes.IsResidentialController.get(index, edit))
      }

  private def handleInvalidForm(credId: String, index: Int, edit: Boolean, f: Form[ActivityStartDate])(implicit
    request: Request[_]
  ): Future[Result] =
    for {
      tp <- getData[TradingPremises](credId, index)
    } yield tp.flatMap(_.yourTradingPremises) match {
      case Some(ytp) =>
        BadRequest(view(f, index, edit, ytp.tradingPremisesAddress))
      case _         => NotFound(notFoundView)
    }
}
