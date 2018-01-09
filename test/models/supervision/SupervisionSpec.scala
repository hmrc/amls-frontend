/*
 * Copyright 2018 HM Revenue & Customs
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

package models.supervision

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.tcsp.Tcsp
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeApplication
import uk.gov.hmrc.http.cache.client.CacheMap

class SupervisionSpec extends PlaySpec with MockitoSugar with SupervisionValues with OneAppPerSuite {

  "Supervision" must {

    "have a default function that" must {

      "provides a default value" when {
        "none is provided" in {
          Supervision.default(None) must be(Supervision())
        }

        "existing value is provided" in {
          Supervision.default(Some(completeModel)) must be(completeModel)
        }
      }

    }

    "have a mongo key" in {
      Supervision.mongoKey() must be("supervision")
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("supervision", NotStarted, false,  controllers.supervision.routes.WhatYouNeedController.get())

        when(cache.getEntry[Supervision]("supervision")) thenReturn None

        Supervision.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Supervision]
        val completedSection = Section("supervision", Completed, false,  controllers.supervision.routes.SummaryController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(complete)

        Supervision.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incomplete = mock[Supervision]
        val startedSection = Section("supervision", Started, false,  controllers.supervision.routes.WhatYouNeedController.get())

        when(incomplete.isComplete) thenReturn false
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(incomplete)

        Supervision.section must be(startedSection)

      }
    }

    "have an isComplete function that" must {

      "show if the model is complete" in {
        completeModel.isComplete must be(true)
      }

      "show if the model is incomplete" when {
        "ProfessionalBodyMember is Yes and businessTypes is not defined" in {
          partialModelNoBusinessTypes.isComplete must be(false)
        }
        "multiple properties are missing" in {
          partialModel.isComplete must be(false)
        }
      }
    }

    "convert between json formats" which {
      "Serialises" in {
        Json.toJson(completeModel) must be(completeJson)
      }

      "Deserialises" when {
        "json is current format" in {
          (completeJson - "hasChanged").as[Supervision] must be(completeModel)
        }
        "json is old format" in {
          (completeJsonOldFormat - "hasChanged").as[Supervision] must be(completeModel)
        }
      }

    }

    "with all values set as None" must {
      val initial: Option[Supervision] = None

      "return Supervision with anotherBody set and indicating changes have been made" in {
        val result = initial.anotherBody(DefaultValues.DefaultAnotherBody)
        result must be(Supervision(Some(DefaultValues.DefaultAnotherBody), None, hasChanged = true, hasAccepted = false))
      }
      "return Supervision with professionalBody set and indicating changes have been made" in {
        val result = initial.professionalBody(DefaultValues.DefaultProfessionalBody)
        result must be(Supervision(professionalBody = Some(DefaultValues.DefaultProfessionalBody), hasChanged = true, hasAccepted = false))
      }
      "return Supervision with professionalBodyMember set and indicating changes have been made" in {
        val result = initial.professionalBodyMember(DefaultValues.DefaultProfessionalBodyMember)
        result must be(Supervision(professionalBodyMember = Some(DefaultValues.DefaultProfessionalBodyMember), hasChanged = true, hasAccepted = false))
      }

    }

    "with existing values" must {
      val initial: Supervision = completeModel

      "return Supervision with anotherBody set and indicating changes have been made" in {
        val result = initial.anotherBody(NewValues.NewAnotherBody)
        result must be(completeModel.copy(Some(NewValues.NewAnotherBody), hasChanged = true, hasAccepted = false))
      }
      "return Supervision with professionalBody set and indicating changes have been made" in {
        val result = initial.professionalBody(NewValues.NewProfessionalBody)
        result must be(completeModel.copy(professionalBody = Some(NewValues.NewProfessionalBody), hasChanged = true, hasAccepted = false))
      }
      "return Supervision with professionalBodyMember set and indicating changes have been made" in {
        val result = initial.professionalBodyMember(NewValues.ProfessionalBodyMemberYes)
        result must be(completeModel.copy(professionalBodyMember = Some(NewValues.ProfessionalBodyMemberYes), hasChanged = true, hasAccepted = false))
      }
    }

    "have function to provide property data" when {
      "anotherBody value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.anotherBody(DefaultValues.DefaultAnotherBody)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }

        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.anotherBody(NewValues.NewAnotherBody)
            res.hasChanged must be(true)
            res.anotherBody must be(Some(NewValues.NewAnotherBody))
          }
        }
      }

      "professionalBodyMember value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.professionalBodyMember(DefaultValues.DefaultProfessionalBodyMember)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }
        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.professionalBodyMember(NewValues.ProfessionalBodyMemberYes)
            res.hasChanged must be(true)
            res.professionalBodyMember must be(Some(NewValues.ProfessionalBodyMemberYes))
          }
        }
      }

      "businessTypes value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.businessTypes(Some(DefaultValues.DefaultBusinessTypes))
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }
        "is different" must {
          "set the hasChanged update the value" in {
            val res = completeModel.businessTypes(Some(NewValues.NewBusinessTypes))
            res.hasChanged must be(true)
            res.businessTypes must be(Some(NewValues.NewBusinessTypes))
          }
        }
      }

      "professionalBody value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.professionalBody(DefaultValues.DefaultProfessionalBody)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }
        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.professionalBody(NewValues.NewProfessionalBody)
            res.hasChanged must be(true)
            res.professionalBody must be(Some(NewValues.NewProfessionalBody))
          }
        }
      }
    }
  }
}

trait SupervisionValues {

  object DefaultValues {

    private val supervisor = "Company A"
    private val start = new LocalDate(1993, 8, 25)
    //scalastyle:off magic.number
    private val end = new LocalDate(1999, 8, 25)
    //scalastyle:off magic.number
    private val reason = "Ending reason"

    val DefaultAnotherBody = AnotherBodyYes(supervisor, start, end, reason)
    val DefaultProfessionalBody = ProfessionalBodyYes("details")
    val DefaultProfessionalBodyMember = ProfessionalBodyMemberYes
    val DefaultBusinessTypes = BusinessTypes(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("test")))
  }

  object NewValues {
    val NewAnotherBody = AnotherBodyNo
    val NewProfessionalBody = ProfessionalBodyNo
    val ProfessionalBodyMemberYes = ProfessionalBodyMemberNo
    val NewBusinessTypes = BusinessTypes(Set(AccountantsIreland))
  }

  val completeModel = Supervision(
    Some(DefaultValues.DefaultAnotherBody),
    Some(DefaultValues.DefaultProfessionalBodyMember),
    Some(DefaultValues.DefaultBusinessTypes),
    Some(DefaultValues.DefaultProfessionalBody),
    hasAccepted = true
  )

  val partialModelNoBusinessTypes = Supervision(
    Some(DefaultValues.DefaultAnotherBody),
    Some(DefaultValues.DefaultProfessionalBodyMember),
    None,
    Some(DefaultValues.DefaultProfessionalBody),
    hasAccepted = true)

  val partialModel = Supervision(Some(DefaultValues.DefaultAnotherBody))

  val completeJson = Json.obj(
    "anotherBody" -> Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> "Company A",
      "startDate" -> "1993-08-25",
      "endDate" -> "1999-08-25",
      "endingReason" -> "Ending reason"
    ),
    "professionalBodyMember" -> Json.obj(
      "isAMember" -> true
    ),
    "businessTypes" -> Json.obj(
      "businessType" -> Json.arr("01", "02", "14"),
      "specifyOtherBusiness" -> "test"
    ),
    "professionalBody" -> Json.obj(
      "penalised" -> true,
      "professionalBody" -> "details"
    ),
    "hasChanged" -> false,
    "hasAccepted" -> true
  )

  val completeJsonOldFormat = Json.obj(
    "anotherBody" -> Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> "Company A",
      "startDate" -> "1993-08-25",
      "endDate" -> "1999-08-25",
      "endingReason" -> "Ending reason"
    ),
    "professionalBodyMember" -> Json.obj(
      "isAMember" -> true,
      "businessType" -> Json.arr("01", "02", "14"),
      "specifyOtherBusiness" -> "test"
    ),
    "professionalBody" -> Json.obj(
      "penalised" -> true,
      "professionalBody" -> "details"
    ),
    "hasChanged" -> false,
    "hasAccepted" -> true
  )

}