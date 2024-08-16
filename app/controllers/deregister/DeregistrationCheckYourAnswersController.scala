/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.deregister

import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.BusinessMatching
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthEnrolmentsService
import services.cache.Cache
import uk.gov.hmrc.mongo.play.json.Codecs
import utils.{AckRefGenerator, AuthAction}
import views.html.deregister.DeregistrationCheckYourAnswersView

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class DeregistrationCheckYourAnswersController @Inject()(authAction: AuthAction,
                                                         ds: CommonPlayDependencies,
                                                         dataCacheConnector: DataCacheConnector,
                                                         amlsConnector: AmlsConnector,
                                                         authEnrolmentsService: AuthEnrolmentsService,
                                                         cc: MessagesControllerComponents,
                                                         view: DeregistrationCheckYourAnswersView) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector
        .fetchAll(request.credId)
        .map(_.getOrElse(throw new RuntimeException("missing 'Cache' in mongo")))
        .map { cache: Cache =>
          val deregistrationReason: DeregistrationReason = cache
            .getEntry[DeregistrationReason](DeregistrationReason.key)
            .getOrElse(throw new RuntimeException("missing 'DeregistrationReason'"))
          Ok(view(deregistrationReason = deregistrationReason))
      }
  }

  def post: Action[AnyContent] = authAction.async {
    implicit request =>

      for {
        deregistrationReason: DeregistrationReason <- dataCacheConnector
          .fetch[DeregistrationReason](request.credId, DeregistrationReason.key)
          .map(_.getOrElse(throw new RuntimeException("missing 'DeregistrationReason'")))
        deregistrationReasonOthers: Option[String] = deregistrationReason match {
          case DeregistrationReason.Other(reason) => reason.some
          case _ => None
        }
        deRegisterSubscriptionRequest: DeRegisterSubscriptionRequest = DeRegisterSubscriptionRequest(
          acknowledgementReference = AckRefGenerator(),
          deregistrationDate = LocalDate.now(),
          deregistrationReason = deregistrationReason,
          deregReasonOther = deregistrationReasonOthers
        )
        amlsRegistrationNumber: String <- authEnrolmentsService
          .amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier)
          .map(_.getOrElse(throw new RuntimeException("Missing 'amlsRegistrationNumber'")))
        _ <- amlsConnector.deregister(
          amlsRegistrationNumber = amlsRegistrationNumber,
          request = deRegisterSubscriptionRequest,
          accountTypeId = request.accountTypeId
        )
      } yield Redirect(controllers.routes.LandingController.get)
  }
}
