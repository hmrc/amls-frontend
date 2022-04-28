/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{AccountancyServices, BillPaymentServices, TelephonePaymentService, TrustAndCompanyServices}
import models.flowmanagement.{AddBusinessTypeFlowModel, NeedMoreInformationPageId}
import play.api.mvc.MessagesControllerComponents
import services.flowmanagement.Router
import utils.{AuthAction, ControllerHelper}
import views.html.businessmatching.updateservice.add.new_service_information

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NeedMoreInformationController @Inject()(authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              implicit val dataCacheConnector: DataCacheConnector,
                                              val router: Router[AddBusinessTypeFlowModel],
                                              val cc: MessagesControllerComponents,
                                              new_service_information: new_service_information)(implicit ec: ExecutionContext) extends AmlsBaseController(ds, cc) {

  def get() = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[ServiceChangeRegister](request.credId, ServiceChangeRegister.key))
          activity <- OptionT.fromOption[Future](model.addedActivities)
          cacheMap <- OptionT(dataCacheConnector.fetchAll(request.credId))
        } yield {
          val activityNames = activity map {
            _.getMessage()
          }

          val isTdiOrBpspPresent = activity exists {
            case BillPaymentServices | TelephonePaymentService => true
            case _ => false
          }

          val isAspOrTcspPresent = activity exists {
            case AccountancyServices | TrustAndCompanyServices => true
            case _ => false
          }

          val subSectors = model.addedSubSectors.getOrElse(Set.empty)

          Ok(new_service_information(activityNames, ControllerHelper.supervisionComplete(cacheMap), subSectors, isTdiOrBpspPresent, isAspOrTcspPresent))
        }) getOrElse InternalServerError("Get: Unable to show New Service Information page")
  }

  def post() = authAction.async {
      implicit request =>
        (for {
          route <- OptionT.liftF(router.getRoute(request.credId, NeedMoreInformationPageId, new AddBusinessTypeFlowModel))
          _ <- OptionT.liftF(dataCacheConnector.removeByKey[ServiceChangeRegister](request.credId, ServiceChangeRegister.key))
        } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: Add : NewServiceInformationController")
  }
}
