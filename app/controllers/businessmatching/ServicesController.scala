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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.Play
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import play.api.mvc.Result

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait ServicesController extends BaseController {

  def dataCacheConnector: DataCacheConnector
  def businessMatchingService: BusinessMatchingService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        businessMatchingService.getModel.value map { maybeBM =>
            val form = (for {
              bm <- maybeBM
              services <- bm.msbServices
            } yield Form2[MsbServices](services)).getOrElse(EmptyForm)

            Ok(views.html.businessmatching.services(form, edit, maybeBM.fold(false)(_.preAppComplete)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>
        Form2[MsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.services(f, edit)))
          case ValidForm(_, data) =>

            lazy val updateModel = for {
              bm <- businessMatchingService.getModel
              cache <- businessMatchingService.updateModel(data.msbServices.contains(TransmittingMoney) match {
                case true => bm.msbServices(data)
                case false => bm.msbServices(data).clearPSRNumber
              })
              _ <- OptionT.liftF(updateMsb(bm.msbServices, data.msbServices, cache))
            } yield cache

            lazy val redirectResult = OptionT.some[Future, Result](data.msbServices.contains(TransmittingMoney) match {
              case true => Redirect(routes.BusinessAppliedForPSRNumberController.get(edit))
              case false => Redirect(routes.SummaryController.get())
            })

            updateModel flatMap { _ => redirectResult } getOrElse InternalServerError("Could not update services")
        }
  }

  private def updateMsb(existingServices: Option[MsbServices], updatedServices: Set[MsbService], cache: CacheMap)
                       (implicit ac: AuthContext, hc: HeaderCarrier) = {

    cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold[Future[CacheMap]](Future.successful(cache)) { msb =>

      existingServices.fold[Future[CacheMap]](Future.successful(cache)) { msbServices =>

        def updateCurrencyExchange = {
          if (msbServices.msbServices.contains(CurrencyExchange) && !updatedServices.contains(CurrencyExchange)) {
            msb.copy(ceTransactionsInNext12Months = None, whichCurrencies = None)
          } else {
            msb
          }
        }

        def updateTransmittingMoney(msb: MoneyServiceBusiness) = {
          if (msbServices.msbServices.contains(TransmittingMoney) && !updatedServices.contains(TransmittingMoney)) {
            msb.copy(
              businessUseAnIPSP = None,
              fundsTransfer = None,
              transactionsInNext12Months = None,
              sendMoneyToOtherCountry = None,
              sendTheLargestAmountsOfMoney = None,
              mostTransactions = None
            )
          } else {
            msb
          }
        }

        dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key, updateTransmittingMoney(updateCurrencyExchange))

      }

    }
  }

}

object ServicesController extends ServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override lazy val businessMatchingService = Play.current.injector.instanceOf[BusinessMatchingService]
}
