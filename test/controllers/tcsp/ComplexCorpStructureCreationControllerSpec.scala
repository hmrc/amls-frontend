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

package controllers.tcsp

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.tcsp._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, redirectLocation, status, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction, AuthorisedFixture}

import scala.concurrent.Future


class ComplexCorpStructureCreationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]

    lazy val controller = new ComplexCorpStructureCreationController(SuccessfulAuthAction, commonDependencies, cache)

    val tcsp = Tcsp(
      Some(TcspTypes(Set(
        NomineeShareholdersProvider,
        TrusteeProvider))),
      None,
      None,
      Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
      Some(true),
      None,
      hasAccepted = true
    )

    when(cache.fetch[Tcsp](any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(tcsp)))

    when(cache.save[Tcsp](any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "The ComplexCorpStructureCreationController" when {
    "get is called" must {

      "respond with BAD_REQUEST" when {
        "given invalid data" in new TestFixture {

          val newRequest = request.withFormUrlEncodedBody(
            "complexCorpStructureCreation" -> "invalid"
          )

          val result = controller.post(true)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with OK and include complexCorpStructureCreation" in new TestFixture {

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include("complexCorpStructureCreation")
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe BAD_REQUEST
      }

      "respond with SUCCESS" when {
        "edit is 'true'" which {
          "redirects to SummaryController" in new TestFixture {

            val expected = Tcsp(
              tcspTypes = Some(TcspTypes(Set(
                NomineeShareholdersProvider,
                TrusteeProvider))),
              onlyOffTheShelfCompsSold = None,
              complexCorpStructureCreation = Some(ComplexCorpStructureCreationYes),
              providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
              doesServicesOfAnotherTCSP = Some(true),
              servicesOfAnotherTCSP = None,
              hasAccepted = false,
              hasChanged = true
            )

            val result = controller.post(true)(request.withFormUrlEncodedBody("complexCorpStructureCreation" -> "true"))

            status(result) mustBe SEE_OTHER
            verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any(), any())
            redirectLocation(result) mustBe Some(controllers.tcsp.routes.SummaryController.get().url)
          }
        }

        "edit is 'false'" which {
          "contains RegisteredOfficeEtc" when {
            "redirects to ProvidedServicesController" in new TestFixture {

              val regOfficeTcsp = Tcsp(
                Some(TcspTypes(Set(
                  RegisteredOfficeEtc,
                  TrusteeProvider))),
                None,
                None,
                Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
                Some(true),
                None,
                hasAccepted = true
              )

              when(cache.fetch[Tcsp](any(), any())(any(), any()))
                .thenReturn(Future.successful(Some(regOfficeTcsp)))

              val expected = Tcsp(
                tcspTypes = Some(TcspTypes(Set(
                  RegisteredOfficeEtc,
                  TrusteeProvider))),
                onlyOffTheShelfCompsSold = None,
                complexCorpStructureCreation = Some(ComplexCorpStructureCreationNo),
                providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
                doesServicesOfAnotherTCSP = Some(true),
                servicesOfAnotherTCSP = None,
                hasAccepted = false,
                hasChanged = true
              )

              val result = controller.post(false)(request.withFormUrlEncodedBody("complexCorpStructureCreation" -> "false"))
              status(result) mustBe SEE_OTHER
              verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any(), any())
              redirectLocation(result) mustBe Some(controllers.tcsp.routes.ProvidedServicesController.get().url)
            }
          }

          "not contains RegisteredOfficeEtc" when {
            "redirects to ServicesOfAnotherTCSPController" in new TestFixture {

              val expected = Tcsp(
                tcspTypes = Some(TcspTypes(Set(
                  NomineeShareholdersProvider,
                  TrusteeProvider))),
                onlyOffTheShelfCompsSold = None,
                complexCorpStructureCreation = Some(ComplexCorpStructureCreationNo),
                providedServices = Some(ProvidedServices(Set(PhonecallHandling, Other("other service")))),
                doesServicesOfAnotherTCSP = Some(true),
                servicesOfAnotherTCSP = None,
                hasAccepted = false,
                hasChanged = true
              )

              val result = controller.post(false)(request.withFormUrlEncodedBody("complexCorpStructureCreation" -> "false"))
              status(result) mustBe SEE_OTHER
              verify(controller.dataCacheConnector).save[Tcsp](any(), any(), eqTo(expected))(any(), any())
              redirectLocation(result) mustBe Some(controllers.tcsp.routes.ServicesOfAnotherTCSPController.get().url)
            }
          }
        }
      }
    }
  }
}
