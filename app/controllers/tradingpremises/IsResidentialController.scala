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
import forms.tradingpremises.IsResidentialFormProvider
import models.businesscustomer.Address
import models.businessmatching.BusinessMatching
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.tradingpremises.IsResidentialView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class IsResidentialController @Inject() (
  override val messagesApi: MessagesApi,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: IsResidentialFormProvider,
  view: IsResidentialView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId).map { cacheO =>
      (for {
        cache           <- cacheO
        tradingPremises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
        tp              <- tradingPremises.lift(index - 1)
      } yield {

        val form = tp.yourTradingPremises match {
          case Some(YourTradingPremises(_, _, Some(boolean), _, _)) => formProvider().fill(IsResidential(boolean))
          case _                                                    => formProvider()
        }

        Ok(view(form, tp.yourTradingPremises.map(_.tradingPremisesAddress.toBCAddress), index, edit))
      }) getOrElse NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId).flatMap { cacheO =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithError => {
            val address = for {
              cache           <- cacheO
              tradingPremises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
              bm              <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              address         <- getAddress(bm) if isFirstTradingPremises(tradingPremises, index)
            } yield address

            Future.successful(BadRequest(view(formWithError, address, index, edit)))
          },
          data =>
            for {
              result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_, tp) =>
                          val ytp =
                            tp.yourTradingPremises.fold[Option[YourTradingPremises]](None) { yourTradingPremises =>
                              Some(yourTradingPremises.copy(isResidential = Some(data.isResidential)))
                            }
                          tp.yourTradingPremises(ytp)
                        }
            } yield
              if (edit) {
                Redirect(routes.CheckYourAnswersController.get(index))
              } else {
                Redirect(routes.WhatDoesYourBusinessDoController.get(index))
              }
        )
    }
  }

  private def getAddress(businessMatching: BusinessMatching): Option[Address] =
    businessMatching.reviewDetails.fold[Option[Address]](None)(r => Some(r.businessAddress))

  def isFirstTradingPremises(tradingPremises: Seq[TradingPremises], index: Int): Boolean = {

    val tpWithoutDeleted = tradingPremises.zipWithIndex.filterNot { case (tp, _) =>
      tp.status.contains(StatusConstants.Deleted)
    }

    tpWithoutDeleted match {
      case (_, i) :: _ if i equals (index - 1) => true
      case _                                   => false
    }

  }

}
