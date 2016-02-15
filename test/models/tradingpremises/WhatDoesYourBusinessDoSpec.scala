package models.tradingpremises

import models.businessmatching.{BillPaymentServices, MoneyServiceBusiness, EstateAgentBusinessService, AccountancyServices}
import org.scalatest.{Pending, MustMatchers, WordSpec}
import play.api.data.mapping.Success

class WhatDoesYourBusinessDoSpec extends WordSpec with MustMatchers{
  "WhatDoesYourBusinessDo" must {

    val model = WhatDoesYourBusinessDo(
                  Set(
                    BillPaymentServices,
                    EstateAgentBusinessService,
                    MoneyServiceBusiness))

    "convert form data correctly" in {
      val formData = Map("activities[]" -> Seq("02", "03", "05"))
      WhatDoesYourBusinessDo.formRule.validate(formData) must be (Success(model))
    }

    "write correctly to a form" in {
      val formData = Map("activities" -> Seq("02", "03", "05"))
      WhatDoesYourBusinessDo.formWrite.writes(model) must be (formData)
    }
  }
}
