package models.tradingpremises

import models.businessmatching.{BillPaymentServices, MoneyServiceBusiness, EstateAgentBusinessService, AccountancyServices}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.mapping.Success

class WhatDoesYourBusinessDoSpec extends WordSpec with MustMatchers{
  "WhatDoesYourBusinessDo" must {
    "convert form data correctly" in {
      val inputForm : Map[String, Seq[String]]= Map("activities[]" -> Seq("02", "05", "03"))

      WhatDoesYourBusinessDo.formRule.validate(inputForm) must be (
        Success(
          WhatDoesYourBusinessDo(
            Set(
              BillPaymentServices,
              EstateAgentBusinessService,
              MoneyServiceBusiness))))
    }
  }
}
