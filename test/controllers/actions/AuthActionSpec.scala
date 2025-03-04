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

import config.ApplicationConfig
import generators.AmlsReferenceNumberGenerator
import models.ReturnLocation
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utils.{AuthAction, DefaultAuthAction}

import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with GuiceOneAppPerSuite
    with AmlsReferenceNumberGenerator {

  import AuthActionSpec._

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockParser: BodyParsers.Default          = mock[BodyParsers.Default]

  private lazy val unauthorisedUrl = URLEncoder.encode(
    ReturnLocation(controllers.routes.AmlsController.unauthorised_role)(mockApplicationConfig).absoluteUrl,
    "utf-8"
  )
  def unauthorised                 = s"${mockApplicationConfig.logoutUrl}?continue=$unauthorisedUrl"
  def signout                      = s"${mockApplicationConfig.logoutUrl}"

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  lazy val headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter =
    app.injector.instanceOf[HeaderCarrierForPartialsConverter]

  implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(fakeRequest)

  "Auth Action" when {
    "AffinityGroup is Organisation" must {
      "the user has valid credentials" must {
        "redirect the user to amls frontend" in {
          val authAction = new DefaultAuthAction(
            fakeAuthConnector(orgAuthRetrievals),
            mockApplicationConfig,
            mockParser,
            headerCarrierForPartialsConverter
          )
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
    }

    "AffinityGroup is not Organisation" must {
      "the user has valid credentials for sa" must {
        "redirect the user to amls frontend" in {
          val authAction = new DefaultAuthAction(
            fakeAuthConnector(agentSaAuthRetrievals),
            mockApplicationConfig,
            mockParser,
            headerCarrierForPartialsConverter
          )
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }

      "the user has inactive credentials for sa" must {
        "redirect the user to amls frontend" in {
          val authAction = new DefaultAuthAction(
            fakeAuthConnector(agentSaAuthRetrievalsInactive),
            mockApplicationConfig,
            mockParser,
            headerCarrierForPartialsConverter
          )
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(unauthorised)
        }
      }

      "the user has valid credentials for ct"    must {
        "redirect the user to amls frontend" in {
          val authAction = new DefaultAuthAction(
            fakeAuthConnector(agentCtAuthRetrievals),
            mockApplicationConfig,
            mockParser,
            headerCarrierForPartialsConverter
          )
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe OK
        }
      }
      "the user has inactive credentials for ct" must {
        "redirect the user to amls frontend" in {
          val authAction = new DefaultAuthAction(
            fakeAuthConnector(agentCtAuthRetrievalsInactive),
            mockApplicationConfig,
            mockParser,
            headerCarrierForPartialsConverter
          )
          val controller = new Harness(authAction)

          val result = controller.onPageLoad()(fakeRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(unauthorised)
        }
      }
    }

    "erroneous retrievals are obtained" must {
      "redirect the user to unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(erroneousRetrievals),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to signout " in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new MissingBearerToken)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(signout)
      }
    }

    "the user's session has expired" must {
      "redirect the user to signout " in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new BearerTokenExpired)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(signout)
      }
    }

    "the user doesn't have sufficient enrolments" must {
      "redirect the user to the unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new InsufficientEnrolments)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }

    "the user doesn't have sufficient confidence level" must {
      "redirect the user to the unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new InsufficientConfidenceLevel)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }

    "the user used an unaccepted auth provider" must {
      "redirect the user to the unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new UnsupportedAuthProvider)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }

    "the user has an unsupported affinity group" must {
      "redirect the user to the unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new UnsupportedAffinityGroup)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }

    "the user has an unsupported credential role" must {
      "redirect the user to the unauthorised" in {
        val authAction = new DefaultAuthAction(
          fakeAuthConnector(Future.failed(new UnsupportedCredentialRole)),
          mockApplicationConfig,
          mockParser,
          headerCarrierForPartialsConverter
        )
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(unauthorised)
      }
    }
  }

}

object AuthActionSpec extends AmlsReferenceNumberGenerator {
  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      stubbedRetrievalResult.asInstanceOf[Future[A]]
  }

  val enrolments: Enrolments    = Enrolments(
    Set(
      Enrolment("HMCE-VATVAR-ORG", Seq(EnrolmentIdentifier("VATRegNo", "000000000")), "Activated"),
      Enrolment("HMRC-MLR-ORG", Seq(EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated")
    )
  )
  private def orgAuthRetrievals = Future.successful(
    new ~(
      new ~(
        new ~(new ~(enrolments, Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Organisation)),
        Some("groupIdentifier")
      ),
      Some(User)
    )
  )

  val enrolmentsSa: Enrolments      = Enrolments(
    Set(
      Enrolment("HMCE-VATVAR-ORG", Seq(EnrolmentIdentifier("VATRegNo", "000000000")), "Activated"),
      Enrolment("HMRC-MLR-ORG", Seq(EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated"),
      Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "saRef")), "Activated")
    )
  )
  private def agentSaAuthRetrievals = Future.successful(
    new ~(
      new ~(
        new ~(new ~(enrolmentsSa, Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Agent)),
        Some("groupIdentifier")
      ),
      Some(User)
    )
  )

  val enrolmentsSaInactive: Enrolments      = Enrolments(
    Set(
      Enrolment("HMCE-VATVAR-ORG", Seq(EnrolmentIdentifier("VATRegNo", "000000000")), "Active"),
      Enrolment("HMRC-MLR-ORG", Seq(EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Active"),
      Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "saRef")), "Inactive")
    )
  )
  private def agentSaAuthRetrievalsInactive = Future.successful(
    new ~(
      new ~(
        new ~(new ~(enrolmentsSaInactive, Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Agent)),
        Some("groupIdentifier")
      ),
      Some(User)
    )
  )

  val enrolmentsCt: Enrolments      = Enrolments(
    Set(
      Enrolment("HMCE-VATVAR-ORG", Seq(EnrolmentIdentifier("VATRegNo", "000000000")), "Activated"),
      Enrolment("HMRC-MLR-ORG", Seq(EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Activated"),
      Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "ctRef")), "Activated")
    )
  )
  private def agentCtAuthRetrievals = Future.successful(
    new ~(
      new ~(
        new ~(new ~(enrolmentsCt, Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Agent)),
        Some("groupIdentifier")
      ),
      Some(User)
    )
  )

  val enrolmentsCtInactive: Enrolments      = Enrolments(
    Set(
      Enrolment("HMCE-VATVAR-ORG", Seq(EnrolmentIdentifier("VATRegNo", "000000000")), "Active"),
      Enrolment("HMRC-MLR-ORG", Seq(EnrolmentIdentifier("MLRRefNumber", amlsRegistrationNumber)), "Active"),
      Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "ctRef")), "Inactive")
    )
  )
  private def agentCtAuthRetrievalsInactive = Future.successful(
    new ~(
      new ~(
        new ~(new ~(enrolmentsCtInactive, Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Agent)),
        Some("groupIdentifier")
      ),
      Some(User)
    )
  )

//  private def emptyAuthRetrievals = Future.successful(
//    new ~(new ~(new ~(Enrolments(Set()), Some(Credentials("gg", "cred-1234"))), Some(AffinityGroup.Organisation)), Some("groupIdentifier"))
//  )
  private def erroneousRetrievals = Future.successful(
    new ~(
      new ~(new ~(new ~(Enrolments(Set()), None), Some(AffinityGroup.Organisation)), Some("groupIdentifier")),
      Some(User)
    )
  )

  class Harness(authAction: AuthAction) extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Ok)

    override protected def controllerComponents: ControllerComponents = ???
  }

}
