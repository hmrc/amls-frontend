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

package controllers.tcsp

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.tcsp.OnlyOffTheShelfCompsSoldFormProvider
import models.tcsp.ProvidedServices._
import models.tcsp.TcspTypes._
import models.tcsp._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.tcsp.OnlyOffTheShelfCompsSoldView

import scala.concurrent.Future

class OnlyOffTheShelfCompsSoldControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache           = mock[DataCacheConnector]
    lazy val view       = inject[OnlyOffTheShelfCompsSoldView]
    lazy val controller = new OnlyOffTheShelfCompsSoldController(
      SuccessfulAuthAction,
      commonDependencies,
      cache,
      cc = mockMcc,
      formProvider = inject[OnlyOffTheShelfCompsSoldFormProvider],
      view = view
    )

    val tcsp = Tcsp(
      Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))),
      None,
      None,
      Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
      Some(true),
      None,
      hasAccepted = true
    )

    when(cache.fetch[Tcsp](any(), any())(any()))
      .thenReturn(Future.successful(Some(tcsp)))

    when(cache.save[Tcsp](any(), any(), any())(any()))
      .thenReturn(Future.successful(Cache.empty))
  }

  "The OnlyOffTheShelfCompsSoldController" when {
    "get is called" must {

      "respond with BAD_REQUEST" when {
        "given invalid data" in new TestFixture {

          val newRequest = FakeRequest(POST, routes.OnlyOffTheShelfCompsSoldController.post().url)
            .withFormUrlEncodedBody(
              "onlyOffTheShelfCompsSold" -> "invalid"
            )

          val result = controller.post(true)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with OK and include onlyOffTheShelfCompsSold" in new TestFixture {

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include("onlyOffTheShelfCompsSold")
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe BAD_REQUEST
      }

      "respond with SUCCESS" when {

        "where CompanyFormationAgent" which {
          "redirects to ComplexCorpStructureCreationController" in new TestFixture {

            val companyFormationAgentTcsp = Tcsp(
              Some(TcspTypes(Set(NomineeShareholdersProvider, CompanyFormationAgent))),
              None,
              None,
              Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
              Some(true),
              None,
              hasAccepted = true
            )

            val expected = Tcsp(
              tcspTypes = Some(TcspTypes(Set(NomineeShareholdersProvider, CompanyFormationAgent))),
              onlyOffTheShelfCompsSold = Some(OnlyOffTheShelfCompsSoldYes),
              complexCorpStructureCreation = None,
              providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
              doesServicesOfAnotherTCSP = Some(true),
              servicesOfAnotherTCSP = None,
              hasAccepted = false,
              hasChanged = true
            )

            when(cache.fetch[Tcsp](any(), any())(any()))
              .thenReturn(Future.successful(Some(companyFormationAgentTcsp)))

            val result = controller.post()(
              FakeRequest(POST, routes.OnlyOffTheShelfCompsSoldController.post().url)
                .withFormUrlEncodedBody("onlyOffTheShelfCompsSold" -> "true")
            )

            status(result) mustBe SEE_OTHER
            verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any())
            redirectLocation(result) mustBe Some(
              controllers.tcsp.routes.ComplexCorpStructureCreationController.get().url
            )
          }
        }

        "where not CompanyFormationAgent" which {
          "edit is 'true'" which {
            "redirects to SummaryController" in new TestFixture {

              val expected = Tcsp(
                tcspTypes = Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))),
                onlyOffTheShelfCompsSold = Some(OnlyOffTheShelfCompsSoldYes),
                complexCorpStructureCreation = None,
                providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
                doesServicesOfAnotherTCSP = Some(true),
                servicesOfAnotherTCSP = None,
                hasAccepted = false,
                hasChanged = true
              )

              val result = controller.post(true)(
                FakeRequest(POST, routes.OnlyOffTheShelfCompsSoldController.post().url)
                  .withFormUrlEncodedBody("onlyOffTheShelfCompsSold" -> "true")
              )

              status(result) mustBe SEE_OTHER
              verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any())
              redirectLocation(result) mustBe Some(controllers.tcsp.routes.SummaryController.get().url)
            }
          }

          "edit is 'false'" which {
            "redirects to SummaryController" in new TestFixture {

              val expected = Tcsp(
                tcspTypes = Some(TcspTypes(Set(NomineeShareholdersProvider, TrusteeProvider))),
                onlyOffTheShelfCompsSold = Some(OnlyOffTheShelfCompsSoldNo),
                complexCorpStructureCreation = None,
                providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
                doesServicesOfAnotherTCSP = Some(true),
                servicesOfAnotherTCSP = None,
                hasAccepted = false,
                hasChanged = true
              )

              val result = controller.post(false)(
                FakeRequest(POST, routes.OnlyOffTheShelfCompsSoldController.post().url)
                  .withFormUrlEncodedBody("onlyOffTheShelfCompsSold" -> "false")
              )
              status(result) mustBe SEE_OTHER
              verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any())
              redirectLocation(result) mustBe Some(controllers.tcsp.routes.SummaryController.get().url)
            }
          }
        }
      }
    }
  }
}
