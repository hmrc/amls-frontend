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
import controllers.actions.SuccessfulAuthAction
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.amp.AmpService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction, AuthorisedFixture}
import org.mockito.Matchers._
import play.api.test.FakeRequest

import scala.concurrent.Future


class AmpControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  val dateVal = LocalDateTime.now

  val completeData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val completeJson = Json.obj(
    "_id"     -> "someid",
    "data"           -> completeData,
    "lastUpdated"    -> Json.obj("$date" -> dateVal.atZone(ZoneOffset.UTC).toInstant.toEpochMilli),
    "hasChanged"     -> false,
    "hasAccepted"    -> false
  )

  trait Fixture extends AuthorisedFixture {
    self =>
    val request      = addToken(authRequest)
    val ampService   = mock[AmpService]
    val controller   = app.injector.instanceOf[AmpController]
    val mockCacheMap = mock[CacheMap]
    val credId       = "someId"

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[AmpService].to(ampService))
      .build()
  }

  "get returns 200" when {
    "no amp section in cache" in new Fixture {
      when(ampService.get(any())(any())).thenReturn(Future.successful(Some(Json.obj())))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe(Json.obj())
    }

    "amp section in cache" in new Fixture {
      when(ampService.get(any())(any())).thenReturn(Future.successful(Some(completeJson)))

      val result = controller.get(credId)(request)
      status(result) must be(OK)

      val document = Json.parse(contentAsString(result))
      document mustBe(completeJson)
    }
  }

  "set" when {
    "passed valid json" in new Fixture {
      val postRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](completeJson)

      when(ampService.set(any(), any())(any())).thenReturn(Future.successful(mockCacheMap))

      val result = controller.set(credId)(postRequest)
      status(result) must be(OK)
      val document = Json.parse(contentAsString(result))
      document mustBe(Json.obj("_id" -> credId))
    }
  }
}
