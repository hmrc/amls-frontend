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
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

final case class AuthorisedRequest[A](request: Request[A],
                                      amlsRefNumber: Option[String],
                                      credId: String,
                                      affinityGroup: AffinityGroup,
                                      enrolments: Enrolments,
                                      accountTypeId: (String, String),
                                      groupIdentifier: Option[String],
                                      credentialRole: Option[CredentialRole]) extends WrappedRequest[A](request)

final case class enrolmentNotFound(msg: String = "enrolmentNotFound") extends AuthorisationException(msg)

class DefaultAuthAction @Inject() (val authConnector: AuthConnector,
                                   applicationConfig: ApplicationConfig,
                                   val parser: BodyParsers.Default)
                                  (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  private val amlsKey              = "HMRC-MLR-ORG"
  private val amlsNumberKey        = "MLRRefNumber"

  private lazy val unauthorisedUrl = URLEncoder.encode(
    ReturnLocation(controllers.routes.AmlsController.unauthorised_role()).absoluteUrl, "utf-8"
  )

  def unauthorised = s"${applicationConfig.logoutUrl}?continue=$unauthorisedUrl"
  def signout      = s"${applicationConfig.logoutUrl}"

  override final protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(User).retrieve(
      Retrievals.allEnrolments and
        Retrievals.credentials and
        Retrievals.affinityGroup and
        Retrievals.groupIdentifier and
        Retrievals.credentialRole
    ) {
      case enrolments ~ Some(credentials) ~ Some(affinityGroup) ~ groupIdentifier ~ credentialRole =>
        Logger.debug("DefaultAuthAction:Refine - Enrolments:" + enrolments)

        Future.successful(
          Right(
            AuthorisedRequest(
              request,
              amlsRefNo(enrolments),
              credentials.providerId,
              affinityGroup,
              enrolments,
              accountTypeAndId(affinityGroup, enrolments, credentials.providerId),
              groupIdentifier,
              credentialRole
            )
          )
        )
      case _ =>
        Logger.debug("DefaultAuthAction:Refine - Non match (enrolments ~ Some(credentials) ~ Some(affinityGroup))")
        Future.successful(Left(Redirect(Call("GET", unauthorised))))
    }.recover[Either[Result, AuthorisedRequest[A]]] {
      case nas: NoActiveSession =>
        Logger.debug("DefaultAuthAction:Refine - NoActiveSession:" + nas)
        Left(Redirect(Call("GET", signout)))
      case ie: InsufficientEnrolments =>
        Logger.debug("DefaultAuthAction:Refine - InsufficientEnrolments:" + ie)
        Left(Redirect(Call("GET", unauthorised)))
      case icl: InsufficientConfidenceLevel =>
        Logger.debug("DefaultAuthAction:Refine - InsufficientConfidenceLevel:" + icl)
        Left(Redirect(Call("GET", unauthorised)))
      case uap: UnsupportedAuthProvider =>
        Logger.debug("DefaultAuthAction:Refine - UnsupportedAuthProvider:" + uap)
        Left(Redirect(Call("GET", unauthorised)))
      case uag: UnsupportedAffinityGroup =>
        Logger.debug("DefaultAuthAction:Refine - UnsupportedAffinityGroup:" + uag)
        Left(Redirect(Call("GET", unauthorised)))
      case ucr: UnsupportedCredentialRole =>
        Logger.debug("DefaultAuthAction:Refine - UnsupportedCredentialRole:" + ucr)
        Left(Redirect(Call("GET", unauthorised)))
      case enf: enrolmentNotFound =>
        Logger.debug("DefaultAuthAction:Refine - enrolmentNotFound:" + enf)
        Left(Redirect(Call("GET", unauthorised)))
      case e : AuthorisationException =>
        Logger.debug("DefaultAuthAction:Refine - AuthorisationException:" + e)
        Left(Redirect(Call("GET", unauthorised)))
    }
  }

  private def amlsRefNo(enrolments: Enrolments): Option[String] = {
    val amlsRefNumber = for {
      enrolment      <- enrolments.getEnrolment(amlsKey)
      amlsIdentifier <- enrolment.getIdentifier(amlsNumberKey)
    } yield amlsIdentifier.value
    amlsRefNumber
  }

  private def accountTypeAndId(affinityGroup: AffinityGroup,
                               enrolments: Enrolments,
                               credId: String) = {
    /*
    * Set the `accountType` to `"org"` if `affinityGroup = "Organisation"` (which you get through retrievals)
    * Set the `accountId` as a hash of the CredId. Its possible to get the `credId` through retrievals
    */

    /*
     * For an affinity group other than Org;
     * Retrieve the enrolments through retrievals.
     * If one of them is `"IR-SA"`, you can set `accountType` to `"sa"` and `accountId` to the `value` for `key` `"UTR"`
     * If one of them is `"IR-CT"`, you can set `accountType` to `"ct"` and `accountId` to the `value` for `key` `"UTR"`

     */

    affinityGroup match {
      case AffinityGroup.Organisation => ("org", UrlHelper.hash(credId))
      case _ =>

        val sa = for {
          enrolment <- enrolments.getEnrolment("IR-SA")
          utr       <- enrolment.getIdentifier("UTR")
        } yield "sa" -> utr.value

        val ct = for {
          enrolment <- enrolments.getEnrolment("IR-CT")
          utr       <- enrolment.getIdentifier("UTR")
        } yield "ct" -> utr.value

        (sa orElse ct).getOrElse(throw new enrolmentNotFound)
    }
  }
}

@com.google.inject.ImplementedBy(classOf[DefaultAuthAction])
trait AuthAction extends ActionRefiner[Request, AuthorisedRequest] with ActionBuilder[AuthorisedRequest, AnyContent]