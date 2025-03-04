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

package controllers.actions

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, BodyParser, Call, Request, Result}
import play.api.test.Helpers
import uk.gov.hmrc.auth.core._
import utils.{AuthAction, AuthorisedRequest}

import scala.concurrent.{ExecutionContext, Future}

object SuccessfulAuthAction extends AuthAction {

  val affinityGroup: AffinityGroup.Organisation.type = AffinityGroup.Organisation
  val enrolments: Enrolments                         = Enrolments(Set(Enrolment("HMRC-MLR-ORG")))

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.anyContent

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext

  val credentialId: String = "internalId"
  val amlsRefNumber        = "amlsRefNumber"

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(
      Right(
        AuthorisedRequest(
          request = request,
          amlsRefNumber = Some(amlsRefNumber),
          credId = credentialId,
          affinityGroup = affinityGroup,
          enrolments = enrolments,
          accountTypeId = ("accType", "id"),
          groupIdentifier = Some("GROUP_ID"),
          credentialRole = Some(User)
        )
      )
    )
}

object SuccessfulAuthActionNoAmlsRefNo extends AuthAction {

  val affinityGroup: AffinityGroup.Organisation.type = AffinityGroup.Organisation
  val enrolments: Enrolments                         = Enrolments(Set(Enrolment("HMRC-MLR-ORG")))

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.anyContent

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(
      Right(
        AuthorisedRequest(
          request,
          None,
          "internalId",
          affinityGroup,
          enrolments,
          ("accType", "id"),
          Some("GROUP_ID"),
          Some(User)
        )
      )
    )
}

object SuccessfulAuthActionNoUserRole extends AuthAction {

  val affinityGroup: AffinityGroup.Organisation.type = AffinityGroup.Organisation
  val enrolments: Enrolments                         = Enrolments(Set(Enrolment("HMRC-MLR-ORG")))

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.anyContent

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(
      Right(
        AuthorisedRequest(
          request,
          Some("amlsRefNumber"),
          "internalId",
          affinityGroup,
          enrolments,
          ("accType", "id"),
          Some("GROUP_ID"),
          Some(Assistant)
        )
      )
    )
}

object FailedAuthAction extends AuthAction {

  val signoutUrl = "/test/signout"

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.anyContent

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    Future.successful(Left(Redirect(Call("GET", signoutUrl))))
}
