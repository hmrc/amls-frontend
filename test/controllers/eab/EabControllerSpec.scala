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

package controllers.eab

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.eab.Eab
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ProxyCacheService
import services.businessmatching.ServiceFlow
import utils.{AmlsSpec, AuthAction, AuthorisedFixture, CacheMocks}

import scala.concurrent.Future

class EabControllerSpec extends AmlsSpec with CacheMocks {

  val completeEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct"       -> true,
    "penalisedEstateAgentsActDetail" -> "details"
  )

  val completePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody"       -> true,
    "penalisedProfessionalBodyDetail" -> "details"
  )

  val completeRedressScheme = Json.obj(
    "redressScheme"       -> "propertyRedressScheme",
    "redressSchemeDetail" -> "null"
  )

  val completeMoneyProtectionScheme = Json.obj(
    "clientMoneyProtectionScheme" -> true
  )

  val completeServiceList = Seq(
    "assetManagement",
    "auctioneering",
    "businessTransfer",
    "commercial",
    "developmentCompany",
    "landManagement",
    "lettings",
    "relocation",
    "residential",
    "socialHousingProvision"
  )

  val updatedServiceList = Seq("assetManagement", "auctioneering", "lettings", "socialHousingProvision")

  val completeServices = Json.obj("eabServicesProvided" -> completeServiceList)
  val updatedServices  = Json.obj("eabServicesProvided" -> updatedServiceList)

  val completeEabData = completeServices ++
    completeEstateAgencyActPenalty ++
    completePenalisedProfessionalBody ++
    completeRedressScheme ++
    completeMoneyProtectionScheme

  val updatedEabData = updatedServices ++
    completeEstateAgencyActPenalty ++
    completePenalisedProfessionalBody ++
    completeRedressScheme ++
    completeMoneyProtectionScheme

  val noEabData = Json.obj()

  val completeEabJson = Json.obj(
    "data"        -> completeEabData,
    "hasChanged"  -> false,
    "hasAccepted" -> false
  )

  val updatedEabJson = Json.obj(
    "data"        -> completeEabData,
    "hasChanged"  -> false,
    "hasAccepted" -> false
  )

  val completeEabModel = Eab(completeEabData)
  val updatedEabModel  = Eab(updatedEabData)
  val noEabModel       = Eab(noEabData)

  trait Fixture extends AuthorisedFixture {
    self =>
    val request           = addToken(authRequest)
    val proxyCacheService = mock[ProxyCacheService]
    val credId            = "someId"
    val mockServiceFlow   = mock[ServiceFlow]

    lazy val app = new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[ProxyCacheService].to(proxyCacheService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[ServiceFlow].to(mockServiceFlow))
      .build()

    val controller = app.injector.instanceOf[EabController]
  }

  "get returns 200" when {
    "no eab section in cache" in new Fixture {
      when(proxyCacheService.getEab(any())).thenReturn(Future.successful(Some(Json.obj())))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe (Json.obj())
    }

    "eab section in cache" in new Fixture {
      when(proxyCacheService.getEab(any())).thenReturn(Future.successful(Some(completeEabJson)))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe completeEabJson
    }
  }

  "set" when {
    "passed valid json" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      when(proxyCacheService.setEab(any(), any())).thenReturn(Future.successful(mockCacheMap))

      val result = controller.set(credId)(postRequest)
      status(result) must be(OK)
      val document = Json.parse(contentAsString(result))
      document mustBe (Json.obj("_id" -> credId))
    }
  }

  "accept" must {
    "set accept flag to true and redirect to RegistrationProgressController" in new Fixture {

      when(mockCacheConnector.fetch[Eab](any(), any())(any()))
        .thenReturn(Future.successful(Some(completeEabJson.as[Eab])))

      when(mockCacheConnector.save[Eab](any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.accept.apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.RegistrationProgressController.get().toString

      verify(mockCacheConnector)
        .save[Eab](any(), eqTo(Eab.key), eqTo(completeEabJson.as[Eab].copy(hasAccepted = true)))(any())
    }
  }

  "requireDateOfChange" must {
    "return false where is new activity" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      val status = "Approved"

      when(mockServiceFlow.isNewActivity(any(), any())(any())).thenReturn(Future.successful(true))

      val result = controller.requireDateOfChange(credId, status)(postRequest)

      val document = Json.parse(contentAsString(result))

      document mustBe (Json.obj("requireDateOfChange" -> false))
    }

    "return false where the current list of activities does not differ from the new" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      val status = "Approved"

      when(mockServiceFlow.isNewActivity(any(), any())(any())).thenReturn(Future.successful(false))

      when(mockCacheConnector.fetch[Eab](any(), any())(any())).thenReturn(
        Future.successful(Some(completeEabModel))
      )

      val result = controller.requireDateOfChange(credId, status)(postRequest)

      val document = Json.parse(contentAsString(result))

      document mustBe (Json.obj("requireDateOfChange" -> false))
    }

    "return false where the raw status from API9 is not Approved" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      val status = "NotYetSubmitted"

      when(mockServiceFlow.isNewActivity(any(), any())(any())).thenReturn(Future.successful(false))

      when(mockCacheConnector.fetch[Eab](any(), any())(any())).thenReturn(
        Future.successful(Some(updatedEabModel))
      )

      val result = controller.requireDateOfChange(credId, status)(postRequest)

      val document = Json.parse(contentAsString(result))

      document mustBe (Json.obj("requireDateOfChange" -> false))
    }

    "return true where is not new activity and list of activities differs and status is Approved" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      val status = "Approved"

      when(mockServiceFlow.isNewActivity(any(), any())(any())).thenReturn(Future.successful(false))

      when(mockCacheConnector.fetch[Eab](any(), any())(any())).thenReturn(
        Future.successful(Some(updatedEabModel))
      )

      val result = controller.requireDateOfChange(credId, status)(postRequest)

      val document = Json.parse(contentAsString(result))

      document mustBe (Json.obj("requireDateOfChange" -> true))
    }

    "return false where is not new activity and no current EAB model and status is NotYetSubmitted" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      val status = "NotYetSubmitted"

      when(mockServiceFlow.isNewActivity(any(), any())(any())).thenReturn(Future.successful(false))

      when(mockCacheConnector.fetch[Eab](any(), any())(any())).thenReturn(
        Future.successful(Some(noEabModel))
      )

      val result = controller.requireDateOfChange(credId, status)(postRequest)

      val document = Json.parse(contentAsString(result))

      document mustBe (Json.obj("requireDateOfChange" -> false))
    }
  }
}
