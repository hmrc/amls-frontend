/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BillPaymentServices, TelephonePaymentService}
import models.flowmanagement.{AddBusinessTypeFlowModel, NeedMoreInformationPageId}
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.add.new_service_information

import scala.concurrent.Future

@Singleton
class NeedMoreInformationController @Inject()( val authConnector: AuthConnector,
                                               implicit val dataCacheConnector: DataCacheConnector,
                                               val router: Router[AddBusinessTypeFlowModel]
                                               ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[ServiceChangeRegister](ServiceChangeRegister.key))
          activity <- OptionT.fromOption[Future](model.addedActivities)
        } yield {
          val activityNames = activity filterNot {
            case BillPaymentServices | TelephonePaymentService => true
            case _ => false
          } map {
            _.getMessage()
          }

          Ok(new_service_information(activityNames))
        }) getOrElse InternalServerError("Get: Unable to show New Service Information page")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          //model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](AddBusinessTypeFlowModel.key))
          route <- OptionT.liftF(router.getRoute(NeedMoreInformationPageId, new AddBusinessTypeFlowModel))
        } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: Add : NewServiceInformationController")
  }
}
