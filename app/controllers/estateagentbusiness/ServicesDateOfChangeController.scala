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

package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.aboutthebusiness.AboutTheBusiness
import models.estateagentbusiness.EstateAgentBusiness
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.date_of_change

import scala.concurrent.Future

trait ServicesDateOfChangeController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get = Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(date_of_change(EmptyForm, "summary.estateagentbusiness", routes.ServicesDateOfChangeController.post())))
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
    getModelWithDateMap() flatMap {
      case (eab, startDate) =>
      Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDate) match {
        case f: InvalidForm =>
      Future.successful(BadRequest(date_of_change(f, "summary.estateagentbusiness", routes.ServicesDateOfChangeController.post())))
        case ValidForm(_, data) => {
          for {
          _ <- dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key,
          eab.services match {
            case Some(service) => {
              eab.copy(services = Some(service.copy(dateOfChange = Some(data))))
            }
            case None => eab
          })
          } yield {
            Redirect(routes.SummaryController.get())
          }
        }
      }
    }
  }

  private def getModelWithDateMap()(implicit authContext: AuthContext, hc: HeaderCarrier): Future[(EstateAgentBusiness, Map[_ <: String, Seq[String]])] = {
    dataCacheConnector.fetchAll map {
      optionalCache =>
        (for {
          cache <- optionalCache
          aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
          eab <- cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)
        } yield (eab, aboutTheBusiness.activityStartDate)) match {
          case Some((eab, Some(activityStartDate))) => (eab, Map("activityStartDate" -> Seq(activityStartDate.startDate.toString("yyyy-MM-dd"))))
          case Some((eab, _)) => (eab, Map())
          case _ =>(EstateAgentBusiness(), Map())
        }
    }
  }
}

object ServicesDateOfChangeController extends ServicesDateOfChangeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

