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

package utils

import cats.data.OptionT
import controllers.{declaration, routes}
import models.responsiblepeople.{ResponsiblePeople, Partner}
import models.status._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object DeclarationHelper {

  def numberOfPartners(responsiblePeople: Seq[ResponsiblePeople]): Int = {
    responsiblePeople.collect({
      case p if p.positions.isDefined & p.status.isEmpty => p.positions.get.positions
    }).count(_.contains(Partner))
  }

  def routeDependingOnNominatedOfficer(hasNominatedOfficer: Boolean, status: SubmissionStatus) = {
    hasNominatedOfficer match {
      case true => routeWithNominatedOfficer(status)
      case false => routeWithoutNominatedOfficer(status)
    }
  }

  private def routeWithNominatedOfficer(status: SubmissionStatus) = {
    status match {
      case SubmissionReady | NotCompleted => routes.FeeGuidanceController.get()
      case SubmissionReadyForReview => declaration.routes.WhoIsRegisteringController.get()
      case ReadyForRenewal(_) => declaration.routes.WhoIsRegisteringController.getWithRenewal()
      case _ => declaration.routes.WhoIsRegisteringController.getWithAmendment()
    }
  }

  private def routeWithoutNominatedOfficer(status: SubmissionStatus) = {
    status match {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview => declaration.routes.WhoIsTheBusinessNominatedOfficerController.get()
      case _ => declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment()
    }
  }

}
