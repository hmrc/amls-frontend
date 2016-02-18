package models.tradingpremises

import models.businessmatching.{BillPaymentServices, MoneyServiceBusiness, EstateAgentBusinessService, AccountancyServices}
import org.scalatest.{Pending, MustMatchers, WordSpec}
import play.api.data.mapping.{Path, Success, Failure}
import play.api.data.validation.ValidationError

class WhatDoesYourBusinessDoSpec extends WordSpec with MustMatchers{
  val model = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness))

  "WhatDoesYourBusinessDo" when {
    "input data is valid" must {
      "convert form data correctly" in {
        val formData = Map("activities[]" -> Seq("02", "03", "05"))
        WhatDoesYourBusinessDo.formRule.validate(formData) must be (Success(model))
      }
    }

    "no items have been selected" must {
      "reject with a required message" in {
        val formData = Map("activities[]" -> Seq())
        WhatDoesYourBusinessDo.formRule.validate(formData) must be (Failure(List((Path \ "activities") -> List(ValidationError("error.required")))))
      }
    }
  }

  it must {
    "write correctly to a form" in {
      val formData = Map("activities" -> Seq("02", "03", "05"))
      WhatDoesYourBusinessDo.formWrite.writes(model) must be (formData)
    }
  }
}
