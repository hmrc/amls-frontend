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

package models.amp

import models.registrationprogress._
import models.renewal.AMPTurnover
import org.mockito.Mockito._
import play.api.libs.json.Json
import services.cache.Cache
import utils.AmlsSpec

import java.time.{LocalDate, LocalDateTime}

trait AmpValues {

  val dateVal = LocalDateTime.now

  val completeData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val newData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> false,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val missingTypeOfParticipantData = Json.obj(
    "typeOfParticipantDetail"      -> "some other type",
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val missingTypeOfParticipantDetailData = Json.obj(
    "typeOfParticipant"            -> Seq("somethingElse"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val missingSoldOverThresholdData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val missingDateTransactionOverThresholdData = Json.obj(
    "typeOfParticipant"          -> Seq("artGalleryOwner"),
    "soldOverThreshold"          -> true,
    "identifyLinkedTransactions" -> true,
    "percentageExpectedTurnover" -> "fortyOneToSixty"
  )

  val missingIdentifyLinkedTransactionsData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val MissingPercentageExpectedTurnoverData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true
  )

  val completeJson = Json.obj(
    "data"        -> completeData,
    "hasChanged"  -> false,
    "hasAccepted" -> true
  )

  val completeModel                            = Amp(completeData, false, true)
  val updatedModel                             = Amp(completeData, true, true)
  val missingTypeOfParticipantModel            = Amp(missingTypeOfParticipantData)
  val missingTypeOfParticipantDetailModel      = Amp(missingTypeOfParticipantDetailData)
  val missingSoldOverTheThresholdModel         = Amp(missingSoldOverThresholdData)
  val missingDateTransactionOverThresholdModel = Amp(missingDateTransactionOverThresholdData)
  val missingIdentifyLinkedTransactionsModel   = Amp(missingIdentifyLinkedTransactionsData)
  val MissingPercentageExpectedTurnoverModel   = Amp(MissingPercentageExpectedTurnoverData)
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
        result must be(Amp(completeData, false, true))
      }
    }

    "have a task row function that" must {
      implicit val cache    = mock[Cache]
      val ampWhatYouNeedUrl = "http://localhost:9223/anti-money-laundering/art-market-participant/what-you-need"
      val ampSummaryUrl     = "http://localhost:9223/anti-money-laundering/art-market-participant/check-your-answers"

      "returns a Not Started task row when model is empty" in {
        val notStartedTaskRow = TaskRow("amp", ampWhatYouNeedUrl, false, NotStarted, TaskRow.notStartedTag)

        when(cache.getEntry[Amp]("amp")) thenReturn None
        Amp.taskRow(appConfig) mustBe notStartedTaskRow
      }

      "returns a Completed task row when model is complete" in {
        val completedTaskRow = TaskRow("amp", ampSummaryUrl, false, Completed, TaskRow.completedTag)

        when(cache.getEntry[Amp]("amp")) thenReturn Some(completeModel)
        Amp.taskRow(appConfig) mustBe completedTaskRow
      }

      "returns a Updated task row when model is complete and has changed" in {
        val updatedTaskRow = TaskRow("amp", ampSummaryUrl, true, Updated, TaskRow.updatedTag)

        when(cache.getEntry[Amp]("amp")) thenReturn Some(updatedModel)
        Amp.taskRow(appConfig) mustBe updatedTaskRow
      }

      "return a Started Section when model is incomplete" in {
        val incompleteTaskRow = TaskRow("amp", ampWhatYouNeedUrl, false, Started, TaskRow.incompleteTag)

        when(cache.getEntry[Amp]("amp")) thenReturn Some(missingTypeOfParticipantDetailModel)
        Amp.taskRow(appConfig) mustBe incompleteTaskRow
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

        "missing soldOverThreshold" in {
          missingSoldOverTheThresholdModel.isComplete must be(false)
        }

        "soldOverThreshold true and missing dateTransactionOverThreshold" in {
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
      "Convert amp section data" in {
        val ampData = Amp(
          Json.obj(
            "typeOfParticipant"            -> Seq("artGalleryOwner"),
            "soldOverThreshold"            -> true,
            "dateTransactionOverThreshold" -> LocalDate.now.toString,
            "identifyLinkedTransactions"   -> true,
            "percentageExpectedTurnover"   -> "zeroToTwenty"
          )
        )

        Amp.convert(ampData) must be(AMPTurnover.First)
      }

      "throw exception when given incorrect data" in {
        val ampData = Amp(
          Json.obj(
            "typeOfParticipant"            -> Seq("artGalleryOwner"),
            "soldOverThreshold"            -> true,
            "dateTransactionOverThreshold" -> LocalDate.now.toString,
            "identifyLinkedTransactions"   -> true,
            "percentageExpectedTurnover"   -> "testException"
          )
        )

        intercept[Exception] {
          Amp.convert(ampData)
        }
      }
    }
  }
}
