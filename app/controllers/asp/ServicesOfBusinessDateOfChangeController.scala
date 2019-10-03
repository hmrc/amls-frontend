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

package controllers.asp

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.DateOfChange
import models.businessdetails.BusinessDetails
import models.asp.Asp
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.date_of_change

import scala.concurrent.Future

class ServicesOfBusinessDateOfChangeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                         val authAction: AuthAction,
                                                         val ds: CommonPlayDependencies,
                                                         val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
      implicit request =>
        Future.successful(Ok(date_of_change(EmptyForm, "summary.asp", routes.ServicesOfBusinessDateOfChangeController.post())))
  }


  def post = authAction.async {
      implicit request =>
        getModelWithDateMap(request.credId) flatMap {
          case (asp, startDate) =>
            Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDate) match {
              case f: InvalidForm =>
                Future.successful(BadRequest(date_of_change(f, "summary.asp", routes.ServicesOfBusinessDateOfChangeController.post())))
              case ValidForm(_, data) => {
                for {
                  _ <- dataCacheConnector.save[Asp](request.credId, Asp.key,
                    asp.services match {
                      case Some(service) => {
                        val a = asp.copy(services = Some(service.copy(dateOfChange = Some(data))))
                        a
                      }
                      case None => asp
                    })
                } yield {
                  Redirect(routes.SummaryController.get())
                }
              }
            }
        }
  }

  private def getModelWithDateMap(cacheId: String)(implicit hc: HeaderCarrier): Future[(Asp, Map[_ <: String, Seq[String]])] = {
    dataCacheConnector.fetchAll(cacheId) map {
      optionalCache =>
        (for {
          cache <- optionalCache
          businessDetails <- cache.getEntry[BusinessDetails](BusinessDetails.key)
          asp <- cache.getEntry[Asp](Asp.key)
        } yield (asp, businessDetails.activityStartDate)) match {
          case Some((asp, Some(activityStartDate))) => (asp, Map("activityStartDate" -> Seq(activityStartDate.startDate.toString("yyyy-MM-dd"))))
          case Some((asp, _)) => (asp, Map())
          case _ => (Asp(), Map())
        }
    }
  }
}

