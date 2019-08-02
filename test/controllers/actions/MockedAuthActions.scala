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

package controllers.actions

import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Request, Result}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, Enrolments}
import utils.{AuthAction, AuthorisedRequest}

import scala.concurrent.Future

object SuccessfulAuthAction extends AuthAction {

  val affinityGroup = AffinityGroup.Organisation
  val enrolments = Enrolments(Set(Enrolment("HMRC-MLR-ORG")))

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(Right(AuthorisedRequest(request, Some("amlsRefNumber"), "internalId", affinityGroup, enrolments, ("accType", "id"), Some("GROUP_ID"))))
}

object SuccessfulAuthActionNoAmlsRefNo extends AuthAction {

  val affinityGroup = AffinityGroup.Organisation
  val enrolments = Enrolments(Set(Enrolment("HMRC-MLR-ORG")))

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(Right(AuthorisedRequest(request, None, "internalId", affinityGroup, enrolments, ("accType", "id"))))
}

object FailedAuthAction extends AuthAction {

  val signoutUrl = "/test/signout"

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(Left(Redirect(Call("GET", signoutUrl))))
}