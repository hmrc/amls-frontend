package models.asp

import models.registrationprogress.{Completed, Started, NotStarted, Section}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

trait AspValues {

  object DefaultValues {

    val DefaultOtherBusinessTax = OtherBusinessTaxMattersYes("123456789")

    val DefaultServices = ServicesOfBusiness(Set(Accountancy, Auditing, FinancialOrTaxAdvice))
  }

  object NewValues {

    val NewOtherBusinessTax = OtherBusinessTaxMattersNo

    val NewServices = ServicesOfBusiness(Set(Accountancy, PayrollServices, FinancialOrTaxAdvice))
  }

  val completeJson = Json.obj(
    "services" -> Json.obj(
      "services" -> Seq("01", "04", "05")
    ),
    "otherBusinessTaxMatters" -> Json.obj(
      "otherBusinessTaxMatters" -> true,
      "agentRegNo" -> "123456789"
    )
  )
  val completeModel = Asp(
    Some(DefaultValues.DefaultServices),
    Some(DefaultValues.DefaultOtherBusinessTax)
  )

}

class AspSpec extends PlaySpec with MockitoSugar with AspValues {

  "Asp" must {


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

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("asp", NotStarted, controllers.asp.routes.WhatYouNeedController.get())

        when(cache.getEntry[Asp]("asp")) thenReturn None

        Asp.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Asp]
        val completedSection = Section("asp", Completed, controllers.routes.RegistrationProgressController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Asp]("asp")) thenReturn Some(complete)

        Asp.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Asp]
        val startedSection = Section("asp", Started, controllers.asp.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Asp]("asp")) thenReturn Some(incompleteTcsp)

        Asp.section must be(startedSection)

      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }
    }

    "Complete Model" when {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }

      "correctly convert between json formats" when {

        "Serialise as expected" in {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" in {
          completeJson.as[Asp] must be(completeModel)
        }
      }
    }

    "None" when {

      val initial: Option[Asp] = None

      "correctly show if the model is incomplete" in {

        val incompleteModel = completeModel.copy(otherBusinessTaxMatters = None)
        incompleteModel.isComplete must be(false)
      }

      "Merged with other business tax matters" must {
        "return Asp with correct other business tax matters" in {

          val result = initial.otherBusinessTaxMatters(NewValues.NewOtherBusinessTax)
          result must be(Asp(otherBusinessTaxMatters = Some(NewValues.NewOtherBusinessTax)))

        }

        "Merged with services does your business provide" must {
          "return Asp with correct services does your business provide" in {

            val result = initial.services(NewValues.NewServices)
            result must be(Asp(services = Some(NewValues.NewServices)))

          }
        }

      }
    }

    "Asp:merge with completeModel" when {

      "model is complete" when {

        "Merged with other business tax matters" must {
          "return Asp with correct Company Service Providers" in {

            val result = completeModel.otherBusinessTaxMatters(NewValues.NewOtherBusinessTax)
            result.otherBusinessTaxMatters must be(Some(NewValues.NewOtherBusinessTax))

          }
        }

        "Merged with services does your business provide" must {
          "return Asp with correct services does your business provide" in {

            val result = completeModel.services(NewValues.NewServices)
            result.services must be(Some(NewValues.NewServices))

          }
        }
      }
    }

  }

}
