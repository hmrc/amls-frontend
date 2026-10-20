/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.{ExecutionContext, Future}

final case class AuthorisedRequest[A](
  request: Request[A],
  amlsRefNumber: Option[String],
  credId: String,
  affinityGroup: AffinityGroup,
  enrolments: Enrolments,
  accountTypeId: (String, String),
  groupIdentifier: Option[String],
  credentialRole: Option[CredentialRole]
) extends WrappedRequest[A](request)

final case class enrolmentNotFound(msg: String = "enrolmentNotFound") extends AuthorisationException(msg)

class DefaultAuthAction @Inject() (
  val authConnector: AuthConnector,
  applicationConfig: ApplicationConfig,
  val parser: BodyParsers.Default,
  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
)(implicit val executionContext: ExecutionContext)
    extends AuthAction
    with AuthorisedFunctions
    with Logging {

  private val amlsKey       = "HMRC-MLR-ORG"
  private val amlsNumberKey = "MLRRefNumber"
  private val saKey         = "IR-SA"
  private val ctKey         = "IR-CT"

  private def logoutAndContinueTo(call: Call): String = {
    val continueUrl = URLEncoder.encode(ReturnLocation(call)(applicationConfig).absoluteUrl, "utf-8")
    s"${applicationConfig.logoutUrl}?continue=$continueUrl"
  }

  def unauthorised             = logoutAndContinueTo(controllers.routes.AmlsController.unauthorised_role)
  def unauthorisedAgent        = logoutAndContinueTo(controllers.routes.AmlsController.unauthorised_agent)
  def unauthorisedAuthProvider = logoutAndContinueTo(controllers.routes.AmlsController.unauthorised_auth_provider)
  def signout                  = s"${applicationConfig.logoutUrl}"

  override final protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {

    implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    val prefix      = "[DefaultAuthAction][refine]"

    authorised(authPredicate)
      .retrieve(
        Retrievals.allEnrolments and
          Retrievals.credentials and
          Retrievals.affinityGroup and
          Retrievals.groupIdentifier and
          Retrievals.credentialRole
      ) {
        case enrolments ~ Some(_) ~ Some(affinityGroup) ~ _ ~ _ if !isPermitted(affinityGroup, enrolments) =>
          logger.warn(
            s"$prefix - Affinity group not permitted, redirecting to unauthorised - affinityGroup: $affinityGroup"
          )
          val target = if (affinityGroup == AffinityGroup.Agent) unauthorisedAgent else unauthorised
          Future.successful(Left(Redirect(Call("GET", target))))
        case enrolments ~ Some(credentials) ~ Some(affinityGroup) ~ groupIdentifier ~ credentialRole       =>
          // $COVERAGE-OFF$
          logger.debug(
            s"$prefix - Authorised - affinityGroup: $affinityGroup, enrolments: ${enrolments.enrolments.size}"
          )
          // $COVERAGE-ON$

          Future.successful(
            Right(
              AuthorisedRequest(
                request = request,
                amlsRefNumber = amlsRefNo(enrolments),
                credId = credentials.providerId,
                affinityGroup = affinityGroup,
                enrolments = enrolments,
                accountTypeId = accountTypeAndId(affinityGroup, enrolments, credentials.providerId),
                groupIdentifier = groupIdentifier,
                credentialRole = credentialRole
              )
            )
          )
        case _                                                                                             =>
          logger.warn(s"$prefix - Auth retrieval missing credentials or affinity group, redirecting to unauthorised")
          Future.successful(Left(Redirect(Call("GET", unauthorised))))
      }
      .recover[Either[Result, AuthorisedRequest[A]]] {
        case nas: NoActiveSession             =>
          logger.debug(s"$prefix - No active session, redirecting to sign out - type: ${nas.getClass.getSimpleName}")
          Left(Redirect(Call("GET", signout)))
        case ie: InsufficientEnrolments       =>
          logger.warn(s"$prefix - Insufficient enrolments, redirecting to unauthorised - reason: ${ie.reason}")
          Left(Redirect(Call("GET", unauthorised)))
        case icl: InsufficientConfidenceLevel =>
          logger.warn(s"$prefix - Insufficient confidence level, redirecting to unauthorised - reason: ${icl.reason}")
          Left(Redirect(Call("GET", unauthorised)))
        case uap: UnsupportedAuthProvider     =>
          logger.warn(s"$prefix - Unsupported auth provider, redirecting to unauthorised - reason: ${uap.reason}")
          Left(Redirect(Call("GET", unauthorisedAuthProvider)))
        case uag: UnsupportedAffinityGroup    =>
          logger.warn(
            s"$prefix - Unsupported affinity group, redirecting to unauthorised agent - reason: ${uag.reason}"
          )
          Left(Redirect(Call("GET", unauthorisedAgent)))
        case ucr: UnsupportedCredentialRole   =>
          logger.warn(s"$prefix - Unsupported credential role, redirecting to unauthorised - reason: ${ucr.reason}")
          Left(Redirect(Call("GET", unauthorised)))
        case enf: enrolmentNotFound           =>
          logger.warn(
            s"$prefix - No SA or CT enrolment found, redirecting to unauthorised - type: ${enf.getClass.getSimpleName}"
          )
          Left(Redirect(Call("GET", unauthorised)))
        case e: AuthorisationException        =>
          logger.warn(
            s"$prefix - Authorisation failed, redirecting to unauthorised - reason: ${e.reason}, type: ${e.getClass.getSimpleName}"
          )
          Left(Redirect(Call("GET", unauthorised)))
      }
  }

  private def authPredicate =
    AuthProviders(AuthProvider.GovernmentGateway) and User

  private def isPermitted(affinityGroup: AffinityGroup, enrolments: Enrolments): Boolean =
    affinityGroup == AffinityGroup.Organisation ||
      getActiveEnrolment(enrolments, saKey).isDefined ||
      getActiveEnrolment(enrolments, ctKey).isDefined

  private def amlsRefNo(enrolments: Enrolments): Option[String] = {
    val amlsRefNumber = for {
      enrolment      <- enrolments.getEnrolment(amlsKey)
      amlsIdentifier <- enrolment.getIdentifier(amlsNumberKey)
    } yield amlsIdentifier.value
    amlsRefNumber
  }

  private def getActiveEnrolment(enrolments: Enrolments, key: String) =
    /*
     *  Look for activated enrolments only for SA and CT.
     *  Enrolments can be 'Activated' or 'NotYetActivated'.
     */
    enrolments.getEnrolment(key).filter(e => e.isActivated)

  private def accountTypeAndId(affinityGroup: AffinityGroup, enrolments: Enrolments, credId: String): (String, String) =
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
      case _                          =>
        val sa: Option[(String, String)] = for {
          enrolment <- getActiveEnrolment(enrolments, saKey)
          utr       <- enrolment.getIdentifier("UTR")
        } yield "sa" -> utr.value

        val ct: Option[(String, String)] = for {
          enrolment <- getActiveEnrolment(enrolments, ctKey)
          utr       <- enrolment.getIdentifier("UTR")
        } yield "ct" -> utr.value

        (sa orElse ct).getOrElse(throw new enrolmentNotFound)
    }
}

@com.google.inject.ImplementedBy(classOf[DefaultAuthAction])
trait AuthAction extends ActionRefiner[Request, AuthorisedRequest] with ActionBuilder[AuthorisedRequest, AnyContent]
