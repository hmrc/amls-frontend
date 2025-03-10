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

package controllers.amp

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.amp.Amp
import org.mockito.Mockito._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthAction, AuthorisedFixture, CacheMocks}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import services.ProxyCacheService

import scala.concurrent.Future

class AmpControllerSpec extends AmlsSpec with CacheMocks {

  val dateVal: LocalDateTime = LocalDateTime.now

  val completeData: JsObject = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val completeJson: JsObject = Json.obj(
    "_id"         -> "someid",
    "data"        -> completeData,
    "lastUpdated" -> Json.obj("$date" -> dateVal.atZone(ZoneOffset.UTC).toInstant.toEpochMilli),
    "hasChanged"  -> false,
    "hasAccepted" -> false
  )

  trait Fixture extends AuthorisedFixture {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    val proxyCacheService: ProxyCacheService     = mock[ProxyCacheService]
    val credId                                   = "someId"

    lazy val app: Application = new GuiceApplicationBuilder()
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[ProxyCacheService].to(proxyCacheService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

    val controller: AmpController = app.injector.instanceOf[AmpController]
  }

  "get returns 200" when {
    "no amp section in cache" in new Fixture {
      when(proxyCacheService.getAmp(any())).thenReturn(Future.successful(Some(Json.obj())))

      val result: Future[Result] = controller.get(credId)(request)
      status(result) must be(OK)

      val document: JsValue = Json.parse(contentAsString(result))
      document mustBe Json.obj()
    }

    "amp section in cache" in new Fixture {
      when(proxyCacheService.getAmp(any())).thenReturn(Future.successful(Some(completeJson)))

      val result: Future[Result] = controller.get(credId)(request)
      status(result) must be(OK)

      val document: JsValue = Json.parse(contentAsString(result))
      document mustBe completeJson
    }
  }

  "set" when {
    "passed valid json" in new Fixture {
      val postRequest: FakeRequest[JsValue] = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeJson)

      when(proxyCacheService.setAmp(any(), any())).thenReturn(Future.successful(mockCacheMap))

      val result: Future[Result] = controller.set(credId)(postRequest)
      status(result) must be(OK)
      val document: JsValue = Json.parse(contentAsString(result))
      document mustBe Json.obj("_id" -> credId)
    }
  }

  "accept" must {
    "set accept flag to true and redirect to RegistrationProgressController" in new Fixture {

      when(mockCacheConnector.fetch[Amp](any(), any())(any()))
        .thenReturn(Future.successful(Some(completeJson.as[Amp])))

      when(mockCacheConnector.save[Amp](any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result: Future[Result] = controller.accept.apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.RegistrationProgressController.get().toString

      verify(mockCacheConnector)
        .save[Amp](any(), eqTo(Amp.key), eqTo(completeJson.as[Amp].copy(hasAccepted = true)))(any())
    }
  }
}
