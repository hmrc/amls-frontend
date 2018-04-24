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

package controllers.businessmatching.updateservice.add

import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddServiceFlowModel, SubServicesPageId}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


@Singleton
class SubServicesController @Inject()(
                                       val authConnector: AuthConnector,
                                       implicit val dataCacheConnector: DataCacheConnector,
                                       val statusService: StatusService,
                                       val businessMatchingService: BusinessMatchingService,
                                       val helper: UpdateServiceHelper,
                                       val router: Router[AddServiceFlowModel]
                                     ) extends BaseController {


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        businessMatchingService.getModel.value map { maybeBM =>
            val form = (for {
              bm <- maybeBM
              services <- bm.msbServices
            } yield Form2[MsbServices](services)).getOrElse(EmptyForm)

            Ok(views.html.businessmatching.updateservice.add.msb_subservices(form, edit, maybeBM.fold(false)(_.preAppComplete)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>
        Form2[MsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.updateservice.add.msb_subservices(f, edit)))

          case ValidForm(_, data) => {
            dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) {
              case Some(model) => {
                model.msbServices(data)
              }
            } flatMap {
              case Some(model) => {
                router.getRoute(SubServicesPageId, model, edit)
              }
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
          }
        }
  }

//  private def updateMsb(existingServices: Option[MsbServices], updatedServices: Set[MsbService], cache: CacheMap)
//                       (implicit ac: AuthContext, hc: HeaderCarrier) = {
//
//    cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key).fold[Future[CacheMap]](Future.successful(cache)) { msb =>
//
//      existingServices.fold[Future[CacheMap]](Future.successful(cache)) { msbServices =>
//
//        def updateCurrencyExchange = {
//          if (msbServices.msbServices.contains(CurrencyExchange) && !updatedServices.contains(CurrencyExchange)) {
//            msb.copy(ceTransactionsInNext12Months = None, whichCurrencies = None)
//          } else {
//            msb
//          }
//        }
//
//        def updateTransmittingMoney(msb: MoneyServiceBusiness) = {
//          if (msbServices.msbServices.contains(TransmittingMoney) && !updatedServices.contains(TransmittingMoney)) {
//            msb.copy(
//              businessUseAnIPSP = None,
//              fundsTransfer = None,
//              transactionsInNext12Months = None,
//              sendMoneyToOtherCountry = None,
//              sendTheLargestAmountsOfMoney = None,
//              mostTransactions = None
//            )
//          } else {
//            msb
//          }
//        }
//
//        dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key, updateTransmittingMoney(updateCurrencyExchange))
//
//      }
//
//    }
//  }

}
