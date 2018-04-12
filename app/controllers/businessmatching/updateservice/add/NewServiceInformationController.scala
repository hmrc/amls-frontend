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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BillPaymentServices, TelephonePaymentService, BusinessActivities => BusinessMatchingActivities}
import models.flowmanagement.{AddServiceFlowModel, NewServiceInformationPageId}
import play.api.i18n.MessagesApi
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.new_service_information

import scala.concurrent.Future


class NewServiceInformationController @Inject()
(
  val authConnector: AuthConnector,
  implicit val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val messages: MessagesApi,
  val router: Router[AddServiceFlowModel]
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request => (for {
      model <- OptionT(dataCacheConnector.fetch[ServiceChangeRegister](ServiceChangeRegister.key))
      activity <- OptionT.fromOption[Future](model.addedActivities)
    } yield {
      val activityNames = activity filterNot {
        case BillPaymentServices | TelephonePaymentService => true
        case _ => false
      } map { _.getMessage }

      Ok(new_service_information(activityNames))
    }) getOrElse InternalServerError("Could not get the flow model")
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => (for {
      model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key))
      route <- OptionT.liftF(router.getRoute(NewServiceInformationPageId, model))
    } yield route) getOrElse InternalServerError("Could not get the flow model")
  }
}
