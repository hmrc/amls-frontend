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

package models.amp
import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec

trait AmpValues {

  val data = Json.obj(
    "typeOfParticipant" -> Seq("artGalleryOwner"),
    "typeOfParticipantDetail" -> "some other type",
    "boughtOrSoldOverThreshold" -> true,
    "dateTransactionOverThreshold" -> new LocalDate(2015, 2, 24),
    "identifyLinkedTransactions" -> true,
    "percentageExpectedTurnover" -> "fortyOneToSixty"
  )

  val completeJson = Json.obj(
    "_id" -> "someid",
    "data" -> data
  )

  val completeModel = Amp("someid", data)
}

class AmpSpec extends AmlsSpec with AmpValues {

  "Amp" must {
    "correctly show if the model is incomplete" in {
    }

    "have a mongo key that" must {
      "be correctly set" in {
        Amp.mongoKey() must be("amp")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {
      }

      "return a Completed Section when model is complete" in {
      }

      "return a Started Section when model is incomplete" in {
      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }
    }
  }

  "Amp Serialisation" must {

    "correctly convert between json formats" when {

      "Serialise as expected" in {
        Json.toJson(completeModel) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[Amp] must be(completeModel)
      }
    }
  }
}
