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

package controllers.businessmatching.updateservice

import javax.inject.Inject

import cats.implicits._
import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd, ChangeServicesRemove}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.change_services

import scala.concurrent.Future

class ChangeServicesController @Inject()(
                                          val authConnector: AuthConnector,
                                          implicit val dataCacheConnector: DataCacheConnector
                                        ) extends BaseController with RepeatingSection {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        OptionT(getActivities) map { activities =>
          Ok(change_services(EmptyForm, activities))
        } getOrElse InternalServerError("Unable to show the page")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[ChangeServices](request.body) match {
          case f: InvalidForm =>
            OptionT(getActivities) map { activities =>
              BadRequest(change_services(f, activities))
            } getOrElse InternalServerError("Unable to show the page")
          case ValidForm(_, data) => data match {
            case ChangeServicesAdd => Future.successful(Redirect(controllers.businessmatching.routes.RegisterServicesController.get()))
            case ChangeServicesRemove => Future.successful(Redirect(routes.RemoveActivitiesController.get()))
          }
        }
      }
  }

  private def getActivities(implicit dataCacheConnector: DataCacheConnector, hc: HeaderCarrier, ac: AuthContext) = {
    dataCacheConnector.fetchAll map {
      optionalCache =>
        for {
          cache <- optionalCache
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        } yield businessMatching.activities.fold(Set.empty[String])(_.businessActivities.map(_.getMessage))
    }
  }

}