/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businesscustomer.{ReviewDetails, Address => BCAddress}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{Address, ConfirmAddress, TradingPremises, YourTradingPremises}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def getAddress(businessMatching: BusinessMatching): Option[BCAddress] =
    businessMatching.reviewDetails.fold[Option[BCAddress]](None)(r => Some(r.businessAddress))

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      val redirect = Redirect(routes.WhereAreTradingPremisesController.get(index))
      def ok(address: BCAddress) = Ok(views.html.tradingpremises.confirm_address(EmptyForm, address, index))

      {
        for {
          cache <- OptionT(dataCacheConnector.fetchAll)
          bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
          address <- OptionT.fromOption[Future](getAddress(bm))
        } yield {
          getData[TradingPremises](cache, index) map {
            case x if x.yourTradingPremises.isDefined => redirect
            case _ => ok(address)
          } getOrElse ok(address)
        }
      } getOrElse redirect
  }

  def updateAddressFromBM(maybeYtp: Option[YourTradingPremises], maybeBm: Option[BusinessMatching]): Option[YourTradingPremises] = {
    val f: ReviewDetails => (String, Address) = { r =>
      (r.businessName, Address(r.businessAddress.line_1,
        r.businessAddress.line_2,
        r.businessAddress.line_3,
        r.businessAddress.line_4,
        r.businessAddress.postcode.getOrElse("")))
    }

    (maybeBm, maybeYtp) match {
      case (Some(bm), Some(ytp)) => bm.reviewDetails.fold(maybeYtp) { r =>
        f(r) match {
          case (name, address) => Some(ytp.copy(name, address))
        }
      }
      case (Some(bm), _) => bm.reviewDetails.fold(maybeYtp) { r =>
        f(r) match {
          case (name, address) => Some(YourTradingPremises(name, address))
        }
      }
      case _ => maybeYtp
    }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ConfirmAddress](request.body) match {
          case f: InvalidForm => {
            for {
              bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
              address <- OptionT.fromOption[Future](getAddress(bm))
            } yield BadRequest(views.html.tradingpremises.confirm_address(f, address, index))
          } getOrElse Redirect(routes.WhereAreTradingPremisesController.get(index))
          case ValidForm(_, data) =>
            data.confirmAddress match {
              case true => {
                for {
                  _ <- fetchAllAndUpdateStrict[TradingPremises](index) { (cache, tp) =>
                    tp.copy(yourTradingPremises = updateAddressFromBM(tp.yourTradingPremises, cache.getEntry[BusinessMatching](BusinessMatching.key)))
                  }
                } yield Redirect(routes.ActivityStartDateController.get(index))
              }
              case false => {
                Future.successful(Redirect(routes.WhereAreTradingPremisesController.get(index)))
              }
            }
        }
  }

}
