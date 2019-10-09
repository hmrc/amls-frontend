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
import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import org.mockito.Mockito._
import play.api.mvc.Call

trait AmpValues {

  val dateVal = LocalDateTime.now

  val completeData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val newData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> false,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val missingTypeOfParticipantData = Json.obj(
    "typeOfParticipantDetail"       -> "some other type",
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val missingTypeOfParticipantDetailData = Json.obj(
    "typeOfParticipant"     -> Seq("somethingElse"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val missingBoughtOrSoldOverThresholdData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val missingDateTransactionOverThresholdData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "identifyLinkedTransactions"    -> true,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val missingIdentifyLinkedTransactionsData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "percentageExpectedTurnover"    -> "fortyOneToSixty"
  )

  val MissingPercentageExpectedTurnoverData = Json.obj(
    "typeOfParticipant"     -> Seq("artGalleryOwner"),
    "boughtOrSoldOverThreshold"     -> true,
    "dateTransactionOverThreshold"  -> LocalDate.now,
    "identifyLinkedTransactions"    -> true
  )

  val completeJson = Json.obj(
    "data"           -> completeData,
    "hasChanged"     -> false,
    "hasAccepted"    -> true
  )

  val completeModel                             = Amp(completeData, false, true)
  val missingTypeOfParticipantModel             = Amp(missingTypeOfParticipantData)
  val missingTypeOfParticipantDetailModel       = Amp(missingTypeOfParticipantDetailData)
  val missingBoughtOrSoldOverTheThresholdModel  = Amp(missingBoughtOrSoldOverThresholdData)
  val missingDateTransactionOverThresholdModel  = Amp(missingDateTransactionOverThresholdData)
  val missingIdentifyLinkedTransactionsModel    = Amp(missingIdentifyLinkedTransactionsData)
  val MissingPercentageExpectedTurnoverModel    = Amp(MissingPercentageExpectedTurnoverData)
}

class AmpSpec extends AmlsSpec with AmpValues {
  "Amp" must {
    "have a mongo key that" must {
      "be correctly set" in {
        Amp.mongoKey() must be("amp")
      }
    }

    "when setting new data" must {
      "return Amp with hasChanged true" in {
        val result = completeModel.data(newData)
        result must be(Amp(newData, true, false))
      }
    }

    "when setting data that has not changed" must {
      "return Amp with hasChanged false" in {
        val result = completeModel.data(completeData)
        result must be(Amp(completeData,  false, true))
      }
    }

    "have a section function that" must {
      implicit val cache         = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {
        val notStartedSection = Section("amp", NotStarted, false, Call("GET", ApplicationConfig.ampWhatYouNeedUrl))

        when(cache.getEntry[Amp]("amp")) thenReturn None
        Amp.section must be(notStartedSection)
      }

      "return a Completed Section when model is complete" in {
        val completedSection = Section("amp", Completed, false, Call("GET", ApplicationConfig.ampSummeryUrl))

        when(cache.getEntry[Amp]("amp")) thenReturn Some(completeModel)
        Amp.section must be(completedSection)
      }

      "return a Started Section when model is incomplete" in {
        val startedSection = Section("amp", Started, false, Call("GET", ApplicationConfig.ampWhatYouNeedUrl))

        when(cache.getEntry[Amp]("amp")) thenReturn Some(missingTypeOfParticipantDetailModel)
        Amp.section must be(startedSection)
      }
    }

    "have an isComplete function that" must {
      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }

      "correctly show if the model is incomplete" when {
        "missing typeOfParticipant" in {
          missingTypeOfParticipantModel.isComplete must be(false)
        }

        "typeOfParticipant something else and missing typeOfParticipantDetail" in {
          missingTypeOfParticipantDetailModel.isComplete must be(false)
        }

        "missing boughtOrSoldOverThreshold" in {
          missingBoughtOrSoldOverTheThresholdModel.isComplete must be(false)
        }

        "boughtOrSoldOverThreshold true and missing dateTransactionOverThreshold" in {
          missingDateTransactionOverThresholdModel.isComplete must be(false)
        }

        "missing identifyLinkedTransactions" in {
          missingIdentifyLinkedTransactionsModel.isComplete must be(false)
        }

        "missing percentageExpectedTurnover" in {
          MissingPercentageExpectedTurnoverModel.isComplete must be(false)
        }
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
