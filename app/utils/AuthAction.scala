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

package utils

import java.net.URLEncoder

import config.ApplicationConfig
import javax.inject.Inject
import models.ReturnLocation
import play.api.Logger
import play.api.mvc._
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

final case class AuthorisedRequest[A](request: Request[A],
                                      amlsRefNumber: Option[String],
                                      cacheId: String,
                                      affinityGroup: AffinityGroup,
                                      enrolments: Enrolments) extends WrappedRequest[A](request)

class DefaultAuthAction @Inject() (
                             val authConnector: AuthConnector
                           )(implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  private val amlsKey = "HMRC-MLR-ORG"
  private val amlsNumberKey = "MLRRefNumber"

  private lazy val unauthorisedUrl = URLEncoder.encode(
    ReturnLocation(controllers.routes.AmlsController.unauthorised_role()).absoluteUrl, "utf-8"
  )

  def signoutUrl = s"${ApplicationConfig.logoutUrl}?continue=$unauthorisedUrl"

  override final protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(
      request.headers,
      Some(request.session)
    )

    Logger.debug("DefaultAuthAction calling authorised(Admin) - Admin: " + Admin)

    authorised(Admin).retrieve(
      Retrievals.allEnrolments and
      Retrievals.credentials and
      Retrievals.affinityGroup
    ) {
      case enrolments ~ Some(credentials) ~ Some(affinityGroup) =>

        val amlsRefNumber = for {
          enrolment      <- enrolments.getEnrolment(amlsKey)
          amlsIdentifier <- enrolment.getIdentifier(amlsNumberKey)
        } yield amlsIdentifier.value

        Logger.debug(s"DefaultAuthAction calling authorised(User) - success")
        Future.successful(Right(AuthorisedRequest(request, amlsRefNumber, credentials.providerId, affinityGroup, enrolments)))
      case _ =>
        Future.successful(Left(Redirect(Call("GET", signoutUrl))))
    }.recover[Either[Result, AuthorisedRequest[A]]] {
      case e : AuthorisationException =>
        Logger.debug("DefaultAuthAction calling authorised(User) - fail with exception: " + e)
        Left(Redirect(Call("GET", signoutUrl)))
    }
  }
}


@com.google.inject.ImplementedBy(classOf[DefaultAuthAction])
trait AuthAction extends ActionRefiner[Request, AuthorisedRequest] with ActionBuilder[AuthorisedRequest]
