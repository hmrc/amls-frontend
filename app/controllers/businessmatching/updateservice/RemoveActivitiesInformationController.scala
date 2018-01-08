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

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import models.businessmatching.BusinessActivities
import play.api.i18n.Messages
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class RemoveActivitiesInformationController @Inject()(
                                                     val authConnector: AuthConnector,
                                                     val businessMatchingService: BusinessMatchingService
                                                     ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        titlePlaceholder map { placeholder =>
          Ok(views.html.businessmatching.updateservice.remove_activities_information(placeholder))
        }
  }

  private def titlePlaceholder(implicit hc: HeaderCarrier, ac: AuthContext) = businessMatchingService.getModel.value map { bm =>
    val activities = for {
      businessMatching <- bm
      businessActivities <- businessMatching.activities
    } yield businessActivities.businessActivities map BusinessActivities.getValue

    activities match {
      case Some(act) if act.size equals 1 => Messages(s"businessmatching.registerservices.servicename.lbl.${act.head}")
      case _ => "all services"
    }

  }

}
