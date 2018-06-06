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
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.mvc.Result
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class MsbSubSectorsController @Inject()(val authConnector: AuthConnector,
                                        val dataCacheConnector: DataCacheConnector,
                                        val businessMatchingService: BusinessMatchingService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        businessMatchingService.getModel.value map { maybeBM =>
          val form = (for {
            bm <- maybeBM
            services <- bm.msbServices
          } yield Form2[BusinessMatchingMsbServices](services)).getOrElse(EmptyForm)

          Ok(views.html.businessmatching.services(form, edit, maybeBM.fold(false)(_.preAppComplete)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>
        Form2[BusinessMatchingMsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.services(f, edit)))

          case ValidForm(_, data) =>
            lazy val updateModel = for {
              bm <- businessMatchingService.getModel
              cache <- businessMatchingService.updateModel(if (data.msbServices.contains(TransmittingMoney)) {
                bm.msbServices(Some(data))
              } else {
                bm.msbServices(Some(data)).clearPSRNumber
              })
              _ <- OptionT.liftF(updateMsb(bm.msbServices, data.msbServices, cache))
            } yield cache

            lazy val redirectResult = OptionT.some[Future, Result](if (data.msbServices.contains(TransmittingMoney)) {
              Redirect(routes.BusinessAppliedForPSRNumberController.get(edit))
            } else {
              Redirect(routes.SummaryController.get())
            })

            updateModel flatMap { _ => redirectResult } getOrElse InternalServerError("Could not update services")
        }
  }

  private def updateMsb(existingServices: Option[BusinessMatchingMsbServices], updatedServices: Set[BusinessMatchingMsbService], cache: CacheMap)
                       (implicit ac: AuthContext, hc: HeaderCarrier) = {

    val updateCE = (msb: MoneyServiceBusiness, subSectorDiff: Set[BusinessMatchingMsbService]) => {
      if (subSectorDiff.contains(CurrencyExchange)) {
        msb.copy(ceTransactionsInNext12Months = None, whichCurrencies = None)
      } else {
        msb
      }
    }

    val updateMT = (msb: MoneyServiceBusiness, subSectorDiff: Set[BusinessMatchingMsbService]) => {
      if (subSectorDiff.contains(TransmittingMoney)) {
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

    cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold[Future[CacheMap]](Future.successful(cache)) { msb =>
      existingServices.fold[Future[CacheMap]](Future.successful(cache)) { _ =>
        val sectorDiff = existingServices.fold(Set.empty[BusinessMatchingMsbService])(_.msbServices) diff updatedServices
        val updatedMsb = updateMT(updateCE(msb, sectorDiff), sectorDiff)

        dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key, updatedMsb)
      }
    }
  }
}
