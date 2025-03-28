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

package models.asp

import models.asp.Service._
import models.registrationprogress._
import org.mockito.Mockito._
import play.api.libs.json.Json
import services.cache.Cache
import utils.AmlsSpec

trait AspValues {

  object DefaultValues {

    val DefaultOtherBusinessTax = OtherBusinessTaxMattersYes

    val DefaultServices = ServicesOfBusiness(Set(Accountancy, Auditing, FinancialOrTaxAdvice))
  }

  object NewValues {

    val NewOtherBusinessTax = OtherBusinessTaxMattersNo

    val NewServices = ServicesOfBusiness(Set(Accountancy, PayrollServices, FinancialOrTaxAdvice))
  }

  val completeJson        = Json.obj(
    "services"                -> Json.obj(
      "services" -> Seq("01", "04", "05")
    ),
    "otherBusinessTaxMatters" -> Json.obj(
      "otherBusinessTaxMatters" -> true
    ),
    "hasChanged"              -> false,
    "hasAccepted"             -> true
  )
  val completeJsonWithReg = Json.obj(
    "services"                -> Json.obj(
      "services" -> Seq("01", "04", "05")
    ),
    "otherBusinessTaxMatters" -> Json.obj(
      "otherBusinessTaxMatters" -> true,
      "agentRegNo"              -> "123456789"
    ),
    "hasChanged"              -> false,
    "hasAccepted"             -> true
  )
  val completeModel       = Asp(
    Some(DefaultValues.DefaultServices),
    Some(DefaultValues.DefaultOtherBusinessTax),
    hasAccepted = true
  )

}

class AspSpec extends AmlsSpec with AspValues {

  "None" when {
    val initial: Option[Asp] = None

    "Merged with other business tax matters" must {
      "return Asp with correct other business tax matters" in {
        val result = initial.otherBusinessTaxMatters(NewValues.NewOtherBusinessTax)
        result must be(Asp(otherBusinessTaxMatters = Some(NewValues.NewOtherBusinessTax), hasChanged = true))
      }
    }

    "Merged with services" must {
      "return Asp with correct services" in {
        val result = initial.services(NewValues.NewServices)
        result must be(Asp(services = Some(NewValues.NewServices), hasChanged = true))
      }
    }
  }

  "Asp" must {
    "correctly show if the model is incomplete" in {
      val incompleteModel = completeModel.copy(otherBusinessTaxMatters = None)
      incompleteModel.isComplete must be(false)
    }

    "have a default function that" must {
      "correctly provides a default value when none is provided" in {
        Asp.default(None) must be(Asp())
      }

      "correctly provides a default value when existing value is provided" in {
        Asp.default(Some(completeModel)) must be(completeModel)
      }
    }

    "have a mongo key that" must {
      "be correctly set" in {
        Asp.mongoKey() must be("asp")
      }
    }

    "have a task row function that" must {

      implicit val cache = mock[Cache]

      "return a NotStarted Task Row when model is empty" in {

        val notStartedRow = TaskRow(
          "asp",
          controllers.asp.routes.WhatYouNeedController.get.url,
          false,
          NotStarted,
          TaskRow.notStartedTag
        )

        when(cache.getEntry[Asp]("asp")) thenReturn None

        Asp.taskRow mustBe notStartedRow
      }

      "return a Completed Task Row when model is complete" in {

        val complete = mock[Asp]

        val completedTaskRow = TaskRow(
          "asp",
          controllers.asp.routes.SummaryController.get.url,
          false,
          Completed,
          TaskRow.completedTag
        )

        when(complete.isComplete) thenReturn true
        when(complete.hasChanged) thenReturn false
        when(cache.getEntry[Asp]("asp")) thenReturn Some(complete)

        Asp.taskRow mustBe completedTaskRow
      }

      "return a Updated Task Row when model is complete" in {

        val complete = mock[Asp]

        val updatedTaskRow = TaskRow(
          "asp",
          controllers.asp.routes.SummaryController.get.url,
          true,
          Updated,
          TaskRow.updatedTag
        )

        when(complete.isComplete) thenReturn true
        when(complete.hasChanged) thenReturn true
        when(cache.getEntry[Asp]("asp")) thenReturn Some(complete)

        Asp.taskRow mustBe updatedTaskRow
      }

      "return a Started Task Row when model is incomplete" in {

        val incompleteTcsp = mock[Asp]
        val startedSection = TaskRow(
          "asp",
          controllers.asp.routes.WhatYouNeedController.get.url,
          false,
          Started,
          TaskRow.incompleteTag
        )

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Asp]("asp")) thenReturn Some(incompleteTcsp)

        Asp.taskRow mustBe startedSection
      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }
    }
  }

  "Asp Serialisation" must {

    "correctly show if the model is complete" in {
      completeModel.isComplete must be(true)
    }

    "correctly convert between json formats" when {

      "Serialise as expected" in {
        Json.toJson(completeModel) must be(completeJson)
      }

      "deserialise as expected when a reg number is present" in {
        completeJsonWithReg.as[Asp] must be(completeModel)
      }

      "Deserialise as expected" in {
        completeJson.as[Asp] must be(completeModel)
      }
    }
  }

  it when {
    "hasChanged field is missing from the Json" must {
      "Deserialise correctly" in {
        (completeJson - "hasChanged").as[Asp] must
          be(completeModel)
      }
    }
  }

  "ASP class" when {
    "services value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.services(DefaultValues.DefaultServices)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.services(ServicesOfBusiness(Set(BookKeeping)))
          res.hasChanged must be(true)
          res.services   must be(Some(ServicesOfBusiness(Set(BookKeeping))))
        }
      }
    }

    "otherBusinessTaxMatters value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.otherBusinessTaxMatters(DefaultValues.DefaultOtherBusinessTax)
          res            must be(completeModel)
          res.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.otherBusinessTaxMatters(OtherBusinessTaxMattersNo)
          res.hasChanged              must be(true)
          res.otherBusinessTaxMatters must be(Some(OtherBusinessTaxMattersNo))
        }
      }
    }
  }
}
