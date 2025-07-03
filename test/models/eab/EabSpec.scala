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

package models.eab

import models.registrationprogress._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.libs.json._
import services.cache.Cache
import utils.AmlsSpec

class EabSpec extends AmlsSpec {

  val completeEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct"       -> true,
    "penalisedEstateAgentsActDetail" -> "details"
  )

  val incompleteEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct"       -> true,
    "penalisedEstateAgentsActDetail" -> ""
  )

  val completePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody"       -> true,
    "penalisedProfessionalBodyDetail" -> "details"
  )

  val incompletePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody"       -> true,
    "penalisedProfessionalBodyDetail" -> ""
  )

  val completeRedressScheme = Json.obj(
    "redressScheme"       -> "propertyRedressScheme",
    "redressSchemeDetail" -> "null"
  )

  val incompleteRedressScheme = Json.obj(
    "redressScheme"       -> "",
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

  val completeServices = Json.obj("eabServicesProvided" -> completeServiceList)

  val completeServicesWithoutResidential = Json.obj(
    "eabServicesProvided" -> completeServiceList.filter(s => !s.equals("residential"))
  )

  val completeServicesWithoutLetting = Json.obj(
    "eabServicesProvided" -> completeServiceList.filter(s => !s.equals("lettings"))
  )

  val completeDateOfChange = Json.obj(
    "dateOfChange" -> "2019-01-01"
  )

  "A constructed Eab model" when {
    "data are complete" must {
      val completeData = completeServices ++
        completeDateOfChange ++
        completeEstateAgencyActPenalty ++
        completePenalisedProfessionalBody ++
        completeRedressScheme ++
        completeMoneyProtectionScheme

      val constructedEab = Eab(completeData, hasAccepted = true)

      val completeEab = Json.obj(
        "data"        -> completeData,
        "hasChanged"  -> false,
        "hasAccepted" -> true
      )

      "serialise correctly" in {
        val serialisedEab = Json.toJson(constructedEab)
        serialisedEab mustBe completeEab
      }

      "deserialise correctly" in {
        val deserialisedEab = completeEab.as[Eab]
        deserialisedEab mustBe constructedEab
      }

      checkIsComplete(constructedEab)
    }

    "data are incomplete" must {
      val incompleteData = completeServices ++
        completeEstateAgencyActPenalty

      val constructedEab = Eab(incompleteData)

      val incompleteEab = Json.obj(
        "data"        -> incompleteData,
        "hasChanged"  -> false,
        "hasAccepted" -> false
      )

      "serialise correctly" in {
        val serialisedEab = Json.toJson(constructedEab)
        serialisedEab mustBe incompleteEab
      }

      "deserialise correctly" in {
        val deserialisedEab = incompleteEab.as[Eab]
        deserialisedEab mustBe constructedEab
      }

      checkIsComplete(
        constructedEab,
        isProfessionalBodyPenaltyComplete = false,
        isRedressSchemeComplete = false,
        isProtectionSchemeComplete = false,
        isComplete = false
      )
    }
  }

  "Pre-existing data" when {

    val oldServices = Json.obj("services" -> Json.arr("08", "03", "07", "02", "05", "01", "06", "09", "04"))

    val oldDateOfChange = Json.obj("dateOfChange" -> "2002-02-02")

    val oldProfessionalBody = Json.obj("penalised" -> true, "professionalBody" -> "test10")

    val oldEstateAct =
      Json.obj("penalisedUnderEstateAgentsAct" -> true, "penalisedUnderEstateAgentsActDetails" -> "test10")

    val oldRedressScheme = Json.obj("isRedress" -> false, "propertyRedressScheme" -> "03")

    val oldRedressSchemeNoRedress = Json.obj("isRedress" -> false)

    val oldClientProtection = Json.obj("clientMoneyProtection" -> false)

    "data are complete" must {
      val completeOldEab = (oldServices ++
        oldDateOfChange ++
        oldRedressScheme ++
        oldProfessionalBody ++
        oldEstateAct ++
        oldClientProtection ++ Json.obj("hasChanged" -> true, "hasAccepted" -> true)).as[Eab]

      checkIsComplete(completeOldEab)
    }

    "data are complete no redress" must {
      val completeOldEab = (oldServices ++
        oldDateOfChange ++
        oldRedressSchemeNoRedress ++
        oldProfessionalBody ++
        oldEstateAct ++ Json.obj("hasChanged" -> true, "hasAccepted" -> true)).as[Eab]

      checkIsComplete(completeOldEab)
    }

    "data are incomplete" must {
      val incompleteOldEab = (oldServices ++
        oldRedressScheme ++
        Json.obj("hasChanged" -> false, "hasAccepted" -> false)).as[Eab]

      checkIsComplete(
        incompleteOldEab,
        isEstateAgentActPenaltyComplete = false,
        isProfessionalBodyPenaltyComplete = false,
        isComplete = false
      )
    }
  }

  "An Eab model" when {
    "EstateAgentActPenalty is true but EstateAgentActPenaltyDetails is empty" must {

      val eab = Json
        .obj(
          "data"        -> (completeServices ++
            incompleteEstateAgencyActPenalty ++
            completePenalisedProfessionalBody ++
            completeRedressScheme ++
            completeMoneyProtectionScheme),
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )
        .as[Eab]

      "return false for isEstateAgentActPenaltyComplete" in {
        eab.isEstateAgentActPenaltyComplete mustBe false
      }

      "return false for isComplete" in {
        eab.isComplete mustBe false
      }
    }

    "PenalisedProfessionalBody is true but PenalisedProfessionalBodyDetails is empty" must {

      val eab = Json
        .obj(
          "data"        -> (completeServices ++
            completeEstateAgencyActPenalty ++
            incompletePenalisedProfessionalBody ++
            completeRedressScheme ++
            completeMoneyProtectionScheme),
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )
        .as[Eab]

      "return false for isProfessionalBodyPenaltyComplete" in {
        eab.isProfessionalBodyPenaltyComplete mustBe false
      }

      "return false for isComplete" in {
        eab.isComplete mustBe false
      }
    }

    "protection scheme is empty" when {
      "services contain 'lettings'" must {
        val eab = Json
          .obj(
            "data"        -> (completeServices ++
              completeEstateAgencyActPenalty ++
              completePenalisedProfessionalBody ++
              completeRedressScheme),
            "hasChanged"  -> false,
            "hasAccepted" -> true
          )
          .as[Eab]

        "return false for isProtectionSchemeComplete" in {
          eab.isProtectionSchemeComplete mustBe false
        }

        "return false for isComplete" in {
          eab.isComplete mustBe false
        }
      }

      "services do not contain 'lettings'" must {
        val completeEab = Json.obj(
          "data"        -> (completeServicesWithoutLetting ++
            completeEstateAgencyActPenalty ++
            completePenalisedProfessionalBody ++
            completeRedressScheme ++
            completeMoneyProtectionScheme),
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )

        val eab = completeEab.as[Eab]

        "return true for isProtectionSchemeComplete" in {
          eab.isProtectionSchemeComplete mustBe true
        }

        "return true for isComplete" in {
          eab.isComplete mustBe true
        }
      }
    }

    "redress scheme is empty" when {
      "services contain 'residential'" must {
        val incompleteData = completeServices ++
          completeEstateAgencyActPenalty ++
          completePenalisedProfessionalBody ++
          incompleteRedressScheme ++
          completeMoneyProtectionScheme

        val incompleteEab = Json.obj(
          "data"        -> incompleteData,
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )

        val eab = incompleteEab.as[Eab]

        "return false for isRedressSchemeComplete" in {
          eab.isRedressSchemeComplete mustBe false
        }

        "return false for isComplete" in {
          eab.isComplete mustBe false
        }
      }

      "services do not contain 'residential'" must {
        val incompleteData = completeServicesWithoutResidential ++
          completeEstateAgencyActPenalty ++
          completePenalisedProfessionalBody ++
          incompleteRedressScheme ++
          completeMoneyProtectionScheme

        val incompleteEab = Json.obj(
          "data"        -> incompleteData,
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )

        val eab = incompleteEab.as[Eab]

        "return true for isRedressSchemeComplete" in {
          eab.isRedressSchemeComplete mustBe true
        }

        "return true for isComplete" in {
          eab.isComplete mustBe true
        }
      }
    }

    "taskRow is called" must {

      val mockCache = mock[Cache]
      val msgKey    = "eab"

      val completeData = completeServices ++
        completeDateOfChange ++
        completeEstateAgencyActPenalty ++
        completePenalisedProfessionalBody ++
        completeRedressScheme ++
        completeMoneyProtectionScheme

      def completeEab(hasChanged: Boolean = false, hasAccepted: Boolean = true) = Json.obj(
        "data"        -> completeData,
        "hasChanged"  -> hasChanged,
        "hasAccepted" -> hasAccepted
      )

      "return a Not Started task row when model is empty" in {

        val notStartedTaskRow = TaskRow(
          msgKey,
          appConfig.eabWhatYouNeedUrl,
          false,
          NotStarted,
          TaskRow.notStartedTag
        )

        when(mockCache.getEntry[Eab](eqTo(Eab.key))(any()))
          .thenReturn(None)

        Eab.taskRow(appConfig)(mockCache, messages) mustBe notStartedTaskRow
      }

      "return a Completed task row when the model is complete" in {
        val eabSummaryUrl = appConfig.eabSummaryUrl
        val completedTaskRow = TaskRow(
          msgKey,
          controllers.routes.YourResponsibilitiesUpdateController.get(eabSummaryUrl).url,
          false,
          Completed,
          TaskRow.completedTag
        )

        when(mockCache.getEntry[Eab](eqTo(Eab.key))(any()))
          .thenReturn(Some(completeEab().as[Eab]))

        Eab.taskRow(appConfig)(mockCache, messages) mustBe completedTaskRow
      }

      "return an Updated task row when model has changed and is complete" in {

        val updatedTaskRow = TaskRow(
          msgKey,
          appConfig.eabSummaryUrl,
          true,
          Updated,
          TaskRow.updatedTag
        )

        when(mockCache.getEntry[Eab](eqTo(Eab.key))(any()))
          .thenReturn(Some(completeEab(true).as[Eab]))

        Eab.taskRow(appConfig)(mockCache, messages) mustBe updatedTaskRow
      }

      "return an Incomplete task row when model is not complete" in {

        val incompleteTaskRow = TaskRow(
          msgKey,
          appConfig.eabWhatYouNeedUrl,
          false,
          Started,
          TaskRow.incompleteTag
        )

        val incompleteData = completeServices ++
          completeEstateAgencyActPenalty ++
          completePenalisedProfessionalBody ++
          incompleteRedressScheme ++
          completeMoneyProtectionScheme

        val incompleteEab = Json.obj(
          "data"        -> incompleteData,
          "hasChanged"  -> false,
          "hasAccepted" -> true
        )

        when(mockCache.getEntry[Eab](eqTo(Eab.key))(any()))
          .thenReturn(Some(incompleteEab.as[Eab]))

        Eab.taskRow(appConfig)(mockCache, messages) mustBe incompleteTaskRow
      }
    }
  }

  def checkIsComplete(
    eab: Eab,
    isServicesComplete: Boolean = true,
    isRedressSchemeComplete: Boolean = true,
    isProtectionSchemeComplete: Boolean = true,
    isEstateAgentActPenaltyComplete: Boolean = true,
    isProfessionalBodyPenaltyComplete: Boolean = true,
    isComplete: Boolean = true
  ) = {
    s"return $isServicesComplete for isServicesComplete" in {
      eab.isServicesComplete mustBe isServicesComplete
    }

    s"return $isRedressSchemeComplete for isRedressSchemeComplete" in {
      eab.isRedressSchemeComplete mustBe isRedressSchemeComplete
    }

    s"return $isProtectionSchemeComplete for isProtectionSchemeComplete" in {
      eab.isProtectionSchemeComplete mustBe isProtectionSchemeComplete
    }

    s"return $isEstateAgentActPenaltyComplete for isEstateAgentActPenaltyComplete" in {
      eab.isEstateAgentActPenaltyComplete mustBe isEstateAgentActPenaltyComplete
    }

    s"return $isProfessionalBodyPenaltyComplete for isProfessionalBodyPenaltyComplete" in {
      eab.isProfessionalBodyPenaltyComplete mustBe isProfessionalBodyPenaltyComplete
    }

    s"return $isComplete for isComplete" in {
      eab.accept.isComplete mustBe isComplete
    }
  }
}
