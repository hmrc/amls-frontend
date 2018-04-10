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

package controllers.businessmatching.updateservice.add

import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities._
import models.businessmatching.{AccountancyServices, HighValueDealing}
import org.scalatest.MustMatchers
import services.TradingPremisesService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._

class UpdateServicesSummaryControllerHelperSpec extends GenericTestHelper with MustMatchers with BusinessActivitiesGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val tradingPremisesService = mock[TradingPremisesService]

    val helper = new UpdateServicesSummaryControllerHelper(
      self.authConnector,
      mockCacheConnector,
      tradingPremisesService
    )

    val businessActivitiesSection = BusinessActivities(
        involvedInOther = Some(InvolvedInOtherNo),
        whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
        accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
        taxMatters = Some(TaxMatters(true)),
        hasAccepted = true
      )
  }

  "updateBusinessActivities" must {
    "remove the accountancy data from the 'business activities' section" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val result = await(helper.updateBusinessActivities(AccountancyServices))

      result.involvedInOther mustBe Some(InvolvedInOtherNo)
      result.whoIsYourAccountant must not be defined
      result.accountantForAMLSRegulations must not be defined
      result.taxMatters must not be defined
      result.hasAccepted mustBe true
    }

    "not touch the accountancy data if the activity is not 'accountancy services'" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val result = await(helper.updateBusinessActivities(HighValueDealing))

      result.whoIsYourAccountant mustBe defined
      result.accountantForAMLSRegulations mustBe Some(AccountantForAMLSRegulations(true))
      result.taxMatters mustBe Some(TaxMatters(true))
      result.hasAccepted mustBe true
    }
  }
}
