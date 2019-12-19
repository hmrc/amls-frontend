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

package controllers.amp

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import akka.stream.Materializer
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.amp.Amp
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.OptionValues
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.amp.AmpCacheService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class AmpControllerSpec extends AmlsSpec with OptionValues {

  val dateVal = LocalDateTime.now

  val completeData = Json.obj(
    "typeOfParticipant" -> Seq("artGalleryOwner"),
    "soldOverThreshold" -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions" -> true,
    "percentageExpectedTurnover" -> "fortyOneToSixty"
  )

  val completeJson = Json.obj(
    "_id" -> "someid",
    "data" -> completeData,
    "lastUpdated" -> Json.obj("$date" -> dateVal.atZone(ZoneOffset.UTC).toInstant.toEpochMilli),
    "hasChanged" -> false,
    "hasAccepted" -> false
  )

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val ampCacheService = mock[AmpCacheService]
    val mockCacheMap = mock[CacheMap]
    val credId = "someId"
    val mockCacheConnector = mock[DataCacheConnector]

    val controller = new AmpController(ampCacheService, SuccessfulAuthAction, mockCacheConnector, commonDependencies, mockMcc)
  }

  "get returns 200" when {
    "no amp section in cache" in new Fixture {
      when(ampCacheService.get(any())(any())).thenReturn(Future.successful(Some(Json.obj())))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe (Json.obj())
    }

    "amp section in cache" in new Fixture {
      when(ampCacheService.get(any())(any())).thenReturn(Future.successful(Some(completeJson)))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe (completeJson)
    }
  }

  "set" when {
    "passed valid json" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeJson)

      when(ampCacheService.set(any(), any())(any())).thenReturn(Future.successful(mockCacheMap))

      val result = controller.set(credId)(postRequest)
      status(result) must be(OK)
      val document = Json.parse(contentAsString(result))
      document mustBe (Json.obj("_id" -> credId))
    }
  }

  "accept" must {
    "set accept flag to true and redirect to RegistrationProgressController" in new Fixture {
      when(controller.cacheConnector.fetch[Amp](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(completeJson.as[Amp])))

      when(controller.cacheConnector.save[Amp](any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = call(controller.accept, FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.RegistrationProgressController.get().toString

      verify(controller.cacheConnector).save[Amp](any(), eqTo(Amp.key),
        eqTo(completeJson.as[Amp].copy(hasAccepted = true)))(any(), any())
    }
  }
}
