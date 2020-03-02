/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.amp.EabController
import models.estateagentbusiness.Eab
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.amp.ProxyCacheService
import utils.{AmlsSpec, AuthAction, AuthorisedFixture, CacheMocks}

import scala.concurrent.Future

class EabControllerSpec extends AmlsSpec with CacheMocks {

  val completeEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct" -> true,
    "penalisedEstateAgentsActDetail" -> "details"
  )

  val completePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody" -> true,
    "penalisedProfessionalBodyDetail" -> "details"
  )

  val completeRedressScheme = Json.obj(
    "redressScheme" -> "propertyRedressScheme",
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
    "socialHousingProvision")

  val completeServices = Json.obj("eabServicesProvided" -> completeServiceList )

  val completeEabData = completeServices ++
    completeEstateAgencyActPenalty ++
    completePenalisedProfessionalBody ++
    completeRedressScheme ++
    completeMoneyProtectionScheme

  val completeEabJson = Json.obj(
    "data"           -> completeEabData,
    "hasChanged"     -> false,
    "hasAccepted"    -> false
  )

  val completeEabModel = Eab(completeEabData)

  trait Fixture extends AuthorisedFixture {
    self =>
    val request         = addToken(authRequest)
    val proxyCacheService = mock[ProxyCacheService]
    val credId          = "someId"

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[ProxyCacheService].to(proxyCacheService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    val controller      = app.injector.instanceOf[EabController]
  }

  "get returns 200" when {
    "no amp section in cache" in new Fixture {
      when(proxyCacheService.getEab(any())(any())).thenReturn(Future.successful(Some(Json.obj())))



      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe(Json.obj())
    }

    "amp section in cache" in new Fixture {
      when(proxyCacheService.getEab(any())(any())).thenReturn(Future.successful(Some(completeEabJson)))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe(completeEabJson)
    }
  }

  "set" when {
    "passed valid json" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeEabJson)

      when(proxyCacheService.setEab(any(), any())(any())).thenReturn(Future.successful(mockCacheMap))

      val result = controller.set(credId)(postRequest)
      status(result) must be(OK)
      val document = Json.parse(contentAsString(result))
      document mustBe(Json.obj("_id" -> credId))
    }
  }

  "accept" must {
    "set accept flag to true and redirect to RegistrationProgressController" in new Fixture {

      when(mockCacheConnector.fetch[Eab](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(completeEabJson.as[Eab])))

      when(mockCacheConnector.save[Eab](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.accept.apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.RegistrationProgressController.get().toString

      verify(mockCacheConnector).save[Eab](any(), eqTo(Eab.key),
        eqTo(completeEabJson.as[Eab].copy(hasAccepted = true)))(any(), any())
    }
  }
}