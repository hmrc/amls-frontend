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

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businesscustomer.Address
import models.businessmatching.BusinessMatching
import models.tradingpremises._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.tradingpremises.is_residential

import scala.concurrent.Future

@Singleton
class  IsResidentialController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          val authConnector: AuthConnector,
                                          val dataCacheConnector: DataCacheConnector) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false) = Authorised.async{
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll map { cacheO =>
          (for {
            cache <- cacheO
            tradingPremises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
            tp <- tradingPremises.lift(index)
          } yield {
            val form = tp.yourTradingPremises match {
              case Some(YourTradingPremises(_, _, Some(boolean), _, _)) => Form2[IsResidential](IsResidential(boolean))
              case _ => EmptyForm
            }
            Ok(is_residential(form, getResidentialAddress(index, cacheO), index, edit))
          }) getOrElse NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll flatMap { cacheO =>
          Form2[IsResidential](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(is_residential(f, getResidentialAddress(index, cacheO), index, edit)))
            case ValidForm(_, data) =>
              (cacheO map { cache =>
                for {
                  _ <- updateData[TradingPremises](cache, index) { tpO =>
                    tpO map { tp =>
                      val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None) { x =>
                        Some(x.copy(isResidential = Some(data.isResidential)))
                      }
                      tp.copy(yourTradingPremises = ytp)
                    }
                  }
                } yield edit match {
                  case true => Redirect(routes.SummaryController.getIndividual(index))
                  case false => Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
                }
              }) getOrElse Future.successful(InternalServerError("Cannot update Trading Premises"))
          }
        }
  }

  private def getResidentialAddress(index: Int, cacheO: Option[CacheMap])
                                   (implicit hc: HeaderCarrier, ac: AuthContext): Option[Address] = {

    def getAddress(businessMatching: BusinessMatching): Option[Address] =
      businessMatching.reviewDetails.fold[Option[Address]](None)(r => Some(r.businessAddress))

      for {
        cache <- cacheO
        bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        address <- getAddress(bm) if isFirstTradingPremises(index, cacheO)
      } yield address
  }

  private def isFirstTradingPremises(index: Int, cacheO: Option[CacheMap])(implicit hc: HeaderCarrier, ac: AuthContext): Boolean = {
     ???
  }

}