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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businesscustomer.{ReviewDetails, Address => BCAddress}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{Address, ConfirmAddress, TradingPremises, YourTradingPremises}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import cats.implicits._
import models.DateOfChange
import org.joda.time.LocalDate

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def getAddress(businessMatching: Future[Option[BusinessMatching]]): Future[Option[BCAddress]] = {
    businessMatching map {
      case Some(bm) => bm.reviewDetails.fold[Option[BCAddress]](None)(r => Some(r.businessAddress))
      case _ => None
    }
  }

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getAddress(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) map {
          case Some(address) => Ok(views.html.tradingpremises.confirm_address(EmptyForm, address, index))
          case None => Redirect(routes.WhereAreTradingPremisesController.get(index))
        }
  }

  def updateAddressFromBM(maybeYtp: Option[YourTradingPremises], maybeBm: Option[BusinessMatching]) : Option[YourTradingPremises] = {
    val f: ReviewDetails => (String, Address) = { r =>
      (r.businessName, Address(r.businessAddress.line_1,
        r.businessAddress.line_2,
        r.businessAddress.line_3,
        r.businessAddress.line_4,
        r.businessAddress.postcode.getOrElse("")))
    }

    (maybeBm, maybeYtp) match {
      case (Some(bm), Some(ytp)) => bm.reviewDetails.fold(maybeYtp) { r => f(r) match {
        case (name, address) => Some(ytp.copy(name, address))
      }}
      case (Some(bm), _) => bm.reviewDetails.fold(maybeYtp) { r => f(r) match {
        case (name, address) => Some(YourTradingPremises(name, address))
      }}
      case _ => maybeYtp
    }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ConfirmAddress](request.body) match {
          case f: InvalidForm =>
            getAddress(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)) map {
              case Some(addr) => BadRequest(views.html.tradingpremises.confirm_address(f, addr, index))
              case _ => Redirect(routes.WhereAreTradingPremisesController.get(index))
            }
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
