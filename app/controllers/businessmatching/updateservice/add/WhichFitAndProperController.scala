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
import com.sun.xml.internal.bind.util.Which
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.businessmatching.{BusinessActivity, MoneyServiceBusiness, TrustAndCompanyServices}
import models.flowmanagement.{AddServiceFlowModel, WhichFitAndProperPageId, WhichTradingPremisesPageId}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}
import views.html.businessmatching.updateservice.add._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhichFitAndProperController @Inject()(
                                             val authConnector: AuthConnector,
                                             implicit val dataCacheConnector: DataCacheConnector,
                                             val statusService: StatusService,
                                             val businessMatchingService: BusinessMatchingService,
                                             val helper: UpdateServiceHelper,
                                             val router: Router[AddServiceFlowModel]
                                             )() extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>

        getFormData map { case (flowModel, responsiblePeopleSeq) =>
          val form = flowModel.responsiblePeople.fold[Form2[ResponsiblePeopleFitAndProper]](EmptyForm)(Form2[ResponsiblePeopleFitAndProper])

          Ok(which_fit_and_proper(
            form,
            responsiblePeopleSeq
          ))
        } getOrElse InternalServerError("Cannot retrieve form data")


  }

  def post() = Authorised.async {
      implicit authContext =>
        implicit request =>
          Form2[ResponsiblePeopleFitAndProper](request.body) match {
            case f: InvalidForm => getFormData map { case (_, responsiblePeopleSeq) =>
              BadRequest(which_fit_and_proper(f, responsiblePeopleSeq))
            } getOrElse InternalServerError("Cannot retrieve form data")

            case ValidForm(_, data) => dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) { case Some(model) =>
              model.responsiblePeople(Some(data))
            } flatMap {
              case Some(model) => router.getRoute(WhichFitAndProperPageId, model)
            }
          }
  }


  private def getFormData(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    flowModel <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key))
    responsiblePeopleSeq <- OptionT.liftF(responsiblePeopleFutureSeq)
  } yield (flowModel, responsiblePeopleSeq)

  private def responsiblePeopleFutureSeq(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[(ResponsiblePeople, Int)]] =
    getData[ResponsiblePeople].map { _.zipWithIndex.filterNot { case (tp, _) =>
      tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
    }
  }
}