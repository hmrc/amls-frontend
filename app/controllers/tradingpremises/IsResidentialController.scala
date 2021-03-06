/*
 * Copyright 2021 HM Revenue & Customs
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
import forms._
import javax.inject.{Inject, Singleton}
import models.businesscustomer.Address
import models.businessmatching.BusinessMatching
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, RepeatingSection, StatusConstants}
import views.html.tradingpremises.is_residential

import scala.concurrent.Future


@Singleton
class  IsResidentialController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val dataCacheConnector: DataCacheConnector,
                                          val cc: MessagesControllerComponents,
                                          is_residential: is_residential,
                                          implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false) = authAction.async{
      implicit request =>
        dataCacheConnector.fetchAll(request.credId).map { cacheO =>
          (for {
            cache <- cacheO
            tradingPremises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
            tp <- tradingPremises.lift(index - 1)
          } yield {

            val form = tp.yourTradingPremises match {
              case Some(YourTradingPremises(_, _, Some(boolean), _, _)) => Form2[IsResidential](IsResidential(boolean))
              case _ => EmptyForm
            }

            Ok(is_residential(form, tp.yourTradingPremises.map(_.tradingPremisesAddress.toBCAddress), index, edit))
          }) getOrElse NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetchAll(request.credId).flatMap { cacheO =>
          Form2[IsResidential](request.body) match {
            case f: InvalidForm =>
              val address = for {
                cache <- cacheO
                tradingPremises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                address <- getAddress(bm) if isFirstTradingPremises(tradingPremises, index)
              } yield address

              Future.successful(BadRequest(is_residential(f, address, index, edit)))
            case ValidForm(_, data) =>
              for {
                result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_, tp) =>
                  val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None) { yourTradingPremises =>
                    Some(yourTradingPremises.copy(isResidential = Some(data.isResidential)))
                  }
                  tp.yourTradingPremises(ytp)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.WhatDoesYourBusinessDoController.get(index))
              }
          }
        }
  }

  private def getAddress(businessMatching: BusinessMatching): Option[Address] =
    businessMatching.reviewDetails.fold[Option[Address]](None)(r => Some(r.businessAddress))

  private def isFirstTradingPremises(tradingPremises: Seq[TradingPremises], index: Int): Boolean = {

    val tpWithoutDeleted =  tradingPremises.zipWithIndex.filterNot { case (tp, _) =>
      tp.status.contains(StatusConstants.Deleted)
    }

    tpWithoutDeleted match {
      case (_, i) :: _ if i equals (index - 1) => true
      case _ => false
    }

  }

}