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

package services.amp

import java.time.{LocalDate, LocalDateTime}

import models.amp.Amp
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global

class AmpCacheServiceSpec extends AmlsSpec with MockitoSugar
  with ScalaFutures
  with IntegrationPatience {

  val dateVal = LocalDateTime.now

  val completeData = Json.obj(
    "typeOfParticipant"             -> Seq("artGalleryOwner"),
    "soldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val completeJson = Json.obj(
    "data"           -> completeData,
    "hasChanged"     -> false,
    "hasAccepted"    -> false
  )

  val credId        = "someId"
  val completeModel = Amp(completeData)

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val svc = new AmpCacheService(mockCacheConnector)
  }

  "get" when {
    "cache data exists" when {
      "returns the amp section" in new Fixture {
        mockCacheFetch[Amp](Some(completeModel), Some(Amp.key))

        whenReady(svc.get(credId)){ result =>
          result mustBe Some(completeJson)
        }
      }
    }

    "cache data does not exist" when {
      "returns null" in new Fixture {
        mockCacheFetch[Amp](None, Some(Amp.key))
        whenReady(svc.get(credId)){ result =>
          result mustBe None
        }
      }
    }
  }

  "set" when {
    "given valid json" when {
      "updates an existing model" in new Fixture {
        mockCacheFetch[Amp](Some(completeModel), Some(Amp.key))
        mockCacheSave[Amp]

        whenReady(svc.set(credId, completeJson)){ result =>
          result mustBe mockCacheMap
        }
      }

      "saves a new model" in new Fixture {
        mockCacheFetch[Amp](None, Some(Amp.key))
        mockCacheSave[Amp]

        whenReady(svc.set(credId, completeJson)){ result =>
          result mustBe mockCacheMap
        }
      }
    }
  }
}
