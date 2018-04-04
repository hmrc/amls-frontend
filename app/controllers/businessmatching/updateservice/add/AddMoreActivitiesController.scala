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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.flowmanagement.{AddMoreAcivitiesPageId, AddServiceFlowModel}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.routings.VariationAddServiceRouter.router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BooleanFormReadWrite

@Singleton
class AddMoreActivitiesController @Inject()(
                                           val authConnector: AuthConnector,
                                           implicit val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {


  val fieldName = "addmoreactivities"

  implicit val boolWrite = BooleanFormReadWrite.formWrites(fieldName)
  implicit val boolRead = BooleanFormReadWrite.formRule(fieldName, "error.businessmatching.updateservice.addmoreactivities")

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          activities <- OptionT(getActivities)
          preApplicationComplete <- OptionT.liftF(businessMatchingService.preApplicationComplete)
        } yield Ok(views.html.businessmatching.updateservice.add_more_activities(EmptyForm, activities, showReturnLink = false))) getOrElse InternalServerError("Unable to show the page")
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            OptionT(getActivities) map { activities =>
              BadRequest(views.html.businessmatching.updateservice.add_more_activities(f, activities))
            } getOrElse InternalServerError("Unable to show the page")
          case ValidForm(_, data) =>
            val flowModel = AddServiceFlowModel(addMoreActivities = Some(data))
            router.getRoute(AddMoreAcivitiesPageId, flowModel)
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