/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.DataCacheConnector
import javax.inject.Inject
import models.businessmatching.BusinessType.Partnership
import models.businessmatching.{BusinessActivities => _, _}
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ControllerHelper, DeclarationHelper}

import scala.concurrent.{ExecutionContext, Future}



class ProgressService @Inject()(
                                 val cacheConnector: DataCacheConnector,
                                 val statusService: StatusService,
                                 config: ApplicationConfig
                               ){

  def getSubmitRedirect (amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)
                        (implicit ec: ExecutionContext, hc: HeaderCarrier) : Future[Option[Call]] = {

    val result: OptionT[Future, Option[Call]] = for {
      status <- OptionT.liftF(statusService.getStatus(amlsRegistrationNo, accountTypeId, credId))
      responsiblePeople <- OptionT(cacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key))
      hasNominatedOfficer <- OptionT.liftF(ControllerHelper.hasNominatedOfficer(Future.successful(Some(responsiblePeople))))
      businessmatching <- OptionT(cacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      reviewDetails <- OptionT.fromOption[Future](businessmatching.reviewDetails)
      businessType <- OptionT.fromOption[Future](reviewDetails.businessType)
    } yield businessType match {
      case Partnership if DeclarationHelper.numberOfPartners(responsiblePeople) < 2 =>
        Some(controllers.declaration.routes.RegisterPartnersController.get)
      case _ => Some(DeclarationHelper.routeDependingOnNominatedOfficer(hasNominatedOfficer, status))
    }
    result getOrElse none[Call]
  }
}