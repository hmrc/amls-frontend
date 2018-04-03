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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.businessmatching.updateservice.ChangeServices
import models.flowmanagement.ChangeServicesPageId
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.routings.ChangeServicesRouter.router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.change_services

class ChangeServicesController @Inject()(
                                          val authConnector: AuthConnector,
                                          implicit val dataCacheConnector: DataCacheConnector,
                                          val businessMatchingService: BusinessMatchingService
                                        ) extends BaseController with RepeatingSection {


  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          activities <- OptionT(getActivities)
          preApplicationComplete <- OptionT.liftF(businessMatchingService.preApplicationComplete)
        } yield Ok(change_services(EmptyForm, activities, showReturnLink = preApplicationComplete))) getOrElse InternalServerError("Unable to show the page")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[ChangeServices](request.body) match {
          case f: InvalidForm =>
            OptionT(getActivities) map { activities =>
              BadRequest(change_services(f, activities))
            } getOrElse InternalServerError("Unable to show the page")
          case ValidForm(_, data) =>
                router.getRoute(ChangeServicesPageId, data)

//            case ChangeServicesAdd => Future.successful(Redirect(controllers.businessmatching.routes.RegisterServicesController.get()))
//
//            case ChangeServicesRemove => {
//              OptionT(getActivities) map { activities =>
//                if (activities.size < 2) {
//                  Redirect(RemoveActivitiesInformationController.get())
//                } else {
//                  Redirect(RemoveActivitiesController.get())
//                }
//              } getOrElse InternalServerError("Unable to show the page")
//            }

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